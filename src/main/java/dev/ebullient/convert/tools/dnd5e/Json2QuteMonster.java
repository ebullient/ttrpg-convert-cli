package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.JsonSourceCopier.MetaFields;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.dnd5e.qute.AcHp;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.SavesAndSkills;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.Spellcasting;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.Spells;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QuteMonster extends Json2QuteCommon {
    public static boolean isNpc(JsonNode source) {
        return MonsterFields.isNpc.booleanOrDefault(source,
                MonsterFields.isNamedCreature.booleanOrDefault(source,
                        false));
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
                            lgSources.getName(),
                            sources.getKey());
                }
            } else {
                tui().debugf(Msg.FILTER, "Legendary group source excluded: %s", lgKey);
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
                tui().warnf("Empty type for %s", getSources());
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
        for (MonsterType t : MonsterType.values()) {
            if (type.toLowerCase().startsWith(t.name())) {
                return t.name();
            }
        }
        switch (type.toLowerCase()) {
            case "abberation", "abberations" -> {
                return MonsterType.aberration.name();
            }
            case "creature", "creatures" -> {
                if (getName().toLowerCase().contains("zoblin")) {
                    return MonsterType.undead.name();
                }
                if (getName().toLowerCase().contains("fandom")) {
                    return MonsterType.humanoid.name();
                }
                return MonsterType.beast.name();
            }
            case "golem", "golems" -> {
                subtype = "golem";
                return MonsterType.construct.name();
            }
            default -> {
                return type;
            }
        }
    }

    public enum MonsterType {
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
        undead;

        public static MonsterType fromString(String type) {
            String compare = type.toLowerCase()
                    .replace("abberation", "aberration"); // correct typo
            for (MonsterType t : MonsterType.values()) {
                if (compare.startsWith(t.name().toLowerCase())) {
                    return t;
                }
            }
            return null;
        }

        public static String toDirectory(String type) {
            MonsterType t = fromString(type);
            if (t == null) {
                return Tui.slugify(type);
            }
            return t.name();
        }
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
                tui().warnf(Msg.UNKNOWN, "Unknown spellcasting for %s: %s", sources.getKey(), array.toPrettyString());
                return null;
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

    public static List<JsonNode> findMonsterVariants(
            Tools5eIndex index, Tools5eIndexType type,
            String key, JsonNode jsonSource) {

        if (key.contains("splugoth the returned") || key.contains("prophetess dran")) {
            MonsterFields.isNpc.setIn(jsonSource, true); // Fix.
        }

        for (JsonNode variant : MonsterFields.variant.iterateArrayFrom(jsonSource)) {
            // There is a code path that is only followed if a variant also has _version
            // but it doesn't seem like there are any examples of this in the data.
            if (MonsterFields._versions.existsIn(variant)) {
                Tui.instance().warnf(Msg.SOMEDAY, "\"Variant for %s has versions: %s", key, variant);
            }
        }

        boolean summonedCreature = MonsterFields.summonedBySpellLevel.existsIn(jsonSource);
        boolean hasVersions = MonsterFields._versions.existsIn(jsonSource);

        if (summonedCreature || hasVersions) {
            List<JsonNode> versions = new ArrayList<>();
            List<String> versionKeys = new ArrayList<>();
            boolean replacedPrimary = false;
            final String origname = SourceField.name.getTextOrEmpty(jsonSource);

            // Expand versions first
            if (hasVersions) {
                for (JsonNode vNode : MonsterFields._versions.iterateArrayFrom(jsonSource)) {
                    if (MonsterFields._abstract.existsIn(vNode) && MonsterFields._implementations.existsIn(vNode)) {
                        versions.addAll(getVersionsTemplate(vNode));
                    } else {
                        versions.add(getVersionsBasic(vNode));
                    }
                }

                // With each version...
                for (JsonNode vNode : versions) {
                    // DataUtil.generic._getVersion(...)
                    String vKey = hydrateVersion(key, jsonSource, (ObjectNode) vNode, index);
                    versionKeys.add(vKey);
                    String variantName = SourceField.name.getTextOrEmpty(vNode);
                    if (variantName.equals(origname)) {
                        replacedPrimary = true;
                    }
                }
                TtrpgValue.indexVersionKeys.setIn(jsonSource, Tui.MAPPER.valueToTree(versionKeys));
            }

            // Add original after processing versions
            if (!replacedPrimary) {
                versions.add(0, jsonSource);
            }
            return versions;
        }
        return List.of(jsonSource);
    }

    public static String hydrateVersion(String parentKey, JsonNode parentSource, ObjectNode version, Tools5eIndex index) {
        // DataUtil.generic._hydrateVersion({key}, {source}, {version})

        Tools5eIndexType type = Tools5eIndexType.monster;
        String versionKey = type.createKey(version);

        ObjectNode parentCopy = (ObjectNode) parentSource.deepCopy();
        MonsterFields._versions.removeFrom(parentCopy);
        Tools5eFields.hasToken.removeFrom(parentCopy);
        Tools5eFields.hasFluff.removeFrom(parentCopy);
        Tools5eFields.hasFluffImages.removeFrom(parentCopy);

        filterSources(Tools5eFields.additionalSources, parentCopy, SourceField.source.getTextOrNull(version));
        filterSources(Tools5eFields.otherSources, parentCopy, SourceField.source.getTextOrNull(version));

        index.copier.mergeNodes(type, parentKey, parentCopy, version);
        TtrpgValue.indexParentKey.setIn(version, parentKey);

        Tools5eSources.constructSources(versionKey, version);
        return versionKey;
    }

    private static void filterSources(JsonNodeReader field, ObjectNode parentCopy, String vesionSource) {
        if (vesionSource == null) {
            return;
        }
        JsonNode sources = field.ensureArrayIn(parentCopy);
        Iterator<JsonNode> it = sources.elements();
        while (it.hasNext()) {
            JsonNode source = it.next();
            if (vesionSource.equals(source.asText())) {
                it.remove();
            }
        }
        if (sources.isEmpty()) {
            field.removeFrom(parentCopy);
        }
    }

    public static JsonNode getVersionsBasic(JsonNode version) {
        mutExpandCopy(version);
        return version;
    }

    public static List<JsonNode> getVersionsTemplate(JsonNode version) {
        // DataUtil.generic._getVersions_template({ver})
        return MonsterFields._implementations.streamFrom(version)
                .map(impl -> {
                    JsonNode cpyTemplate = MonsterFields._abstract.copyFrom(version);
                    mutExpandCopy(cpyTemplate);

                    ObjectNode cpyImpl = impl.deepCopy();
                    JsonNode _variables = MonsterFields._variables.removeFrom(cpyImpl);
                    if (_variables != null) {
                        Tui.instance().warnf(Msg.SOMEDAY, "Replace variables in templates. Templates: %s; Variables: %s",
                                cpyImpl, _variables);
                    }
                    ((ObjectNode) cpyTemplate).setAll(cpyImpl);
                    return cpyTemplate;
                })
                .toList();
    }

    public static void mutExpandCopy(JsonNode node) {
        JsonNode _copy = Tui.MAPPER.createObjectNode();

        // Move fields from the original node to the copy node
        MetaFields._mod.moveFrom(node, _copy);

        // Make sure a preserve element exists (which it will not if the original node is empty)
        MetaFields._preserve.moveFrom(node, _copy);
        if (!MetaFields._preserve.existsIn(_copy)) {
            MetaFields._preserve.setIn(_copy, Tui.MAPPER.createObjectNode().put("*", true));
        }

        // Copy the copy node back to the original node
        MetaFields._copy.setIn(node, _copy);
    }

    enum MonsterFields implements JsonNodeReader {
        _abstract,
        _implementations,
        _variables,
        _versions,
        ac,
        alignment,
        alignmentPrefix,
        average,
        choose,
        cr,
        creatureType, // object -- alternate to monster type
        daily,
        footerEntries,
        formula,
        from,
        headerEntries,
        hp,
        isNamedCreature,
        isNpc,
        legendaryGroup,
        lower,
        original,
        save,
        senses,
        skill,
        slots,
        special,
        spellcasting,
        spells,
        summonedBySpellLevel,
        trait,
        type,
        variant,
        will,
    }
}
