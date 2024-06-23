package dev.ebullient.convert.tools;

import com.fasterxml.jackson.databind.JsonNode;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonNodeReader.FieldValue;

/** Performs copy operations on nodes as a pre-processing step before they're handled by the individual converters. */
public abstract class JsonSourceCopier<T extends IndexType> implements JsonTextConverter<T> {

    /** Handle any {@code _copy} fields which are present in the given node. */
    public abstract JsonNode handleCopy(T type, JsonNode copyTo);

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
