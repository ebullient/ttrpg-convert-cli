package dev.ebullient.convert.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonNodeReader.FieldValue;

import java.util.Map.Entry;

/** Performs copy operations on nodes as a pre-processing step before they're handled by the individual converters. */
public abstract class JsonSourceCopier<T extends IndexType> implements JsonTextConverter<T> {

    /** Return the original node for the given key. */
    protected abstract JsonNode getOriginNode(String key);

    /** Return true if the merge rules indicate that this key should be preserved. */
    protected abstract boolean mergePreserveKey(T type, String key);

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

    public enum MetaFields implements JsonNodeReader {
        _copy,
        _copiedFrom, // mind
        _mod,
        _preserve,
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
