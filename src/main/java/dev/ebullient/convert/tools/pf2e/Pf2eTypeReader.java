package dev.ebullient.convert.tools.pf2e;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.NodeReader;

public interface Pf2eTypeReader extends JsonSource {

    enum Pf2eTypeTradition implements NodeReader.FieldValue {
        arcane,
        divine,
        occult,
        primal;

        @Override
        public String value() {
            return this.name();
        }
    }

    enum Pf2eSpellSchoolTitle implements NodeReader.FieldValue {
        abjuration,
        conjuration,
        divination,
        enchantment,
        evocation,
        illusion,
        necromancy,
        transmutation;

        @Override
        public String value() {
            return this.name();
        }
    }

    enum Pf2eSpellcastingType implements NodeReader.FieldValue {
        innate,
        prepared,
        focus;

        @Override
        public String value() {
            return this.name();
        }
    }

    enum Pf2eSpellAreaType implements NodeReader.FieldValue {
        burst,
        emanation,
        cone,
        cylinder,
        line,
        misc;

        @Override
        public String value() {
            return this.name();
        }
    }

    enum Pf2eSpellComponent implements NodeReader.FieldValue, JsonTextReplacement {
        focus("F"),
        material("M"),
        somatic("S"),
        verbal("V");

        String encoding;

        Pf2eSpellComponent(String encoding) {
            this.encoding = encoding;
        }

        @Override
        public String value() {
            return encoding;
        }

        public String linkify(String rulesRoot) {
            return String.format("[%s](%s)",
                    toTitleCase(this.name()), getRulesPath(rulesRoot));
        }

        public String getRulesPath(String rulesRoot) {
            return String.format("%sTODO.md#%s",
                    rulesRoot, this.name().replace(" ", "%20")
                            .replace(".", ""));
        }

        static Pf2eSpellComponent valueFromEncoding(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            return Stream.of(Pf2eSpellComponent.values())
                    .filter((t) -> t.encoding.equals(value) || t.name().equalsIgnoreCase(value))
                    .findFirst().orElse(null);
        }

        @Override
        public Pf2eIndex index() {
            throw new IllegalStateException("Don't call this method");
        }

        @Override
        public Pf2eSources getSources() {
            throw new IllegalStateException("Don't call this method");
        }
    }

    enum Pf2eSavingThrowType implements NodeReader.FieldValue {
        fortitude("F"),
        reflex("R"),
        will("W");

        String encoding;

        Pf2eSavingThrowType(String encoding) {
            this.encoding = encoding;
        }

        @Override
        public String value() {
            return encoding;
        }

        static Pf2eSavingThrowType valueFromEncoding(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            return Stream.of(Pf2eSavingThrowType.values())
                    .filter((t) -> t.encoding.equals(value) || t.name().equalsIgnoreCase(value))
                    .findFirst().orElse(null);
        }
    }

    static class NumberUnitEntry {
        public Integer number;
        public String unit;
        public String entry;

        public String convertToDurationString(Pf2eTypeReader convert) {
            if (entry != null) {
                return convert.replaceText(entry);
            }
            Pf2eTypeActivity activity = Pf2eTypeActivity.toActivity(unit, number);
            if (activity != null && activity != Pf2eTypeActivity.timed) {
                return activity.linkify(convert.cfg().rulesRoot());
            }
            return String.format("%s %s%s", number, unit, number > 1 ? "s" : "");
        }

        public String convertToRangeString(Pf2eTypeReader convert) {
            if (entry != null) {
                return convert.replaceText(entry);
            }
            if ("feet".equals(unit)) {
                return String.format("%s %s", number, number > 1 ? "foot" : "feet");
            } else if ("miles".equals(unit)) {
                return String.format("%s %s", number, number > 1 ? "mile" : "miles");
            }
            return unit;
        }
    }

    default List<String> transformListFrom(JsonNode node, Field field) {
        List<String> list = field.getListOfStrings(node, tui());
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream().map(s -> replaceText(s)).collect(Collectors.toList());
    }

    default String transformTextFrom(JsonNode node, NodeReader field, String join) {
        List<String> list = field.getListOfStrings(node, tui());
        if (list == null || list.isEmpty()) {
            return null;
        }
        return list.stream().map(s -> replaceText(s)).collect(Collectors.joining(join));
    }

    default String getOrdinalForm(String level) {
        switch (level) {
            case "1":
                return "1st";
            case "2":
                return "2nd";
            case "3":
                return "3rd";
            default:
                return level + "th";
        }
    }

    default String getFrequency(JsonNode node) {
        JsonNode frequency = Field.frequency.getFrom(node);
        if (frequency == null) {
            return null;
        }
        String special = Field.special.getTextOrNull(frequency);
        if (special != null) {
            return special;
        }

        String number = numberToText(frequency, Field.freq, true);
        String unit = Field.unit.getTextOrEmpty(frequency);
        String customUnit = Field.customUnit.getTextOrDefault(frequency, unit);
        Optional<Integer> interval = Field.interval.getIntFrom(frequency);
        boolean overcharge = Field.overcharge.booleanOrDefault(frequency, false);
        boolean recurs = Field.recurs.booleanOrDefault(frequency, false);

        return String.format("%s %s %s%s%s",
                number,
                recurs ? "every" : "per",
                interval.map(integer -> integer + " ").orElse(""),
                interval.isPresent() && interval.get() > 2 ? unit + "s" : customUnit,
                overcharge ? ", plus overcharge" : "");
    }

    default String numberToText(JsonNode baseNode, Field field, boolean freq) {
        JsonNode node = field.getFrom(baseNode);
        if (node == null) {
            throw new IllegalArgumentException("undefined or null object");
        } else if (node.isTextual()) {
            return node.asText();
        }
        int number = node.asInt();
        String numString = intToString(number, freq);

        return (number < 0 ? "negative " : "") + numString;
    }

    default String intToString(int number, boolean freq) {
        int abs = Math.abs(number);
        if (abs >= 100) {
            return abs + "";
        }
        switch (abs) {
            case 0:
                return "zero";
            case 1:
                return freq ? "once" : "one";
            case 2:
                return freq ? "twice" : "two";
            case 3:
                return "three";
            case 4:
                return "four";
            case 5:
                return "five";
            case 6:
                return "six";
            case 7:
                return "seven";
            case 8:
                return "eight";
            case 9:
                return "nine";
            case 10:
                return "ten";
            case 11:
                return "eleven";
            case 12:
                return "twelve";
            case 13:
                return "thirteen";
            case 14:
                return "fourteen";
            case 15:
                return "fifteen";
            case 16:
                return "sixteen";
            case 17:
                return "seventeen";
            case 18:
                return "eighteen";
            case 19:
                return "nineteen";
            case 20:
                return "twenty";
            case 30:
                return "thirty";
            case 40:
                return "forty";
            case 50:
                return "fifty";
            case 60:
                return "sixty";
            case 70:
                return "seventy";
            case 80:
                return "eighty";
            case 90:
                return "ninety";
            default:
                int r = abs % 10;
                return intToString(abs - r, freq) + "-" + intToString(r, freq);
        }
    }

}
