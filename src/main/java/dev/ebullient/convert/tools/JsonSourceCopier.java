package dev.ebullient.convert.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.DoubleNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import dev.ebullient.convert.io.Tui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNullElse;

public abstract class JsonSourceCopier<T extends IndexType> implements JsonTextConverter<T> {
    static final List<String> GENERIC_WALKER_ENTRIES_KEY_BLOCKLIST = List.of(
        "caption", "type", "colLabels", "colLabelGroups",
        "name", "colStyles", "style", "shortName", "subclassShortName", "id", "path");

    static final List<String> _MERGE_REQUIRES_PRESERVE_BASE = List.of(
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
    static final List<String> COPY_ENTRY_PROPS = List.of(
            "action", "bonus", "reaction", "trait", "legendary", "mythic", "variant", "spellcasting",
            "actionHeader", "bonusHeader", "reactionHeader", "legendaryHeader", "mythicHeader");

    static final Pattern variable_subst = Pattern.compile("<\\$(?<variable>[^$]+)\\$>");

    static boolean metaPreserveKey(JsonNode _preserve, String key) {
        return _preserve != null && (_preserve.has("*") || _preserve.has(key));
    }

    protected boolean mergePreserveKey(T type, String key) {
        return _MERGE_REQUIRES_PRESERVE_BASE.contains(key);
    }

    protected abstract JsonNode getOriginNode(String key);

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

    protected abstract String getExternalTemplateKey(JsonNode trait);

    // 	utils.js: static getCopy (impl, copyFrom, copyTo, templateData,...) {
    JsonNode mergeNodes(T type, String originKey, JsonNode copyFrom, JsonNode copyTo) {
        // edit in place: if you don't, lower-level copies will keep being revisted.
        ObjectNode target = (ObjectNode) copyTo;

        JsonNode _copy = MetaFields._copy.getFrom(copyTo);
        JsonNode _mod = _copy == null ? null : normalizeMods(MetaFields._mod.getFrom(_copy));
        JsonNode _trait = _copy == null ? null : MetaFields._trait.getFrom(_copy);

        JsonNode templateApplyRoot = null;

        if (_trait != null) {
            // fetch and apply external template mods
            String templateKey = getExternalTemplateKey(_trait);
            JsonNode template = getOriginNode(templateKey);
            if (template == null) {
                tui().warn("Unable to find trait for " + templateKey);
            } else {
                template = copyNode(template); // copy fast

                JsonNode apply = MetaFields.apply.getFrom(template);
                templateApplyRoot = MetaFields._root.getFrom(apply);

                JsonNode templateMods = normalizeMods(MetaFields._mod.getFrom(apply));
                if (templateMods != null) {
                    if (_mod == null) {
                        _mod = templateMods;
                    } else {
                        ObjectNode _modRw = (ObjectNode) _mod;
                        for (Entry<String, JsonNode> e : iterableFields(templateMods)) {
                            if (_modRw.has(e.getKey())) {
                                appendToArray(_modRw.withArray(e.getKey()), e.getValue());
                            } else {
                                _modRw.set(e.getKey(), e.getValue());
                            }
                        }
                    }
                }
            }
            MetaFields._trait.removeFrom(_copy);
        }

        JsonNode _preserve = _copy == null
                ? mapper().createObjectNode()
                : MetaFields._preserve.getFrom(_copy);

        // Copy required values from...
        for (Entry<String, JsonNode> from : iterableFields(copyFrom)) {
            String k = from.getKey();
            JsonNode copyToField = copyTo.get(k);
            if (copyToField != null && copyToField.isNull()) {
                // copyToField exists as `null`. Remove the field.
                target.remove(k);
                continue;
            }
            if (copyToField == null) {
                // not already present in copyTo -- should we copyFrom?
                // Do merge rules indicate the value should be preserved
                if (mergePreserveKey(type, k)) {
                    // Does metadata indicate that it should be copied?
                    if (metaPreserveKey(_preserve, k)) {
                        target.set(k, copyNode(from.getValue()));
                    }
                } else {
                    // in general, yes.
                    target.set(k, copyNode(from.getValue()));
                }
            }
        }

        // Apply template _root properties
        List<String> copyToRootProps = streamOfFieldNames(copyTo).toList();
        if (templateApplyRoot != null) {
            for (Entry<String, JsonNode> from : iterableFields(templateApplyRoot)) {
                String k = from.getKey();
                if (!copyToRootProps.contains(k)) {
                    continue; // avoid overwriting any real properties with templates
                }
                target.set(k, copyNode(from.getValue()));
            }
        }

        // Apply mods
        if (_mod != null) {
            for (Entry<String, JsonNode> entry : iterableFields(_mod)) {
                // use the copyTo value as the attribute source for resolving dynamic text
                entry.setValue(resolveDynamicText(originKey, entry.getValue(), copyTo));
            }

            for (Entry<String, JsonNode> entry : iterableFields(_mod)) {
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
        target.put("_isCopy", true);
        target.remove("_rawName");
        MetaFields._copiedFrom.setIn(target, String.format("%s (%s)",
                SourceField.name.getTextOrEmpty(copyFrom),
                SourceField.source.getTextOrEmpty(copyFrom)));
        MetaFields._copy.removeFrom(target);
        return target;
    }

    protected abstract JsonNode resolveTemplateVariable(
        TemplateVariable variableMode, String originKey, JsonNode value, JsonNode target, String... pieces);

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
            return requireNonNullElse(
                resolveTemplateVariable(TemplateVariable.valueFrom(pieces[0]), originKey, value, target, pieces),
                value);
        }
        return value;
    }

    private JsonNode normalizeMods(JsonNode copyMeta) {
        if (copyMeta == null || !copyMeta.isObject()) {
            return copyMeta;
        }
        for (String name : iterableFieldNames(copyMeta)) {
            JsonNode mod = copyMeta.get(name);
            if (!mod.isArray()) {
                ((ObjectNode) copyMeta).set(name, mapper().createArrayNode().add(mod));
            }
        }
        return copyMeta;
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

    /**
     * Handle specific modifier properties before the common ones are handled by
     * {@link #doModProp(String, JsonNode, JsonNode, String, ObjectNode)}.
     *
     * @param mode The specific modifier to handle
     * @param originKey The key used to retrieve the original node
     * @param modInfo Holds the info about the modification
     * @param copyFrom The node to copy data from
     * @param target The node to modify
     * @return True if the mod was fully handled by this method, false if we need to fall back to the common handlers.
     */
    protected abstract boolean doModProp(
        ModFieldMode mode, String originKey, JsonNode modInfo, JsonNode copyFrom, ObjectNode target);

    void doModProp(String originKey, JsonNode modInfos, JsonNode copyFrom, String prop, ObjectNode target) {
        for (JsonNode modInfo : iterableElements(modInfos)) {
            if (modInfo.isTextual()) {
                if ("remove".equals(modInfo.asText()) && prop != null) {
                    target.remove(prop);
                } else {
                    tui().errorf("Error(%s): Unknown text modification mode for %s: %s", originKey, prop, modInfo);
                }
            } else {
                ModFieldMode mode = ModFieldMode.valueFrom(modInfo, MetaFields.mode);
                if (doModProp(mode, originKey, modInfo, copyFrom, target)) {
                    return;
                }
                // Handle common mod props not handled by the specific implementation
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
                    case addSaves, addAllSaves, addAllSkills -> mode.notSupported(tui(), originKey, modInfo);
                    // MATH
                    case calculateProp -> mode.notSupported(tui(), originKey, modInfo);
                    case scalarAddProp -> doScalarAddProp(originKey, modInfo, prop, target);
                    case scalarMultProp -> doScalarMultProp(originKey, modInfo, prop, target);
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
            target.put(prop, (target.has(prop) ? target.get(prop).asText() : "") + joiner
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
                    ? new TextNode("+%d".formatted(value))
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
                    ? new TextNode("+%f".formatted(value))
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

    private void doMaxSize(String originKey, JsonNode modInfo, ObjectNode target) {
        final String SIZES = "FDTSMLHGCV";
        if (!MetaFields.size.existsIn(target)) {
            tui().errorf("Error (%s): enforcing maxSize on an object that does not define size; %s", originKey, target);
            return;
        }

        String maxValue = MetaFields.max.getTextOrEmpty(modInfo);
        int maxIdx = SIZES.indexOf(maxValue);
        if (maxValue.isBlank() || maxIdx < 0) {
            tui().errorf("Error (%s): Invalid maxSize value %s", originKey, maxValue.isBlank() ? "(missing)" : maxValue);
            return;
        }

        ArrayNode size = MetaFields.size.ensureArrayIn(target);
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
        if (tgtArray.isEmpty()) {
            appendToArray(tgtArray, items);
        } else {
            // Remove inbound items that already exist in the target array
            // Use anyMatch to stop filtering ASAP
            List<JsonNode> filtered = streamOf(items)
                    .filter(it -> streamOf(tgtArray).noneMatch(it::equals))
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

    protected int matchFirstIndexByName(String originKey, ArrayNode haystack, Pattern needle) {
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

    protected int findIndex(ArrayNode haystack, JsonNode needle) {
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
        _mod,
        _preserve,
        _root,
        _trait,
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
        size,
        skills,
        str,
        type,
        value,
        with,
        ;
    }

    protected enum TemplateVariable implements JsonNodeReader.FieldValue {
        name,
        short_name,
        title_short_name,
        dc,
        spell_dc,
        to_hit,
        damage_mod,
        damage_avg;

        static TemplateVariable valueFrom(String value) {
            return Stream.of(TemplateVariable.values())
                    .filter((t) -> t.matches(value))
                    .findFirst().orElse(null);
        }

        @Override
        public String value() {
            return name();
        }

        public void notSupported(Tui tui, String originKey, JsonNode variableText) {
            tui.errorf("Error (%s): Support for %s must be implemented. Raise an issue with this message. Text: %s",
                    originKey, this.value(), variableText);
        }
    }

    protected enum ModFieldMode implements JsonNodeReader.FieldValue {
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

        static ModFieldMode valueFrom(JsonNode source, JsonNodeReader field) {
            String textOrNull = field.getTextOrNull(source);
            if (textOrNull == null) {
                return null;
            }
            return Stream.of(ModFieldMode.values())
                    .filter((t) -> t.matches(textOrNull))
                    .findFirst().orElse(null);
        }

        @Override
        public String value() {
            return name();
        }

        public void notSupported(Tui tui, String originKey, JsonNode modInfo) {
            tui.errorf("Error (%s): %s must be implemented. Raise an issue with this message. modInfo: %s",
                    originKey, this.value(), modInfo);
        }
    }
}
