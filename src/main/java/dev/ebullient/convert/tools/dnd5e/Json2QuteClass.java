package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndex.OptionalFeatureType;
import dev.ebullient.convert.tools.dnd5e.qute.QuteClass;
import dev.ebullient.convert.tools.dnd5e.qute.QuteSubclass;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QuteClass extends Json2QuteCommon {

    final static Map<String, ClassFeature> keyToClassFeature = new HashMap<>();

    final Map<String, List<String>> startingText = new HashMap<>();
    final List<ClassFeature> classFeatures = new ArrayList<>();
    final List<Subclass> subclasses = new ArrayList<>();
    boolean additionalFromBackground;
    final String classSource;
    final String subclassTitle;
    final String decoratedClassName;

    Json2QuteClass(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
        boolean pushed = parseState().push(getSources(), rootNode); // store state
        try {
            if (!isSidekick()) {
                findClassHitDice();
                findStartingEquipment();
                findClassProficiencies();
            }

            decoratedClassName = decoratedTypeName(getSources());
            classSource = jsonNode.get("source").asText();
            subclassTitle = getTextOrEmpty(rootNode, "subclassTitle");

            // class features can be text elements or object elements (classFeature field)
            findClassFeatures(Tools5eIndexType.classfeature, jsonNode.get("classFeatures"), classFeatures, "classFeature");
            findSubclasses();
        } finally {
            parseState().pop(pushed); // restore state
        }
    }

    @Override
    protected QuteClass buildQuteResource() {
        Tags tags = new Tags(getSources());

        tags.add("class", slugify(getName()));

        List<String> text = new ArrayList<>();

        text.add("## Class Features");
        for (ClassFeature cf : classFeatures) {
            cf.appendText(this, text, getSources().primarySource());
        }

        addOptionalFeatureText(rootNode, text);

        List<String> progression = new ArrayList<>();
        buildFeatureProgression(progression);
        maybeAddBlankLine(progression);
        buildClassProgression(rootNode, progression, "classTableGroups");

        appendFootnotes(text, 0);

        return new QuteClass(getSources(),
                decoratedClassName,
                getSourceText(getSources()),
                startingHitDice(),
                String.join("\n", progression),
                buildStartingEquipment(),
                buildStartMulticlassing(),
                String.join("\n", text),
                tags);
    }

    public List<QuteSubclass> buildSubclasses() {
        List<QuteSubclass> quteSc = new ArrayList<>();

        for (Subclass sc : subclasses) {
            boolean pushed = parseState().push(sc.sources);
            try {
                Tags tags = new Tags(sc.sources);
                String tag = slugify(getName()) + "/" + slugify(sc.shortName);
                tags.add("class", tag);

                if (tag.contains("cleric")) {
                    tags.add("domain", tag.substring(tag.lastIndexOf("/") + 1));
                }

                List<String> text = new ArrayList<>();

                text.add("## Class Features");
                for (ClassFeature scf : sc.classFeatures) {
                    scf.appendText(this, text, sc.sources.primarySource());
                }

                addOptionalFeatureText(sc.subclassNode, text);

                List<String> progression = new ArrayList<>();
                buildClassProgression(sc.subclassNode, progression, "subclassTableGroups");

                quteSc.add(new QuteSubclass(sc.sources,
                        sc.getName(),
                        getSourceText(sc.sources),
                        getName(), // parentClassName
                        String.format("[%s](%s.md)", decoratedClassName, slugify(decoratedClassName)),
                        sc.parentClassSource, // parentClassSource
                        subclassTitle,
                        String.join("\n", progression),
                        String.join("\n", text),
                        tags));
            } finally {
                parseState().pop(pushed);
            }
        }

        return quteSc;
    }

    void buildFeatureProgression(List<String> progression) {
        List<List<String>> row_levels = new ArrayList<>(21);
        for (int i = 0; i < 21; i++) {
            row_levels.add(new ArrayList<>());
        }

        Map<Object, List<ClassFeature>> levelToFeature = classFeatures.stream()
                .collect(Collectors.groupingBy(x -> Integer.valueOf(x.level)));

        // Headings are in row 0
        row_levels.get(0).add("Level");
        row_levels.get(0).add("PB");
        row_levels.get(0).add("Features");
        // Values
        for (int level = 1; level < row_levels.size(); level++) {
            final int featureLevel = level;
            row_levels.get(level).add(JsonSource.levelToString(level));
            row_levels.get(level).add("+" + levelToPb(level));

            List<ClassFeature> features = levelToFeature.get(level);
            if (features == null || features.isEmpty()) {
                row_levels.get(level).add("⏤");
            } else {
                row_levels.get(level).add(features.stream()
                        .map(x -> markdownLinkify(x.getName(), featureLevel))
                        .collect(Collectors.joining(", ")));
            }
        }

        progression.addAll(
                convertRowsToTable(row_levels, "Feature progression",
                        List.of("- PB: Proficiency Bonus"),
                        "feature-progression"));
        maybeAddBlankLine(progression);
    }

    void buildClassProgression(JsonNode node, List<String> progression, String field) {
        if (!node.has(field)) {
            return;
        }

        List<List<String>> row_levels = new ArrayList<>(21);
        for (int i = 0; i < 21; i++) {
            row_levels.add(new ArrayList<>());
            if (i == 0) {
                row_levels.get(i).add("Level");
            } else {
                row_levels.get(i).add(JsonSource.levelToString(i));
            }
        }

        for (JsonNode table : iterableElements(node.get(field))) {
            // Headings
            for (JsonNode c : iterableElements(table.get("colLabels"))) {
                String label = c.asText();
                if (label.contains("|spells|")) {
                    row_levels.get(0).add(
                            label.substring(label.indexOf(" ") + 1, label.indexOf("|")));
                } else {
                    row_levels.get(0).add(replaceText(label));
                }
            }

            // Values
            if (table.has("rows")) {
                ArrayNode rows = table.withArray("rows");
                for (int i = 0; i < rows.size(); i++) {
                    int level = i + 1;
                    rows.get(i).forEach(c -> row_levels.get(level).add(columnValue(c)));
                }
            } else if (table.has("rowsSpellProgression")) {
                ArrayNode rows = table.withArray("rowsSpellProgression");
                for (int i = 0; i < rows.size(); i++) {
                    int level = i + 1;
                    rows.get(i).forEach(c -> row_levels.get(level).add(columnValue(c)));
                }
            }
        }

        progression.addAll(convertRowsToTable(row_levels, "Class progression",
                List.of("- 1st-9th: Spell slots per level"), "class-progression"));
    }

    List<String> convertRowsToTable(List<List<String>> row_levels, String title, List<String> footer, String blockid) {
        List<String> text = new ArrayList<>();
        // Convert each row to markdown columns
        row_levels.forEach(r -> text.add("| " + String.join(" | ", r) + " |"));

        // insert a header delimiting row (copy row 0, replace everything not a "|" with a "-")
        text.add(1, text.get(0).replaceAll("[^|]", "-"));

        if (footer != null && !footer.isEmpty()) {
            maybeAddBlankLine(text);
            text.addAll(footer);
        }

        // Move everything into a callout box
        text.replaceAll(s -> "> " + s);

        // add start of block
        text.add(0, "> [!tldr]- " + title);
        text.add(1, "> "); // must have a blank line before table starts

        text.add("^" + blockid);
        return text;
    }

    String buildStartingEquipment() {
        List<String> startingEquipment = new ArrayList<>();
        startingEquipment.add(String.format("You are proficient with the following items%s.",
                additionalFromBackground
                        ? ", in addition to any proficiencies provided by your race or background"
                        : ""));
        maybeAddBlankLine(startingEquipment);

        if (startingText.containsKey("saves")) {
            startingEquipment.add(String.format("- **Saving Throws**: %s", startingTextJoinOrDefault("saves", "none")));
        }
        startingEquipment.add(String.format("- **Armor**: %s", startingTextJoinOrDefault("armor", "none")));
        startingEquipment.add(String.format("- **Weapons**: %s", startingTextJoinOrDefault("weapons", "none")));
        startingEquipment.add(String.format("- **Tools**: %s", startingTextJoinOrDefault("tools", "none")));
        startingEquipment.add(String.format("- **Skills**: %s", startingTextJoinOrDefault("skills", "none")));

        if (!isSidekick()) {
            maybeAddBlankLine(startingEquipment);
            startingEquipment.add(String.format("You begin play with the following equipment%s.",
                    additionalFromBackground ? ", in addition to any equipment provided by your background" : ""));
            maybeAddBlankLine(startingEquipment);
            List<String> equipment = startingText.get("equipment");
            if (equipment == null) {
                startingEquipment.add("- None");
            } else {
                startingEquipment.addAll(equipment);
            }
            String wealth = startingTextJoinOrDefault("wealth", "");
            if (!wealth.isEmpty()) {
                maybeAddBlankLine(startingEquipment);
                startingEquipment.add(String.format("Alternatively, you may start with %s gp and choose your own equipment.",
                        startingTextJoinOrDefault("wealth", "3d4 x 10")));
            }
        }
        return String.join("\n", startingEquipment);
    }

    String buildStartMulticlassing() {
        JsonNode multiclassing = rootNode.get("multiclassing");
        if (multiclassing == null) {
            return null;
        }

        final List<String> startMulticlass = new ArrayList<>();
        startMulticlass.add(String.format("To multiclass as a %s, you must meet the following prerequisites:", getName()));

        maybeAddBlankLine(startMulticlass);
        JsonNode requirements = multiclassing.get("requirements");
        if (requirements.has("or")) {
            List<String> options = new ArrayList<>();
            requirements.get("or").get(0).fields().forEachRemaining(ability -> options.add(String.format("%s %s",
                    SkillOrAbility.format(ability.getKey()), ability.getValue().asText())));
            startMulticlass.add("- " + String.join(", or ", options));
        } else {
            requirements.fields().forEachRemaining(
                    ability -> startMulticlass.add(String.format("- %s %s",
                            SkillOrAbility.format(ability.getKey()), ability.getValue().asText())));
        }

        JsonNode gained = multiclassing.get("proficienciesGained");
        if (gained != null) {
            maybeAddBlankLine(startMulticlass);
            startMulticlass.add("You gain the following proficiencies:");
            maybeAddBlankLine(startMulticlass);

            startMulticlass.add(String.format("- **Armor**: %s",
                    startingTextJoinOrDefault(gained, "armor")));
            startMulticlass.add(String.format("- **Weapons**: %s",
                    startingTextJoinOrDefault(gained, "weapons")));
            startMulticlass.add(String.format("- **Tools**: %s",
                    startingTextJoinOrDefault(gained, "tools")));

            if (gained.has("skills")) {
                List<String> list = new ArrayList<>();
                int count = classSkills(gained, list, sources);
                startMulticlass.add(String.format("- **Skills**: %s",
                        skillChoices(list, count)));
            }
        }
        return String.join("\n", startMulticlass);
    }

    boolean isSidekick() {
        return getSources().getName().toLowerCase().contains("sidekick");
    }

    void findClassHitDice() {
        JsonNode hd = rootNode.get("hd");
        if (hd != null) {
            put("hd", List.of(hd.get("faces").asText()));
        }
    }

    void findClassFeatures(Tools5eIndexType type, JsonNode arrayElement, List<ClassFeature> features, String fieldName) {
        for (JsonNode cf : iterableElements(arrayElement)) {

            ClassFeature feature = findClassFeature(this, type, cf, fieldName);
            if (feature == null) {
                continue;
            }

            features.add(feature);

            if (isSidekick() && "1".equals(feature.level) && feature.getName().equals("Bonus Proficiencies")) {
                sidekickProficiencies(feature.cfNode);
            }
        }
    }

    static ClassFeature findClassFeature(JsonSource converter, Tools5eIndexType type, JsonNode cf, String fieldName) {
        String lookup = cf.isTextual() ? cf.asText() : cf.get(fieldName).asText();

        String finalKey = type.fromRawKey(lookup);
        JsonNode featureJson = converter.index().resolveClassFeatureNode(finalKey);

        if (featureJson == null) {
            return null; // skipped or not found
        }

        ClassFeature feature = keyToClassFeature.get(finalKey);
        if (feature == null) {
            feature = new ClassFeature();
            feature.cfNode = featureJson;
            feature.cfType = type;
            feature.level = lookup.replaceAll(".*\\|(\\d+)\\|?.*", "$1");
            feature.sources = Tools5eSources.findSources(finalKey);

            keyToClassFeature.put(finalKey, feature);
        }
        return feature;
    }

    void findSubclasses() {
        Map<JsonNode, String> scNodes = new HashMap<>();
        for (JsonNode x : index().classElementsMatching(Tools5eIndexType.subclass, getSources().getName(), classSource)) {
            scNodes.put(x, classSource);
        }
        for (String aliasKey : index.getAliasesTo(getSources().getKey())) {
            int lastSegment = aliasKey.lastIndexOf('|');
            String aliasSource = aliasKey.substring(lastSegment + 1);
            for (JsonNode x : index().classElementsMatching(Tools5eIndexType.subclass, getSources().getName(), aliasSource)) {
                scNodes.put(x, aliasSource);
            }
        }

        for (JsonNode scNode : scNodes.keySet()) {
            String parentClassSource = scNodes.get(scNode);
            String scKey = Tools5eIndexType.subclass.createKey(scNode);
            JsonNode resolved = index.resolveClassFeatureNode(scKey, scNode);
            if (resolved == null) {
                continue; // e.g. excluded
            }

            Subclass sc = new Subclass();
            sc.subclassNode = resolved;
            sc.parentClassSource = parentClassSource; // e.g. PHB or DMG
            sc.parentKey = getSources().getKey();
            sc.shortName = resolved.get("shortName").asText();
            sc.sources = Tools5eSources.findSources(scKey);

            // subclass features are text elements (null field)
            findClassFeatures(Tools5eIndexType.subclassFeature, resolved.get("subclassFeatures"), sc.classFeatures, null);

            subclasses.add(sc);
        }
    }

    void addOptionalFeatureText(JsonNode entry, List<String> text) {
        JsonNode optionalFeatureProgession = entry.get("optionalfeatureProgression");
        if (optionalFeatureProgession == null) {
            return;
        }

        maybeAddBlankLine(text);
        text.add("## Optional Features");

        String relativePath = Tools5eQuteBase.getRelativePath(Tools5eIndexType.optionalFeatureTypes);
        String source = entry.get("source").asText();
        for (JsonNode ofp : iterableElements(optionalFeatureProgession)) {
            for (String featureType : Tools5eFields.featureType.getListOfStrings(ofp, tui())) {
                OptionalFeatureType oft = index.getOptionalFeatureTypes(featureType, source);

                if (oft != null) {
                    maybeAddBlankLine(text);
                    text.add("> [!example]- " + oft.title);
                    text.add(String.format("> ![%s](%s%s/%s.md#%s)",
                            oft.title,
                            index().compendiumVaultRoot(), relativePath,
                            oft.getFilename(),
                            toAnchorTag(oft.title)));
                    text.add("^list-" + slugify(oft.title));
                } else {
                    tui().errorf("Can not find OptionalFeatureType for optional feature progression %s: %s", featureType, ofp);
                }
            }
        }
    }

    void findStartingEquipment() {
        JsonNode equipment = rootNode.get("startingEquipment");
        if (equipment != null) {
            String wealth = getWealth(equipment);
            put("wealth", List.of(wealth));
            put("equipment", defaultEquipment(equipment));
            additionalFromBackground = booleanOrDefault(equipment, "additionalFromBackground", true);
        }
    }

    public void findClassProficiencies() {
        if (rootNode.has("proficiency")) {
            List<String> savingThrows = new ArrayList<>();
            rootNode.withArray("proficiency").forEach(n -> savingThrows.add(asAbilityEnum(n)));
            put("saves", savingThrows);
        }

        JsonNode startingProf = rootNode.get("startingProficiencies");
        if (startingProf == null) {
            tui().errorf("%s has no starting proficiencies", sources);
        } else {
            if (startingProf.has("armor")) {
                put("armor", findAndReplace(startingProf, "armor"));
            }
            if (startingProf.has("weapons")) {
                put("weapons", findAndReplace(startingProf, "weapons"));
            }
            if (startingProf.has("tools")) {
                put("tools", findAndReplace(startingProf, "tools"));
            }
            if (startingProf.has("skills")) {
                Set<String> set = new HashSet<>();
                int count = classSkills(startingProf, set, sources);
                put("numSkills", List.of(count + ""));

                if (count == SkillOrAbility.allSkills.size()) { // any
                    set.addAll(SkillOrAbility.allSkills);
                }
                put("skills", List.of(skillChoices(set, count)));
            }
        }
    }

    void sidekickProficiencies(JsonNode sidekickClassFeature) {
        for (JsonNode e : iterableEntries(sidekickClassFeature)) {
            String line = e.asText();
            if (line.contains("saving throw")) {
                //"The sidekick gains proficiency in one saving throw of your choice: Dexterity, Intelligence, or Charisma.",
                //"The sidekick gains proficiency in one saving throw of your choice: Wisdom, Intelligence, or Charisma.",
                //"The sidekick gains proficiency in one saving throw of your choice: Strength, Dexterity, or Constitution.",
                String text = line.replaceAll(".*in one saving throw of your choice: (.*)", "$1")
                        .replaceAll("or ", "").replace(".", "");
                put("saves", List.of(text));
            }
            if (line.contains("skills")) {
                // "In addition, the sidekick gains proficiency in five skills of your choice, and it gains proficiency with light armor. If it is a humanoid or has a simple or martial weapon in its stat block, it also gains proficiency with all simple weapons and with two tools of your choice."
                // "In addition, the sidekick gains proficiency in two skills of your choice from the following list: {@skill Arcana}, {@skill History}, {@skill Insight}, {@skill Investigation}, {@skill Medicine}, {@skill Performance}, {@skill Persuasion}, and {@skill Religion}.",
                // "In addition, the sidekick gains proficiency in two skills of your choice from the following list: {@skill Acrobatics}, {@skill Animal Handling}, {@skill Athletics}, {@skill Intimidation}, {@skill Nature}, {@skill Perception}, and {@skill Survival}.",
                String numSkills = line.replaceAll(".* proficiency in (.*) skills .*", "$1");
                int count = Integer.parseInt(textToInt(numSkills));
                put("numSkills", List.of(count + ""));

                Collection<String> skills;
                int start = line.indexOf("list:");
                if (start >= 0) {
                    int end = line.indexOf('.');
                    String text = line.substring(start + 5, end).trim()
                            .replaceAll("\\{@skill ([^}]+)}", "$1")
                            .replace(".", "")
                            .replace("and ", "");
                    skills = Set.of(text.split("\\s*,\\s*"));
                } else {
                    skills = SkillOrAbility.allSkills;
                }
                put("skills", List.of(skillChoices(skills, count)));
            }
            if (line.contains("armor")) {
                // "In addition, the sidekick gains proficiency in five skills of your choice, and it gains proficiency with light armor. If it is a humanoid or has a simple or martial weapon in its stat block, it also gains proficiency with all simple weapons and with two tools of your choice."
                // "The sidekick gains proficiency with light armor, and if it is a humanoid or has a simple or martial weapon in its stat block, it also gains proficiency with all simple weapons."
                // "The sidekick gains proficiency with all armor, and if it is a humanoid or has a simple or martial weapon in its stat block, it gains proficiency with shields and all simple and martial weapons."
                if (line.contains("all armor")) { // Warrior Sidekick
                    put("armor", List.of("light, medium, heavy, shields"));
                    put("weapons", List.of("martial"));
                } else {
                    put("armor", List.of("light"));
                    put("weapons", List.of("simple"));
                }
            }
            if (line.contains("tools")) {
                put("tools", List.of("two tools of your choice"));
            }
        }
    }

    String skillChoices(Collection<String> skills, int numSkills) {
        return String.format("Choose %s from %s",
                numSkills,
                skills.stream().map(SkillOrAbility::fromTextValue)
                        .sorted(Comparator.comparingInt(Enum::ordinal))
                        .map(x -> "*" + x.value() + "*")
                        .collect(Collectors.joining(", ")));
    }

    int classSkills(JsonNode source, Collection<String> list, Tools5eSources sources) {
        ArrayNode skillNode = source.withArray("skills");
        if (skillNode.size() > 1) {
            tui().errorf("Multivalue skill array in %s: %s", sources, source.toPrettyString());
        }
        JsonNode skills = skillNode.get(0);
        int count = 2;

        if (skills.has("choose")) {
            count = chooseSkillListFrom(skills.get("choose"), list);
        } else if (skills.has("from")) {
            count = chooseSkillListFrom(skills, list);
        } else if (skills.has("any")) {
            count = skills.get("any").asInt();
            list.addAll(SkillOrAbility.allSkills);
        } else {
            tui().errorf("Unexpected skills in starting proficiencies for %s: %s",
                    sources, source.toPrettyString());
        }
        return count;
    }

    int chooseSkillListFrom(JsonNode choose, Collection<String> skillList) {
        int count = choose.has("count")
                ? choose.get("count").asInt()
                : 1;

        ArrayNode from = choose.withArray("from");
        from.forEach(s -> skillList.add(s.asText()));
        return count;
    }

    List<String> defaultEquipment(JsonNode equipment) {
        List<String> text = new ArrayList<>();
        appendList(text, equipment.withArray("default"));
        return text;
    }

    String getWealth(JsonNode equipment) {
        return replaceText(getTextOrEmpty(equipment, "goldAlternative"));
    }

    void put(String key, List<String> value) {
        startingText.put(key, value);
    }

    int startingHitDice() {
        List<String> text = startingText.get("hd");
        if (text == null || text.isEmpty()) {
            return 0;
        }
        if (text.size() > 1) {
            throw new IllegalArgumentException("Unable to parse int from starting text field: " + text);
        }
        return Integer.parseInt(text.get(0));
    }

    String startingTextJoinOrDefault(String field, String value) {
        List<String> text = startingText.get(field);
        return text == null ? value : String.join(", ", text);
    }

    String startingTextJoinOrDefault(JsonNode source, String field) {
        return startingTextJoinOrDefault(source, field, s -> s);
    }

    String startingTextJoinOrDefault(JsonNode source, String field, Function<String, String> replacements) {
        List<String> text = findAndReplace(source, field, replacements);
        return text == null || text.isEmpty() ? "none" : String.join(", ", text);
    }

    String textToInt(String text) {
        switch (text) {
            case "two" -> {
                return "2";
            }
            case "three" -> {
                return "3";
            }
            case "five" -> {
                return "5";
            }
            default -> {
                tui().errorf("Unknown number of skills (%s) listed in sidekick class features (%s)", text, sources);
                return "1";
            }
        }
    }

    static class ClassFeature {
        JsonNode cfNode;
        String level;

        Tools5eSources sources;
        Tools5eIndexType cfType;

        public String getName() {
            return sources.getName();
        }

        void appendText(JsonSource converter, List<String> text, String pageSource) {
            converter.maybeAddBlankLine(text);
            text.add("### " + converter.decoratedFeatureTypeName(sources, cfNode) + " (Level " + level + ")");
            if (!sources.primarySource().equalsIgnoreCase(pageSource)) {
                text.add(converter.getSourceText(sources));
            }
            text.add("");
            converter.appendToText(text, cfNode.get("entries"), "####");
        }

        public void appendListItemText(JsonSource converter, List<String> text, String pageSource) {
            text.add("**" + converter.decoratedFeatureTypeName(sources, cfNode) + "**");
            if (!sources.primarySource().equalsIgnoreCase(pageSource)) {
                text.add(converter.getSourceText(sources));
            }
            text.add("");
            converter.appendToText(text, cfNode.get("entries"), null);
            text.add("");
        }
    }

    static class Subclass {
        public String parentKey;
        JsonNode subclassNode;
        String shortName;

        Tools5eSources sources;
        String parentClassSource;

        final List<ClassFeature> classFeatures = new ArrayList<>();

        public String getName() {
            return sources.getName();
        }
    }

    String markdownLinkify(String x, int level) {
        return String.format("[%s](#%s)", x, toAnchorTag(x + " (Level " + level + ")"));
    }

    String columnValue(JsonNode c) {
        if (c.isTextual() || c.isIntegralNumber()) {
            String value = c.asText();
            if (value.isEmpty() || value.equals("0")) {
                return "⏤";
            } else {
                return replaceText(value);
            }
        } else if (c.isObject()) {
            String type = getTextOrEmpty(c, "type");
            switch (type) {
                case "dice" -> {
                    JsonNode toRoll = c.get("toRoll");
                    List<String> rolls = new ArrayList<>();
                    toRoll.forEach(f -> rolls.add(String.format("%sd%s", f.get("number").asText(), f.get("faces").asText())));
                    return String.join(", ", rolls);
                }
                case "bonus", "bonusSpeed" -> {
                    return "+" + c.get("value").asText();
                }
            }
            throw new IllegalArgumentException("Unknown column object value: " + c.toPrettyString());
        } else {
            throw new IllegalArgumentException("Unknown column value: " + c.toPrettyString());
        }
    }
}
