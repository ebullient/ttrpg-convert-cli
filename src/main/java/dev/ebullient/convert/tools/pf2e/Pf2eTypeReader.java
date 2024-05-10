package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.Arrays;
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
import dev.ebullient.convert.qute.QuteUtil;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.JsonTextConverter;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataActivity;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataArmorClass;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataDefenses;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataDefenses.QuteSavingThrows;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataHpHardnessBt;
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

            Map<String, QuteDataHpHardnessBt> hpHardnessBt = getHpHardnessBt(source, convert);

            return new QuteDataDefenses(
                    getArmorClass(source, convert),
                    getSavingThrowString(source, convert),
                    hpHardnessBt.remove(std.name()),
                    hpHardnessBt,
                    immunities.linkifyListFrom(source, Pf2eIndexType.trait, convert), // immunities
                    getWeakResist(resistances, source, convert), // resistances
                    getWeakResist(weaknesses, source, convert));
        }

        /**
         * Example input JSON for a hazard:
         *
         * <pre>
         *     "hardness": {
         *         "std": 13,
         *         "Reflection ": 14,
         *         "notes": {
         *             "std": "per mirror"
         *         }
         *     },
         *     "hp": {
         *         "std": 54,
         *         "Reflection ": 30,
         *         "notes": {
         *             "Reflection ": "some note"
         *         }
         *     },
         *     "bt": {
         *         "std": 27,
         *         "Reflection ": 15
         *     }
         * </pre>
         *
         * Broken threshold is only valid for hazards. Example input JSON for a creature:
         *
         * <pre>
         *     "hardness": {
         *         "std": 13,
         *     },
         *     "hp": [
         *         { "hp": 90, "name": "body", "abilities": [ "hydra regeneration" ] },
         *         { "hp": 15, "name": "head", "abilities": [ "head regrowth" ] }
         *     ],
         * </pre>
         */
        public static Map<String, QuteDataHpHardnessBt> getHpHardnessBt(JsonNode source, Pf2eTypeReader convert) {
            // We need to do HP mapping separately because creature and hazard HP are structured differently
            Map<String, QuteDataHpHardnessBt.HpStat> hpStats = hp.isArrayIn(source)
                    ? Pf2eHpStat.mappedHpFromArray(hp.ensureArrayIn(source), convert)
                    : Pf2eHpStat.mappedHpFromObject(hp.getFromOrEmptyObjectNode(source), convert);

            // Collect names from the field names of the stat objects
            Set<String> names = Stream.of(bt, hardness)
                    .filter(k -> k.isObjectIn(source))
                    .flatMap(k -> k.streamPropsExcluding(source, Pf2eHpStat.values()))
                    .map(Entry::getKey)
                    .map(s -> s.equals("default") ? std.name() : s) // compensate for data irregularity
                    .collect(Collectors.toSet());
            names.addAll(hpStats.keySet());

            JsonNode btNode = bt.getFromOrEmptyObjectNode(source);
            JsonNode hardnessNode = hardness.getFromOrEmptyObjectNode(source);
            Map<String, String> hardnessNotes = notes.getMapOfStrings(hardnessNode, convert.tui());

            // Map each known name to the known stats for that name
            return names.stream().collect(Collectors.toMap(
                    String::trim,
                    k -> new QuteDataHpHardnessBt(
                            hpStats.getOrDefault(k, null),
                            hardnessNode.has(k)
                                    ? new Pf2eSimpleStat(
                                            hardnessNode.get(k).asInt(), convert.replaceText(hardnessNotes.get(k)))
                                    : null,
                            btNode.has(k) ? btNode.get(k).asInt() : null)));
        }

        public static List<String> getWeakResist(Pf2eDefenses field, JsonNode source, Pf2eTypeReader convert) {
            List<String> items = new ArrayList<>();
            for (JsonNode wr : field.iterateArrayFrom(source)) {
                NameAmountNote nmn = convert.tui().readJsonValue(wr, NameAmountNote.class);
                items.add(nmn.flatten(convert));
            }
            return items;
        }

        public static QuteDataArmorClass getArmorClass(JsonNode source, Pf2eTypeReader convert) {
            JsonNode acNode = ac.getFrom(source);
            return acNode == null ? null
                    : new QuteDataArmorClass(
                            std.getIntOrThrow(acNode),
                            ac.streamPropsExcluding(source, note, abilities, notes, std)
                                    .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().asInt())),
                            (note.existsIn(acNode) ? note : notes).replaceTextFromList(acNode, convert),
                            abilities.replaceTextFromList(acNode, convert));
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

    enum Pf2eHpStat implements JsonNodeReader {
        abilities,
        hp,
        name,
        notes;

        /**
         * Read HP stats mapped to names from a JSON array. Each entry in the array corresponds to a different HP
         * component in a single creature - e.g. the heads and body on a hydra:
         *
         * <pre>
         *     [
         *       {"hp": 10, "name": "head", "abilities": ["head regrowth"]},
         *       {"hp": 20, "name": "body", "notes": ["some note"]}
         *     ]
         * </pre>
         *
         * If there is only a single HP component, then {@code name} may be omitted. In this case, the key in the map
         * will be {@code std}.
         *
         * <pre>
         *     [{"hp": 10, "abilities": ["some ability"], "notes": ["some note"]}]
         * </pre>
         */
        static Map<String, QuteDataHpHardnessBt.HpStat> mappedHpFromArray(JsonNode source, JsonTextConverter<?> convert) {
            return convert.streamOf(convert.ensureArray(source)).collect(Collectors.toMap(
                    n -> {
                        // Capitalize the names
                        if (!Pf2eHpStat.name.existsIn(n)) {
                            return Pf2eDefenses.std.name();
                        }
                        String name = Pf2eHpStat.name.getTextOrThrow(n);
                        return name.substring(0, 1).toUpperCase() + name.substring(1);
                    },
                    n -> new QuteDataHpHardnessBt.HpStat(
                            hp.getIntOrThrow(n),
                            notes.replaceTextFromList(n, convert),
                            abilities.replaceTextFromList(n, convert))));
        }

        /**
         * Read HP stats mapped to names from a JSON object. Each resulting entry corresponds to a different HP
         * component in a single hazard - e.g. the standard HP, and the reflection HP on a Clone Mirror hazard:
         *
         * <pre>
         *     {
         *         "std": 54,
         *         "Reflection ": 30,
         *         "notes": {
         *             "std": "per mirror"
         *         }
         *     }
         * </pre>
         */
        static Map<String, QuteDataHpHardnessBt.HpStat> mappedHpFromObject(
                JsonNode source, JsonTextConverter<?> convert) {
            JsonNode notesNode = notes.getFromOrEmptyObjectNode(source);
            return convert.streamPropsExcluding(convert.ensureObjectNode(source), notes).collect(Collectors.toMap(
                    Entry::getKey,
                    e -> new QuteDataHpHardnessBt.HpStat(
                            e.getValue().asInt(),
                            notesNode.has(e.getKey()) ? convert.replaceText(notesNode.get(e.getKey())) : null)));
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
                    Optional.ofNullable(note.getTextOrNull(source))
                            .map(s -> List.of(convert.replaceText(s)))
                            .orElse(List.of()));
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

    default List<String> getAlignments(JsonNode alignNode) {
        return streamOf(alignNode)
            .map(JsonNode::asText)
            .map(a -> a.length() > 2 ? a : linkifyTrait(a.toUpperCase()))
            .collect(Collectors.toList());
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

    /** A generic container for a PF2e stat value which may have an attached note. */
    interface Pf2eStat extends QuteUtil {
        /** Returns the value of the stat. */
        Integer value();

        /** Returns any notes associated with this value. */
        List<String> notes();

        /** Return the value formatted with a leading +/-. */
        default String bonus() {
            return String.format("%+d", value());
        }

        /** Return notes formatted as space-delimited parenthesized strings. */
        default String formattedNotes() {
            return notes().stream().map(s -> String.format("(%s)", s)).collect(Collectors.joining(" "));
        }
    }

    /**
     * A basic {@link Pf2eStat} which provides only a value and possibly a note. Default representation:
     * <p>
     * 10 (some note) (some other note)
     * </p>
     */
    record Pf2eSimpleStat(Integer value, List<String> notes) implements Pf2eStat {
        Pf2eSimpleStat(Integer value) {
            this(value, List.of());
        }

        Pf2eSimpleStat(Integer value, String note) {
            this(value, note == null || note.isBlank() ? List.of() : List.of(note));
        }

        @Override
        public String toString() {
            return value.toString() + (notes.isEmpty() ? "" : " " + formattedNotes());
        }
    }
}
