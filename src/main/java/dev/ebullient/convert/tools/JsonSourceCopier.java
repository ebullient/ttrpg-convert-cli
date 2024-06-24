package dev.ebullient.convert.tools;

import com.fasterxml.jackson.databind.JsonNode;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonNodeReader.FieldValue;

/** Performs copy operations on nodes as a pre-processing step before they're handled by the individual converters. */
public abstract class JsonSourceCopier<T extends IndexType> implements JsonTextConverter<T> {

    /** Handle any {@code _copy} fields which are present in the given node. */
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

    /** Return the original node for the given key. */
    protected abstract JsonNode getOriginNode(String key);

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
