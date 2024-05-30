package dev.ebullient.convert.tools.pf2e;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.pluralize;
import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.StringUtil;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataActivity;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataArmorClass;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataDefenses;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataDuration;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataFrequency;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataGenericStat;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataGenericStat.QuteDataNamedBonus;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataHpHardnessBt;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataRange;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataSpeed;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataTimedDuration;
import dev.ebullient.convert.tools.pf2e.qute.QuteInlineAttack;

/** A utility class which extends {@link JsonNodeReader} with PF2e-specific functionality. */
public interface Pf2eJsonNodeReader extends JsonNodeReader {

    /**
     * Return alignments as a list of formatted strings from this field in the given node.
     * Returns an empty list if we couldn't get alignments.
     */
    default List<String> getAlignmentsFrom(JsonNode alignNode, JsonSource convert) {
        return streamFrom(alignNode)
                .map(JsonNode::asText)
                .map(a -> a.length() > 2 ? a : convert.linkifyTrait(a.toUpperCase()))
                .toList();
    }

    /** Return a {@link QuteDataSpeed} read from this field of the {@code source} node, or null. */
    default QuteDataSpeed getSpeedFrom(JsonNode source, JsonSource convert) {
        return getObjectFrom(source).map(n -> Pf2eSpeed.getSpeed(n, convert)).orElse(null);
    }

    /** Return a {@link QuteDataFrequency} read from {@code source}, or null. */
    default QuteDataFrequency getFrequencyFrom(JsonNode source, JsonSource convert) {
        return getObjectFrom(source).map(n -> Pf2eFrequency.getFrequency(n, convert)).orElse(null);
    }

    /** Return a {@link QuteDataDefenses} read from this field in {@code source}, or null. */
    default QuteDataDefenses getDefensesFrom(JsonNode source, JsonSource convert) {
        return getObjectFrom(source).map(n -> Pf2eDefenses.getDefenses(n, convert)).orElse(null);
    }

    /** Return a {@link QuteDataActivity} read from this field in {@code source}, or null */
    default QuteDataActivity getActivityFrom(JsonNode source, JsonSource convert) {
        return getObjectFrom(source).map(n -> Pf2eNumberUnitEntry.getActivity(n, convert)).orElse(null);
    }

    /** Return a {@link QuteDataTimedDuration} read from this field in {@code source}, or null. */
    default QuteDataDuration getDurationFrom(JsonNode source, JsonSource convert) {
        return getObjectFrom(source).map(n -> Pf2eNumberUnitEntry.getDuration(n, convert)).orElse(null);
    }

    /** Returns {@link QuteDataDefenses.QuteSavingThrows} read from this field in {@code source}, or null. */
    default QuteDataDefenses.QuteSavingThrows getSavingThrowsFrom(JsonNode source, JsonSource convert) {
        return getObjectFrom(source).map(n -> Pf2eSavingThrows.getSavingThrows(n, convert)).orElse(null);
    }

    /**
     * Return a {@link QuteDataNamedBonus} from this field in {@code source} with a name of this field's name, or
     * return null if this field is not present in {@code source}.
     */
    default QuteDataNamedBonus getNamedBonusFrom(JsonNode source, JsonSource convert) {
        return existsIn(source) ? Pf2eNamedBonus.getNamedBonus(name(), getFrom(source), convert) : null;
    }

    /** Return a {@link QuteDataRange} from this field in {@code source}, or null. */
    default QuteDataRange getRangeFrom(JsonNode source, JsonSource convert) {
        return getObjectFrom(source).map(n -> Pf2eNumberUnitEntry.getRange(n, convert)).orElse(null);
    }

    /** Return a list of {@link QuteInlineAttack}, or an empty list. */
    default List<QuteInlineAttack> getAttacksFrom(JsonNode source, JsonSource convert) {
        return streamFrom(source).map(n -> Pf2eAttack.getAttack(n, convert)).toList();
    }

    /**
     * A {@link Pf2eJsonNodeReader} which reads JSON like the following:
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
    enum Pf2eSpeed implements Pf2eJsonNodeReader {
        walk,
        speedNote,
        abilities;

        /** Read a {@link QuteDataSpeed} from the {@code source} node. */
        private static QuteDataSpeed getSpeed(JsonNode source, JsonSource convert) {
            return new QuteDataSpeed(
                    walk.getIntFrom(source).orElse(null),
                    convert.streamPropsExcluding(source, speedNote, abilities)
                            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().asInt())),
                    speedNote.getTextFrom(source)
                            .map(convert::replaceText)
                            // Remove parens around the note
                            .map(s -> s.replaceFirst("^\\((%s)\\)$", "\1"))
                            .map(List::of).orElse(List.of()),
                    // Specifically make this mutable because we later need to add additional abilities for deities
                    new ArrayList<>(abilities.replaceTextFromList(source, convert)));
        }
    }

    /**
     * A {@link Pf2eJsonNodeReader} which reads JSON like the following:
     *
     * <pre>
     *     "unit": "round",
     *     "number": 1,
     *     "recurs": true,
     *     "overcharge": true,
     *     "interval": 2,
     * </pre>
     *
     * <p>
     * Or, with a custom unit:
     * </p>
     *
     * <pre>
     *     "customUnit": "gem",
     *     "number": 1,
     *     "recurs": true,
     *     "overcharge": true,
     *     "interval": 2,
     * </pre>
     *
     * <p>
     * Or, for a special frequency with a custom string:
     * </p>
     *
     * <pre>
     *     "special": "once per day, and recharges when the great cyclops uses Ferocity"
     * </pre>
     */
    enum Pf2eFrequency implements Pf2eJsonNodeReader {
        special,
        number,
        recurs,
        overcharge,
        interval,
        unit,
        customUnit;

        /** Return a {@link QuteDataFrequency} read from {@code node}. */
        private static QuteDataFrequency getFrequency(JsonNode node, JsonSource convert) {
            if (special.getTextFrom(node).isPresent()) {
                return new QuteDataFrequency(special.replaceTextFrom(node, convert));
            }
            return new QuteDataFrequency(
                    // This should usually be an integer, but some entries deviate from the schema and use a word
                    number.getIntFrom(node).orElseGet(() -> {
                        // Try to coerce the word back into a number, and otherwise log an error and give 0
                        String freqString = number.getTextOrThrow(node).trim();
                        if (freqString.equalsIgnoreCase("once")) {
                            return 1;
                        }
                        convert.tui().errorf("Got unexpected frequency value \"%s\"", freqString);
                        return 0;
                    }),
                    interval.getIntFrom(node).orElse(null),
                    unit.getTextFrom(node).orElseGet(() -> customUnit.getTextOrThrow(node)),
                    recurs.booleanOrDefault(node, false),
                    overcharge.booleanOrDefault(node, false));
        }
    }

    /** Read {@link QuteDataDefenses} from JSON input data. */
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

        /**
         * Read {@link QuteDataDefenses} from {@code source}. Example JSON input:
         *
         * <pre>
         *     "ac": {
         *         "std": 14,
         *         "with mage armor": 16,
         *         "notes": ["another armor note"],
         *         "abilities": ["some armor ability"]
         *     },
         *     "savingThrows": { ... },
         *     "hp": { ... },
         *     "hardness": { ... },
         *     "bt": { ... },
         *     "immunities": [
         *         "critical hits",
         *         "precision damage"
         *     ],
         *     "resistances": [ ... ],
         *     "weaknesses": [ ... ],
         *     "notes": { "some key": "some value" }
         * </pre>
         */
        private static QuteDataDefenses getDefenses(JsonNode source, JsonSource convert) {
            if (notes.existsIn(source)) {
                convert.tui().warnf("Defenses has notes: %s", source.toString());
            }

            Map<String, QuteDataHpHardnessBt> hpHardnessBt = getHpHardnessBt(source, convert);

            return new QuteDataDefenses(
                    ac.getObjectFrom(source)
                            .map(acNode -> new QuteDataArmorClass(
                                    std.getIntOrThrow(acNode),
                                    ac.streamPropsExcluding(source, note, abilities, notes, std)
                                            .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().asInt())),
                                    (note.existsIn(acNode) ? note : notes).replaceTextFromList(acNode, convert),
                                    abilities.replaceTextFromList(acNode, convert)))
                            .orElse(null),
                    savingThrows.getSavingThrowsFrom(source, convert),
                    hpHardnessBt.remove(std.name()),
                    hpHardnessBt,
                    immunities.linkifyListFrom(source, Pf2eIndexType.trait, convert),
                    resistances.streamFrom(source).collect(Pf2eNameAmountNote.mappedStatCollector(convert)),
                    weaknesses.streamFrom(source).collect(Pf2eNameAmountNote.mappedStatCollector(convert)));
        }

        /**
         * Returns a map of names to {@link QuteDataHpHardnessBt} read from {@code source}. This can read data from
         * creatures or from hazards.
         *
         * <p>
         * Example input JSON for a hazard:
         * </p>
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
         * <p>
         * Broken threshold is only valid for hazards. Example input JSON for a creature:
         * </p>
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
        private static Map<String, QuteDataHpHardnessBt> getHpHardnessBt(JsonNode source, JsonSource convert) {
            Map<String, QuteDataHpHardnessBt.HpStat> hpStats = hp.getHpFrom(source, convert);
            JsonNode btNode = bt.getFromOrEmptyObjectNode(source);
            JsonNode hardnessNode = hardness.getFromOrEmptyObjectNode(source);
            Map<String, String> hardnessNotes = notes.getMapOfStrings(hardnessNode, convert.tui());
            // Collect distinct names from the field names of the stat objects
            Stream<String> names = Stream.of(
                    hpStats.keySet().stream(),
                    bt.streamPropsExcluding(source, abilities, notes).map(Map.Entry::getKey),
                    hardness.streamPropsExcluding(source, abilities, notes).map(Map.Entry::getKey))
                    .flatMap(Function.identity())
                    .distinct();
            // Map each known name to the known stats for that name
            return names.collect(Collectors.toMap(
                    String::trim,
                    k -> new QuteDataHpHardnessBt(
                            hpStats.getOrDefault(k, null),
                            hardnessNode.has(k)
                                    ? new QuteDataGenericStat.SimpleStat(
                                            hardnessNode.get(k).asInt(), convert.replaceText(hardnessNotes.get(k)))
                                    : null,
                            btNode.has(k) ? btNode.get(k).asInt() : null)));
        }

        /**
         * Returns a map of names to {@link QuteDataHpHardnessBt.HpStat} from this field of {@code source}, or an empty
         * map.
         */
        private Map<String, QuteDataHpHardnessBt.HpStat> getHpFrom(JsonNode source, JsonSource convert) {
            // We need to do HP mapping separately because creature and hazard HP are structured differently
            return isArrayIn(source)
                    ? Pf2eHpStat.getHpMapFromArray(ensureArrayIn(source), convert)
                    : Pf2eHpStat.getHpMapFromObject(getFromOrEmptyObjectNode(source), convert);
        }

    }

    /** A {@link Pf2eJsonNodeReader} which reads JSON HP data. */
    enum Pf2eHpStat implements Pf2eJsonNodeReader {
        abilities,
        hp,
        name,
        notes,
        std;

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
        private static Map<String, QuteDataHpHardnessBt.HpStat> getHpMapFromArray(
                JsonNode source, JsonSource convert) {
            return convert.streamOf(convert.ensureArray(source)).collect(Collectors.toMap(
                    n -> Pf2eHpStat.name.getTextFrom(n).map(StringUtil::toTitleCase).orElse(std.name()),
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
         *     "std": 54,
         *     "Reflection ": 30,
         *     "notes": {
         *         "std": "per mirror"
         *     }
         * </pre>
         */
        private static Map<String, QuteDataHpHardnessBt.HpStat> getHpMapFromObject(
                JsonNode source, JsonSource convert) {
            Map<String, String> noteMap = notes.getMapOfStrings(source, convert.tui());
            return convert.streamPropsExcluding(convert.ensureObjectNode(source), notes).collect(Collectors.toMap(
                    Map.Entry::getKey,
                    e -> new QuteDataHpHardnessBt.HpStat(
                            e.getValue().asInt(),
                            convert.replaceText(noteMap.get(e.getKey())))));
        }
    }

    /**
     * A {@link Pf2eJsonNodeReader} which reads JSON structured like the following:
     *
     * <pre>
     *     "name": "physical",
     *     "amount": 5,
     *     "note": "except bludgeoning"
     * </pre>
     */
    enum Pf2eNameAmountNote implements Pf2eJsonNodeReader {
        name,
        amount,
        note;

        /** Return a collector which returns a map of names to {@link QuteDataGenericStat} */
        private static Collector<JsonNode, ?, Map<String, QuteDataGenericStat>> mappedStatCollector(
                JsonSource convert) {
            return Collectors.toMap(
                    name::getTextOrThrow,
                    n -> new QuteDataGenericStat.SimpleStat(
                            amount.getIntFrom(n).orElse(null),
                            note.replaceTextFrom(n, convert)));
        }
    }

    /**
     * A {@link Pf2eJsonNodeReader} which reads JSON input structured like the following:
     *
     * <pre>
     *     "number": 1,
     *     "unit": "round",
     *     "entry": "until the start of your next turn"
     * </pre>
     *
     * Or, possibly for some range entries
     *
     * <pre>
     *     "entry": "10 miles",
     *     "distance": {"type": "mile", "amount": 10}
     * </pre>
     */
    enum Pf2eNumberUnitEntry implements Pf2eJsonNodeReader {
        number,
        unit,
        entry,
        distance,
        type,
        amount;

        /**
         * Return a {@link QuteDataActivity} read from {@code node}. Example JSON input:
         *
         * <pre>
         *     {"number": 1, "unit": "action"}
         * </pre>
         *
         * Or, for an activity with a custom entry:
         *
         * <pre>
         *     {"number": 1, "unit": "varies", "entry": "{&#64;as 3} command,"}
         * </pre>
         */
        private static QuteDataActivity getActivity(JsonNode node, JsonSource convert) {
            String actionType = unit.getTextOrNull(node);
            Pf2eActivity activity = switch (actionType) {
                case "single", "action", "free", "reaction" ->
                    Pf2eActivity.toActivity(actionType, number.getIntOrThrow(node));
                case "varies" -> Pf2eActivity.varies;
                case "day", "minute", "hour", "round" -> Pf2eActivity.timed;
                default -> null;
            };

            if (activity == null) {
                throw new IllegalArgumentException("Can't parse activity from: %s".formatted(node));
            }

            String extra = entry.getTextFrom(node)
                    .filter(s -> !s.toLowerCase().contains("varies"))
                    .filter(Predicate.not(String::isBlank))
                    .map(convert::replaceText).map(StringUtil::parenthesize)
                    .orElse("");

            return activity.toQuteActivity(
                    convert, activity == Pf2eActivity.timed ? join(" ", number.getIntOrThrow(node), actionType, extra) : extra);
        }

        /**
         * Return a {@link QuteDataDuration} read from {@code node}. This will be either a {@link QuteDataActivity},
         * or a {@link QuteDataTimedDuration}. Examples:
         *
         * <ul>
         * <li>{@code {"entry": "1 round or 1 minute"}} timed duration with custom display of "1 round or 1 minute"</li>
         * <li>{@code {"number": 1, "unit": "action"}} single-action activity</li>
         * <li>{@code {"number": 1, "unit": "day"}} timed duration with value 1, unit "day"</li>
         * </ul>
         */
        private static QuteDataDuration getDuration(JsonNode node, JsonSource convert) {
            QuteDataTimedDuration timedDuration = new QuteDataTimedDuration(
                    number.getIntFrom(node).orElse(1),
                    unit.getEnumValueFrom(node, QuteDataTimedDuration.DurationUnit.class),
                    entry.replaceTextFrom(node, convert));
            // Prioritize using a custom display string if we have one
            if (timedDuration.hasCustomDisplay()) {
                return timedDuration;
            }
            String unitText = unit.getTextOrNull(node);
            // This is disallowed by the schema, but if we don't get a unit, then discard the whole duration
            if (unitText == null) {
                return null;
            }
            // The activity is more specific unless we have a custom display. Otherwise, fall back to the timed duration
            return Optional.ofNullable(Pf2eActivity.toActivity(unitText, timedDuration.value()))
                    .map(a -> (QuteDataDuration) a.toQuteActivity(convert, null))
                    .orElse(timedDuration);
        }

        /**
         * Return a {@link QuteDataRange} read from {@code node}. Example JSON input:
         *
         * <pre>
         *     "number": 3,
         *     "unit": "mile",
         *     "entry": "some note"
         * </pre>
         *
         * Or
         *
         * <pre>
         *     "entry": "10 miles",
         *     "distance": {"type": "mile", "amount": 10}
         * </pre>
         */
        private static QuteDataRange getRange(JsonNode source, JsonSource convert) {
            // Sometimes the amount and unit are in different fields of a nested "distance" object
            JsonNode node = distance.getObjectFrom(source).orElse(source);
            QuteDataRange.RangeUnit rangeUnit = unit.getRangeUnitFrom(node)
                    .or(() -> type.getRangeUnitFrom(node))
                    .or(() -> entry.getRangeUnitFrom(node)) // sometimes the entry is the unit
                    .orElse(null);
            Integer rangeValue = number.getIntFrom(node)
                    .or(() -> amount.getIntFrom(node))
                    .orElse(null);
            String entryText = entry.replaceTextFrom(node, convert);
            if (rangeValue == null && rangeUnit == null && !isPresent(entryText)) {
                convert.tui().errorf("No range data in %s", source.toPrettyString());
            }
            return new QuteDataRange(
                    rangeValue, rangeUnit,
                    // Don't include the entry text if it's just the unit again
                    entry.getRangeUnitFrom(node).map(ru -> ru == rangeUnit).orElse(false) ? null : entryText);
        }

        private Optional<QuteDataRange.RangeUnit> getRangeUnitFrom(JsonNode source) {
            return getTextFrom(source)
                    // normalize the unit to match enum names
                    .map(s -> pluralize(s, 1).toUpperCase())
                    .map(s -> {
                        try {
                            return QuteDataRange.RangeUnit.valueOf(s);
                        } catch (IllegalArgumentException ignored) {
                            return null;
                        }
                    });
        }
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

        /** Returns a {@link QuteInlineAttack} read from {@code node} */
        public static QuteInlineAttack getAttack(JsonNode node, JsonSource convert) {
            List<String> attackEffects = Pf2eAttack.effects.transformListFrom(node, convert);
            // Either the effects are a list of short descriptors which are also included in the damage, or they are a
            // long multi-line description of a complicated effect.
            String formattedDamage = damage.replaceTextFrom(node, convert);
            boolean hasMultilineEffect = attackEffects.stream().anyMatch(Predicate.not(formattedDamage::contains));

            return new QuteInlineAttack(
                    name.replaceTextFrom(node, convert),
                    Optional.ofNullable(activity.getActivityFrom(node, convert))
                            .orElse(Pf2eActivity.single.toQuteActivity(convert, "")),
                    QuteInlineAttack.AttackRangeType.valueOf(range.getTextOrDefault(node, "Melee").toUpperCase()),
                    attack.getIntFrom(node).orElse(null),
                    formattedDamage,
                    types.replaceTextFromList(node, convert),
                    convert.collectTraitsFrom(node, null),
                    hasMultilineEffect ? List.of() : attackEffects,
                    hasMultilineEffect ? String.join("\n", attackEffects) : null,
                    noMAP.booleanOrDefault(node, false) ? List.of() : List.of("no multiple attack penalty"),
                    convert);
        }
    }

    /**
     * A {@link Pf2eJsonNodeReader} which parses JSON named bonuses. Example:
     *
     * <pre>
     *     "std": 10,
     *     "in woods": 12,
     *     "note": "some note"
     * </pre>
     */
    enum Pf2eNamedBonus implements Pf2eJsonNodeReader {
        std,
        note,
        notes,
        abilities;

        /**
         * Return a {@link QuteDataNamedBonus} read from {@code source} with the given {@code skillName}. If
         * {@code source} is an integer, then return a bonus of that amount. If {@code source} is null, then return a
         * bonus of 0.
         *
         * <p>
         * Reads either single note from {@code note}, or multiple notes from {@code abilities}.
         * </p>
         */
        public static QuteDataNamedBonus getNamedBonus(
                String skillName, JsonNode source, JsonSource convert) {
            String displayName = toTitleCase(skillName);

            if (source == null) {
                return new QuteDataNamedBonus(displayName, 0);
            }

            if (source.isInt()) {
                return new QuteDataNamedBonus(displayName, source.asInt());
            }

            return new QuteDataNamedBonus(
                    displayName,
                    std.getIntOrThrow(source),
                    convert.streamPropsExcluding(source, std, note)
                            .collect(Collectors.toMap(e -> convert.replaceText(e.getKey()), e -> e.getValue().asInt())),
                    note.getTextFrom(source).map(convert::replaceText).map(List::of)
                            .orElse((abilities.existsIn(source) ? abilities : notes).replaceTextFromList(source, convert)));
        }
    }

    /**
     * A {@link Pf2eJsonNodeReader} which reads JSON saving throw data. Example:
     *
     * <pre>
     *     "fort": {
     *         "std": 6,
     *         "vs. poisons": +8
     *         "abilities": ["some fort ability"]
     *     },
     *     "ref": { ... },
     *     "will": { ... },
     *     "abilities": ["+1 status to all saves vs. positive"]
     * </pre>
     */
    enum Pf2eSavingThrows implements Pf2eJsonNodeReader {
        fort,
        ref,
        will,
        abilities,
        std;

        /** Return {@link QuteDataDefenses.QuteSavingThrows} read from this field in {@code source}. */
        private static QuteDataDefenses.QuteSavingThrows getSavingThrows(JsonNode source, JsonSource convert) {
            return new QuteDataDefenses.QuteSavingThrows(
                    fort.getNamedBonusFrom(source, convert),
                    ref.getNamedBonusFrom(source, convert),
                    will.getNamedBonusFrom(source, convert),
                    abilities.replaceTextFromList(source, convert));
        }
    }
}
