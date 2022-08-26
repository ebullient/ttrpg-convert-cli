package dev.ebullient.json5e.tools5e;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import dev.ebullient.json5e.qute.QuteClass;
import dev.ebullient.json5e.qute.QuteSource;

public class Json2QuteClass extends Json2QuteCommon {

    Map<String, List<String>> startingText = new HashMap<>();
    Set<String> featureNames = new HashSet<>();
    List<ClassFeature> classFeatures = new ArrayList<>();
    List<Subclass> subclasses = new ArrayList<>();
    boolean additionalFromBackground;
    String classSource;
    String subclassTitle;

    Json2QuteClass(JsonIndex index, IndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
        if (!isSidekick()) {
            findClassHitDice();
            findStartingEquipment();
            findClassProficiencies();
        }

        classSource = node.get("source").asText();
        subclassTitle = getTextOrEmpty(node, "subclassTitle");
        findSubclasses();
        findClassFeatures();
    }

    @Override
    public QuteSource build() {
        List<String> tags = new ArrayList<>();
        if (subclasses.isEmpty()) {
            tags.add("class/" + tui().slugify(getName()));
        } else {
            String root = "class/" + tui().slugify(getName()) + "/";
            for(Subclass sc : subclasses) {
                tags.add(root + tui().slugify(sc.shortName));
            }
        }

        List<String> text = new ArrayList<>();
        text.add("## Class Features");
        classFeatures.forEach(cf -> {
            maybeAddBlankLine(text);
            text.add("### " + cf.name);
            text.add("");
            text.addAll(cf.text);
        });

        subclasses.forEach(sc -> {
            maybeAddBlankLine(text);
            text.add("## " + subclassTitle + ": " + sc.name);
            if (!classSource.equals(sc.sources.primarySource())) {
                maybeAddBlankLine(text);
                text.add("_Source: " + sc.sources.getSourceText() + "_");
            }
            sc.features.forEach(scf -> {
                maybeAddBlankLine(text);
                if (!sc.name.startsWith(scf.name)) {
                    text.add("### " + scf.name);
                    text.add("");
                }
                text.addAll(scf.text);
            });
        });

        return new QuteClass(
            decoratedTypeName(getName(), getSources()),
            getSources().getSourceText(),
            startingTextIntOrDefault("hd", 0),
            buildClassProgression(),
            buildStartingEquipment(),
            buildStartMulticlassing(),
            String.join("\n", text),
            tags);
    }

    String buildClassProgression() {
        if (!node.has("classTableGroups")) {
            return null;
        }
        ArrayNode classTableGroups = node.withArray("classTableGroups");

        List<String> classProgression = new ArrayList<>();

        List<List<String>> row_levels = new ArrayList<>(21);
        for(int i = 0; i < 21; i++) {
            row_levels.add(new ArrayList<>());
        }

        Map<Object, List<ClassFeature>> levelToFeature = classFeatures.stream().collect(Collectors.groupingBy(x -> Integer.valueOf(x.level)));

        // Headings are in row 0
        row_levels.get(0).add("Level");
        row_levels.get(0).add("PB");
        row_levels.get(0).add("Features");
        // Values
        for (int level = 1; level < row_levels.size(); level++) {
            row_levels.get(level).add(levelToText("" + level).replace("-level", ""));
            row_levels.get(level).add("+" + levelToPb(level));

            List<ClassFeature> features = levelToFeature.get(level);
            if (features == null || features.isEmpty()) {
                row_levels.get(level).add("⏤");
            } else {
                row_levels.get(level).add(features.stream()
                    .map(x -> markdownLinkify(x.name))
                    .collect(Collectors.joining(", ")));
            }
        }

        classProgression.addAll(convertRowsToTable(row_levels, "Feature progression",
                List.of("- PB: Proficiency Bonus"), "feature-progression"));
        classProgression.add("");

        for(int i = 0; i < 21; i++) {
            row_levels.get(i).clear();
            if (i == 0) {
                row_levels.get(i).add("Level");
            } else {
                row_levels.get(i).add(levelToText("" + i).replace("-level", ""));
            }
        }

        classTableGroups.forEach(table -> {
            // Headings
            table.withArray("colLabels").forEach(c -> {
                String label = c.asText();
                if (label.contains("|spells|")) {
                    row_levels.get(0).add(
                            label.substring(label.indexOf(" ") + 1, label.indexOf("|")));
                } else {
                    row_levels.get(0).add(replaceText(label));
                }
            });
            // Values
            if (table.has("rows")) {
                ArrayNode rows = table.withArray("rows");
                for(int i = 0; i < rows.size(); i++) {
                    int level = i+1;
                    rows.get(i).forEach(c ->
                            row_levels.get(level).add(columnValue(c.asText())));
                }
            } else if (table.has("rowsSpellProgression")) {
                ArrayNode rows = table.withArray("rowsSpellProgression");
                for(int i = 0; i < rows.size(); i++) {
                    int level = i+1;
                    rows.get(i).forEach(c ->
                            row_levels.get(level).add(columnValue(c.asText())));
                }
            }
        });

        classProgression.addAll(convertRowsToTable(row_levels, "Class progression",
                List.of("- 1st-9th: Spell slots per level"), "class-progression"));

        return String.join("\n", classProgression);
    }

    List<String> convertRowsToTable(List<List<String>> row_levels, String title, List<String> footer, String blockid) {
        List<String> text = new ArrayList<>();
        // Convert each row to markdown columns
        row_levels.forEach(r -> text.add("| " + String.join(" | ", r)  + " |"));

        // insert a header delimiting row (copy row 0, replace everything not a "|" with a "-")
        text.add(1, text.get(0).replaceAll("[^|]", "-"));

        if (footer != null && !footer.isEmpty()) {
            maybeAddBlankLine(text);
            text.addAll(footer);
        }

        // Move everything into a callout box
        for(int i = 0; i < text.size(); i++) {
            text.set(i, "> " + text.get(i));
        }

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
            startingEquipment.add(String.format("- **Saving Throws:** %s", startingTextJoinOrDefault("saves", "none")));
        }
        startingEquipment.add(String.format("- **Armor:** %s", startingTextJoinOrDefault("armor", "none")));
        startingEquipment.add(String.format("- **Weapons:** %s", startingTextJoinOrDefault("weapons", "none")));
        startingEquipment.add(String.format("- **Tools:** %s", startingTextJoinOrDefault("tools", "none")));
        startingEquipment.add(String.format("- **Skills:** %s", startingTextJoinOrDefault("skills", "none")));

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
                startingEquipment.add(String.format("Alternatively, you may start with %s gp and choose your own equipment.", startingTextJoinOrDefault("wealth", "3d4 x 10")));
            }
        }
        return String.join("\n", startingEquipment);
    }

    String buildStartMulticlassing() {
        JsonNode multiclassing = node.get("multiclassing");
        if (multiclassing == null) {
            return null;
        }

        final List<String> startMulticlass = new ArrayList<>();
        startMulticlass.add(String.format("To multiclass as a %s, you must meet the following prerequisites:", getName()));

        maybeAddBlankLine(startMulticlass);
        JsonNode requirements = multiclassing.with("requirements");
        if (requirements.has("or")) {
            List<String> options = new ArrayList<>();
            requirements.get("or").get(0).fields().forEachRemaining(ability ->
                    options.add(String.format("%s %s",
                            asAbilityEnum(ability.getKey()), ability.getValue().asText())));
            startMulticlass.add("- " + String.join(", or ", options));
        } else {
            requirements.fields().forEachRemaining(
                    ability -> startMulticlass.add(String.format("- %s %s",
                            asAbilityEnum(ability.getKey()), ability.getValue().asText())));
        }

        JsonNode gained = multiclassing.get("proficienciesGained");
        if (gained != null) {
            maybeAddBlankLine(startMulticlass);
            startMulticlass.add("You gain the following proficiencies:");
            maybeAddBlankLine(startMulticlass);

            startMulticlass.add(String.format("- **Armor:** %s",
                    startingTextJoinOrDefault(gained, "armor", "none", s -> s.replace("shield", "shields"))));
            startMulticlass.add(String.format("- **Weapons:** %s",
                    startingTextJoinOrDefault(gained, "weapons", "none")));
            startMulticlass.add(String.format("- **Tools:** %s",
                    startingTextJoinOrDefault(gained, "tools", "none")));

            if (gained.has("skills")) {
                List<String> list = new ArrayList<>();
                int count = classSkills(gained, list, sources);
                startMulticlass.add(String.format("- **Skills:** %s",
                        skillChoices(list, count)));
            }
        }
        return String.join("\n", startMulticlass);
    }

    boolean isSidekick() {
        return getSources().getName().toLowerCase().contains("sidekick");
    }

    void findClassHitDice() {
        JsonNode hd = node.get("hd");
        if (hd != null) {
            put("hd", List.of(hd.get("faces").asText()));
        }
    }

    void findSubclasses() {
        index().classElementsMatching(IndexType.subclass, getSources().getName(), classSource).forEach(s -> {
            String scKey = index.getKey(IndexType.subclass, s);
            JsonNode resolved = index.resolveClassFeatureNode(scKey, s);
            if (resolved == null) {
                return; // e.g. excluded
            }

            Subclass sc = new Subclass();
            sc.name = s.get("name").asText();
            sc.shortName = s.get("shortName").asText();
            sc.sources = index.constructSources(IndexType.subclass, resolved);

            // Subclass features
            s.withArray("subclassFeatures").forEach(f -> {
                ClassFeature scf = lookupSubclassFeature(IndexType.subclassfeature, f.asText(), "####", sc.sources.primarySource());
                if (scf != null) {
                    sc.features.add(scf);
                }
            });

            // TODO: Subclass spellcasting?
            subclasses.add(sc);
        });
    }

    void findClassFeatures() {
        for (Iterator<JsonNode> i = node.withArray("classFeatures").elements(); i.hasNext();) {
            JsonNode f = i.next();
            if (f.isTextual()) {
                lookupFeature(IndexType.classfeature, f.asText(), "####");
            } else {
                lookupFeature(IndexType.classfeature, f.get("classFeature").asText(), "####");
            }
        }
    }

    private void lookupFeature(IndexType type, String lookup, String heading) {
        String finalKey = index.getRefKey(type, lookup);
        JsonNode featureJson = index.resolveClassFeatureNode(finalKey);
        if (featureJson == null) {
            return; // skipped or not found
        }
        classFeatures.add(new ClassFeature(finalKey, featureJson, type, heading, classSource));
    }

    private ClassFeature lookupSubclassFeature(IndexType type, String lookup, String heading, String source) {
        String finalKey = index.getRefKey(type, lookup);
        JsonNode featureJson = index.resolveClassFeatureNode(finalKey);
        if (featureJson == null) {
            return null; // skipped or not found
        }
        return new ClassFeature(finalKey, featureJson, type, heading, source);
    }

    String findStartingEquipment() {
        String wealth = null;
        JsonNode equipment = node.get("startingEquipment");
        if (equipment != null) {
            wealth = getWealth(equipment);
            put("wealth", List.of(wealth));
            put("equipment", defaultEquipment(equipment));
            additionalFromBackground = booleanOrDefault(equipment, "additionalFromBackground", true);
        }
        return wealth;
    }

    public void findClassProficiencies() {
        if (node.has("proficiency")) {
            List<String> savingThrows = new ArrayList<>();
            node.withArray("proficiency").forEach(n -> {
                savingThrows.add(asAbilityEnum(n));
            });
            put("saves", savingThrows);
        }

        JsonNode startingProf = node.get("startingProficiencies");
        if (startingProf == null) {
            tui().errorf("%s has no starting proficiencies", sources);
        } else {
            if (startingProf.has("armor")) {
                put("armor", findAndReplace(startingProf, "armor", s -> s.replace("shield", "shields")));
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
        sidekickClassFeature.withArray("entries").forEach(e -> {
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
        });
    }

    String skillChoices(Collection<String> skills, int numSkills) {
        return String.format("Choose %s from %s",
                numSkills,
                skills.stream().map(SkillOrAbility::fromTextValue)
                        .sorted(Comparator.comparingInt(Enum::ordinal))
                        .map(x -> "*" + x.value() + "*")
                        .collect(Collectors.joining(", ")));
    }

    int classSkills(JsonNode source, Collection<String> list, CompendiumSources sources) {
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

    int startingTextIntOrDefault(String field, int value) {
        List<String> text = startingText.get(field);
        if (text == null || text.isEmpty()) {
            return value;
        }
        if ( text.size() > 1) {
            throw new IllegalArgumentException("Unable to parse int from starting text field: " + text);
        }
        return Integer.parseInt(text.get(0));
    }

    String startingTextJoinOrDefault(String field, String value) {
        List<String> text = startingText.get(field);
        return text == null ? value : String.join(", ", text);
    }

    String startingTextJoinOrDefault(JsonNode source, String field, String value) {
        return startingTextJoinOrDefault(source, field, value, s -> s);
    }

    String startingTextJoinOrDefault(JsonNode source, String field, String value, Function<String, String> replacements) {
        List<String> text = findAndReplace(source, field, replacements);
        return text == null || text.isEmpty() ? value : String.join(", ", text);
    }

    String textToInt(String text) {
        switch (text) {
            case "two":
                return "2";
            case "three":
                return "3";
            case "five":
                return "5";
            default:
                tui().errorf("Unknown number of skills (%s) listed in sidekick class features (%s)", text, sources);
                return "1";
        }
    }

    class Subclass {
        String name;
        String shortName;
        CompendiumSources sources;
        List<ClassFeature> features = new ArrayList<>();
    }

    class ClassFeature {
        final String name;
        final String level;
        final boolean optional;
        final List<String> text;

        public ClassFeature(String lookup, JsonNode featureJson, IndexType type, String heading, String parentSource) {
            String level = lookup.replaceAll(".*\\|(\\d+)\\|?.*", "$1");

            CompendiumSources featureSources = new CompendiumSources(type, lookup, featureJson);
            String name = decoratedFeatureTypeName(featureSources, featureJson);
            if (!featureNames.add(name)) {
                // A class feature already uses this name. Add the level.
                name += " (Level " + level + ")";
            }
            boolean optional = type == IndexType.optionalfeature || booleanOrDefault(featureJson, "isClassFeatureVariant", false);

            List<String> text = new ArrayList<>();
            replaceElementRefs(featureJson, "entries", text, heading, featureSources.primarySource());

            if (!parentSource.equals(featureSources.primarySource())) {
                maybeAddBlankLine(text);
                text.add("_Source: " + featureSources.getSourceText() + "_");
            }

            if (isSidekick() && "1".equals(level) && name.equals("Bonus Proficiencies")) {
                sidekickProficiencies(featureJson);
            }

            this.name = name;
            this.level = level;
            this.optional = optional;
            this.text = text;
        }

        void replaceElementRefs(JsonNode featureJson, String fieldName, List<String> text, String heading, String parentSource) {
            JsonNode field = featureJson.get(fieldName);
            if ( field == null ) {
                return;
            }
            if (containsReference(field.toString())) {
                ArrayNode copy = (ArrayNode) copyNode(field);
                replaceNodes(copy, parentSource);
                appendEntryToText(text, copy, heading);
            } else {
                appendEntryToText(text, field, heading);
            }
        }

        boolean containsReference(String allEntries) {
            return allEntries.contains("refSubclassFeature")
                    || allEntries.contains("refOptionalfeature")
                    || allEntries.contains("refClassFeature");
        }

        IndexType referenceType(String referenceType) {
            switch(referenceType) {
                case "refSubclassFeature": return IndexType.subclassfeature;
                case "refOptionalfeature": return IndexType.optionalfeature;
                case "refClassFeature": return IndexType.classfeature;
                default: return null;
            }
        }

        String referenceField(String referenceType) {
            switch(referenceType) {
                case "refSubclassFeature": return "subclassFeature";
                case "refOptionalfeature": return "optionalfeature";
                case "refClassFeature": return "classFeature";
                default: return null;
            }
        }

        void replaceNodes(ArrayNode copy, String parentSource) {
            for(int i = 0; i < copy.size(); i++) {
                if (copy.get(i).isObject() && copy.get(i).has("type")) {
                    String typeField = getTextOrEmpty(copy.get(i), "type");
                    final IndexType refType = referenceType(typeField);
                    final String refField = referenceField(typeField);
                    if ( refField != null ) {
                        // replace refClassFeature with class feature entries
                        String refKey = index.getRefKey(refType, copy.get(i).get(refField).asText());
                        JsonNode refJson = index.resolveClassFeatureNode(refKey);
                        String source = refJson.get("source").asText();

                        ObjectNode replace = (ObjectNode) copyNode(copy.get(i));
                        replace.remove(refField);
                        replace.set("type", new TextNode("entries"));
                        replace.set("name", refJson.get("name"));
                        replace.set("entries", refJson.get("entries"));
                        if (!source.equals(parentSource)) {
                            CompendiumSources sources = index().constructSources(refType, refJson);
                            replace.set("entry", new TextNode("_Source: " + sources.getSourceText() + "_"));
                        }
                        copy.set(i, replace);
                    } else if (typeField.equals("options")) {
                        ArrayNode copyEntries = (ArrayNode) copyNode(copy.get(i).get("entries"));
                        replaceNodes(copyEntries, parentSource);
                        ObjectNode replace = (ObjectNode) copyNode(copy.get(i));
                        replace.set("entries", copyEntries);
                        copy.set(i, replace);
                    } else if (typeField.equals("entries")) {
                        // Nested entries object
                        ArrayNode copyEntries = (ArrayNode) copyNode(copy.get(i).get("entries"));
                        replaceNodes(copyEntries, parentSource);
                        ObjectNode replace = (ObjectNode) copyNode(copy.get(i));
                        replace.set("entries", copyEntries);
                        copy.set(i, replace);
                    }
                }
            }
        }
    }

    String markdownLinkify(String x) {
        return String.format("[%s](#%s)", x,
                x.replace(" ", "%20")
                    .replace(":", "")
                    .replace(".", ""));
    }

    String columnValue(Object c) {
        String x = c == null ? null : c.toString();
        if ( x == null || x.isEmpty() || "0".equals(x) ) {
            return "⏤";
        }
        return replaceText(x);
    }

}
