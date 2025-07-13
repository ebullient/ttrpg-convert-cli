package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.joinConjunct;
import static dev.ebullient.convert.StringUtil.markdownLinkToHtml;
import static dev.ebullient.convert.StringUtil.toAnchorTag;
import static dev.ebullient.convert.StringUtil.toOrdinal;
import static dev.ebullient.convert.StringUtil.toTitleCase;
import static dev.ebullient.convert.StringUtil.uppercaseFirst;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.dnd5e.OptionalFeatureIndex.OptionalFeatureType;
import dev.ebullient.convert.tools.dnd5e.qute.QuteClass;
import dev.ebullient.convert.tools.dnd5e.qute.QuteClass.HitPointDie;
import dev.ebullient.convert.tools.dnd5e.qute.QuteClass.Multiclassing;
import dev.ebullient.convert.tools.dnd5e.qute.QuteClass.StartingEquipment;
import dev.ebullient.convert.tools.dnd5e.qute.QuteSubclass;

public class Json2QuteClass extends Json2QuteCommon {
    final static Pattern footnotePattern = Pattern.compile("\\^\\[([^\\]]+)\\]");

    final static Map<String, ClassFeature> keyToClassFeature = new HashMap<>();

    final Map<String, List<String>> startingText = new HashMap<>();
    final boolean isSidekick;
    final String decoratedClassName;
    final String classSource;
    final String subclassTitle;
    final String primaryAbility;

    String filename = null;
    SidekickProficiencies sidekickProficiencies = null;

    Json2QuteClass(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
        decoratedClassName = linkifier().decoratedName(type, jsonNode);
        classSource = jsonNode.get("source").asText();
        isSidekick = ClassFields.isSidekick.booleanOrDefault(jsonNode, false);
        subclassTitle = ClassFields.subclassTitle.getTextOrEmpty(jsonNode);
        primaryAbility = buildPrimaryAbility();
    }

    @Override
    public String getFileName() {
        return filename == null
                ? super.getFileName()
                : filename;
    }

    @Override
    protected QuteClass buildQuteResource() {
        // Some compensation for sidekicks. Do this first
        List<ClassFeature> features = findClassFeatures(
                Tools5eIndexType.classfeature,
                ClassFields.classFeatures.ensureArrayIn(rootNode),
                ClassFields.classFeature);

        Tags tags = new Tags(getSources());
        tags.add("class", getName());

        List<String> progression = buildProgressionTable(features, rootNode, ClassFields.classTableGroups);

        List<ImageRef> images = new ArrayList<>();
        List<String> text = getFluff(Tools5eIndexType.classFluff, "##", images);

        maybeAddBlankLine(text);
        text.add("## Class Features");
        for (ClassFeature cf : features) {
            cf.appendText(this, text, getSources().primarySource());
        }

        addOptionalFeatureText(rootNode, getSources().primarySource(), text);

        return new QuteClass(getSources(),
                decoratedClassName,
                getSourceText(getSources()),
                String.join("\n", progression),
                primaryAbility,
                buildHitDie(),
                buildStartingEquipment(),
                buildMulticlassing(),
                String.join("\n", text),
                images,
                tags);
    }

    public List<QuteSubclass> buildSubclasses() {
        List<QuteSubclass> quteSc = new ArrayList<>();

        // List of subclasses may include duplicates
        // See recovery for included subclass features that get abandoned
        // over edition crossing
        Set<String> subclassKeys = index.findSubclasses(getSources().getKey());
        for (String scKey : subclassKeys) {
            JsonNode scNode = index.getNode(scKey);
            Tools5eSources scSources = Tools5eSources.findSources(scKey);
            String scName = scSources.getName();
            String scShortName = ClassFields.shortName.getTextOrDefault(scNode, scName);

            List<ClassFeature> scFeatures = findClassFeatures(
                    Tools5eIndexType.subclassFeature,
                    ClassFields.subclassFeatures.ensureArrayIn(scNode),
                    ClassFields.subclassFeature);

            filename = linkifier().getTargetFileName(scName, scSources);
            boolean pushed = parseState().push(scSources);
            try {
                Tags tags = new Tags(scSources);
                tags.add("subclass", getName(), scShortName);

                if (getName().matches(".*[Cc]leric.*")) {
                    tags.add("domain", scShortName);
                }

                List<String> progression = buildProgressionTable(scFeatures, scNode, ClassFields.subclassTableGroups);

                List<ImageRef> images = new ArrayList<>();
                List<String> text = getFluff(scNode, Tools5eIndexType.subclassFluff, "##", images);

                // A bit hacky, but the isClassic setting won't always be present for homebrew
                // Homebrew based on the phb is considered classic
                boolean scIsClassic = scSources.isClassic()
                        || (TtrpgValue.isHomebrew.booleanOrDefault(scNode, false)
                                && Tools5eFields.classSource.getTextOrDefault(scNode, "phb").equalsIgnoreCase("phb"));

                if (scIsClassic && !getSources().isClassic()) {
                    // insert warning about mixed edition content
                    text.add(0,
                            "> This subclass is from a different game edition. You will need to do some adjustment to resolve differences.");
                    text.add(0, "> [!caution] Mixed edition content");
                }

                maybeAddBlankLine(text);
                text.add("## Class Features");
                for (ClassFeature scf : scFeatures) {
                    scf.appendText(this, text, scSources.primarySource());
                }

                addOptionalFeatureText(scNode, scSources.primarySource(), text);

                quteSc.add(new QuteSubclass(scSources,
                        scName,
                        getSourceText(scSources),
                        getName(), // parentClassName
                        String.format("[%s](./%s.md)", decoratedClassName, // peer/sibling
                                linkifier().getClassResource(getName(), getSources().primarySource())),
                        getSources().primarySource(),
                        subclassTitle,
                        String.join("\n", progression),
                        String.join("\n", text),
                        images,
                        tags));

            } finally {
                parseState().pop(pushed);
                filename = null;
            }
        }
        return quteSc;
    }

    private ClassFeature getClassFeature(String featureKey, Tools5eIndexType featureType) {
        return keyToClassFeature.computeIfAbsent(featureKey, k -> {
            JsonNode featureNode = index().getOriginNoFallback(featureKey);
            return new ClassFeature(featureType, featureKey, featureNode);
        });
    }

    List<ClassFeature> findClassFeatures(Tools5eIndexType featureType, JsonNode featureElements,
            ClassFields field) {
        List<ClassFeature> features = new ArrayList<>();
        for (JsonNode featureNode : featureElements) {

            String featureKey = featureNode.isTextual()
                    ? featureType.fromTagReference(featureNode.asText())
                    : featureType.fromTagReference(ClassFields.classFeature.getTextOrEmpty(featureNode));

            var classFeature = getClassFeature(featureKey, featureType);

            if (isSidekick && "1".equals(classFeature.level())
                    && "Bonus Proficiencies".equalsIgnoreCase(classFeature.getName())) {
                // sidekick classes don't have startingProficiencies, they have a bonus
                // proficiency feature instead.
                sidekickProficiencies = new SidekickProficiencies(classFeature.cfNode());
            }
            // Add to list of features (for content rendering later)
            features.add(classFeature);
        }
        return features;
    }

    void addOptionalFeatureText(JsonNode optFeatures, String primarySource, List<String> text) {
        JsonNode optionalFeatureProgression = ClassFields.optionalfeatureProgression.getFrom(optFeatures);
        if (optionalFeatureProgression == null) {
            return;
        }

        String relativePath = linkifier().getRelativePath(Tools5eIndexType.optionalFeatureTypes);

        maybeAddBlankLine(text);
        text.add("## Optional Features");
        for (JsonNode ofp : iterableElements(optionalFeatureProgression)) {
            for (String featureType : Tools5eFields.featureType.getListOfStrings(ofp, tui())) {
                OptionalFeatureType oft = index.getOptionalFeatureType(featureType);
                if (oft != null) {
                    maybeAddBlankLine(text);
                    String title = oft.getTitle(); // this could be long if homebrew mixed
                    text.add("> [!example]- Optional Features: " + title);
                    text.add(String.format("> ![%s](%s%s/%s.md#%s)",
                            title,
                            index().compendiumVaultRoot(), relativePath,
                            oft.getFilename(),
                            toAnchorTag(title)));
                    text.add("^list-optfeature-" + slugify(oft.abbreviation));
                } else {
                    tui().errorf(
                            Msg.UNRESOLVED, "Can not find optional feature type %s for progression. Source: %s; Reference: %s",
                            featureType, ofp, parseState().getSource());
                }
            }
        }
    }

    String buildPrimaryAbility() {
        // primary ability only exists in 2024 class versions
        if (!ClassFields.primaryAbility.existsIn(rootNode)) {
            return null;
        }

        // Array of objects with multiple properties
        List<String> abilities = ClassFields.primaryAbility.streamProps(rootNode)
                .map(e -> {
                    return streamPropsExcluding(rootNode)
                            .filter(x -> x.getValue().asBoolean())
                            .map(x -> SkillOrAbility.format(x.getKey(), index(), getSources()))
                            .toList();
                })
                .map(l -> joinConjunct(" and ", l))
                .toList();
        return joinConjunct(" or ", abilities);
    }

    HitPointDie buildHitDie() {
        if (isSidekick) {
            // Sidekicks do not have a hit die. Hit points depend on creature statblock
            return new HitPointDie(getName(), 0, 0, this.sources.isClassic(), isSidekick);
        }
        JsonNode hdNode = ClassFields.hd.getFrom(rootNode);
        if (hdNode != null) {
            // both attributes are required. Default should not be necessary
            return new HitPointDie(
                    getName(),
                    ClassFields.number.intOrDefault(hdNode, 1),
                    ClassFields.faces.intOrDefault(hdNode, 1),
                    this.sources.isClassic(),
                    isSidekick);
        }
        return null;
    }

    StartingEquipment buildStartingEquipment() {
        if (isSidekick) {
            if (sidekickProficiencies == null) {
                tui().warnf(Msg.UNKNOWN, "Sidekick class %s has no proficiencies", getName());
                return null;
            }
            return new StartingEquipment(
                    sidekickProficiencies.savingThrows(),
                    sidekickProficiencies.skills(),
                    sidekickProficiencies.weapons(),
                    sidekickProficiencies.tools(),
                    sidekickProficiencies.armor(),
                    "",
                    sources.isClassic());
        }

        List<String> savingThrows = ClassFields.proficiency.streamFrom(rootNode)
                .map(n -> SkillOrAbility.format(n.asText(), index(), getSources()))
                .sorted()
                .toList();

        JsonNode startingProficiencies = ClassFields.startingProficiencies.getFrom(rootNode);

        var armor = listOfArmorProficiencies(startingProficiencies);
        var skills = listOfSkillProfiencies(startingProficiencies);
        var tools = listOfToolProfiencies(startingProficiencies);
        var weapons = listOfWeaponProfiencies(startingProficiencies);

        return new StartingEquipment(savingThrows, skills, weapons, tools, armor,
                equipmentDescription(ClassFields.startingEquipment.getFrom(rootNode)),
                sources.isClassic());
    }

    Multiclassing buildMulticlassing() {
        JsonNode multiclassing = ClassFields.multiclassing.getFrom(rootNode);
        if (multiclassing == null || multiclassing.isEmpty()) {
            return null;
        }
        // primary ablity only exists in 2024 class versions (or homebrew)
        // requirements only exist in 2014 class versions (or homebrew)
        String requirements = null;
        if (ClassFields.requirements.existsIn(multiclassing)) {
            List<String> reqContents = new ArrayList<>();

            JsonNode reqNode = ClassFields.requirements.getFrom(multiclassing);
            JsonNode orNode = ClassFields.or.getFrom(reqNode);
            if (orNode == null) {
                reqContents.add("**Ability Score Minimum:** " + abilityRequirements(reqNode, ", "));
            } else {
                reqContents.add("**Ability Score Minimum:** " + streamOf(orNode)
                        .map(n -> abilityRequirements(n, " or "))
                        .collect(Collectors.joining("; ")));
            }
            appendToText(reqContents, SourceField.entries.getFrom(reqNode), null);
            requirements = String.join("\n", reqContents);
        }

        JsonNode reqSpecialNode = ClassFields.requirementsSpecial.getFrom(multiclassing);
        String requirementsSpecial = reqSpecialNode == null
                ? null
                : replaceText(reqSpecialNode);

        JsonNode profGained = ClassFields.proficienciesGained.getFrom(multiclassing);

        List<String> skillsGained = listOfSkillProfiencies(profGained);
        List<String> weaponsGained = listOfWeaponProfiencies(profGained);
        List<String> toolsGained = listOfToolProfiencies(profGained);
        List<String> armorGained = listOfArmorProficiencies(profGained);

        return new Multiclassing(
                primaryAbility,
                requirements,
                requirementsSpecial,
                join(", ", skillsGained),
                join(", ", weaponsGained),
                join(", ", toolsGained),
                join(", ", armorGained),
                flattenToString(SourceField.entries.getFrom(multiclassing), "\n"),
                getSources().isClassic());
    }

    List<String> buildProgressionTable(List<ClassFeature> features, JsonNode sourceNode, ClassFields field) {
        List<String> headings = new ArrayList<>();
        List<String> spellCasting = new ArrayList<>();
        List<String> footnotes = new ArrayList<>();

        Map<Integer, LevelProgression> levels = new HashMap<>();
        for (int i = 1; i < 21; i++) {
            // Create LevelProgression for each level
            // this sets level and proficiency bonus for level
            levels.put(i, new LevelProgression(i));
        }

        for (ClassFeature feature : features) {
            var lp = levels.get(Integer.valueOf(feature.level()));
            lp.addFeature(feature);
        }

        for (JsonNode tableNode : field.iterateArrayFrom(sourceNode)) {
            if (ClassFields.rows.existsIn(tableNode)) {
                // headings for other/middle columns
                for (JsonNode label : ClassFields.colLabels.iterateArrayFrom(tableNode)) {
                    headings.add(stripTableMarkdown(label, footnotes));
                }

                // values for other/middle columns
                int i = 1;
                for (JsonNode row : ClassFields.rows.iterateArrayFrom(tableNode)) {
                    var lp = levels.get(Integer.valueOf(i));
                    for (JsonNode col : iterableElements(row)) {
                        lp.addValue(progressionColumnValue(col));
                    }
                    i++;
                }
            } else if (ClassFields.rowsSpellProgression.existsIn(tableNode)) {
                // headings for other/middle columns
                for (JsonNode label : ClassFields.colLabels.iterateArrayFrom(tableNode)) {
                    spellCasting.add(replaceText(label));
                }
                int i = 1;
                for (JsonNode row : ClassFields.rowsSpellProgression.iterateArrayFrom(tableNode)) {
                    var lp = levels.get(Integer.valueOf(i));
                    for (JsonNode col : iterableElements(row)) {
                        lp.addSpellSlot(progressionColumnValue(col));
                    }
                    i++;
                }
            }
        }

        return progressionAsTable(headings, spellCasting, levels, footnotes);
    }

    private String stripTableMarkdown(JsonNode label, List<String> footnotes) {
        String text = markdownLinkToHtml(replaceText(label));

        // Extract footnotes and replace with markers
        Matcher matcher = footnotePattern.matcher(text);

        return matcher.replaceAll(matchResult -> {
            String footnoteContent = matchResult.group(1);
            footnotes.add(footnoteContent);
            return " <sup>‡" + footnotes.size() + "</sup>";
        });
    }

    List<String> progressionAsTable(List<String> headings,
            List<String> spellCasting,
            Map<Integer, LevelProgression> levels,
            List<String> footnotes) {
        List<String> text = new ArrayList<>();
        text.add("[!tldr] Class and Feature Progression");
        text.add("");
        text.add("<table class=\"class-progression\">");
        text.add("<thead>");
        // Top-level heading row to group spell casting columns
        text.add("<tr><th colspan='%s'></th>%s</tr>"
                .formatted(
                        3 + headings.size(),
                        spellCasting.isEmpty()
                                ? ""
                                : "<th colspan='%s'>Spell Slots per Spell Level</th>"
                                        .formatted(spellCasting.size())));

        text.add(
                "<tr class=\"class-progression\"><th class\"level\">Level</th><th class\"pb\">PB</th><th class\"feature\">Features</th>%s%s</tr>"
                        .formatted(
                                headings.isEmpty()
                                        ? ""
                                        : "<th class=\"value\">" + join("</th><th class=\"value\">", headings)
                                                + "</th>",
                                spellCasting.isEmpty()
                                        ? ""
                                        : "<th class=\"spellSlot\">"
                                                + join("</th><th class=\"spellSlot\">", spellCasting) + "</th>"));

        text.add("</thead><tbody>");

        for (int i = 1; i < 21; i++) {
            var lp = levels.get(Integer.valueOf(i));
            text.add(
                    "<tr class=\"class-progression\"><td class\"level\">%s</td><td class\"pb\">%s</td><td class\"feature\">%s</td>%s%s</tr>"
                            .formatted(
                                    lp.level, lp.pb,
                                    join(", ", lp.features),
                                    lp.values.isEmpty()
                                            ? ""
                                            : "<td class=\"value\">" + join("</td><td class=\"value\">", lp.values)
                                                    + "</td>",
                                    spellCasting.isEmpty()
                                            ? ""
                                            : "<td class=\"spellSlot\">"
                                                    + join("</td><td class=\"spellSlot\">", lp.spellSlots) + "</td>"));
        }

        text.add("</tbody></table>");
        if (!footnotes.isEmpty()) {
            text.add("<section class=\"footnotes\"><ul>");
            for (var i = 0; i < footnotes.size(); i++) {
                text.add("<li>‡" + (i + 1) + ": " + footnotes.get(i) + "</li>");
            }
            text.add("</ul></section>");
        }

        // Move everything into a callout box
        text.replaceAll(s -> "> " + s);
        text.add("");
        text.add("^class-progression");
        return text;
    }

    String progressionColumnValue(JsonNode c) {
        if (c == null || c.isNull()) {
            return "⏤";
        }
        if (c.isObject()) {
            String type = ClassFields.type.getTextOrEmpty(c);
            return switch (type) {
                case "dice" -> {
                    List<String> rolls = new ArrayList<>();
                    for (JsonNode roll : ClassFields.toRoll.iterateArrayFrom(c)) {
                        rolls.add("%sd%s".formatted(
                                ClassFields.number.getTextOrEmpty(roll),
                                ClassFields.faces.getTextOrEmpty(roll)));
                    }
                    yield join(",", rolls);
                }
                case "bonus", "bonusSpeed" -> {
                    yield "+" + ClassFields.value.getTextOrEmpty(c);
                }
                default -> throw new IllegalArgumentException("Unknown column object value: " + c.toPrettyString());
            };
        }
        String value = c.asText();
        return value.isEmpty() || value.equals("0")
                ? "⏤"
                : replaceText(value);
    }

    String abilityRequirements(JsonNode reqNode, String joiner) {
        return streamProps(reqNode)
                .filter(n -> SkillOrAbility.fromTextValue(n.getKey()) != null)
                .map(e -> "%s %s".formatted(
                        SkillOrAbility.format(e.getKey(), index(), getSources()),
                        e.getValue().asText()))
                .sorted()
                .collect(Collectors.joining(joiner));
    }

    List<String> listOfArmorProficiencies(JsonNode containingNode) {
        return ClassFields.armor.streamFrom(containingNode)
                .map(n -> {
                    if (n.isTextual()) {
                        return armorToLink(n.asText());
                    }
                    return armorToLink(ClassFields.full.getTextOrDefault(n,
                            ClassFields.proficiency.getTextOrEmpty(n)));
                })
                .toList();
    }

    List<String> listOfSkillProfiencies(JsonNode containingNode) {
        if (isSidekick) {
            return List.of();
        }

        return ClassFields.skills.streamFrom(containingNode)
                // ARRAY of objects
                .map(n -> {
                    String choose = null;
                    List<String> baseSkills = new ArrayList<>();
                    // any: integer
                    // choose: {
                    // count: integer,
                    // from: [
                    // ...
                    // ]
                    // }
                    // skillName: true
                    for (var x : iterableFields(n)) {
                        if ("any".equals(x.getKey())) {
                            choose = skillChoices(List.of(),
                                    x.getValue().asInt());
                        } else if ("choose".equals(x.getKey())) {
                            choose = skillChoices(
                                    ClassFields.from.getListOfStrings(x.getValue(), tui()),
                                    ClassFields.count.intOrDefault(x.getValue(), 1));
                        } else {
                            SkillOrAbility skill = index.findSkillOrAbility(n.asText(), getSources());
                            if (skill != null) {
                                baseSkills.add(linkifySkill(skill));
                            }
                        }
                    }

                    String allSkills = joinConjunct(" and ", baseSkills);
                    if (baseSkills.size() > 0 && choose != null) {
                        return "%s; and %s".formatted(allSkills, choose);
                    }
                    return choose == null ? allSkills : choose;
                })
                .toList();
    }

    List<String> listOfWeaponProfiencies(JsonNode containingNode) {
        return ClassFields.weapons.streamFrom(containingNode)
                .map(n -> {
                    if (ClassFields.optional.booleanOrDefault(n, false)) {
                        return "%s (optional)".formatted(replaceText(ClassFields.proficiency.getTextOrEmpty(n)));
                    }
                    String weaponType = n.asText();
                    if (weaponType.matches("(?i)(simple|martial)")) {
                        return "%s weapons".formatted(
                                sources.isClassic() ? weaponType : toTitleCase(weaponType));
                    }
                    return replaceText(weaponType);
                })
                .toList();
    }

    List<String> listOfToolProfiencies(JsonNode containingNode) {
        return ClassFields.tools.streamFrom(containingNode)
                .map(this::replaceText)
                .toList();
    }

    String armorToLink(String armor) {
        return armor
                .replaceAll("^light", linkify(Tools5eIndexType.itemType,
                        sources.isClassic() ? "la|phb|light armor" : "la|xphb|Light armor"))
                .replaceAll("^medium", linkify(Tools5eIndexType.itemType,
                        sources.isClassic() ? "ma|phb|medium armor" : "ma|xphb|Medium armor"))
                .replaceAll("^heavy", linkify(Tools5eIndexType.itemType,
                        sources.isClassic() ? "ha|phb|heavy armor" : "ha|xphb|Heavy armor"))
                .replaceAll("^shields?", linkify(Tools5eIndexType.item,
                        sources.isClassic() ? "shield|phb|shields" : "shield|xphb|Shields"));
    }

    String skillChoices(Collection<String> skills, int numSkills) {
        if (skills.isEmpty() || skills.size() >= 18) {
            String link = "||skill%s".formatted(numSkills == 1 ? "" : "s");
            String linkToSkills = linkifyRules(Tools5eIndexType.skill, link);
            return sources.isClassic()
                    ? "choose any %s %s".formatted(numSkills, linkToSkills)
                    : "Choose %s %s".formatted(numSkills, linkToSkills);
        }

        List<String> formatted = skills.stream().map(x -> index.findSkillOrAbility(x, getSources()))
                .filter(x -> x != null)
                .sorted(SkillOrAbility.comparator)
                .map(x -> linkifySkill(x))
                .toList();
        return sources.isClassic()
                ? "choose %s from %s".formatted(numSkills,
                        joinConjunct(" and ", formatted))
                : "*Choose %s:* %s".formatted(numSkills,
                        joinConjunct(" or ", formatted));
    }

    String equipmentDescription(JsonNode startingEquipment) {
        List<String> text = new ArrayList<>();

        if (ClassFields.additionalFromBackground.existsIn(startingEquipment)
                && ClassFields.defaultEquipment.existsIn(startingEquipment)) {
            // Older default format.
            if (ClassFields.additionalFromBackground.booleanOrDefault(startingEquipment, false)) {
                text.add("You start with the following items, plus anything provided by your background.");
                text.add("");
            }

            for (JsonNode item : ClassFields.defaultEquipment.iterateArrayFrom(startingEquipment)) {
                text.add("- %s".formatted(replaceText(item)));
            }

            String goldAlternative = ClassFields.goldAlternative.getTextOrNull(startingEquipment);
            if (isPresent(goldAlternative)) {
                text.add("");
                text.add("Alternatively, you may start with %s gp to buy your own equipment."
                        .formatted(replaceText(goldAlternative)));
            }
        } else {
            JsonNode entries = SourceField.entries.getFrom(startingEquipment);
            appendToText(text, entries, null);
        }
        return String.join("\n", text);
    }

    static ClassFeature findClassFeature(JsonSource converter, Tools5eIndexType type, JsonNode cf, String fieldName) {
        String lookup = cf.isTextual() ? cf.asText() : cf.get(fieldName).asText();

        String finalKey = type.fromTagReference(lookup);
        ClassFeature feature = keyToClassFeature.get(finalKey);
        if (feature == null) {
            JsonNode cfNode = converter.index().getNode(finalKey);
            if (cfNode == null) {
                return null; // skipped or not found
            }
            feature = new ClassFeature(type, finalKey, cfNode);
            keyToClassFeature.putIfAbsent(finalKey, feature);
        }
        return feature;
    }

    static ClassFeature getClassFeature(String featureKey) {
        return keyToClassFeature.get(featureKey);
    }

    static record ClassFeature(
            Tools5eIndexType cfType,
            JsonNode cfNode,
            Tools5eSources cfSources,
            KeyData keyData) {
        public ClassFeature(Tools5eIndexType cfType, String key, JsonNode cfNode) {
            this(cfType, cfNode, Tools5eSources.findSources(key),
                    cfType == Tools5eIndexType.classfeature
                            ? new ClassFeatureKeyData(key)
                            : new SubclassFeatureKeyData(key));
        }

        protected Tools5eLinkifier linkifier() {
            return Tools5eLinkifier.instance();
        }

        public String getName() {
            return cfSources.getName();
        }

        public String level() {
            return keyData.level();
        }

        void appendLink(JsonSource converter, List<String> text, String pageSource) {
            converter.maybeAddBlankLine(text);
            String x = linkifier().decoratedFeatureTypeName(cfSources, cfNode);
            text.add(String.format("[%s](#%s)", x, toAnchorTag(x + " (Level " + level() + ")")));
        }

        public void appendListItemText(JsonSource converter, List<String> text, String pageSource) {
            boolean pushed = converter.parseState().pushFeatureType();
            try {
                text.add("**" + linkifier().decoratedFeatureTypeName(cfSources, cfNode) + "**");
                if (!cfSources.primarySource().equalsIgnoreCase(pageSource)) {
                    text.add(converter.getLabeledSource(cfSources));
                }
                text.add("");
                converter.appendToText(text, SourceField.entries.getFrom(cfNode), null);
                text.add("");
            } finally {
                converter.parseState().pop(pushed);
            }
        }

        void appendText(JsonSource converter, List<String> text, String primarySource) {
            boolean pushed = converter.parseState().pushFeatureType();
            try {
                converter.maybeAddBlankLine(text);
                text.add("### " + linkifier().decoratedFeatureTypeName(cfSources, cfNode) + " (Level " + level() + ")");
                if (!cfSources.primarySource().equalsIgnoreCase(primarySource)) {
                    text.add(converter.getLabeledSource(cfSources));
                }
                converter.maybeAddBlankLine(text);
                converter.appendToText(text, SourceField.entries.getFrom(cfNode), "####");
            } finally {
                converter.parseState().pop(pushed);
            }
        }
    }

    static interface KeyData {
        String name();

        String parentName();

        String parentSource();

        String level();

        String itemSource();
    }

    static class ClassFeatureKeyData implements KeyData {
        final String cfName;
        final String className;
        final String classSource;
        final String level;
        final String cfSource;

        public ClassFeatureKeyData(String key) {
            String[] parts = key.split("\\|");
            this.cfName = parts[1];
            this.className = parts[2];
            this.classSource = parts[3];
            this.level = parts[4];
            this.cfSource = parts[5];
        }

        @Override
        public String name() {
            return cfName;
        }

        @Override
        public String parentName() {
            return className;
        }

        @Override
        public String parentSource() {
            return classSource;
        }

        @Override
        public String level() {
            return level;
        }

        @Override
        public String itemSource() {
            return cfSource;
        }
    }

    // Unpack a subclass key
    public static class SubclassKeyData implements KeyData {
        String scName;
        String className;
        String classSource;
        String scSource;

        public SubclassKeyData(String key) {
            String[] parts = key.split("\\|");
            this.scName = parts[1];
            this.className = parts[2];
            this.classSource = parts[3];
            this.scSource = parts[4];
        }

        @Override
        public String name() {
            return scName;
        }

        @Override
        public String parentName() {
            return className;
        }

        @Override
        public String parentSource() {
            return classSource;
        }

        @Override
        public String level() {
            return "";
        }

        @Override
        public String itemSource() {
            return scSource;
        }
    }

    // Unpack a subclass feature key
    static class SubclassFeatureKeyData implements KeyData {
        String scfName;
        String className;
        String classSource;
        String scName;
        String scSource;
        String level;
        String scfSource;

        public SubclassFeatureKeyData(String key) {
            String[] parts = key.split("\\|");
            this.scfName = parts[1];
            this.className = parts[2];
            this.classSource = parts[3];
            this.scName = parts[4];
            this.scSource = parts[5];
            this.level = parts[6];
            this.scfSource = parts[7];
        }

        @Override
        public String name() {
            return scfName;
        }

        @Override
        public String parentName() {
            return scName;
        }

        @Override
        public String parentSource() {
            return scSource;
        }

        @Override
        public String level() {
            return level;
        }

        @Override
        public String itemSource() {
            return scfSource;
        }

        public String toKey() {
            return String.join("|",
                    Tools5eIndexType.subclassFeature.name(),
                    scfName,
                    className, classSource,
                    scName, scSource,
                    level, scfSource)
                    .toLowerCase();
        }

        public String toSubclassKey() {
            return String.join("|",
                    Tools5eIndexType.subclass.name(),
                    scName,
                    className, classSource,
                    scSource)
                    .toLowerCase();
        }

        public String toClassKey() {
            return String.join("|",
                    Tools5eIndexType.classtype.name(),
                    className, classSource)
                    .toLowerCase();
        }
    }

    static class LevelProgression {
        final String level;
        final String pb;
        List<String> features = new ArrayList<>();
        List<String> values = new ArrayList<>();
        List<String> spellSlots = new ArrayList<>();

        LevelProgression(int level) {
            this.level = toOrdinal(level);
            this.pb = "+" + JsonSource.levelToPb(level);
        }

        void addFeature(ClassFeature cf) {
            features.add(toHtmlLink(cf.getName(), cf.level()));
        }

        void addValue(String value) {
            values.add(value);
        }

        void addSpellSlot(String value) {
            spellSlots.add(value);
        }

        String toHtmlLink(String x, String level) {
            return String.format("<a href='#%s' class='internal-link'>%s</a>",
                    x + " (Level " + level + ")", x);
        }
    }

    // Not static. Relies on Json2QuteClass members
    class SidekickProficiencies {
        static final Pattern sidekickArmor = Pattern.compile("(?<=with ).*? armor");
        static final Pattern sidekickHumanoid = Pattern.compile("[Ii]f it is a humanoid[^.]+\\.($| )");
        static final Pattern sidekickSavingThrows = Pattern.compile("\\b[^ ]+ saving throw of your choice.*");
        static final Pattern sidekickSkills = Pattern
                .compile("\\b[^ ]+ skills of your choice(,| from the following list.*)");
        static final Pattern sidekickTools = Pattern.compile("\\b[^ ]+ tools of your choice");
        static final Pattern sidekickWeapons = Pattern.compile("(?<=(with|and) )all simple( and martial)? weapons");

        private String armor;
        private String skills;
        private String savingThrows;
        private String tools;
        private String weapons;

        SidekickProficiencies(JsonNode node) {
            String text = String.join("\n", SourceField.entries.replaceTextFromList(node, index()));
            String humanoidClause = null;

            Matcher humanoidMatcher = sidekickHumanoid.matcher(text);
            if (humanoidMatcher.find()) {
                humanoidClause = humanoidMatcher.group(0);
                // Remove the humanoid clause from the text
                text = humanoidMatcher.replaceAll("");
            }

            Matcher armorMatcher = sidekickArmor.matcher(text);
            if (armorMatcher.find()) {
                armor = uppercaseFirst(armorMatcher.group());
                if (humanoidClause.contains("shields")) {
                    armor += "; and shields if [humanoid](#%s)".formatted(toAnchorTag("Bonus Proficiencies (Level 1)"));
                }
            }

            Matcher savingThrowsMatcher = sidekickSavingThrows.matcher(text);
            if (savingThrowsMatcher.find()) {
                savingThrows = uppercaseFirst(savingThrowsMatcher.group());
            }

            Matcher skillsMatcher = sidekickSkills.matcher(text);
            if (skillsMatcher.find()) {
                skills = uppercaseFirst(skillsMatcher.group()).replaceAll(",$", "");
            }

            // Only present in the humanoid clause
            Matcher toolMatcher = sidekickTools.matcher(humanoidClause);
            if (toolMatcher.find()) {
                tools = "%s if [humanoid](#%s)".formatted(
                        uppercaseFirst(toolMatcher.group()),
                        toAnchorTag("Bonus Proficiencies (Level 1)"));
            }

            // Only present in the humanoid clause
            Matcher weaponsMatcher = sidekickWeapons.matcher(humanoidClause);
            if (weaponsMatcher.find()) {
                weapons = "%s if [humanoid](#%s)".formatted(
                        uppercaseFirst(weaponsMatcher.group()),
                        toAnchorTag("Bonus Proficiencies (Level 1)"));
            }
        }

        List<String> armor() {
            return isPresent(armor) ? List.of(armor) : List.of();
        }

        List<String> savingThrows() {
            return isPresent(savingThrows) ? List.of(savingThrows) : List.of();
        }

        List<String> skills() {
            return isPresent(skills) ? List.of(skills) : List.of();
        }

        List<String> tools() {
            return isPresent(tools) ? List.of(tools) : List.of();
        }

        List<String> weapons() {
            return isPresent(weapons) ? List.of(weapons) : List.of();
        }
    }

    enum ClassFields implements JsonNodeReader {
        additionalFromBackground,
        additionalProperties,
        any,
        armor,
        choose,
        classFeature,
        classFeatures,
        classTableGroups,
        colLabels,
        count,
        defaultEquipment("default"), // default is a reserved word
        faces,
        from,
        full,
        gainSubclassFeature,
        goldAlternative,
        hd,
        isSidekick,
        multiclassing,
        number,
        optional,
        optionalfeatureProgression,
        or,
        primaryAbility,
        proficienciesGained,
        proficiency,
        properties,
        required,
        requirements,
        requirementsSpecial,
        rows,
        rowsSpellProgression,
        shortName,
        skills,
        startingEquipment,
        startingProficiencies,
        subclassFeature,
        subclassFeatures,
        subclassShortName,
        subclassSource,
        subclassTableGroups,
        subclassTitle,
        toRoll,
        tools,
        type,
        value,
        weapons,
        ;

        final String nodeName;

        ClassFields() {
            nodeName = name();
        }

        ClassFields(String nodeName) {
            this.nodeName = nodeName;
        }

        @Override
        public String nodeName() {
            return nodeName;
        }
    }
}
