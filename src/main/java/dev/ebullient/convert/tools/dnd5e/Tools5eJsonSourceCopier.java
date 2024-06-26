package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.function.ToDoubleFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import dev.ebullient.convert.tools.JsonCopyException;
import dev.ebullient.convert.tools.JsonSourceCopier;
import dev.ebullient.convert.tools.dnd5e.Json2QuteMonster.MonsterFields;
import dev.ebullient.convert.tools.dnd5e.Json2QuteRace.RaceFields;

public class Tools5eJsonSourceCopier extends JsonSourceCopier<Tools5eIndexType> implements JsonSource {
    static final List<String> GENERIC_WALKER_ENTRIES_KEY_BLOCKLIST = List.of("caption", "type", "colLabels", "colLabelGroups",
        "name", "colStyles", "style", "shortName", "subclassShortName", "id", "path");

    private static final List<String> _MERGE_REQUIRES_PRESERVE_BASE = List.of(
        "_versions",
        "basicRules",
        "hasFluff",
        "hasFluffImages",
        "hasToken",
        "indexInputType", // mine: do I have the usage right?
        "indexKey", // mine: do I have the usage right?
        "otherSources",
        "page",
        "reprintedAs",
        "srd");
    private static final Map<Tools5eIndexType, List<String>> _MERGE_REQUIRES_PRESERVE = Map.of(
        Tools5eIndexType.monster, List.of("legendaryGroup", "environment", "soundClip",
            "altArt", "variant", "dragonCastingColor", "familiar"),
        Tools5eIndexType.item, List.of("lootTables", "tier"),
        Tools5eIndexType.itemGroup, List.of("lootTables", "tier"));
    static final List<String> COPY_ENTRY_PROPS = List.of(
        "action", "bonus", "reaction", "trait", "legendary", "mythic", "variant", "spellcasting",
        "actionHeader", "bonusHeader", "reactionHeader", "legendaryHeader", "mythicHeader");
    static final List<String> LEARNED_SPELL_TYPE = List.of("constant", "will", "ritual");
    static final List<String> SPELL_CAST_FREQUENCY = List.of("recharge", "charges", "rest", "daily", "weekly", "yearly");

    static final Pattern variable_subst = Pattern.compile("<\\$(?<variable>[^$]+)\\$>");
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

    // render.js: _getMergedSubrace
    public JsonNode mergeSubrace(JsonNode subraceNode, JsonNode raceNode) {
        ObjectNode copyFrom = (ObjectNode) copyNode(subraceNode);
        ObjectNode subraceOut = (ObjectNode) copyNode(raceNode);

        List.of("name", "source", "srd", "basicRules")
            .forEach(p -> subraceOut.set("_base" + toTitleCase(p), subraceOut.get(p)));
        List.of("subraces", "srd", "basicRules", "_versions", "hasFluff", "hasFluffImages", "_rawName")
            .forEach(subraceOut::remove);

        copyFrom.remove("__prop"); // cleanup: we copy remainder later

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
        Iterator<Entry<String, JsonNode>> fields = subraceOut.fields();
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
    protected JsonNode mergeNodes(Tools5eIndexType type, String originKey, JsonNode copyFrom, JsonNode copyTo) {
        // edit in place: if you don't, lower-level copies will keep being revisted.
        ObjectNode target = (ObjectNode) copyTo;

        JsonNode _copy = MetaFields._copy.getFromOrEmptyObjectNode(copyTo);
        normalizeMods(_copy);

        // fetch and apply any external template
        // append them to existing copy mods where available
        ArrayNode templates = MetaFields._templates.readArrayFrom(_copy);
        for (JsonNode _template : templates) {

            String templateKey = Tools5eIndexType.monsterTemplate.createKey(_template);
            JsonNode templateNode = getOriginNode(templateKey);

            if (templateNode == null) {
                tui().warn("Unable to find traits for " + templateKey);
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
        List<String> copyToRootProps = streamOfFieldNames(copyTo).toList();
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
        if (MetaFields._mod.existsIn(_copy)) {
            // pre-convert any dynamic text
            JsonNode copyMetaMod = MetaFields._mod.getFrom(_copy);
            for (Entry<String, JsonNode> entry : iterableFields(copyMetaMod)) {
                // use the copyTo value as the attribute source for resolving dynamic text
                entry.setValue(resolveDynamicText(originKey, entry.getValue(), copyTo));
            }

            // Now iterate and apply mod rules
            for (Entry<String, JsonNode> entry : iterableFields(copyMetaMod)) {
                String prop = entry.getKey();
                JsonNode modInfos = entry.getValue();
                if ("*".equals(prop)) {
                    doMod(originKey, target, copyFrom, modInfos, COPY_ENTRY_PROPS);
                } else if ("_".equals(prop)) {
                    doMod(originKey, target, copyFrom, modInfos, null);
                } else {
                    doMod(originKey, target, copyFrom, modInfos, List.of(prop));
                }
            }
        }

        // indicate that this is a copy, and remove copy metadata (avoid revisit)
        cleanupCopy(target, copyFrom);

        return target;
    }

    // DataUtil.generic.variableResolver
    /**
     * @param value JsonNode to be checked for values to replace
     * @param target JsonNode with attributes that can be used to resolve templates
     */
    private JsonNode resolveDynamicText(String originKey, JsonNode value, JsonNode target) {
        if (value == null || !(value.isArray() || value.isObject() || value.isTextual())) {
            return value;
        }
        if (value.isArray()) {
            for (int i = 0; i < value.size(); i++) {
                ((ArrayNode) value).set(i, resolveDynamicText(originKey, value.get(i), target));
            }
            return value;
        }
        if (value.isObject()) {
            for (Entry<String, JsonNode> e : iterableFields(value)) {
                e.setValue(resolveDynamicText(originKey, e.getValue(), target));
            }
            return value;
        }
        Matcher matcher = variable_subst.matcher(value.toString());
        if (matcher.find()) {
            String[] pieces = matcher.group("variable").split("__");
            TemplateVariable variableMode = TemplateVariable.valueFrom(pieces[0]);
            return switch (variableMode) {
                case name -> new TextNode(SourceField.name.getTextOrEmpty(target));
                case short_name -> new TextNode(getShortName(target, false));
                case title_short_name -> new TextNode(getShortName(target, true));
                case dc, spell_dc -> {
                    if (pieces.length < 2 || !target.has(pieces[1])) {
                        tui().errorf("Error (%s): Missing detail for %s", originKey, value);
                        yield value;
                    }
                    int mod = getAbilityModNumber(target.get(pieces[1]).asInt());
                    int pb = crToPb(MonsterFields.cr.getFrom(target));
                    yield new TextNode("" + (8 + pb + mod));
                }
                case to_hit -> {
                    if (pieces.length < 2 || !target.has(pieces[1])) {
                        tui().errorf("Error (%s): Missing detail for %s", originKey, value);
                        yield value;
                    }
                    int mod = getAbilityModNumber(target.get(pieces[1]).asInt());
                    int pb = crToPb(MonsterFields.cr.getFrom(target));
                    yield new TextNode(asModifier(pb + mod));
                }
                case damage_mod -> {
                    if (pieces.length < 2 || !target.has(pieces[1])) {
                        tui().errorf("Error (%s): Missing detail for %s", originKey, value);
                        yield value;
                    }
                    int mod = getAbilityModNumber(target.get(pieces[1]).asInt());
                    yield new TextNode(mod == 0 ? "" : asModifier(mod));
                }
                case damage_avg -> {
                    Matcher m = dmg_avg_subst.matcher(pieces[1]);
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
        return value;
    }

    private void doMod(String originKey, ObjectNode target, JsonNode copyFrom, JsonNode modInfos, List<String> props) {
        if (props == null || props.isEmpty()) { // '_' case
            doModProp(originKey, modInfos, copyFrom, null, target);
        } else {
            for (String prop : props) {
                doModProp(originKey, modInfos, copyFrom, prop, target);
            }
        }
    }

    void doModProp(String originKey, JsonNode modInfos, JsonNode copyFrom, String prop, ObjectNode target) {
        for (JsonNode modInfo : iterableElements(modInfos)) {
            if (modInfo.isTextual()) {
                if ("remove".equals(modInfo.asText()) && prop != null) {
                    target.remove(prop);
                } else {
                    tui().errorf("Error(%s): Unknown text modification mode for %s: %s", originKey, prop, modInfo);
                }
            } else {
                ModFieldMode mode = ModFieldMode.getModMode(modInfo);
                switch (mode) {
                    // Strings & text
                    case appendStr -> doAppendText(originKey, modInfo, copyFrom, prop, target);
                    case replaceName -> mode.notSupported(tui(), originKey, modInfo);
                    case replaceTxt -> doReplaceText(originKey, modInfo, copyFrom, prop, target);
                    // Arrays
                    case prependArr, appendArr, replaceArr, replaceOrAppendArr, appendIfNotExistsArr, insertArr, removeArr ->
                        doModArray(
                            originKey, mode, modInfo, prop, target);
                    // Properties
                    case setProp -> doSetProp(originKey, modInfo, prop, target);
                    // Bestiary
                    case addSenses -> doAddSenses(originKey, modInfo, copyFrom, target); // no prop
                    case addSaves -> mode.notSupported(tui(), originKey, modInfo);
                    case addSkills -> doAddSkills(originKey, modInfo, target); // no prop
                    case addAllSaves -> mode.notSupported(tui(), originKey, modInfo);
                    case addAllSkills -> mode.notSupported(tui(), originKey, modInfo);
                    case addSpells -> doAddSpells(originKey, modInfo, copyFrom, target); // no prop
                    case replaceSpells -> doReplaceSpells(originKey, modInfo, copyFrom, target); // no prop
                    case removeSpells -> doRemoveSpells(originKey, modInfo, copyFrom, target); // no prop
                    // MATH
                    case calculateProp -> mode.notSupported(tui(), originKey, modInfo);
                    case scalarAddProp -> doScalarAddProp(originKey, modInfo, prop, target);
                    case scalarMultProp -> doScalarMultProp(originKey, modInfo, prop, target);
                    case scalarMultXp -> doScalarMultXp(originKey, modInfo, target); // no prop
                    case scalarAddDc -> doScalarAddDc(originKey, modInfo, prop, target);
                    case scalarAddHit -> doScalarAddHit(originKey, modInfo, prop, target);
                    case maxSize -> doMaxSize(originKey, modInfo, target); // no prop
                    default -> tui().errorf("Error (%s): Unknown modification mode: %s", originKey, modInfo);
                }
            }
        }
    }

    void doAppendText(String originKey, JsonNode modInfo, JsonNode copyFrom, String prop, ObjectNode target) {
        if (target.has(prop)) {
            String joiner = MetaFields.joiner.getTextOrEmpty(modInfo);
            target.put(prop, getTextOrEmpty(target, prop) + joiner
                + MetaFields.str.getTextOrEmpty(modInfo));
        } else {
            target.put(prop, MetaFields.str.getTextOrEmpty(modInfo));
        }
    }

    void doReplaceText(String originKey, JsonNode modInfo, JsonNode copyFrom, String prop, ObjectNode target) {
        if (!target.has(prop)) {
            return;
        }
        if (!target.get(prop).isArray()) {
            tui().warnf("replaceTxt for %s with a property %s that is not an array %s: %s", originKey, prop, modInfo,
                target.get(prop));
            return;
        }

        String replace = MetaFields.replace.getTextOrEmpty(modInfo);
        String with = MetaFields.with.getTextOrEmpty(modInfo);
        JsonNode flags = MetaFields.flags.getFrom(modInfo);

        final Pattern pattern;
        if (flags != null) {
            int pFlags = 0;
            if (flags.asText().contains("i")) {
                pFlags |= Pattern.CASE_INSENSITIVE;
            }
            pattern = Pattern.compile("\\b" + replace, pFlags);
        } else {
            pattern = Pattern.compile("\\b" + replace);
        }

        final boolean findPlainText;
        final List<String> propNames;
        JsonNode props = MetaFields.props.getFrom(modInfo);
        if (props == null) {
            findPlainText = true;
            propNames = List.of("entries", "headerEntries", "footerEntries");
        } else if (props.isEmpty()) {
            tui().warnf("replaceText with empty props in %s: %s", originKey, modInfo);
            return;
        } else {
            propNames = new ArrayList<>();
            props.forEach(x -> propNames.add(x.isNull() ? "null" : x.asText()));
            findPlainText = propNames.remove("null");
        }

        ArrayNode tgtArray = target.withArray(prop);
        for (int i = 0; i < tgtArray.size(); i++) {
            JsonNode it = tgtArray.get(i);
            if (it.isTextual() && findPlainText) {
                tgtArray.set(i, copyReplaceText(it, pattern, with));
            } else if (it.isObject()) {
                for (String k : propNames) {
                    if (it.has(k)) {
                        ((ObjectNode) it).set(k, copyReplaceText(it.get(k), pattern, with));
                    }
                }
            }
        }
    }

    private JsonNode copyReplaceText(JsonNode sourceNode, Pattern replace, String with) {
        String modified = replace.matcher(sourceNode.toString()).replaceAll(with);
        return createNode(modified);
    }

    ArrayNode sortArrayNode(ArrayNode array) {
        if (array == null || array.size() <= 1) {
            return array;
        }
        Set<JsonNode> elements = new TreeSet<>(Comparator.comparing(a -> a.asText().toLowerCase()));
        array.forEach(elements::add);
        ArrayNode sorted = mapper().createArrayNode();
        sorted.addAll(elements);
        return sorted;
    }

    private void doSetProp(String originKey, JsonNode modInfo, String prop, ObjectNode target) {
        List<String> propPath = List.of(MetaFields.prop.getTextOrEmpty(modInfo).split("\\."));
        if (!"*".equals(prop)) {
            propPath = new ArrayList<>(propPath);
            propPath.add(0, prop);
        }
        String last = propPath.remove(propPath.size() - 1);

        ObjectNode targetRw = ((ObjectNode) target).withObject("/" + String.join("/", propPath));
        targetRw.set(last, copyNode(MetaFields.value.getFrom(modInfo)));
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

    private void doScalarAddProp(String originKey, JsonNode modInfo, String prop, ObjectNode target) {
        if (!target.has(prop)) {
            return;
        }
        ObjectNode propRw = (ObjectNode) target.get(prop);
        int scalar = MetaFields.scalar.getFrom(modInfo).asInt();
        Consumer<String> scalarAdd = (k) -> {
            JsonNode node = propRw.get(k);
            boolean isString = node.isTextual();
            int value = isString
                ? Integer.parseInt(node.asText())
                : node.asInt();
            value += scalar;
            propRw.replace(k, isString
                ? new TextNode(asModifier(value))
                : new IntNode(value));
        };

        String modProp = MetaFields.prop.getTextOrNull(modInfo);
        if ("*".equals(modProp)) {
            for (String fieldName : iterableFieldNames(propRw)) {
                scalarAdd.accept(fieldName);
            }
        } else {
            scalarAdd.accept(modProp);
        }
    }

    private void doScalarMultProp(String originKey, JsonNode modInfo, String prop, ObjectNode target) {
        if (!target.has(prop)) {
            return;
        }
        ObjectNode propRw = (ObjectNode) target.get(prop);
        double scalar = MetaFields.scalar.getFrom(modInfo).asDouble();
        boolean floor = MetaFields.floor.booleanOrDefault(modInfo, false);
        Consumer<String> scalarMult = (k) -> {
            JsonNode node = propRw.get(k);
            boolean isString = node.isTextual();
            double value = isString
                ? Double.parseDouble(node.asText())
                : node.asDouble();
            value *= scalar;
            if (floor) {
                value = Math.floor(value);
            }
            propRw.replace(k, isString
                ? new TextNode(asModifier(value))
                : new DoubleNode(value));
        };

        String modProp = MetaFields.prop.getTextOrNull(modInfo);
        if ("*".equals(modProp)) {
            for (String fieldName : iterableFieldNames(propRw)) {
                scalarMult.accept(fieldName);
            }
        } else {
            scalarMult.accept(modProp);
        }
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
            int modRange = MetaFields.range.getIntOrThrow(modSense);
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
            int mode = MetaFields.mode.getIntOrThrow(entry.getValue());
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

    void doModArray(String originKey, ModFieldMode mode, JsonNode modInfo, String prop, ObjectNode target) {
        JsonNode items = ensureArray(MetaFields.items.getFrom(modInfo));
        switch (mode) {
            case prependArr -> {
                ArrayNode tgtArray = target.withArray(prop);
                insertIntoArray(tgtArray, 0, items);
            }
            case appendArr -> {
                ArrayNode tgtArray = target.withArray(prop);
                appendToArray(tgtArray, items);
            }
            case appendIfNotExistsArr -> {
                ArrayNode tgtArray = target.withArray(prop);
                appendIfNotExistsArr(tgtArray, items);
            }
            case insertArr -> {
                if (!target.has(prop)) {
                    tui().errorf("Error (%s): Unable to insert into array; %s is not present: %s", originKey, prop, target);
                    return;
                }
                ArrayNode tgtArray = target.withArray(prop);
                int index = MetaFields.index.intOrDefault(modInfo, -1);
                if (index < 0) {
                    index = tgtArray.size();
                }
                insertIntoArray(tgtArray, index, items);
            }
            case removeArr -> {
                if (!target.has(prop)) {
                    tui().errorf("Error (%s): Unable to remove from array; %s is not present: %s", originKey, prop, target);
                    return;
                }
                ArrayNode tgtArray = target.withArray(prop);
                removeFromArray(originKey, modInfo, prop, tgtArray);
            }
            case replaceArr -> {
                if (!target.has(prop)) {
                    tui().errorf("Error (%s): Unable to replace array; %s is not present: %s", originKey, prop, target);
                    return;
                }
                ArrayNode tgtArray = target.withArray(prop);
                replaceArray(originKey, modInfo, tgtArray, items);
            }
            case replaceOrAppendArr -> {
                ArrayNode tgtArray = target.withArray(prop);
                boolean didReplace = false;
                if (tgtArray.size() > 0) {
                    didReplace = replaceArray(originKey, modInfo, tgtArray, items);
                }
                if (!didReplace) {
                    appendToArray(tgtArray, items);
                }
            }
            default -> tui().errorf("Error (%s): Unknown modification mode for property %s: %s", originKey, prop, modInfo);
        }
    }

    void appendToArray(ArrayNode tgtArray, JsonNode items) {
        if (items == null) {
            return;
        }
        if (items.isArray()) {
            tgtArray.addAll((ArrayNode) items);
        } else {
            tgtArray.add(items);
        }
    }

    void insertIntoArray(ArrayNode tgtArray, int index, JsonNode items) {
        if (items == null) {
            return;
        }
        if (items.isArray()) {
            // iterate backwards so that items end up in the right order @ desired index
            for (int i = items.size() - 1; i >= 0; i--) {
                tgtArray.insert(index, items.get(i));
            }
        } else {
            tgtArray.insert(index, items);
        }
    }

    void appendIfNotExistsArr(ArrayNode tgtArray, JsonNode items) {
        if (items == null) {
            return;
        }
        if (tgtArray.size() == 0) {
            appendToArray(tgtArray, items);
        } else {
            // Remove inbound items that already exist in the target array
            // Use anyMatch to stop filtering ASAP
            List<JsonNode> filtered = streamOf(items)
                .filter(it -> !streamOf(tgtArray).anyMatch(it::equals))
                .collect(Collectors.toList());
            tgtArray.addAll(filtered);
        }
    }

    void removeFromArray(String originKey, JsonNode modInfo, String prop, ArrayNode tgtArray) {
        JsonNode names = ensureArray(MetaFields.names.getFrom(modInfo));
        JsonNode items = ensureArray(MetaFields.items.getFrom(modInfo));
        if (names != null) {
            for (JsonNode name : iterableElements(names)) {
                int index = findIndexByName(originKey, tgtArray, name.asText());
                if (index >= 0) {
                    tgtArray.remove(index);
                } else if (!MetaFields.force.booleanOrDefault(modInfo, false)) {
                    tui().errorf("Error (%s / %s): Unable to remove %s; %s", originKey, prop, name.asText(), modInfo);
                }
            }
        } else if (items != null) {
            removeFromArr(tgtArray, items);
        } else {
            tui().errorf("Error (%s / %s): One of names or items must be provided to remove elements from array; %s", originKey,
                prop, modInfo);
        }
    }

    void removeFromArr(ArrayNode tgtArray, JsonNode items) {
        for (JsonNode itemToRemove : iterableElements(items)) {
            int index = findIndex(tgtArray, itemToRemove);
            if (index >= 0) {
                tgtArray.remove(index);
            }
        }
    }

    boolean replaceArray(String originKey, JsonNode modInfo, ArrayNode tgtArray, JsonNode items) {
        if (items == null || !items.isArray()) {
            return false;
        }
        JsonNode replace = MetaFields.replace.getFrom(modInfo);

        final int index;
        if (replace.isTextual()) {
            index = findIndexByName(originKey, tgtArray, replace.asText());
        } else if (replace.isObject() && MetaFields.index.existsIn(replace)) {
            index = MetaFields.index.intOrDefault(replace, 0);
        } else if (replace.isObject() && MetaFields.regex.existsIn(replace)) {
            Pattern pattern = Pattern.compile("\\b" + MetaFields.regex.getTextOrEmpty(replace));
            index = matchFirstIndexByName(originKey, tgtArray, pattern);
        } else {
            tui().errorf("Error (%s): Unknown replace; %s", originKey, modInfo);
            return false;
        }

        if (index >= 0) {
            tgtArray.remove(index);
            insertIntoArray(tgtArray, index, items);
            return true;
        }
        return false;
    }

    int matchFirstIndexByName(String originKey, ArrayNode haystack, Pattern needle) {
        for (int i = 0; i < haystack.size(); i++) {
            final String toMatch;
            if (haystack.get(i).isObject()) {
                toMatch = SourceField.name.getTextOrEmpty(haystack.get(i));
            } else if (haystack.get(i).isTextual()) {
                toMatch = haystack.asText();
            } else {
                continue;
            }
            if (!toMatch.isBlank() && needle.matcher(toMatch).find()) {
                return i;
            }
        }
        return -1;
    }

    int findIndexByName(String originKey, ArrayNode haystack, String needle) {
        for (int i = 0; i < haystack.size(); i++) {
            final String toMatch;
            if (haystack.get(i).isObject()) {
                toMatch = SourceField.name.getTextOrEmpty(haystack.get(i));
            } else if (haystack.get(i).isTextual()) {
                toMatch = haystack.get(i).asText();
            } else {
                continue;
            }
            if (needle.equals(toMatch)) {
                return i;
            }
        }
        return -1;
    }

    int findIndex(ArrayNode haystack, JsonNode needle) {
        for (int i = 0; i < haystack.size(); i++) {
            if (haystack.get(i).equals(needle)) {
                return i;
            }
        }
        return -1;
    }

    private String getShortName(JsonNode target, boolean isTitleCase) {
        String name = SourceField.name.getTextOrEmpty(target);
        JsonNode shortName = MonsterFields.shortName.getFrom(target);
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

    private String getShortNameFromName(String name, boolean isNamedCreature) {
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
