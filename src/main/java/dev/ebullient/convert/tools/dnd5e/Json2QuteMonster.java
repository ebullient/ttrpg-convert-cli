package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndex.Tuple;
import dev.ebullient.convert.tools.dnd5e.qute.AcHp;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.SavesAndSkills;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.Spellcasting;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.Spells;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class Json2QuteMonster extends Json2QuteCommon {
    public static boolean isNpc(JsonNode source) {
        if (source.has("isNpc")) {
            return source.get("isNpc").asBoolean(false);
        }
        if (source.has("isNamedCreature")) {
            return source.get("isNamedCreature").asBoolean(false);
        }
        return false;
    }

    String creatureType;
    String subtype;
    AcHp acHp = new AcHp();
    final boolean isNpc;

    Json2QuteMonster(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
        findCreatureType();
        findAc(acHp);
        findHp(acHp);
        isNpc = isNpc(rootNode);
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {
        String size = getSize(rootNode);
        String environment = joinAndReplace(rootNode, "environment");
        String cr = monsterCr(rootNode);
        String pb = monsterPb(cr);

        Tags tags = new Tags(getSources());
        tags.add("monster", "size", size);
        tags.add("monster", "cr", cr);
        if (subtype == null || subtype.isEmpty()) {
            tags.add("monster", "type", creatureType);
        } else {
            for (String detail : subtype.split("\\s*,\\s*")) {
                tags.add("monster", "type", creatureType, detail);
            }
        }
        if (!environment.isBlank()) {
            for (String env : environment.split("\\s*,\\s*")) {
                tags.add("monster", "environment", env);
            }
        }

        List<ImageRef> fluffImages = new ArrayList<>();
        String fluff = getFluffDescription(Tools5eIndexType.monsterFluff, "##", fluffImages);

        Collection<NamedText> legendaryGroupText = null;
        String legendaryGroupLink = null;
        if (MonsterFields.legendaryGroup.existsIn(rootNode)) {
            JsonNode nameSource = MonsterFields.legendaryGroup.getFrom(rootNode);
            String lgKey = index().getAliasOrDefault(Tools5eIndexType.legendaryGroup.createKey(nameSource));
            if (index.sourceIncluded(SourceField.source.getTextOrThrow(nameSource))) {
                JsonNode lgNode = index.getOrigin(lgKey);
                if (lgNode == null) {
                    tui().debugf("No legendary group content for %s", lgKey);
                } else {
                    Tools5eSources lgSources = Tools5eSources.findSources(lgKey);
                    legendaryGroupText = legendaryGroup(lgNode);
                    legendaryGroupLink = linkifyType(Tools5eIndexType.legendaryGroup,
                            lgKey,
                            lgSources.getName());
                }
            } else {
                tui().debugf("Legendary group %s source excluded", lgKey);
            }
        }

        return new QuteMonster(sources,
                type.decoratedName(rootNode),
                getSourceText(sources),
                isNpc,
                size, creatureType, subtype, monsterAlignment(),
                acHp,
                speed(Tools5eFields.speed.getFrom(rootNode)),
                abilityScores(),
                monsterSavesAndSkills(),
                joinAndReplace(rootNode, "senses"),
                intOrDefault(rootNode, "passive", 10),
                immuneResist(),
                joinAndReplace(rootNode, "languages"),
                cr, pb,
                collectTraits("trait"),
                collectTraits("action"),
                collectTraits("bonus"),
                collectTraits("reaction"),
                collectTraits("legendary"),
                legendaryGroupText,
                legendaryGroupLink,
                monsterSpellcasting(),
                fluff,
                environment,
                getToken(),
                fluffImages,
                tags);
    }

    void findCreatureType() {
        JsonNode typeNode = type == Tools5eIndexType.monster
                ? SourceField.type.getFrom(rootNode)
                : MonsterFields.creatureType.getFrom(rootNode);
        if (typeNode == null) {
            if (type == Tools5eIndexType.monster) {
                tui().warn("Empty type for " + getSources());
            }
            return;
        }
        if (typeNode.isTextual()) {
            creatureType = mapType(typeNode.asText());
            return;
        }

        // We have an object: type + tags
        creatureType = mapType(SourceField.type.getTextOrEmpty(typeNode));
        List<String> tags = new ArrayList<>();
        typeNode.withArray("tags").forEach(tag -> {
            if (tag.isTextual()) {
                tags.add(tag.asText());
            } else {
                tags.add(String.format("%s %s",
                        tag.get("prefix").asText(),
                        tag.get("tag").asText()));
            }
        });
        if (!tags.isEmpty()) {
            subtype = String.join(", ", tags);
        }
    }

    // Aliases for consistency. Hard-coded for common types/corrections. Won't be fool-proof
    String mapType(String type) {
        for (MonsterTypes t : MonsterTypes.values()) {
            if (type.toLowerCase().startsWith(t.name())) {
                return t.name();
            }
        }
        switch (type.toLowerCase()) {
            case "abberation", "abberations" -> {
                return MonsterTypes.aberration.name();
            }
            case "creature", "creatures" -> {
                if (getName().toLowerCase().contains("zoblin")) {
                    return MonsterTypes.undead.name();
                }
                if (getName().toLowerCase().contains("fandom")) {
                    return MonsterTypes.humanoid.name();
                }
                return MonsterTypes.beast.name();
            }
            case "golem", "golems" -> {
                subtype = "golem";
                return MonsterTypes.construct.name();
            }
            default -> {
                return type;
            }
        }
    }

    enum MonsterTypes {
        aberration,
        beast,
        celestial,
        construct,
        dragon,
        elemental,
        fey,
        fiend,
        giant,
        humanoid,
        monstrosity,
        ooze,
        plant,
        undead
    }

    String monsterPb(String cr) {
        if (cr != null) {
            return "+" + crToPb(cr);
        }
        return "+2";
    }

    SavesAndSkills monsterSavesAndSkills() {
        SavesAndSkills savesSkills = new SavesAndSkills();
        savesSkills.saveMap = new HashMap<>();
        savesSkills.saves = getModifiers("save", savesSkills.saveMap);
        savesSkills.skillMap = new HashMap<>();
        savesSkills.skills = getModifiers("skill", savesSkills.skillMap);
        return savesSkills;
    }

    String getModifiers(String field, Map<String, String> values) {
        if (!rootNode.has(field)) {
            return null;
        }
        List<String> text = new ArrayList<>();
        StringBuilder separator = new StringBuilder();
        rootNode.get(field).fields().forEachRemaining(f -> {
            if (f.getKey().equals("other")) {
                f.getValue().forEach(e -> {
                    if (e.has("oneOf")) {
                        List<String> nested = new ArrayList<>();
                        e.get("oneOf").fields()
                                .forEachRemaining(x -> nested.add(getModifier(x.getKey(), x.getValue(), values)));
                        text.add("_One of_ " + String.join(", ", nested));
                        if (separator.length() == 0) {
                            separator.append("; ");
                        }
                    } else {
                        tui().errorf("What is this (from %s): %s", sources.getKey());
                    }
                });
            } else {
                text.add(getModifier(f.getKey(), f.getValue(), values));
            }
        });
        if (separator.length() == 0) {
            separator.append(", ");
        }
        return String.join(separator.toString(), text);
    }

    String getModifier(String key, JsonNode value, Map<String, String> values) {
        String ability = SkillOrAbility.format(key, index(), getSources());
        String modifier = replaceText(value.asText());
        values.put(ability, modifier);

        return String.format("%s %s%s", ability,
                value.isInt() && value.asInt() > 0 ? "+" : "", modifier);
    }

    String monsterAlignment() {
        ArrayNode a1 = rootNode.withArray("alignment");
        if (a1.size() == 0) {
            return "Unaligned";
        }
        if (a1.size() == 1 && a1.get(0).has("special")) {
            return a1.get(0).get("special").asText();
        }

        String prefix = MonsterFields.alignmentPrefix.getTextOrDefault(rootNode, "");
        prefix = (prefix.isEmpty() ? "" : prefix + " ");

        String choices = a1.toString();
        if (choices.contains("note")) {
            List<String> notes = new ArrayList<>(List.of(choices.split("},\\{")));
            for (int i = 0; i < notes.size(); i++) {
                int pos = notes.get(i).indexOf("note");
                String alignment = mapAlignmentToString(toAlignmentCharacters(notes.get(i).substring(0, pos)));
                String note = notes.get(i).substring(pos + 4).replaceAll("[^A-Za-z ]+", "");
                notes.set(i, String.format("%s (%s)", alignment, note));
            }
            return prefix + String.join(", ", notes);
        } else {
            choices = toAlignmentCharacters(choices);
            return prefix + mapAlignmentToString(choices);
        }
    }

    List<Spellcasting> monsterSpellcasting() {
        boolean pushed = parseState().pushTrait();
        try {
            JsonNode array = MonsterFields.spellcasting.readArrayFrom(rootNode);
            if (array == null || array.isNull()) {
                return null;
            } else if (array.isObject()) {
                tui().errorf("Unknown spellcasting for %s: %s", sources.getKey(), array.toPrettyString());
                throw new IllegalArgumentException("Unknown spellcasting: " + getSources());
            }

            List<Spellcasting> casting = new ArrayList<>();
            for (JsonNode scNode : iterableElements(array)) {
                Spellcasting spellcasting = new Spellcasting();
                spellcasting.name = SourceField.name.replaceTextFrom(scNode, this);

                spellcasting.headerEntries = new ArrayList<>();
                appendToText(spellcasting.headerEntries,
                        MonsterFields.headerEntries.getFrom(scNode), null);

                spellcasting.footerEntries = new ArrayList<>();
                appendToText(spellcasting.footerEntries,
                        MonsterFields.footerEntries.getFrom(scNode), null);

                if (MonsterFields.will.existsIn(scNode)) {
                    spellcasting.will = getSpells(MonsterFields.will.getFrom(scNode));
                }
                if (MonsterFields.daily.existsIn(scNode)) {
                    spellcasting.daily = new TreeMap<>();
                    for (Entry<String, JsonNode> f : iterableFields(MonsterFields.daily.getFrom(scNode))) {
                        spellcasting.daily.put(f.getKey(), getSpells(f.getValue()));
                    }
                }
                if (MonsterFields.spells.existsIn(scNode)) {
                    spellcasting.spells = new TreeMap<>();
                    for (Entry<String, JsonNode> f : iterableFields(MonsterFields.spells.getFrom(scNode))) {
                        JsonNode spellNode = f.getValue();
                        Spells spells = new Spells();
                        if (spellNode.isArray()) {
                            spells.spells = getSpells(spellNode);
                        } else {
                            spells.slots = MonsterFields.slots.intOrDefault(spellNode, 0);
                            spells.lowerBound = MonsterFields.lower.intOrDefault(spellNode, 0);
                            spells.spells = getSpells(MonsterFields.spells.getFrom(spellNode));
                        }
                        spellcasting.spells.put(f.getKey(), spells);
                    }
                }
                parseState().popCitations(spellcasting.footerEntries);
                casting.add(spellcasting);
            }
            return casting;
        } finally {
            parseState().pop(pushed);
        }
    }

    List<String> getSpells(JsonNode source) {
        if (source == null) {
            tui().errorf("Null spells from %s", sources.getKey());
            return List.of();
        }
        List<String> spells = new ArrayList<>();
        source.forEach(s -> spells.add(replaceText(s.asText())));
        return spells;
    }

    Collection<NamedText> legendaryGroup(JsonNode content) {
        List<NamedText> traits = new ArrayList<>();
        for (Entry<String, JsonNode> field : iterableFields(content)) {
            String fieldName = field.getKey();
            if (Json2QuteLegendaryGroup.LEGENDARY_IGNORE_LIST.contains(fieldName)) {
                continue;
            }
            fieldName = fieldName.substring(0, 1).toUpperCase()
                    + Json2QuteLegendaryGroup.UPPERCASE_LETTER.matcher(fieldName.substring(1))
                            .replaceAll(matchResult -> " " + (matchResult.group(1).toLowerCase()));

            addNamedTrait(traits, fieldName, field.getValue());
        }
        return traits;
    }

    @Override
    public String getImagePath() {
        if (type != Tools5eIndexType.monster) {
            return super.getImagePath();
        }
        return Tools5eQuteBase.monsterPath(isNpc, creatureType);
    }

    public static List<Tuple> findMonsterVariants(Tools5eIndex index, Tools5eIndexType type,
            String key, JsonNode jsonSource) {
        if (MonsterFields.summonedBySpellLevel.existsIn(jsonSource)) {
            return findConjuredMonsterVariants(index, type, key, jsonSource);
        } else if (MonsterFields.type.existsIn(jsonSource)
                && MonsterFields.type.existsIn(MonsterFields.type.getFrom(jsonSource))
                && MonsterFields.type.getFrom(MonsterFields.type.getFrom(jsonSource)).has("choose")) {

            List<Tuple> variants = new ArrayList<>();

            // Produce a variant for each type
            // "type": {
            //   "type": {
            //     "choose": [
            //       "celestial",
            //       "fiend"
            //     ]
            //   }
            // },

            JsonNode choose = MonsterFields.choose.getFrom(
                    MonsterFields.type.getFrom(
                            MonsterFields.type.getFrom(jsonSource)));

            String name = SourceField.name.getTextOrEmpty(jsonSource);
            for (JsonNode typeChoice : index.iterableElements(choose)) {
                String variantName = String.format("%s (%s)", name, typeChoice.asText());
                ObjectNode adjustedSource = (ObjectNode) index.copyNode(jsonSource);

                adjustedSource.set("original", adjustedSource.get("name"));
                adjustedSource.set("originalType", adjustedSource.get("type"));
                adjustedSource.put("name", variantName);
                adjustedSource.set("type", typeChoice);

                String newKey = type.createKey(adjustedSource);
                variants.add(new Tuple(newKey, adjustedSource));
            }
            return variants;
        }
        return List.of(new Tuple(key, jsonSource));
    }

    public static List<Tuple> findConjuredMonsterVariants(Tools5eIndex index, Tools5eIndexType type,
            String key, JsonNode jsonSource) {
        final Pattern variantPattern = Pattern.compile("(\\d+) \\((.*?)\\)");
        int startLevel = MonsterFields.summonedBySpellLevel.intOrDefault(jsonSource, 0);

        String name = SourceField.name.getTextOrEmpty(jsonSource);
        String hpString = jsonSource.get("hp").get("special").asText();
        String acString = jsonSource.get("ac").get(0).get("special").asText();

        List<Tuple> variants = new ArrayList<>();
        for (int i = startLevel; i < 10; i++) {
            if (hpString.contains(" or ")) {
                // "50 (Demon only) or 40 (Devil only) or 60 (Yugoloth only) + 15 for each spell
                // level above 6th"
                // "30 (Ghostly and Putrid only) or 20 (Skeletal only) + 10 for each spell level
                // above 3rd"
                String[] parts = hpString.split(" \\+ ");
                String[] variantGroups = parts[0].split(" or ");
                for (String group : variantGroups) {
                    Matcher m = variantPattern.matcher(group);
                    if (m.find()) {
                        String amount = m.group(1);
                        String variant = m.group(2);

                        if (variant.contains(" and ")) {
                            for (String v : variant.split(" and ")) {
                                String variantName = String.format("%s (%s, %s-Level Spell)",
                                        name, v.replace(" only", ""), JsonSource.levelToString(i));
                                createVariant(index, variants, jsonSource, type, variantName, i,
                                        amount + " + " + parts[1], acString);
                            }
                        } else {
                            String variantName = String.format("%s (%s, %s-Level Spell)",
                                    name, variant.replace(" only", ""), JsonSource.levelToString(i));
                            createVariant(index, variants, jsonSource, type, variantName, i,
                                    amount + " + " + parts[1], acString);
                        }
                    } else {
                        index.tui().errorf("Unknown HP variant from %s: %s", key, hpString);
                    }
                }
            } else {
                String variantName = String.format("%s (%s-level Spell)", name, JsonSource.levelToString(i));
                createVariant(index, variants, jsonSource, type, variantName, i, hpString, acString);
            }
        }
        return variants;
    }

    static void createVariant(Tools5eIndex index, List<Tuple> variants,
            JsonNode jsonSource, Tools5eIndexType type,
            String variantName, int level, String hpString, String acString) {

        ConjuredMonster fixed = new ConjuredMonster(level, variantName, hpString, acString, jsonSource);

        ObjectNode adjustedSource = (ObjectNode) index.copyNode(jsonSource);
        adjustedSource.set("original", adjustedSource.get("name"));
        adjustedSource.replace("name", fixed.getName());
        adjustedSource.replace("ac", fixed.getAc());
        adjustedSource.replace("hp", fixed.getHp());

        String newKey = type.createKey(adjustedSource);
        variants.add(new Tuple(newKey, adjustedSource));
    }

    public static class ConjuredMonster {
        final String name;
        final MonsterAC monsterAc;
        final MonsterHp monsterHp;

        public ConjuredMonster(int level, String name, String hpString, String acString, JsonNode jsonSource) {
            this.name = name;
            this.monsterAc = new MonsterAC(level, acString);
            this.monsterHp = new MonsterHp(level, hpString, jsonSource);
        }

        public JsonNode getName() {
            return new TextNode(name);
        }

        public JsonNode getAc() {
            MonsterAC[] result = new MonsterAC[] { monsterAc };
            return Tui.MAPPER.valueToTree(result);
        }

        public JsonNode getHp() {
            return Tui.MAPPER.valueToTree(monsterHp);
        }
    }

    @RegisterForReflection
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class MonsterAC {
        public final int ac;
        public final String[] from;
        public final String original;

        // "ac": [
        // {
        // "ac": 19,
        // "from": [
        // "natural armor"
        // ]
        // }
        // ],
        public MonsterAC(int level, String acString) {
            this.original = acString;
            if (acString.contains(" + ")) {
                String[] parts = acString.split(" \\+ ");
                int value = Integer.parseInt(parts[0]);
                String armor = null;
                // "11 + the level of the spell (natural armor)"
                // "13 + PB (natural armor)"
                if (parts[1].contains("the level of the spell")) {
                    value += level;
                    armor = parts[1]
                            .replace("the level of the spell", "")
                            .replace("(", "")
                            .replace(")", "")
                            .trim();
                } else if (parts[1].contains("PB")) {
                    armor = parts[1]
                            .replace("PB", "")
                            .replace("(", "")
                            .replace(")", "")
                            .trim()
                            + " + caster proficiency bonus";
                }
                this.ac = value;
                this.from = armor == null ? null : new String[] { armor };
            } else {
                throw new IllegalArgumentException("Unknown AC pattern: " + acString);
            }
        }
    }

    @RegisterForReflection
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class MonsterHp {
        static final Pattern hpPattern = Pattern
                .compile("(\\d+) \\+ (\\d+) for each spell level above (\\d)(nd|rd|th|st) ?\\(?(.*?)?\\)?");

        public final String special;
        public final String original;

        // Want to go from:
        // "40 + 10 for each spell level above 4th"
        // "50 + 10 for each spell level above 5th (the dragon has a number of Hit Dice
        // [d10s] equal to the level of the spell)"
        // TO:
        // "hp": {
        // "average": 195,
        // "formula": "17d12 + 85"
        // },
        // OR
        // "hp": {
        // "special": "195",
        // },
        public MonsterHp(int level, String hpString, JsonNode jsonSource) {
            this.original = hpString;

            Integer value = null;
            if (hpString.contains("Constitution modifier")) {
                // "equal the undead's Constitution modifier + your spellcasting ability
                // modifier + ten times the spell's level"
                int con = jsonSource.get("con").asInt();
                value = con + (10 * level);
            } else if (hpString.contains("half the hit point maximum of its summoner")) {
                // nothing we can do
            } else if (hpString.contains(" + ")) {
                Matcher m = hpPattern.matcher(hpString);
                if (m.find()) {
                    value = Integer.parseInt(m.group(1));
                    int scale = level - Integer.parseInt(m.group(3));
                    value += Integer.parseInt(m.group(2)) * scale;
                } else {
                    throw new IllegalArgumentException("Unknown HP pattern: " + hpString);
                }
            } else {
                throw new IllegalArgumentException("Unknown HP pattern: " + hpString);
            }

            this.special = value == null ? "" : value + "";
        }
    }

    enum MonsterFields implements JsonNodeReader {
        ac,
        alignment,
        alignmentPrefix,
        average,
        cr,
        creatureType, // object -- alternate to monster type
        daily,
        footerEntries,
        formula,
        from,
        headerEntries,
        hp,
        isNamedCreature,
        legendaryGroup,
        lower,
        original,
        save,
        senses,
        shortName,
        skill,
        slots,
        special,
        spellcasting,
        spells,
        summonedBySpellLevel,
        type,
        will,
        choose,
    }
}
