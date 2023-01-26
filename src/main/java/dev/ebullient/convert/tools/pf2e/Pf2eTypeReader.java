package dev.ebullient.convert.tools.pf2e;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.NodeReader;
import dev.ebullient.convert.tools.pf2e.qute.QuteActivityType;
import io.quarkus.runtime.annotations.RegisterForReflection;

public interface Pf2eTypeReader extends JsonSource {

    enum Pf2eAlignmentValue implements NodeReader.FieldValue {
        ce("Chaotic Evil"),
        cg("Chaotic Good"),
        cn("Chaotic Neutral"),
        le("Lawful Evil"),
        lg("Lawful Goo"),
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
                    .filter((t) -> t.matches(name))
                    .findFirst().orElse(null);
        }
    }

    enum Pf2eFeat implements NodeReader {
        access,
        activity,
        archetype, // child of featType
        cost,
        featType,
        leadsTo,
        level,
        prerequisites,
        special,
        trigger
    }

    enum Pf2eSavingThrowType implements NodeReader.FieldValue {
        fortitude("F"),
        reflex("R"),
        will("W");

        final String encoding;

        Pf2eSavingThrowType(String encoding) {
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

        static Pf2eSavingThrowType valueFromEncoding(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            return Stream.of(Pf2eSavingThrowType.values())
                    .filter((t) -> t.matches(value))
                    .findFirst().orElse(null);
        }
    }

    enum Pf2eAction implements NodeReader {
        activity,
        actionType,
        cost,
        info,
        prerequisites,
        trigger,

    }

    enum Pf2eSpell implements NodeReader {
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
        plusX,
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
        X;

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

    enum Pf2eSpellComponent implements NodeReader.FieldValue, JsonTextReplacement {
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
                    .filter((t) -> t.matches(value))
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

    @RegisterForReflection
    class Speed {
        public Integer walk;
        public Integer climb;
        public Integer fly;
        public Integer burrow;
        public Integer swim;
        public Integer dimensional;
        public String speedNote;

        public String speedToString(Pf2eTypeReader convert) {
            List<String> parts = new ArrayList<>();
            if (climb != null) {
                parts.add("climb " + climb + " feet");
            }
            if (fly != null) {
                parts.add("fly " + fly + " feet");
            }
            if (burrow != null) {
                parts.add("burrow " + burrow + " feet");
            }
            if (swim != null) {
                parts.add("swim " + swim + " feet");
            }

            return String.format("%s%s%s",
                    walk == null ? "no land Speed" : "Speed " + walk + " feet",
                    (walk == null || parts.isEmpty()) ? "" : ", ",
                    convert.join(", ", parts));
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

        public QuteActivityType toQuteActivity(JsonSource convert) {
            String extra = entry == null || entry.toLowerCase().contains("varies")
                    ? ""
                    : " (" + convert.replaceText(entry) + ")";

            switch (unit) {
                case "action":
                case "free":
                case "reaction":
                    Pf2eTypeActivity activity = Pf2eTypeActivity.toActivity(unit, number);
                    return createActivity(convert,
                            String.format("%s%s", activity == null ? "unknown" : activity.getCaption(), extra),
                            activity);
                case "varies":
                    return createActivity(convert,
                            String.format("%s%s", Pf2eTypeActivity.varies.getCaption(), extra),
                            Pf2eTypeActivity.varies);
                case "day":
                case "minute":
                case "hour":
                case "round":
                    return createActivity(convert,
                            String.format("%s %s%s", number, unit, extra),
                            Pf2eTypeActivity.timed);
                default:
                    throw new IllegalArgumentException("What is this? " + String.format("%s, %s, %s", number, unit, entry));
            }
        }

        QuteActivityType createActivity(JsonSource convert, String text, Pf2eTypeActivity activity) {
            String fileName = activity.getGlyph();
            int x = fileName.lastIndexOf('.');
            Path target = Path.of("img",
                    Tui.slugify(fileName.substring(0, x)) + fileName.substring(x));

            return new QuteActivityType(
                    text,
                    new ImageRef.Builder()
                            .setStreamSource(activity.getGlyph())
                            .setTargetPath(convert.index().rulesPath(), target)
                            .setMarkdownPath(activity.getCaption(), convert.index().rulesRoot())
                            .build(),
                    activity.getTextGlyph(),
                    activity.getRulesPath(convert.index().rulesRoot()));
        }
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
            return replaceText(special);
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
