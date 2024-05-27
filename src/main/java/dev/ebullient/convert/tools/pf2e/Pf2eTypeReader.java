package dev.ebullient.convert.tools.pf2e;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.pluralize;

import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import io.quarkus.runtime.annotations.RegisterForReflection;

public interface Pf2eTypeReader extends JsonSource {

    enum Pf2eAlignmentValue implements Pf2eJsonNodeReader.FieldValue {
        ce("Chaotic Evil"),
        cg("Chaotic Good"),
        cn("Chaotic Neutral"),
        le("Lawful Evil"),
        lg("Lawful Good"),
        ln("Lawful Neutral"),
        n("Neutral"),
        ne("Neutral Evil"),
        ng("Neutral Good");

        final String longName;

        Pf2eAlignmentValue(String s) {
            longName = s;
        }

        @Override
        public String value() {
            return this.name();
        }

        @Override
        public boolean matches(String value) {
            return this.value().equalsIgnoreCase(value) || this.longName.equalsIgnoreCase(value);
        }

        public static Pf2eAlignmentValue fromString(String name) {
            if (name == null) {
                return null;
            }
            return Stream.of(Pf2eAlignmentValue.values())
                    .filter(t -> t.matches(name))
                    .findFirst().orElse(null);
        }
    }

    enum Pf2eSpell implements Pf2eJsonNodeReader {
        amp,
        area,
        basic,
        cast,
        components, // nested array
        cost,
        domains,
        duration,
        focus,
        heightened,
        hidden,
        level,
        plusX, // heightened
        primaryCheck, // ritual
        range,
        savingThrow,
        secondaryCasters, //ritual
        secondaryCheck, // ritual
        spellLists,
        subclass,
        targets,
        traditions,
        trigger,
        type,
        X; // heightened

        List<String> getNestedListOfStrings(JsonNode source, Tui tui) {
            JsonNode result = source.get(this.nodeName());
            if (result == null) {
                return List.of();
            } else if (result.isTextual()) {
                return List.of(result.asText());
            } else {
                JsonNode first = result.get(0);
                return getListOfStrings(first, tui);
            }
        }
    }

    enum Pf2eSavingThrowType implements Pf2eJsonNodeReader.FieldValue {
        fortitude,
        reflex,
        will;

        @Override
        public String value() {
            return this.name();
        }

        @Override
        public boolean matches(String value) {
            return this.name().startsWith(value.toLowerCase());
        }

        static Pf2eSavingThrowType valueFromEncoding(String value) {
            if (!isPresent(value)) {
                return null;
            }
            return Stream.of(Pf2eSavingThrowType.values())
                    .filter(t -> t.matches(value))
                    .findFirst().orElse(null);
        }
    }

    enum Pf2eSpellComponent implements Pf2eJsonNodeReader.FieldValue {
        focus("F"),
        material("M"),
        somatic("S"),
        verbal("V");

        final String encoding;

        Pf2eSpellComponent(String encoding) {
            this.encoding = encoding;
        }

        @Override
        public String value() {
            return encoding;
        }

        @Override
        public boolean matches(String value) {
            return this.encoding.equals(value) || this.name().equalsIgnoreCase(value);
        }

        static Pf2eSpellComponent valueFromEncoding(String value) {
            if (!isPresent(value)) {
                return null;
            }
            return Stream.of(Pf2eSpellComponent.values())
                    .filter(t -> t.matches(value))
                    .findFirst().orElse(null);
        }

        public String getRulesPath(String rulesRoot) {
            return "%sTODO.md#%s".formatted(rulesRoot, toAnchorTag(this.name()));
        }
    }

    default String getOrdinalForm(String level) {
        return switch (level) {
            case "1" -> "1st";
            case "2" -> "2nd";
            case "3" -> "3rd";
            default -> level + "th";
        };
    }

    @RegisterForReflection
    class NumberUnitEntry {
        public Integer number;
        public String unit;
        public String entry;

        public String convertToDurationString(Pf2eTypeReader convert) {
            if (entry != null) {
                return convert.replaceText(entry);
            }
            Pf2eActivity activity = Pf2eActivity.toActivity(unit, number);
            if (activity != null && activity != Pf2eActivity.timed) {
                return activity.linkify(convert.cfg().rulesVaultRoot());
            }
            return "%s %s".formatted(number, pluralize(unit, number));
        }

        public String convertToRangeString(Pf2eTypeReader convert) {
            if (entry != null) {
                return convert.replaceText(entry);
            }
            if ("feet".equals(unit) || "miles".equals(unit)) {
                return "%s %s".formatted(number, pluralize(unit, number));
            }
            return unit;
        }
    }

}
