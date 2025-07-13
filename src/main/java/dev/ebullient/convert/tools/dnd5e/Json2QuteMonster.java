package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.pluralize;

import java.util.ArrayList;
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
import dev.ebullient.convert.tools.JsonTextConverter;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.dnd5e.qute.AbilityScores;
import dev.ebullient.convert.tools.dnd5e.qute.AcHp;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.HiddenType;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.Initiative;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.InitiativeMode;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.SavesAndSkills;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.SavingThrow;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.SkillModifier;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.Spellcasting;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.Spells;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.TraitDescription;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster.Traits;
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

        AbilityScores abilityScores = abilityScores(rootNode);

        return new QuteMonster(sources,
                linkifier().decoratedName(type, rootNode),
                getSourceText(sources),
                isNpc,
                size, creatureType, subtype, monsterAlignment(),
                acHp,
                speed(Tools5eFields.speed.getFrom(rootNode)),
                abilityScores,
                monsterSavesAndSkills(),
                linkedSenses(),
                intOrDefault(rootNode, "passive", 10),
                immuneResist(),
                gear(),
                joinAndReplace(rootNode, "languages"),
                cr, pb,
                initiative(abilityScores, cr),
                collectAllTraits(),
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

    String monsterPb(String cr) {
        if (cr != null) {
            return "+" + crToPb(cr);
        }
        return "+2";
    }

    SavesAndSkills monsterSavesAndSkills() {
        SavesAndSkills savesSkills = new SavesAndSkills();

        JsonNode saveNode = MonsterFields.save.getFrom(rootNode);
        if (saveNode != null) {
            savesSkills.saves = new ArrayList<>();
            for (Entry<String, JsonNode> f : iterableFields(saveNode)) {
                savesSkills.saves.add(getSavingThrow(f.getKey(), f.getValue()));
            }
        }

        JsonNode skillNode = MonsterFields.skill.getFrom(rootNode);
        if (skillNode != null) {
            savesSkills.skills = new ArrayList<>();
            for (var skill : iterableFields(skillNode)) {
                String name = skill.getKey();
                JsonNode value = skill.getValue();
                if ("other".equalsIgnoreCase(name)) {
                    savesSkills.skillChoices = new ArrayList<>();
                    for (var item : ensureArray(value)) {
                        JsonNode oneOf = MonsterFields.oneOf.getFrom(item);
                        if (oneOf == null) {
                            tui().errorf("What is this (from %s): %s", sources.getKey(), item);
                            continue;
                        }
                        List<SkillModifier> choices = new ArrayList<>();
                        for (var e : iterableFields(oneOf)) {
                            choices.add(getModifier(e.getKey(), e.getValue()));
                        }
                        savesSkills.skillChoices.add(choices);
                    }
                } else {
                    savesSkills.skills.add(getModifier(name, value));
                }
            }
        }
        return savesSkills;
    }

    SavingThrow getSavingThrow(String name, JsonNode node) {
        SkillOrAbility save = SkillOrAbility.fromTextValue(name);
        name = save == null ? name : save.value();
        String text = node.asText();
        if (text.matches("[+-]?\\d+")) {
            return new SavingThrow(name, node.asInt());
        } else {
            return new SavingThrow(name, replaceText(text));
        }
    }

    SkillModifier getModifier(String name, JsonNode value) {
        SkillOrAbility skill = SkillOrAbility.fromTextValue(name);
        name = skill == null ? name : skill.value();
        String link = skill == null ? null : linkifySkill(skill);
        String text = value.asText();
        if (text.matches("[+-]?\\d+")) {
            return new SkillModifier(name, link, value.asInt());
        } else {
            return new SkillModifier(name, link, replaceText(text));
        }
    }

    // _getInitiativeBonus
    Initiative initiative(AbilityScores abilityScores, String cr) {
        JsonNode initiative = MonsterFields.initiative.getFrom(rootNode);
        if (initiative == null) {
            // if (mon.initiative == null && (mon.dex == null || mon.dex.special)) return null;
            if (abilityScores.dexterity() == null || abilityScores.dexterity().isSpecial()) {
                return null;
            }
            // if (mon.initiative == null) return Parser.getAbilityModNumber(mon.dex);
            return new Initiative(abilityScores.dexterity().modifier());
        }
        // if (typeof mon.initiative === "number") return mon.initiative;
        if (initiative.isNumber()) {
            return new Initiative(initiative.asInt());
        }
        // if (typeof mon.initiative !== "object") return null;
        if (!initiative.isObject()) {
            return null;
        }
        // if (typeof mon.initiative.initiative === "number") return mon.initiative.initiative;
        JsonNode value = MonsterFields.initiative.getFrom(initiative);
        if (value != null && value.isNumber()) {
            return new Initiative(value.asInt());
        }
        // if (mon.dex == null) return;
        if (abilityScores.dexterity() == null) {
            return null;
        }
        InitiativeMode advantageMode = InitiativeMode.fromString(
                MonsterFields.advantageMode.getTextOrNull(initiative));
        // 1 is proficient, 2 is expert
        int profBonus = 0;
        int profLevel = MonsterFields.proficiency.intOrDefault(initiative, 0);
        double crValue = crToNumber(cr);
        if (profLevel > 0 && crValue < CR_CUSTOM) {
            profBonus = profLevel * crToPb(cr);
        }
        return new Initiative(
                abilityScores.dexterity().modifier() + profBonus,
                advantageMode);
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
            ArrayNode array = MonsterFields.spellcasting.readArrayFrom(rootNode);
            if (array == null || array.isNull()) {
                return null;
            } else if (array.isObject()) {
                tui().warnf(Msg.UNKNOWN, "Unknown spellcasting for %s: %s", sources.getKey(), array.toPrettyString());
                return null;
            }

            List<Spellcasting> casting = new ArrayList<>();
            for (JsonNode scNode : iterableElements(array)) {
                if (scNode == null || scNode.isNull()) {
                    continue;
                }
                Spellcasting spellcasting = new Spellcasting();
                spellcasting.name = SourceField.name.replaceTextFrom(scNode, this);
                spellcasting.displayAs = MonsterFields.displayAs.getTextOrDefault(scNode, "trait");
                spellcasting.hidden = MonsterFields.hidden.getListOfStrings(scNode, tui());
                spellcasting.ability = MonsterFields.ability.getTextOrDefault(scNode, "spellcasting");

                spellcasting.headerEntries = new ArrayList<>();
                appendToText(spellcasting.headerEntries,
                        MonsterFields.headerEntries.getFrom(scNode), null);

                spellcasting.footerEntries = new ArrayList<>();
                appendToText(spellcasting.footerEntries,
                        MonsterFields.footerEntries.getFrom(scNode), null);

                spellcasting.fixed = new HashMap<>();
                spellcasting.variable = new HashMap<>();

                for (var type : HiddenType.values()) {
                    JsonNode value = scNode.get(type.name());
                    if (value == null || value.isNull()) {
                        continue;
                    }
                    switch (type) {
                        case constant, will, ritual -> {
                            List<String> spellList = getSpells(value);
                            if (spellList.isEmpty()) {
                                continue;
                            }
                            spellcasting.fixed.put(type.name(), spellList);
                        }
                        case spells -> {
                            spellcasting.spells = new TreeMap<>();
                            for (Entry<String, JsonNode> f : iterableFields(MonsterFields.spells.getFrom(scNode))) {
                                // value is object defining slots and spells
                                JsonNode spellNode = f.getValue();

                                Spells spellContainer = new Spells();
                                spellContainer.spells = getSpells(MonsterFields.spells.getFrom(spellNode));
                                if (spellContainer.spells.isEmpty()) {
                                    continue;
                                }
                                spellContainer.slots = MonsterFields.slots.intOrDefault(spellNode, 0);
                                spellContainer.lowerBound = MonsterFields.lower.intOrDefault(spellNode, 0);

                                // key is level
                                spellcasting.spells.put(f.getKey(), spellContainer);
                            }
                        }
                        default -> {
                            Map<String, List<String>> frequencySpells = new HashMap<>();
                            for (Entry<String, JsonNode> freqSpellList : iterableFields(value)) {
                                String frequency = freqSpellList.getKey();
                                List<String> spellList = getSpells(freqSpellList.getValue());
                                if (spellList.isEmpty()) {
                                    continue;
                                }
                                frequencySpells.put(frequency, spellList);
                            }
                            if (frequencySpells.isEmpty()) {
                                continue;
                            }
                            spellcasting.variable.put(type.name(), frequencySpells);
                        }
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
        if (source == null || source.isNull()) {
            tui().errorf("Null spells from %s", sources.getKey());
            return List.of();
        }
        List<String> spells = new ArrayList<>();
        for (var item : iterableElements(source)) {
            if (item.isTextual()) {
                spells.add(toLink(item.asText()));
            } else if (item.isObject()) {
                boolean hidden = MonsterFields.hidden.booleanOrDefault(item, false);
                if (hidden) {
                    continue;
                }
                spells.add(toLink(SourceField.entry.getTextOrEmpty(item)));
            } else {
                tui().warnf(Msg.UNKNOWN, "Unknown spell type for %s: %s", sources.getKey(), item.toPrettyString());
            }
        }
        return spells;
    }

    private String toLink(String spellText) {
        return spellText.contains("{@")
                ? replaceText(spellText)
                : linkify(Tools5eIndexType.spell, spellText);
    }

    Traits collectAllTraits() {
        boolean pushed = parseState().pushTrait();
        try {
            String legendaryGroupLink = null;
            TraitDescription lairActions = null;
            TraitDescription regionalEffects = null;

            JsonNode lgNameSource = MonsterFields.legendaryGroup.getFrom(rootNode);
            String lgKey = index().getAliasOrDefault(Tools5eIndexType.legendaryGroup.createKey(lgNameSource));
            if (lgNameSource != null && index().isIncluded(lgKey)) {
                JsonNode lgNode = index.getOrigin(lgKey);
                Tools5eSources lgSources = Tools5eSources.findSources(lgKey);
                lairActions = traitDescription(lgNode, MonsterFields.lairActions, "Lair Actions");
                regionalEffects = traitDescription(lgNode, MonsterFields.regionalEffects, "Regional Effects");
                legendaryGroupLink = linkifyLegendaryGroup(lgSources);
            }

            return new Traits(
                    traitDescription(rootNode, MonsterFields.trait, "Traits"),
                    traitDescription(rootNode, MonsterFields.action, "Actions"),
                    traitDescription(rootNode, MonsterFields.bonus, "Bonus Actions"),
                    traitDescription(rootNode, MonsterFields.reaction, "Reactions"),
                    traitDescription(rootNode, MonsterFields.legendary, "Legendary Actions"),
                    lairActions,
                    regionalEffects,
                    traitDescription(rootNode, MonsterFields.mythic, "Mythic Actions"),
                    legendaryGroupLink);
        } finally {
            parseState().pop(pushed);
        }
    }

    TraitDescription traitDescription(JsonNode source, MonsterFields field, String title) {
        boolean pushed = parseState().pushTrait();
        try {
            String noteKey = field.name() + "Note";
            JsonNode noteNode = source.get(noteKey);
            if (noteNode != null && noteNode.isTextual()) {
                title += " <small>(" + replaceText(noteNode) + ")</small>";
            }

            String headerKey = field.name() + "Header";
            JsonNode headerNode = source.get(headerKey);
            String headerText = null;
            if (headerNode != null) {
                headerText = flattenToString(headerNode);
            }

            List<NamedText> traits = new ArrayList<>();
            JsonNode target = field.getFrom(source);
            if (target != null && target.isArray()) {
                collectTraits(traits, target);
            }

            if (traits.size() > 0 && field == MonsterFields.legendary && headerText == null) {
                int legendaryActionCount = MonsterFields.legendaryActions.intOrDefault(rootNode, 3);
                int legendaryActionsLairCount = MonsterFields.legendaryActionsLair.intOrDefault(rootNode, legendaryActionCount);
                boolean isNamedCreature = MonsterFields.isNamedCreature.booleanOrDefault(rootNode, false);
                var possessive = isNamedCreature ? "their" : "its";

                if (getSources().isClassic()) {
                    var shortName = Tools5eJsonSourceCopier.getShortName(rootNode, true);
                    // The dragon can take 3 legendary actions, choosing from the options below.
                    // Only one legendary action can be used at a time and only at the end of another creature's turn.
                    // The dragon regains spent legendary actions at the start of its turn.
                    headerText = replaceText(
                            "%s can take %d legendary action%s%s, choosing from the options below. Only one legendary action can be used at a time and only at the end of another creature's turn. %s regains spent legendary actions at the start of %s turn."
                                    .formatted(shortName,
                                            legendaryActionCount,
                                            legendaryActionCount == 1 ? "" : "s",
                                            legendaryActionsLairCount != legendaryActionCount
                                                    ? " (or %d when in %s lair)".formatted(legendaryActionsLairCount,
                                                            possessive)
                                                    : "",
                                            shortName,
                                            possessive));
                } else {
                    // Legendary Action Uses: 3 (4 in Lair).
                    // Immediately after another creature's turn, The dragon can expend a use to take one of the following actions.
                    // The dragon regains all expended uses at the start of each of its turns.
                    headerText = replaceText(
                            "Legendary Action Uses: %d%s. Immediately after another creature's turn, %s can expend a use to take one of the following actions. %s regains all expended uses at the start of each of %s turns."
                                    .formatted(
                                            legendaryActionCount,
                                            legendaryActionsLairCount != legendaryActionCount
                                                    ? " (%d in Lair)".formatted(legendaryActionsLairCount)
                                                    : "",
                                            Tools5eJsonSourceCopier.getShortName(rootNode, false),
                                            Tools5eJsonSourceCopier.getShortName(rootNode, true),
                                            possessive));
                }
            }
            return new TraitDescription(title, headerText, traits);
        } finally {
            parseState().pop(pushed);
        }
    }

    String linkedSenses() {
        JsonNode node = MonsterFields.senses.getFrom(rootNode);
        if (node == null || node.isNull()) {
            return "";
        }
        if (node.isTextual()) {
            return linkifySense(node.asText());
        }
        List<String> list = new ArrayList<>();
        for (JsonNode senseNode : iterableElements(node)) {
            list.add(linkifySense(senseNode.asText()));
        }
        return String.join(", ", list);
    }

    String linkifySense(String sense) {
        int pos = sense.indexOf(" "); // find first space
        if (pos < 0) {
            return linkify(Tools5eIndexType.sense, sense);
        }
        return replaceText("{@sense %s}%s".formatted(sense.substring(0, pos), sense.substring(pos)));
    }

    List<String> gear() {
        final List<MonsterFields> gearFields = List.of(
                MonsterFields.gear, MonsterFields.attachedItems);
        List<String> gear = new ArrayList<>();
        for (MonsterFields field : gearFields) {
            for (var node : field.iterateArrayFrom(rootNode)) {
                if (node == null || node.isNull() || node.isArray()) {
                    continue;
                }
                int quantity = MonsterFields.quantity.intOrDefault(node, 1);
                String item = node.isObject()
                        ? MonsterFields.item.getTextOrEmpty(node)
                        : node.asText();
                if (quantity == 1) {
                    gear.add(linkify(Tools5eIndexType.item, item));
                } else {
                    var name = item.split("\\|")[0];
                    var plural = pluralize(name, quantity);
                    gear.add(replaceText("%s {@item %s|%s}".formatted(numberToText(quantity), item, plural)));
                }
            }
        }
        return gear;
    }

    @Override
    public String getImagePath() {
        if (type != Tools5eIndexType.monster) {
            return super.getImagePath();
        }
        return linkifier().monsterPath(isNpc, creatureType);
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
        undead,
        miscellaneous;

        public String toDirectory() {
            return name();
        }

        public static MonsterType fromString(String type) {
            String compare = type.toLowerCase()
                    .replace("abberation", "aberration"); // correct typo
            for (MonsterType t : MonsterType.values()) {
                if (compare.startsWith(t.name().toLowerCase())) {
                    return t;
                }
            }
            return miscellaneous;
        }

        public static MonsterType fromNode(JsonNode node, JsonTextConverter<?> converter) {
            if (!MonsterFields.type.existsIn(node)) {
                Tools5eSources sources = Tools5eSources.findSources(node);
                Tui.instance().warnf("Monster: Empty type for %s", sources);
                return miscellaneous;
            }
            JsonNode typeNode = MonsterFields.type.getFrom(node);
            String text = null;
            if (typeNode.isTextual()) {
                text = converter.replaceText(typeNode.asText());
            } else if (typeNode.isObject() && MonsterFields.type.existsIn(typeNode)) {
                // We have an object: type + tags
                text = MonsterFields.type.replaceTextFrom(typeNode, converter);
            }
            return text == null ? miscellaneous : fromString(text);
        }

        public static String toDirectory(String type) {
            MonsterType t = fromString(type);
            return t.toDirectory();
        }
    }

    enum MonsterFields implements JsonNodeReader {
        _abstract,
        _implementations,
        _variables,
        _versions,
        ability,
        ac,
        action,
        actionHeader,
        actionNote,
        advantageMode,
        alignment,
        alignmentPrefix,
        attachedItems,
        average,
        bonus,
        bonusHeader,
        bonusNote,
        choose,
        cr,
        creatureType, // object -- alternate to monster type
        daily,
        displayAs,
        footerEntries,
        formula,
        from,
        gear,
        headerEntries,
        hidden,
        hp,
        initiative,
        isNamedCreature,
        isNpc,
        item,
        lairActions,
        legendary,
        legendaryActions,
        legendaryActionsLair,
        legendaryGroup,
        legendaryHeader,
        lower,
        mythic,
        mythicHeader,
        oneOf,
        original,
        proficiency,
        quantity,
        reaction,
        reactionHeader,
        reactionNote,
        regionalEffects,
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
