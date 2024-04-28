package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataActivity;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataArmorClass;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataDefenses;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataDefenses.QuteSavingThrows;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataHpHardness;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataSkillBonus;
import dev.ebullient.convert.tools.pf2e.qute.QuteItem.QuteItemWeaponData;
import io.quarkus.runtime.annotations.RegisterForReflection;

public interface Pf2eTypeReader extends JsonSource {

    enum Pf2eAction implements JsonNodeReader {
        activity,
        actionType,
        cost,
        info,
        prerequisites,
        trigger
    }

    enum Pf2eAlignmentValue implements JsonNodeReader.FieldValue {
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
                    .filter((t) -> t.matches(name))
                    .findFirst().orElse(null);
        }
    }

    enum Pf2eWeaponData implements JsonNodeReader {
        ammunition,
        damage,
        damageType,
        damage2,
        damageType2,
        group,
        range,
        reload;

        public static QuteItemWeaponData buildWeaponData(JsonNode source,
                Pf2eTypeReader convert, Tags tags) {

            QuteItemWeaponData weaponData = new QuteItemWeaponData();
            weaponData.traits = convert.collectTraitsFrom(source, tags);
            weaponData.type = SourceField.type.getTextOrEmpty(source);
            weaponData.damage = getDamageString(source, convert);

            weaponData.ranged = new ArrayList<>();
            String ammunition = Pf2eWeaponData.ammunition.getTextOrNull(source);
            if (ammunition != null) {
                weaponData.ranged.add(new NamedText("Ammunution", convert.linkify(Pf2eIndexType.item, ammunition)));
            }
            String range = Pf2eWeaponData.range.getTextOrNull(source);
            if (range != null) {
                weaponData.ranged.add(new NamedText("Range", range + " ft."));
            }
            String reload = Pf2eWeaponData.reload.getTextOrNull(source);
            if (reload != null) {
                weaponData.ranged.add(new NamedText("Reload", convert.replaceText(reload)));
            }

            String group = Pf2eWeaponData.group.getTextOrNull(source);
            if (group != null) {
                weaponData.group = convert.linkify(Pf2eIndexType.group, group);
            }

            return weaponData;
        }

        public static String getDamageString(JsonNode source, Pf2eTypeReader convert) {
            String damage = Pf2eWeaponData.damage.getTextOrNull(source);
            String damage2 = Pf2eWeaponData.damage2.getTextOrNull(source);

            String result = "";
            if (damage != null) {
                result += convert.replaceText(String.format("{@damage %s} %s",
                        damage,
                        Pf2eWeaponData.damageType.getTextOrEmpty(source)));
            }
            if (damage2 != null) {
                result += convert.replaceText(String.format("%s{@damage %s} %s",
                        damage == null ? "" : " and ",
                        damage2,
                        Pf2eWeaponData.damageType2.getTextOrEmpty(source)));
            }
            return result;
        }

        static String getDamageType(JsonNodeReader damageType, JsonNode source) {
            String value = damageType.getTextOrEmpty(source);
            return switch (value) {
                case "A" -> "acid";
                case "B" -> "bludgeoning";
                case "C" -> "cold";
                case "D" -> "bleed";
                case "E" -> "electricity";
                case "F" -> "fire";
                case "H" -> "chaotic";
                case "I" -> "poison";
                case "L" -> "lawful";
                case "M" -> "mental";
                case "Mod" -> "modular";
                case "N" -> "sonic";
                case "O" -> "force";
                case "P" -> "piercing";
                case "R" -> "precision";
                case "S" -> "slashing";
                case "+" -> "positive";
                case "-" -> "negative";
                default -> value;
            };
        }
    }

    enum Pf2eDefenses implements JsonNodeReader {
        abilities,
        ac,
        hardness,
        hp,
        bt,
        immunities,
        note,
        notes,
        resistances,
        savingThrows,
        std,
        weaknesses;

        public static QuteDataDefenses createInlineDefenses(JsonNode source, Pf2eTypeReader convert) {
            JsonNode notesNode = notes.getFrom(source);
            if (notesNode != null) {
                convert.tui().warnf("Defenses has notes: %s", source.toString());
            }
            return new QuteDataDefenses(List.of(),
                    getAcString(source, convert),
                    getSavingThrowString(source, convert),
                    getHpHardness(source, convert), // hp hardness
                    immunities.linkifyListFrom(source, Pf2eIndexType.trait, convert), // immunities
                    getWeakResist(resistances, source, convert), // resistances
                    getWeakResist(weaknesses, source, convert));
        }

        // "hardness": {
        //     "notes": {
        //         "std": "per mirror"
        //     },
        //     "std": 13
        // },
        // "hp": {
        //     "std": 54,
        //     "Reflection ": 30
        // },
        // "bt": {
        //     "std": 27
        // },
        public static List<QuteDataHpHardness> getHpHardness(JsonNode source, Pf2eTypeReader convert) {
            // First pass: for hazards. TODO: creatures
            JsonNode btValueNode = bt.getFromOrEmptyObjectNode(source);
            JsonNode hpValueNode = hp.getFromOrEmptyObjectNode(source);
            Map<String, String> hpNotes = notes.getMapOfStrings(hpValueNode, convert.tui());
            JsonNode hardValueNode = hardness.getFromOrEmptyObjectNode(source);
            Map<String, String> hardNotes = notes.getMapOfStrings(hardValueNode, convert.tui());

            // Collect all keys
            Set<String> keys = new HashSet<>();
            hpValueNode.fieldNames().forEachRemaining(keys::add);
            btValueNode.fieldNames().forEachRemaining(keys::add);
            hardValueNode.fieldNames().forEachRemaining(keys::add);
            keys.removeIf(k -> k.equalsIgnoreCase("notes"));

            List<QuteDataHpHardness> items = new ArrayList<>();
            for (String k : keys) {
                QuteDataHpHardness qhp = new QuteDataHpHardness();
                qhp.name = k.equals("std") ? "" : k;
                qhp.hardnessNotes = convert.replaceText(hardNotes.get(k));
                qhp.hpNotes = convert.replaceText(hpNotes.get(k));
                qhp.hardnessValue = convert.replaceText(hardValueNode.get(k));
                qhp.hpValue = convert.replaceText(hpValueNode.get(k));
                qhp.brokenThreshold = convert.replaceText(btValueNode.get(k));
                items.add(qhp);
            }
            items.sort(Comparator.comparing(a -> a.name));
            return items;
        }

        public static List<String> getWeakResist(Pf2eDefenses field, JsonNode source, Pf2eTypeReader convert) {
            List<String> items = new ArrayList<>();
            for (JsonNode wr : field.iterateArrayFrom(source)) {
                NameAmountNote nmn = convert.tui().readJsonValue(wr, NameAmountNote.class);
                items.add(nmn.flatten(convert));
            }
            ;
            return items;
        }

        public static QuteDataArmorClass getAcString(JsonNode source, Pf2eTypeReader convert) {
            JsonNode acNode = ac.getFrom(source);
            if (acNode == null) {
                return null;
            }
            QuteDataArmorClass ac = new QuteDataArmorClass();
            NamedText.SortedBuilder namedText = new NamedText.SortedBuilder();
            for (Entry<String, JsonNode> e : convert.iterableFields(acNode)) {
                if (e.getKey().equals(note.name()) ||
                        e.getKey().equals(abilities.name()) ||
                        e.getKey().equals(notes.name())) {
                    continue; // skip these three
                }
                namedText.add(
                        (e.getKey().equals("std") ? "AC" : e.getKey() + " AC"),
                        "" + e.getValue());
            }
            ac.armorClass = namedText.build();
            ac.abilities = convert.replaceText(abilities.getTextOrEmpty(acNode));

            // Consolidate "note" and "notes" into different representations of the same data
            List<String> acNotes = notes.getListOfStrings(acNode, convert.tui());
            ac.notes = Stream.concat(acNotes.stream(), Stream.of(note.getTextOrEmpty(acNode)))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(convert::replaceText)
                    .toList();
            ac.note = String.join("; ", ac.notes);

            return ac;
        }

        /**
         * "savingThrows": {
         * "fort": {
         * "std": 12
         * },
         * "ref": {
         * "std": 8
         * }
         * },
         */
        public static QuteSavingThrows getSavingThrowString(JsonNode source, Pf2eTypeReader convert) {
            JsonNode stNode = savingThrows.getFrom(source);
            if (stNode == null) {
                return null;
            }
            QuteSavingThrows svt = new QuteSavingThrows();
            for (Entry<String, JsonNode> e : convert.iterableFields(stNode)) {
                int sv = std.intOrDefault(e.getValue(), 0);
                List<String> svValue = new ArrayList<>();
                svValue.add((sv >= 0 ? "+" : "") + sv);

                if (e.getValue().size() > 1) {
                    for (Entry<String, JsonNode> extra : convert.iterableFields(e.getValue())) {
                        if ("std".equals(extra.getKey())) {
                            continue;
                        }
                        if ("abilities".equals(extra.getKey())) {
                            svt.hasThrowAbilities = true;
                            svValue.addAll(convert.streamOf(extra.getValue())
                                    .map(x -> convert.replaceText(x))
                                    .toList());
                            continue;
                        }
                        int value = extra.getValue().asInt();
                        svt.savingThrows.put(convert.toTitleCase(extra.getKey()),
                                (value >= 0 ? "+" : "") + value);
                    }
                }
                svt.savingThrows.put(convert.toTitleCase(e.getKey()),
                        String.join(", ", svValue));
            }
            svt.abilities = convert.replaceText(abilities.getTextOrNull(stNode));
            return svt;
        }
    }

    enum Pf2eFeat implements JsonNodeReader {
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

    enum Pf2eSavingThrowType implements JsonNodeReader.FieldValue {
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
            if (value == null || value.isBlank()) {
                return null;
            }
            return Stream.of(Pf2eSavingThrowType.values())
                    .filter((t) -> t.matches(value))
                    .findFirst().orElse(null);
        }
    }

    enum Pf2eSpell implements JsonNodeReader {
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

    enum Pf2eSpellComponent implements JsonNodeReader.FieldValue {
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

        public String getRulesPath(String rulesRoot) {
            return String.format("%sTODO.md#%s",
                    rulesRoot, toAnchorTag(this.name()));
        }

        static Pf2eSpellComponent valueFromEncoding(String value) {
            if (value == null || value.isBlank()) {
                return null;
            }
            return Stream.of(Pf2eSpellComponent.values())
                    .filter((t) -> t.matches(value))
                    .findFirst().orElse(null);
        }
    }

    enum Pf2eSkillBonus implements JsonNodeReader {
        std,
        note;

        /**
         * Example JSON object input:
         *
         * <pre>
         * {
         *     "std": 10,
         *     "in woods": 12,
         *     "note": "some note"
         * }
         * </pre>
         *
         * @param skillName The name of the skill
         * @param source Either a single integer bonus, or an object (see above example)
         */
        public static QuteDataSkillBonus createSkillBonus(
                String skillName, JsonNode source, Pf2eTypeReader convert) {
            String displayName = Arrays.stream(skillName.split(" "))
                    .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                    .collect(Collectors.joining(" "));

            if (source.isInt()) {
                return new QuteDataSkillBonus(displayName, source.asInt());
            }

            return new QuteDataSkillBonus(
                    displayName,
                    std.getIntOrThrow(source),
                    source.properties().stream()
                            .filter(e -> !e.getKey().equals(std.name()) && !e.getKey().equals(note.name())) // skip these
                            .collect(
                                    Collectors.toUnmodifiableMap(
                                            e -> convert.replaceText(e.getKey()), e -> e.getValue().asInt())),
                    convert.replaceText(note.getTextOrNull(source)));
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

    static QuteDataActivity getQuteActivity(JsonNode source, JsonNodeReader field, JsonSource convert) {
        NumberUnitEntry jsonActivity = field.fieldFromTo(source, NumberUnitEntry.class, convert.tui());
        return jsonActivity == null ? null : jsonActivity.toQuteActivity(convert);
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

        private QuteDataActivity toQuteActivity(JsonSource convert) {
            String extra = entry == null || entry.toLowerCase().contains("varies")
                    ? ""
                    : " (" + convert.replaceText(entry) + ")";

            switch (unit) {
                case "single", "action", "free", "reaction" -> {
                    Pf2eActivity activity = Pf2eActivity.toActivity(unit, number);
                    if (activity == null) {
                        throw new IllegalArgumentException("What is this? " + String.format("%s, %s, %s", number, unit, entry));
                    }
                    return activity.toQuteActivity(convert,
                            extra.isBlank() ? null : String.format("%s%s", activity.getLongName(), extra));
                }
                case "varies" -> {
                    return Pf2eActivity.varies.toQuteActivity(convert,
                            extra.isBlank() ? null : String.format("%s%s", Pf2eActivity.varies.getLongName(), extra));
                }
                case "day", "minute", "hour", "round" -> {
                    return Pf2eActivity.timed.toQuteActivity(convert,
                            String.format("%s %s%s", number, unit, extra));
                }
                default -> throw new IllegalArgumentException(
                        "What is this? " + String.format("%s, %s, %s", number, unit, entry));
            }
        }
    }

    @RegisterForReflection
    class NameAmountNote {
        public String name;
        public Integer amount;
        public String note;

        public NameAmountNote() {
        }

        public NameAmountNote(String value) {
            note = value;
        }

        public String flatten(Pf2eTypeReader convert) {
            return name
                    + (amount == null ? "" : " " + amount)
                    + (note == null ? "" : convert.replaceText(note));
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

    default String getFrequency(JsonNode node) {
        JsonNode frequency = Field.frequency.getFrom(node);
        if (frequency == null) {
            return null;
        }
        String special = Field.special.getTextOrNull(frequency);
        if (special != null) {
            return replaceText(special);
        }

        String number = numberToText(frequency, Field.number, true);
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
            case 0 -> {
                return "zero";
            }
            case 1 -> {
                return freq ? "once" : "one";
            }
            case 2 -> {
                return freq ? "twice" : "two";
            }
            case 3 -> {
                return "three";
            }
            case 4 -> {
                return "four";
            }
            case 5 -> {
                return "five";
            }
            case 6 -> {
                return "six";
            }
            case 7 -> {
                return "seven";
            }
            case 8 -> {
                return "eight";
            }
            case 9 -> {
                return "nine";
            }
            case 10 -> {
                return "ten";
            }
            case 11 -> {
                return "eleven";
            }
            case 12 -> {
                return "twelve";
            }
            case 13 -> {
                return "thirteen";
            }
            case 14 -> {
                return "fourteen";
            }
            case 15 -> {
                return "fifteen";
            }
            case 16 -> {
                return "sixteen";
            }
            case 17 -> {
                return "seventeen";
            }
            case 18 -> {
                return "eighteen";
            }
            case 19 -> {
                return "nineteen";
            }
            case 20 -> {
                return "twenty";
            }
            case 30 -> {
                return "thirty";
            }
            case 40 -> {
                return "forty";
            }
            case 50 -> {
                return "fifty";
            }
            case 60 -> {
                return "sixty";
            }
            case 70 -> {
                return "seventy";
            }
            case 80 -> {
                return "eighty";
            }
            case 90 -> {
                return "ninety";
            }
            default -> {
                int r = abs % 10;
                return intToString(abs - r, freq) + "-" + intToString(r, freq);
            }
        }
    }

}
