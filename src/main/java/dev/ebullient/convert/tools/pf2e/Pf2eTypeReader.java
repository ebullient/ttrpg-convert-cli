package dev.ebullient.convert.tools.pf2e;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.parenthesize;
import static dev.ebullient.convert.StringUtil.pluralize;
import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.JsonTextConverter;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataActivity;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataArmorClass;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataDefenses;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataDefenses.QuteSavingThrows;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataFrequency;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataGenericStat.SimpleStat;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataHpHardnessBt;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataSkillBonus;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataSpeed;
import dev.ebullient.convert.tools.pf2e.qute.QuteInlineAttack;
import dev.ebullient.convert.tools.pf2e.qute.QuteItem.QuteItemWeaponData;
import io.quarkus.runtime.annotations.RegisterForReflection;

public interface Pf2eTypeReader extends JsonSource {

    enum Pf2eAction implements Pf2eJsonNodeReader {
        activity,
        actionType,
        cost,
        frequency,
        info,
        prerequisites,
        trigger
    }

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

    enum Pf2eWeaponData implements Pf2eJsonNodeReader {
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
                result += convert.replaceText("{@damage %s} %s".formatted(
                        damage,
                        Pf2eWeaponData.damageType.getTextOrEmpty(source)));
            }
            if (damage2 != null) {
                result += convert.replaceText("%s{@damage %s} %s".formatted(
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

    enum Pf2eDefenses implements Pf2eJsonNodeReader {
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
                                    ? new SimpleStat(
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
                                    .map(convert::replaceText)
                                    .toList());
                            continue;
                        }
                        int value = extra.getValue().asInt();
                        svt.savingThrows.put(toTitleCase(extra.getKey()),
                                (value >= 0 ? "+" : "") + value);
                    }
                }
                svt.savingThrows.put(toTitleCase(e.getKey()), String.join(", ", svValue));
            }
            svt.abilities = convert.replaceText(abilities.getTextOrNull(stNode));
            return svt;
        }
    }

    enum Pf2eFeat implements Pf2eJsonNodeReader {
        access,
        activity,
        archetype, // child of featType
        cost,
        featType,
        frequency,
        leadsTo,
        level,
        prerequisites,
        special,
        trigger
    }

    enum Pf2eHpStat implements Pf2eJsonNodeReader {
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
                    n -> Pf2eHpStat.name.existsIn(n)
                            ? toTitleCase(Pf2eHpStat.name.getTextOrThrow(n))
                            : Pf2eDefenses.std.name(),
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

    enum Pf2eSkillBonus implements Pf2eJsonNodeReader {
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
            String displayName = toTitleCase(skillName);

            if (source.isInt()) {
                return new QuteDataSkillBonus(displayName, source.asInt());
            }

            return new QuteDataSkillBonus(
                    displayName,
                    std.getIntOrThrow(source),
                    convert.streamPropsExcluding(source, std, note)
                            .collect(Collectors.toMap(e -> convert.replaceText(e.getKey()), e -> e.getValue().asInt())),
                    note.getTextFrom(source).map(convert::replaceText).map(List::of).orElse(List.of()));
        }
    }

    static QuteDataActivity getQuteActivity(JsonNode source, JsonNodeReader field, JsonSource convert) {
        NumberUnitEntry jsonActivity = field.fieldFromTo(source, NumberUnitEntry.class, convert.tui());
        return jsonActivity == null ? null : jsonActivity.toQuteActivity(convert);
    }

    /**
     * Example JSON input for a creature:
     *
     * <pre>
     *     "range": "Melee",
     *     "name": "jaws",
     *     "attack": 32,
     *     "traits": ["evil", "magical", "reach 10 feet"],
     *     "effects": ["essence drain", "Grab"],
     *     "damage": "3d8+9 piercing plus 1d6 evil, essence drain, and Grab",
     *     "types": ["evil", "piercing"]
     * </pre>
     *
     * An example for a hazard with a complicated effect:
     *
     * <pre>
     *     "type": "attack",
     *     "range": "Ranged",
     *     "name": "eye beam",
     *     "attack": 20,
     *     "traits": ["diving", "evocation", "range 120 feet"],
     *     "effects": [
     *         "The target is subjected to one of the effects summarized below.",
     *         {
     *             "type": "list",
     *             "items": [{
     *                 "type": "item",
     *                 "name": "Green Eye Beam",
     *                 "entries": ["(poison) 6d6 poison damage (DC24 basic Reflex save)"],
     *             }, ...],
     *         },
     *     ],
     *     "types": ["electricity", "fire", "poison", "acid"]
     * </pre>
     *
     */
    enum Pf2eAttack implements Pf2eJsonNodeReader {
        name,
        attack,
        activity,
        damage,
        effects,
        range,
        types,
        noMAP;

        public static QuteInlineAttack createInlineAttack(JsonNode node, JsonSource convert) {
            List<String> effects = new ArrayList<>();
            convert.appendToText(effects, Pf2eAttack.effects.getFrom(node), null);

            // Either the effects are a list of short descriptors which are also included in the damage, or they are a
            // long multi-line description of a complicated effect.
            String formattedDamage = damage.replaceTextFrom(node, convert);
            String multilineEffect = null;
            if (effects.stream().anyMatch(Predicate.not(formattedDamage::contains))) {
                multilineEffect = String.join("\n", effects); // Preserve empty strings for line breaks
                effects = List.of();
            }

            return new QuteInlineAttack(
                    name.replaceTextFrom(node, convert),
                    Optional.ofNullable(getQuteActivity(node, activity, convert))
                            .orElse(Pf2eActivity.single.toQuteActivity(convert, "")),
                    QuteInlineAttack.AttackRangeType.valueOf(range.getTextOrDefault(node, "Melee").toUpperCase()),
                    attack.getIntFrom(node).orElse(null),
                    formattedDamage,
                    types.replaceTextFromList(node, convert),
                    convert.collectTraitsFrom(node, null),
                    effects,
                    multilineEffect,
                    noMAP.booleanOrDefault(node, false) ? List.of() : List.of("no multiple attack penalty"),
                    convert);
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

    enum Pf2eSpeed implements Pf2eJsonNodeReader {
        walk,
        speedNote,
        abilities;

        /**
         * Example JSON input:
         *
         * <pre>
         *     {
         *         "walk": 10,
         *         "fly": 20,
         *         "speedNote": "(with fly spell)",
         *         "abilities": "air walk"
         *     }
         * </pre>
         */
        static QuteDataSpeed getSpeed(JsonNode source, JsonTextConverter<?> convert) {
            return !convert.isObjectNode(source) ? null
                    : new QuteDataSpeed(
                            walk.getIntFrom(source).orElse(null),
                            convert.streamPropsExcluding(source, speedNote, abilities)
                                    .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().asInt())),
                            speedNote.getTextFrom(source)
                                    .map(convert::replaceText)
                                    .map(s -> s.replaceFirst("^\\((%s)\\)$", "\1")) // Remove parens around the note
                                    .map(List::of).orElse(List.of()),
                            // Specifically make this mutable because we later need to add additional abilities for deities
                            new ArrayList<>(abilities.replaceTextFromList(source, convert)));
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

    enum Pf2eFrequency implements Pf2eJsonNodeReader {
        special,
        number,
        recurs,
        overcharge,
        interval,
        unit,
        customUnit;

        public static QuteDataFrequency getFrequency(JsonNode node, JsonTextConverter<?> convert) {
            if (node == null) {
                return null;
            }
            if (special.getTextFrom(node).isPresent()) {
                return new QuteDataFrequency(special.replaceTextFrom(node, convert));
            }
            Integer freqNumber = number.getIntFrom(node).orElseGet(() -> {
                // Handle a data issue where some rules entries deviate from schema with words instead of integers.
                String freqString = number.getTextOrThrow(node).trim();
                if (freqString.equals("once")) {
                    return 1;
                }
                convert.tui().errorf("Got unexpected frequency value \"%s\"", freqString);
                return 0;
            });
            return new QuteDataFrequency(
                    freqNumber,
                    interval.getIntFrom(node).orElse(null),
                    unit.getTextFrom(node).orElseGet(() -> customUnit.getTextOrThrow(node)),
                    recurs.booleanOrDefault(node, false),
                    overcharge.booleanOrDefault(node, false));
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

        private QuteDataActivity toQuteActivity(JsonSource convert) {
            String extra = entry == null || entry.toLowerCase().contains("varies")
                    ? ""
                    : " " + parenthesize(convert.replaceText(entry));

            return switch (unit) {
                case "single", "action", "free", "reaction" -> {
                    Pf2eActivity activity = Pf2eActivity.toActivity(unit, number);
                    if (activity == null) {
                        throw new IllegalArgumentException("What is this? %s, %s, %s".formatted(number, unit, entry));
                    }
                    yield activity.toQuteActivity(convert,
                            extra.isBlank() ? null : "%s%s".formatted(activity.getLongName(), extra));
                }
                case "varies" -> Pf2eActivity.varies.toQuteActivity(convert,
                        extra.isBlank() ? null : "%s%s".formatted(Pf2eActivity.varies.getLongName(), extra));
                case "day", "minute", "hour", "round" -> Pf2eActivity.timed.toQuteActivity(convert,
                        "%s %s%s".formatted(number, unit, extra));
                default -> throw new IllegalArgumentException(
                        "What is this? %s, %s, %s".formatted(number, unit, entry));
            };
        }
    }

}
