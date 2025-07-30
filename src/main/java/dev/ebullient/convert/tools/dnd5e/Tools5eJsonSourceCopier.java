package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.ToDoubleFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.tools.JsonCopyException;
import dev.ebullient.convert.tools.JsonSourceCopier;
import dev.ebullient.convert.tools.dnd5e.Json2QuteMonster.MonsterFields;
import dev.ebullient.convert.tools.dnd5e.Json2QuteRace.RaceFields;

public class Tools5eJsonSourceCopier extends JsonSourceCopier<Tools5eIndexType> implements JsonSource {
    static final List<String> GENERIC_WALKER_ENTRIES_KEY_BLOCKLIST = List.of("caption", "type", "colLabels", "colLabelGroups",
            "name", "colStyles", "style", "shortName", "subclassShortName", "id", "path");

    private static final List<String> _MERGE_REQUIRES_PRESERVE_BASE = List.of(
            "page",
            "otherSources",
            "srd",
            "srd52",
            "basicRules",
            "basicRules2024",
            "reprintedAs",
            "hasFluff",
            "hasFluffImages",
            "hasToken",
            "_versions");
    private static final Map<Tools5eIndexType, List<String>> _MERGE_REQUIRES_PRESERVE = Map.of(
            // Monster fields that must be preserved
            Tools5eIndexType.monster, List.of("legendaryGroup", "environment", "soundClip",
                    "altArt", "variant", "dragonCastingColor", "familiar"),
            // Item fields that must be preserved
            Tools5eIndexType.item, List.of("lootTables", "tier"),
            // Item Group fields that must be preserved
            Tools5eIndexType.itemGroup, List.of("lootTables", "tier"));
    private static final List<String> COPY_ENTRY_PROPS = List.of(
            "action", "bonus", "reaction", "trait", "legendary", "mythic", "variant", "spellcasting",
            "actionHeader", "bonusHeader", "reactionHeader", "legendaryHeader", "mythicHeader");
    static final List<String> LEARNED_SPELL_TYPE = List.of("constant", "will", "ritual");
    static final List<String> SPELL_CAST_FREQUENCY = List.of("recharge", "charges", "rest", "daily", "weekly", "yearly");

    static final Pattern dmg_avg_subst = Pattern.compile("([\\d.,]+)([+*-])([^$]+)");

    final Tools5eIndex index;

    Tools5eJsonSourceCopier(Tools5eIndex index) {
        this.index = index;
    }

    @Override
    public Tools5eIndex index() {
        return index;
    }

    @Override
    public Tools5eSources getSources() {
        throw new IllegalStateException("Should not call getSources while copying source");
    }

    @Override
    protected JsonNode getOriginNode(String key) {
        return index().getOriginNoFallback(key);
    }

    @Override
    protected boolean mergePreserveKey(Tools5eIndexType type, String key) {
        List<String> preserveType = _MERGE_REQUIRES_PRESERVE.getOrDefault(type, List.of());
        return _MERGE_REQUIRES_PRESERVE_BASE.contains(key) || preserveType.contains(key);
    }

    @Override
    protected List<String> getCopyEntryProps() {
        return COPY_ENTRY_PROPS;
    }

    // render.js: _getMergedSubrace
    public JsonNode mergeSubrace(JsonNode subraceNode, JsonNode raceNode) {
        ObjectNode copyFrom = (ObjectNode) copyNode(subraceNode);
        ObjectNode subraceOut = (ObjectNode) copyNode(raceNode);

        List.of("name", "source", "srd", "srd52", "basicRules", "basicRules2024")
                .forEach(p -> subraceOut.set("_base" + toTitleCase(p), subraceOut.get(p)));
        List.of("subraces", "srd", "srd52", "basicRules", "basicRules2024",
                "_versions", "hasFluff", "hasFluffImages",
                "reprintedAs", "_rawName")
                .forEach(subraceOut::remove);

        copyFrom.remove("__prop"); // cleanup

        JsonNode overwrite = MetaFields.overwrite.getFrom(copyFrom);

        // merge names
        if (SourceField.name.existsIn(copyFrom)) {
            String raceName = SourceField.name.getTextOrThrow(raceNode);
            String subraceName = SourceField.name.getTextOrThrow(copyFrom);
            subraceOut.put("_subraceName", subraceName);
            if (MetaFields.alias.existsIn(copyFrom)) {
                ArrayNode alias = (ArrayNode) MetaFields.alias.getFrom(copyFrom);
                for (int i = 0; i < alias.size(); i++) {
                    alias.set(i, Json2QuteRace.getSubraceName(raceName, alias.get(i).asText()));
                }
                MetaFields.alias.setIn(subraceOut, alias);
                MetaFields.alias.removeFrom(copyFrom);
            }
            SourceField.name.removeFrom(copyFrom);
            SourceField.name.setIn(subraceOut, Json2QuteRace.getSubraceName(raceName, subraceName));
        } else {
            Tools5eFields.srd.copy(raceNode, subraceOut);
            Tools5eFields.basicRules.copy(raceNode, subraceOut);
        }

        // merge abilities
        if (RaceFields.ability.existsIn(copyFrom)) {
            ArrayNode cpySrAbility = (ArrayNode) RaceFields.ability.getFrom(copyFrom);
            ArrayNode outAbility = (ArrayNode) RaceFields.ability.getFrom(subraceOut);
            // If the base race doesn't have any ability scores, make a set of empty records
            if (RaceFields.ability.existsIn(overwrite) || outAbility == null) {
                RaceFields.ability.copy(copyFrom, subraceOut);
            } else if (cpySrAbility.size() != outAbility.size()) {
                // if (cpy.ability.length !== cpySr.ability.length) throw new Error(`Race and subrace ability array lengths did not match!`);
                tui().errorf("Error (%s): Unable to merge abilities (different lengths). CopyTo: %s, CopyFrom: %s", subraceOut,
                        copyFrom);
            } else {
                // cpySr.ability.forEach((obj, i) => Object.assign(cpy.ability[i], obj));
                for (int i = 0; i < cpySrAbility.size(); i++) {
                    outAbility.set(i, cpySrAbility.get(i));
                }
            }
            RaceFields.ability.removeFrom(copyFrom);
        }

        // merge entries
        if (SourceField.entries.existsIn(copyFrom)) {
            ArrayNode entries = ensureArray(SourceField.entries.getFrom(subraceOut));
            SourceField.entries.setIn(subraceOut, entries); // make sure set as array
            for (JsonNode entry : iterableEntries(copyFrom)) {
                JsonNode data = MetaFields.data.getFrom(entry);
                if (MetaFields.overwrite.existsIn(data)) {
                    // overwrite
                    int index = findIndexByName("subrace-merge:" + SourceField.name.getTextOrThrow(subraceOut),
                            entries, MetaFields.overwrite.getTextOrThrow(data));
                    if (index >= 0) {
                        entries.set(index, entry);
                    } else {
                        appendToArray(entries, entry);
                    }
                } else {
                    appendToArray(entries, entry);
                }
            }

            SourceField.entries.removeFrom(copyFrom);
        }

        // TODO: ATM, not tracking trait tags, languages, or skills separately from text
        if (Tools5eFields.traitTags.existsIn(copyFrom)) {
            Tools5eFields.traitTags.removeFrom(copyFrom);
        }
        if (RaceFields.languageProficiencies.existsIn(copyFrom)) {
            RaceFields.languageProficiencies.removeFrom(copyFrom);
        }
        if (RaceFields.skillProficiencies.existsIn(copyFrom)) {
            RaceFields.skillProficiencies.removeFrom(copyFrom);
        }

        // overwrite everything else
        for (Entry<String, JsonNode> e : iterableFields(copyFrom)) {
            // already from a copy, just .. move it on over.
            subraceOut.set(e.getKey(), e.getValue());
        }

        // For any null'd out fields on the subrace, delete the field
        Iterator<Entry<String, JsonNode>> fields = subraceOut.properties().iterator();
        while (fields.hasNext()) {
            Entry<String, JsonNode> e = fields.next();
            if (e.getValue().isNull()) {
                fields.remove();
            }
        }

        RaceFields._isSubRace.setIn(subraceOut, BooleanNode.TRUE);
        return subraceOut;
    }

    // 	utils.js: static getCopy (impl, copyFrom, copyTo, templateData,...) {
    @Override
    public JsonNode mergeNodes(Tools5eIndexType type, String originKey, JsonNode copyFrom, ObjectNode target) {
        JsonNode _copy = MetaFields._copy.getFromOrEmptyObjectNode(target);
        normalizeMods(_copy);

        // fetch and apply any external template
        // append them to existing copy mods where available
        ArrayNode templates = MetaFields._templates.readArrayFrom(_copy);
        for (JsonNode _template : templates) {

            String templateKey = Tools5eIndexType.monsterTemplate.createKey(_template);
            JsonNode templateNode = getOriginNode(templateKey);

            if (templateNode == null) {
                tui().warnf(Msg.NOT_SET.wrap("Unable to find traits for %s"), templateKey);
                continue;
            } else {
                if (!MetaFields._mod.nestedExistsIn(MetaFields.apply, templateNode)) {
                    // if template.apply._mod doesn't exist, skip this
                    continue;
                }

                JsonNode template = copyNode(templateNode); // copy fast
                JsonNode templateApply = MetaFields.apply.getFrom(template);
                normalizeMods(templateApply);

                JsonNode templateApplyMods = MetaFields._mod.getFrom(templateApply);
                if (MetaFields._mod.existsIn(_copy)) {
                    ObjectNode copyMods = (ObjectNode) MetaFields._mod.getFrom(_copy);
                    for (Entry<String, JsonNode> e : iterableFields(templateApplyMods)) {
                        if (copyMods.has(e.getKey())) {
                            appendToArray(copyMods.withArray(e.getKey()), e.getValue());
                        } else {
                            copyMods.set(e.getKey(), e.getValue());
                        }
                    }
                } else {
                    MetaFields._mod.setIn(_copy, templateApplyMods);
                }
            }
        }
        MetaFields._templates.removeFrom(_copy);

        // Copy required values from...
        copyValues(type, copyFrom, target, _copy);

        // apply any root template properties after doing base copy
        List<String> copyToRootProps = streamOfFieldNames(target).toList();
        for (JsonNode template : templates) {
            if (!MetaFields._root.nestedExistsIn(MetaFields.apply, template)) {
                continue;
            }
            JsonNode templateApplyRoot = MetaFields._root.getFrom(MetaFields.apply.getFrom(template));
            for (Entry<String, JsonNode> from : iterableFields(templateApplyRoot)) {
                String k = from.getKey();
                if (!copyToRootProps.contains(k)) {
                    continue; // avoid overwriting any real properties with templates
                }
                target.set(k, copyNode(from.getValue()));
            }
        }

        // Apply mods
        applyMods(originKey, copyFrom, target, _copy);

        // indicate that this is a copy, and remove copy metadata (avoid revisit)
        cleanupCopy(target, copyFrom);

        return target;
    }

    @Override
    protected JsonNode resolveDynamicVariable(
            String originKey, JsonNode value, JsonNode target, TemplateVariable variableMode, String[] params) {
        return switch (variableMode) {
            case name -> new TextNode(SourceField.name.getTextOrEmpty(target));
            case short_name -> new TextNode(getShortName(target, false));
            case title_short_name -> new TextNode(getShortName(target, true));
            case dc, spell_dc -> {
                if (params.length == 0 || !target.has(params[0])) {
                    tui().errorf("Error (%s): Missing detail for %s", originKey, value);
                    yield value;
                }
                int mod = getAbilityModNumber(target.get(params[0]).asInt());
                int pb = crToPb(MonsterFields.cr.getFrom(target));
                yield new TextNode("" + (8 + pb + mod));
            }
            case to_hit -> {
                if (params.length == 0 || !target.has(params[0])) {
                    tui().errorf("Error (%s): Missing detail for %s", originKey, value);
                    yield value;
                }
                int mod = getAbilityModNumber(target.get(params[0]).asInt());
                int pb = crToPb(MonsterFields.cr.getFrom(target));
                yield new TextNode(asModifier(pb + mod));
            }
            case damage_mod -> {
                if (params.length == 0 || !target.has(params[0])) {
                    tui().errorf("Error (%s): Missing detail for %s", originKey, value);
                    yield value;
                }
                int mod = getAbilityModNumber(target.get(params[0]).asInt());
                yield new TextNode(mod == 0 ? "" : asModifier(mod));
            }
            case damage_avg -> {
                Matcher m = dmg_avg_subst.matcher(params[0]);
                if (m.matches()) {
                    String amount = m.group(1);
                    String op = m.group(2);
                    int mod = getAbilityModNumber(target.get(m.group(3)).asInt());
                    if ("+".equals(op)) {
                        double total = Double.parseDouble(amount) + mod;
                        yield new TextNode("" + Math.floor(total));
                    }
                }
                tui().errorf("Error (%s): Unrecognized damage average template %s", originKey, value);
                yield value;
            }
            default -> {
                variableMode.notSupported(tui(), originKey, value);
                yield value;
            }
        };
    }

    protected void doModProp(
            String originKey, JsonNode modInfo, JsonNode copyFrom, String prop, ObjectNode target, ModFieldMode mode) {
        if (mode == null) {
            tui().errorf("Error (%s): Missing mode for modProp (add value to ModFieldMode): %s", originKey, modInfo);
            return;
        }
        switch (mode) {
            // Bestiary
            case addAllSaves, addAllSkills, addSaves -> mode.notSupported(tui(), originKey, modInfo);
            case addSenses -> doAddSenses(originKey, modInfo, copyFrom, target); // no prop
            case addSkills -> doAddSkills(originKey, modInfo, target); // no prop
            case addSpells -> doAddSpells(originKey, modInfo, copyFrom, target); // no prop
            case replaceSpells -> doReplaceSpells(originKey, modInfo, copyFrom, target); // no prop
            case removeSpells -> doRemoveSpells(originKey, modInfo, copyFrom, target); // no prop
            // MATH
            case calculateProp -> mode.notSupported(tui(), originKey, modInfo);
            case scalarMultXp -> doScalarMultXp(originKey, modInfo, target); // no prop
            case scalarAddDc -> doScalarAddDc(originKey, modInfo, prop, target);
            case scalarAddHit -> doScalarAddHit(originKey, modInfo, prop, target);
            case maxSize -> doMaxSize(originKey, modInfo, target); // no prop
            default -> super.doModProp(originKey, modInfo, copyFrom, prop, target, mode);
        }
    }

    private void doScalarAddHit(String originKey, JsonNode modInfo, String prop, ObjectNode target) {
        final Pattern hitPattern = Pattern.compile("\\{@hit ([-+]?\\d+)}");
        if (!target.has(prop)) {
            return;
        }

        int scalar = MetaFields.scalar.getFrom(modInfo).asInt();
        String fullNode = hitPattern.matcher(target.get(prop).toString())
                .replaceAll((match) -> "{@hit " + (Integer.parseInt(match.group(1)) + scalar) + "}");
        target.set(prop, createNode(fullNode));
    }

    private void doScalarAddDc(String originKey, JsonNode modInfo, String prop, ObjectNode target) {
        final Pattern dcPattern = Pattern.compile("\\{@dc (\\d+)(?:\\|[^}]+)?}");
        if (!target.has(prop)) {
            return;
        }
        int scalar = MetaFields.scalar.getFrom(modInfo).asInt();
        String fullNode = dcPattern.matcher(target.get(prop).toString())
                .replaceAll((match) -> "{@dc " + (Integer.parseInt(match.group(1)) + scalar) + "}");
        target.set(prop, createNode(fullNode));
    }

    private void doScalarMultXp(String originKey, JsonNode modInfo, ObjectNode target) {
        JsonNode crNode = Tools5eFields.cr.getFrom(target);
        if (crNode == null) {
            tui().errorf("Error (%s): modifying scalarMultXp on an object that does not define cr; %s", originKey, target);
            return;
        }
        double scalar = MetaFields.scalar.getFrom(modInfo).asDouble();
        boolean floor = MetaFields.floor.booleanOrDefault(modInfo, false);

        ToDoubleFunction<Double> scalarMult = (x) -> {
            double value = x * scalar;
            if (floor) {
                value = Math.floor(value);
            }
            return value;
        };

        if (Tools5eFields.xp.existsIn(crNode)) {
            double input = Tools5eFields.xp.getFrom(crNode).asDouble();
            Tools5eFields.xp.setIn(target, new DoubleNode(scalarMult.applyAsDouble(input)));
        } else {
            double crValue = crToXp(crNode);
            if (!Tools5eFields.cr.existsIn(crNode)) {
                // Wrap cr in a containing object
                ObjectNode crNodeRw = mapper().createObjectNode();
                Tools5eFields.cr.setIn(crNodeRw, crNode);
                Tools5eFields.cr.setIn(target, crNodeRw);
                crNode = crNodeRw;
            }
            Tools5eFields.xp.setIn(crNode, new DoubleNode(scalarMult.applyAsDouble(crValue)));
        }
    }

    private void doMaxSize(String originKey, JsonNode modInfo, ObjectNode target) {
        final String SIZES = "FDTSMLHGCV";
        if (!Tools5eFields.size.existsIn(target)) {
            tui().errorf("Error (%s): enforcing maxSize on an object that does not define size; %s", originKey, target);
            return;
        }

        String maxValue = MetaFields.max.getTextOrEmpty(modInfo);
        int maxIdx = SIZES.indexOf(maxValue);
        if (maxValue.isBlank() || maxIdx < 0) {
            tui().errorf("Error (%s): Invalid maxSize value %s", originKey, maxValue.isBlank() ? "(missing)" : maxValue);
            return;
        }

        ArrayNode size = Tools5eFields.size.ensureArrayIn(target);
        List<JsonNode> collect = streamOf(size)
                .filter(x -> SIZES.indexOf(x.asText()) <= maxIdx)
                .collect(Collectors.toList());

        if (size.size() != collect.size()) {
            size.removeAll();
            if (collect.isEmpty()) {
                size.add(new TextNode(maxValue));
            } else {
                size.addAll(collect);
            }
        }
    }

    // static _doMod_addSpells ({copyTo, copyFrom,...
    void doAddSpells(String originKey, JsonNode modInfo, JsonNode copyFrom, ObjectNode target) {
        ObjectNode spellcasting = (ObjectNode) MonsterFields.spellcasting.getFirstFromArray(target);
        if (spellcasting == null) {
            tui().errorf("Error (%s): Can't add spells to a monster without spellcasting", originKey);
            throw new JsonCopyException("Can't add spells to a monster without spellcasting; copy/merge of " + originKey);
        }

        if (MonsterFields.spells.existsIn(modInfo)) {
            ObjectNode spells = ensureObjectNode(MonsterFields.spells.getFrom(spellcasting));
            MonsterFields.spells.setIn(spellcasting, spells); // ensure saved as object

            for (Entry<String, JsonNode> modSpellEntry : iterableFields(MonsterFields.spells.getFrom(modInfo))) {
                String k = modSpellEntry.getKey();
                if (!spells.has(k)) {
                    spells.set(k, copyNode(modSpellEntry.getValue()));
                    continue;
                }

                // merge the spell objects (yikes)
                ObjectNode targetSpell = (ObjectNode) spells.get(k);
                JsonNode modSpell = modSpellEntry.getValue();

                for (Entry<String, JsonNode> modSpellProp : iterableFields(modSpell)) {
                    String prop = modSpellProp.getKey();
                    JsonNode modSpellList = modSpellProp.getValue();
                    JsonNode tgtSpellList = targetSpell.get(prop);

                    if (tgtSpellList == null) {
                        targetSpell.set(prop, copyNode(modSpellList));
                    } else if (tgtSpellList.isArray()) {
                        // append and sort
                        appendToArray((ArrayNode) tgtSpellList, copyNode(modSpellList));
                        targetSpell.set(prop, sortArrayNode((ArrayNode) tgtSpellList));
                    } else if (tgtSpellList.isObject()) {
                        // throw. Not supported
                        tui().errorf("Error (%s): Object at key %s, not an array", originKey, prop);
                        throw new JsonCopyException("Badly formed spell list in " + originKey
                                + "; found JSON Object instead of an array for " + prop);
                    } else {
                        // overwrite
                        targetSpell.set(prop, copyNode(modSpellList));
                    }
                }
            }
        }

        for (String type : LEARNED_SPELL_TYPE) {
            if (modInfo.has(type)) {
                ArrayNode spells = spellcasting.withArray(type);
                spells.addAll((ArrayNode) modInfo.get(type));
            }
        }

        for (String cast : SPELL_CAST_FREQUENCY) {
            JsonNode modSpells = modInfo.get(cast);
            if (modSpells == null) {
                continue;
            }
            ObjectNode spells = ensureObjectNode(spellcasting.get(cast));
            spellcasting.set(cast, spells); // ensure saved as object

            for (int i = 1; i <= 9; i++) {
                String k = i + "";
                if (modSpells.has(k)) {
                    ArrayNode spellList = spells.withArray(k);
                    appendToArray(spellList, (ArrayNode) modSpells.get(k));
                }

                String each = i + "e";
                if (modSpells.has(each)) {
                    ArrayNode spellList = spells.withArray(each);
                    appendToArray(spellList, (ArrayNode) modSpells.get(each));
                }
            }
        }
    }

    void doReplaceSpells(String originKey, JsonNode modInfo, JsonNode copyFrom, ObjectNode target) {
        ObjectNode spellcasting = (ObjectNode) MonsterFields.spellcasting.getFirstFromArray(target);
        if (spellcasting == null) {
            tui().errorf("Error (%s): Can't replace spells for a monster without spellcasting", originKey);
            throw new JsonCopyException(
                    "Can't replace spells for a monster without spellcasting; copy/merge of " + originKey);
        }

        if (MonsterFields.spells.existsIn(modInfo) && MonsterFields.spells.existsIn(spellcasting)) {
            ObjectNode spells = ensureObjectNode(MonsterFields.spells.getFrom(spellcasting));
            MonsterFields.spells.setIn(spellcasting, spells); // ensure saved as object

            for (Entry<String, JsonNode> modSpellEntry : iterableFields(MonsterFields.spells.getFrom(modInfo))) {
                String k = modSpellEntry.getKey();
                if (spells.has(k)) { // replace if exists
                    JsonNode replaceMetas = modSpellEntry.getValue();
                    ObjectNode currentSpells = (ObjectNode) spells.get(k);
                    replaceSpells(originKey, currentSpells, replaceMetas, MonsterFields.spells.name());
                }
            }
        }

        JsonNode modDailySpells = MonsterFields.daily.getFrom(modInfo);
        ObjectNode dailySpells = (ObjectNode) MonsterFields.daily.getFrom(spellcasting);
        if (modDailySpells != null && dailySpells != null) {
            for (int i = 1; i <= 9; i++) {
                String k = i + "";
                for (JsonNode replaceMetas : iterableElements(modDailySpells.get(k))) {
                    replaceSpells(originKey, dailySpells, replaceMetas, k);
                }
                String each = i + "e";
                for (JsonNode replaceMetas : iterableElements(modDailySpells.get(each))) {
                    replaceSpells(originKey, dailySpells, replaceMetas, each);
                }
            }
        }
    }

    void replaceSpells(String originKey, ObjectNode currentSpells, JsonNode replaceMetas, String k) {
        replaceMetas = ensureArray(replaceMetas);
        for (JsonNode replaceMeta : iterableElements(replaceMetas)) {
            JsonNode with = ensureArray(MetaFields.with.getFrom(replaceMeta));
            JsonNode replace = MetaFields.replace.getFrom(replaceMeta);
            if (replace == null) {
                tui().errorf("Error (%s): Missing replace value for %s", originKey, replaceMeta);
                continue;
            }
            ArrayNode spellList = currentSpells.withArray(k);

            int index = findIndexByName(originKey, spellList, replace.asText());
            if (index >= 0) {
                spellList.remove(index);
                insertIntoArray(spellList, index, with);
            } else {
                tui().errorf("Error (%s): Unable to find spell %s to replace", originKey, replace);
            }
        }
    }

    // static _doMod_removeSpells ({copyTo, copyFrom,...
    void doRemoveSpells(String originKey, JsonNode modInfo, JsonNode copyFrom, ObjectNode target) {
        ObjectNode spellcasting = (ObjectNode) MonsterFields.spellcasting.getFirstFromArray(target);
        if (spellcasting == null) {
            tui().errorf("Error (%s): Can't remove spells from a monster without spellcasting", originKey);
            throw new JsonCopyException(
                    "Can't remove spells from a monster without spellcasting; copy/merge of " + originKey);
        }

        if (MonsterFields.spells.existsIn(modInfo) && MonsterFields.spells.existsIn(spellcasting)) {
            ObjectNode spells = (ObjectNode) MonsterFields.spells.getFrom(spellcasting);

            for (Entry<String, JsonNode> modSpellEntry : iterableFields(MonsterFields.spells.getFrom(modInfo))) {
                String k = modSpellEntry.getKey();
                // Look for spell levels: spells.1.spells
                if (MonsterFields.spells.existsIn(spells.get(k))) {
                    removeSpells(originKey,
                            MonsterFields.spells.ensureArrayIn(spells.get(k)),
                            modSpellEntry.getValue());
                }
            }

            for (String k : LEARNED_SPELL_TYPE) {
                if (modInfo.has(k) && spellcasting.has(k)) {
                    ArrayNode spellList = spellcasting.withArray(k);
                    removeSpells(originKey, spellList, modInfo.get(k));
                }
            }

            for (String cast : SPELL_CAST_FREQUENCY) {
                if (modInfo.has(cast) && spellcasting.has(cast)) {
                    ObjectNode spellCast = (ObjectNode) spellcasting.get(cast);
                    for (int i = 1; i <= 9; i++) {
                        String k = i + "";
                        String each = i + "e";
                        if (spellCast.has(k)) {
                            ArrayNode interval = spellcasting.withArray(k);
                            removeSpells(originKey, interval, modInfo.get(k));
                        }
                        if (spellCast.has(each)) {
                            ArrayNode eachList = spellcasting.withArray(each);
                            removeSpells(originKey, eachList, modInfo.get(each));
                        }
                    }
                }
            }
        }
    }

    void removeSpells(String originKey, ArrayNode spellList, JsonNode removeSpellList) {
        for (JsonNode spell : iterableElements(removeSpellList)) {
            int index = findIndexByName(originKey, spellList, spell.asText());
            if (index >= 0) {
                spellList.remove(index);
            }
        }
    }

    void doAddSenses(String originKey, JsonNode modInfo, JsonNode copyFrom, ObjectNode target) {
        ArrayNode senses = ensureArray(MonsterFields.senses.getFrom(target));
        MonsterFields.senses.setIn(target, senses); // make sure set as array

        JsonNode modSenses = ensureArray(MonsterFields.senses.getFrom(modInfo));
        for (JsonNode modSense : iterableElements(modSenses)) {
            boolean found = false;
            String modType = MetaFields.type.getTextOrThrow(modSense);
            int modRange = MetaFields.range.intOrThrow(modSense);
            Pattern p = Pattern.compile(modType + " (\\d+)", Pattern.CASE_INSENSITIVE);
            for (int i = 0; i < senses.size(); i++) {
                Matcher m = p.matcher(senses.get(i).asText());
                if (m.matches()) {
                    found = true;
                    int range = Integer.parseInt(m.group(1));
                    if (range < modRange) {
                        senses.set(i, new TextNode(modType + " " + modRange + " ft."));
                    }
                    break;
                }
            }
            if (!found) {
                senses.add(new TextNode(modType + " " + modRange + " ft."));
            }
        }
    }

    void doAddSkills(String originKey, JsonNode modInfo, ObjectNode target) {
        ObjectNode allSkills = ensureObjectNode(MonsterFields.skill.getFrom(target));
        MonsterFields.skill.setIn(target, allSkills); // ensure saved as object
        int pb = crToPb(Tools5eFields.cr.getFrom(target));

        JsonNode modSkills = ensureArray(MetaFields.skills.getFrom(modInfo));
        for (Entry<String, JsonNode> entry : iterableFields(modSkills)) {
            String modSkill = entry.getKey();
            int abilityScore = intOrThrow(target, getAbilityForSkill(modSkill));
            int abilityMod = getAbilityModNumber(abilityScore);

            // mode: 1 = proficient; 2 = expert
            int mode = MetaFields.mode.intOrThrow(entry.getValue());
            int total = mode * pb + abilityMod;

            if (allSkills.has(modSkill)) {
                int existing = intOrThrow(allSkills, modSkill);
                if (total > existing) {
                    allSkills.put(modSkill, asModifier(total));
                }
            } else {
                allSkills.put(modSkill, asModifier(total));
            }
        }
    }

    public static String getShortName(JsonNode target, boolean isTitleCase) {
        String name = SourceField.name.getTextOrEmpty(target);
        JsonNode shortName = Tools5eFields.shortName.getFrom(target);
        boolean isNamedCreature = MonsterFields.isNamedCreature.booleanOrDefault(target, false);
        String prefix = isNamedCreature
                ? ""
                : isTitleCase ? "The " : "the ";

        if (shortName != null) {
            if (shortName.isBoolean() && shortName.asBoolean()) {
                return prefix + name;
            }
            String text = shortName.asText();
            String result = prefix;
            if (prefix.isBlank() && isTitleCase) {
                result += toTitleCase(text);
            } else {
                result += text.toLowerCase();
            }
            return result;
        }

        return prefix + getShortNameFromName(name, isNamedCreature);
    }

    public static String getShortNameFromName(String name, boolean isNamedCreature) {
        String result = name.split(",")[0]
                .replaceAll("(?i)(?:adult|ancient|young) \\w+ (dragon|dracolich)", "$1");

        return isNamedCreature
                ? result.split(" ")[0]
                : result.toLowerCase();
    }

    int getAbilityModNumber(int abilityScore) {
        return (int) Math.floor((abilityScore - 10) / 2);
    }

    String getAbilityForSkill(String skill) {
        switch (skill) {
            case "athletics":
                return "str";
            case "acrobatics":
            case "sleight of hand":
            case "stealth":
                return "dex";
            case "arcana":
            case "history":
            case "investigation":
            case "nature":
            case "religion":
                return "int";
            case "animal handling":
            case "insight":
            case "medicine":
            case "perception":
            case "survival":
                return "wis";
            case "deception":
            case "intimidation":
            case "performance":
            case "persuasion":
                return "cha";
        }
        throw new IllegalArgumentException("Unknown skill: " + skill);
    }
}
