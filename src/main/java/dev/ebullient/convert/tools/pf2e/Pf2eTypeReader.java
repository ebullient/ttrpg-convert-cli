package dev.ebullient.convert.tools.pf2e;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.pluralize;

import java.util.stream.Stream;

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
