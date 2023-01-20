package dev.ebullient.convert.tools.pf2e;

import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

public interface Pf2eTypeReader extends JsonSource {

    enum Pf2eActivityType {
        single("Single Action", "\\[>\\]", "single_action.svg"),
        two("Two-Action activity", "\\[>>\\]", "two_actions.svg"),
        three("Three-Action activity", "\\[>>>\\]", "three_actions.svg"),
        free("Free Action", "\\[F\\]", "delay.svg"),
        reaction("Reaction", "\\[R\\]", "reaction.svg"),
        varies("Varies", "\\[?\\]", "load.svg"),
        timed("Duration or Frequency", "\\[⏲\\]", "hour-glass.svg");

        String caption;
        String textGlyph;
        String glyph;

        Pf2eActivityType(String caption, String textGlyph, String glyph) {
            this.caption = caption;
            this.textGlyph = textGlyph;
            this.glyph = glyph;
        }

        public static Pf2eActivityType toActivity(String unit, int number) {
            switch (unit) {
                case "action":
                    switch (number) {
                        case 1:
                            return single;
                        case 2:
                            return two;
                        case 3:
                            return three;
                    }
                    break;
                case "free":
                    return free;
                case "reaction":
                    return reaction;
                case "varies":
                    return varies;
                case "timed":
                    return timed;
            }
            throw new IllegalArgumentException("Unable to find Activity for " + number + " " + unit);
        }

        public String getCaption() {
            return this.caption;
        }

        public String getTextGlyph() {
            return this.textGlyph;
        }

        public String getGlyph() {
            return this.glyph;
        }
    }

    default String getFrequency(JsonNode rootNode) {
        JsonNode frequency = Field.frequency.getFrom(rootNode);
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

    default String numberToText(JsonNode rootNode, Field field, boolean freq) {
        JsonNode node = field.getFrom(rootNode);
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
