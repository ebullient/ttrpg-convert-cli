package dev.ebullient.convert.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonNodeReader.FieldValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Comparator;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Performs copy operations on nodes as a pre-processing step before they're handled by the individual converters. */
public abstract class JsonSourceCopier<T extends IndexType> implements JsonTextConverter<T> {
    public static final Pattern VARIABLE_SUBST_PAT = Pattern.compile("<\\$(?<variable>[^$]+)\\$>");

    /** Return the original node for the given key. */
    protected abstract JsonNode getOriginNode(String key);

    /** Return true if the merge rules indicate that this key should be preserved. */
    protected abstract boolean mergePreserveKey(T type, String key);

    /** Return the props to use when a copy mod is applied with a path of {@code "*"}. */
    protected abstract List<String> getCopyEntryProps();

    /**
     * Handle dynamic variables embedded within copy mods info.
     *
     * @param value JsonNode to be checked for values to replace
     * @param target JsonNode with attributes that can be used to resolve templates
     * @param variableMode The particular operation to use when resolving variables
     * @param params Parameters for the variable mode
     */
    protected abstract JsonNode resolveDynamicVariable(
        String originKey, JsonNode value, JsonNode target, TemplateVariable variableMode, String[] params);

    /** Handle any {@code _copy} fields which are present in the given node. This is the main entry point. */
    public JsonNode handleCopy(T type, JsonNode copyTo) {
        String copyToKey = type.createKey(copyTo);
        JsonNode _copy = MetaFields._copy.getFrom(copyTo);
        if (_copy != null) {
            String copyFromKey = type.createKey(_copy);
            JsonNode copyFrom = getOriginNode(copyFromKey);
            if (copyToKey.equals(copyFromKey)) {
                tui().errorf("Error (%s): Self-referencing copy. This is a data entry error. %s", copyToKey, _copy);
                return copyTo;
            }
            if (copyFrom == null) {
                tui().errorf("Error (%s): Unable to find source for %s", copyToKey, copyFromKey);
                return copyTo;
            }
            // is the copy a copy?
            copyFrom = handleCopy(type, copyFrom);
            try {
                copyTo = mergeNodes(type, copyToKey, copyFrom, copyTo);
            } catch (JsonCopyException | StackOverflowError | UnsupportedOperationException e) {
                tui().errorf(e, "Error (%s): Unable to merge nodes. CopyTo: %s, CopyFrom: %s", copyToKey, copyTo, copyFrom);
            }
        }
        return copyTo;
    }

    protected JsonNode mergeNodes(T type, String copyToKey, JsonNode copyFrom, JsonNode copyTo) {
        return copyTo;
    }

    /**
     * Actually do the copy, copying required values from {@code copyFrom} into {@code copyTo}.
     *
     * @param _copy Node containing metadata about the copy
     */
    protected void copyValues(T type, JsonNode copyFrom, ObjectNode copyTo, JsonNode _copy) {
        JsonNode _preserve = MetaFields._preserve.getFromOrEmptyObjectNode(_copy);
        // Copy required values from...
        for (Entry<String, JsonNode> from : iterableFields(copyFrom)) {
            String k = from.getKey();
            JsonNode copyToField = copyTo.get(k);
            if (copyToField != null && copyToField.isNull()) {
                // copyToField exists as `null`. Remove the field.
                copyTo.remove(k);
                continue;
            }
            if (copyToField == null) {
                // not already present in copyTo -- should we copyFrom?
                // Do merge rules indicate the value should be preserved
                if (mergePreserveKey(type, k)) {
                    // Does metadata indicate that it should be copied?
                    if (metaPreserveKey(_preserve, k)) {
                        copyTo.set(k, copyNode(from.getValue()));
                    }
                } else {
                    // in general, yes.
                    copyTo.set(k, copyNode(from.getValue()));
                }
            }
        }
    }

    /**
     * Apply modifiers to the {@code target}.
     *
     * @param originKey The key used to retrieve the target
     * @param copyFrom The node that is being copied from
     * @param target The target that the modifiers apply to
     * @param _copy Metadata about the copy that contains mod data
     */
    protected void applyMods(String originKey, JsonNode copyFrom, ObjectNode target, JsonNode _copy) {
        if (!MetaFields._mod.existsIn(_copy)) {
            return;
        }
        // pre-convert any dynamic text
        JsonNode copyMetaMod = MetaFields._mod.getFrom(_copy);
        for (Entry<String, JsonNode> entry : iterableFields(copyMetaMod)) {
            // use the target value as the attribute source for resolving dynamic text
            entry.setValue(resolveDynamicText(originKey, entry.getValue(), target));
        }

        // Now iterate and apply mod rules
        for (Entry<String, JsonNode> entry : iterableFields(copyMetaMod)) {
            String prop = entry.getKey();
            JsonNode modInfos = entry.getValue();
            if ("*".equals(prop)) {
                doMod(originKey, target, copyFrom, modInfos, getCopyEntryProps());
            } else if ("_".equals(prop)) {
                doMod(originKey, target, copyFrom, modInfos, null);
            } else {
                doMod(originKey, target, copyFrom, modInfos, List.of(prop));
            }
        }
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

    /** Apply a specific mod property. Print an error if the mod mode is not known. */
    protected void doModProp(
            String originKey, JsonNode modInfo, JsonNode copyFrom, String prop, ObjectNode target, ModFieldMode mode) {
        switch (mode) {
            // Strings & text
            case appendStr -> doAppendText(originKey, modInfo, copyFrom, prop, target);
            case replaceTxt -> doReplaceText(originKey, modInfo, copyFrom, prop, target);
            // Properties
            case setProp -> doSetProp(originKey, modInfo, prop, target);
            // Arrays
            case prependArr, appendArr, replaceArr, replaceOrAppendArr, appendIfNotExistsArr, insertArr, removeArr ->
                doModArray(originKey, mode, modInfo, prop, target);
            // MATH
            case scalarAddProp -> doScalarAddProp(originKey, modInfo, prop, target);
            case scalarMultProp -> doScalarMultProp(originKey, modInfo, prop, target);
            default -> tui().errorf("Error (%s): Unknown modification mode: %s", originKey, modInfo);
        }
    }

    protected void doModProp(String originKey, JsonNode modInfos, JsonNode copyFrom, String prop, ObjectNode target) {
        for (JsonNode modInfo : iterableElements(modInfos)) {
            if (modInfo.isTextual()) {
                if ("remove".equals(modInfo.asText()) && prop != null) {
                    target.remove(prop);
                } else {
                    tui().errorf("Error(%s): Unknown text modification mode for %s: %s", originKey, prop, modInfo);
                }
            } else {
                doModProp(originKey, modInfo, copyFrom, prop, target, ModFieldMode.getModMode(modInfo));
            }
        }
    }

    private static boolean metaPreserveKey(JsonNode _preserve, String key) {
        return _preserve != null && (_preserve.has("*") || _preserve.has(key));
    }

    protected void normalizeMods(JsonNode copyMeta) {
        if (MetaFields._mod.existsIn(copyMeta)) {
            ObjectNode mods = (ObjectNode) MetaFields._mod.getFrom(copyMeta);
            for (String name : iterableFieldNames(mods)) {
                JsonNode mod = mods.get(name);
                if (!mod.isArray()) {
                    mods.set(name, mapper().createArrayNode().add(mod));
                }
            }
        }
    }

    /** Indicate that the given node is a copy, and remove copy metadata to avoid revisit. */
    protected void cleanupCopy(ObjectNode target, JsonNode copyFrom) {
        MetaFields._isCopy.setIn(target, true);
        MetaFields._rawName.removeFrom(target);
        MetaFields._copiedFrom.setIn(target, String.format("%s (%s)",
            SourceField.name.getTextOrEmpty(copyFrom),
            SourceField.source.getTextOrEmpty(copyFrom)));
        MetaFields._copy.removeFrom(target);
    }

    // DataUtil.generic.variableResolver
    /**
     * @param originKey The origin key used to get the target from the index
     * @param value JsonNode to be checked for values to replace
     * @param target JsonNode with attributes that can be used to resolve templates
     */
    protected JsonNode resolveDynamicText(String originKey, JsonNode value, JsonNode target) {
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
        Matcher matcher = VARIABLE_SUBST_PAT.matcher(value.toString());
        if (matcher.find()) {
            String[] params = matcher.group("variable").split("__");
            TemplateVariable variableMode = TemplateVariable.valueFrom(params[0]);
            return resolveDynamicVariable(
                originKey, value, target, variableMode, Arrays.copyOfRange(params, 1, params.length));
        }
        return value;
    }

    private void doSetProp(String originKey, JsonNode modInfo, String prop, ObjectNode target) {
        List<String> propPath = new ArrayList<>(List.of(MetaFields.prop.getTextOrEmpty(modInfo).split("\\.")));
        if (!"*".equals(prop)) {
            propPath.add(0, prop);
        }
        String last = propPath.remove(propPath.size() - 1);

        ObjectNode targetRw = target.withObject("/" + String.join("/", propPath));
        targetRw.set(last, copyNode(MetaFields.value.getFrom(modInfo)));
    }

    private void doAppendText(String originKey, JsonNode modInfo, JsonNode copyFrom, String prop, ObjectNode target) {
        if (target.has(prop)) {
            String joiner = MetaFields.joiner.getTextOrEmpty(modInfo);
            target.put(prop, target.get(prop).asText() + joiner
                + MetaFields.str.getTextOrEmpty(modInfo));
        } else {
            target.put(prop, MetaFields.str.getTextOrEmpty(modInfo));
        }
    }

    private void doReplaceText(String originKey, JsonNode modInfo, JsonNode copyFrom, String prop, ObjectNode target) {
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
                ? new TextNode("%+d".formatted(value))
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
                ? new TextNode("%+f".formatted(value))
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

    private void doModArray(String originKey, ModFieldMode mode, JsonNode modInfo, String prop, ObjectNode target) {
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

    protected ArrayNode sortArrayNode(ArrayNode array) {
        if (array == null || array.size() <= 1) {
            return array;
        }
        Set<JsonNode> elements = new TreeSet<>(Comparator.comparing(a -> a.asText().toLowerCase()));
        array.forEach(elements::add);
        ArrayNode sorted = mapper().createArrayNode();
        sorted.addAll(elements);
        return sorted;
    }

    protected void appendToArray(ArrayNode tgtArray, JsonNode items) {
        if (items == null) {
            return;
        }
        if (items.isArray()) {
            tgtArray.addAll((ArrayNode) items);
        } else {
            tgtArray.add(items);
        }
    }

    protected void insertIntoArray(ArrayNode tgtArray, int index, JsonNode items) {
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

    public void appendIfNotExistsArr(ArrayNode tgtArray, JsonNode items) {
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

    protected void removeFromArray(String originKey, JsonNode modInfo, String prop, ArrayNode tgtArray) {
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

    public void removeFromArr(ArrayNode tgtArray, JsonNode items) {
        for (JsonNode itemToRemove : iterableElements(items)) {
            int index = findIndex(tgtArray, itemToRemove);
            if (index >= 0) {
                tgtArray.remove(index);
            }
        }
    }

    protected boolean replaceArray(String originKey, JsonNode modInfo, ArrayNode tgtArray, JsonNode items) {
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

    private int matchFirstIndexByName(String originKey, ArrayNode haystack, Pattern needle) {
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

    protected int findIndexByName(String originKey, ArrayNode haystack, String needle) {
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

    private int findIndex(ArrayNode haystack, JsonNode needle) {
        for (int i = 0; i < haystack.size(); i++) {
            if (haystack.get(i).equals(needle)) {
                return i;
            }
        }
        return -1;
    }

    public enum MetaFields implements JsonNodeReader {
        _copy,
        _copiedFrom, // mind
        _isCopy,
        _mod,
        _preserve,
        _rawName,
        _root,
        _templates,
        alias,
        apply,
        data,
        dex,
        dex_mod,
        flags,
        floor,
        force,
        index,
        items,
        joiner,
        max,
        mode,
        names,
        overwrite,
        prof_bonus,
        prop,
        props,
        range,
        regex,
        replace,
        root,
        scalar,
        skills,
        str,
        type,
        value,
        with,
    }

    public enum TemplateVariable implements JsonNodeReader.FieldValue {
        name,
        short_name,
        title_short_name,
        dc,
        spell_dc,
        to_hit,
        damage_mod,
        damage_avg;

        public void notSupported(Tui tui, String originKey, JsonNode variableText) {
            tui.errorf("Error (%s): Support for %s must be implemented. Raise an issue with this message. Text: %s",
                originKey, this.value(), variableText);
        }

        public static TemplateVariable valueFrom(String value) {
            return FieldValue.valueFrom(value, TemplateVariable.class);
        }
    }

    public enum ModFieldMode implements JsonNodeReader.FieldValue {
        appendStr,
        replaceName,
        replaceTxt,

        prependArr,
        appendArr,
        replaceArr,
        replaceOrAppendArr,
        appendIfNotExistsArr,
        insertArr,
        removeArr,

        calculateProp,
        scalarAddProp,
        scalarMultProp,
        setProp,

        addSenses,
        addSaves,
        addSkills,
        addAllSaves,
        addAllSkills,

        addSpells,
        removeSpells,
        replaceSpells,

        maxSize,
        scalarMultXp,
        scalarAddHit,
        scalarAddDc;

        public void notSupported(Tui tui, String originKey, JsonNode modInfo) {
            tui.errorf("Error (%s): %s must be implemented. Raise an issue with this message. modInfo: %s",
                originKey, this.value(), modInfo);
        }

        public static ModFieldMode getModMode(JsonNode source) {
            return FieldValue.valueFrom(MetaFields.mode.getTextOrNull(source), ModFieldMode.class);
        }
    }
}
