package dev.ebullient.convert.tools.pf2e;

import com.fasterxml.jackson.databind.JsonNode;
import dev.ebullient.convert.StringUtil;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataArmorClass;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataDefenses;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataFrequency;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataGenericStat;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataHpHardnessBt;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataSpeed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.ebullient.convert.StringUtil.toTitleCase;

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
        return getObjectFrom(source).map(n -> Pf2eSpeed.read(n, convert)).orElse(null);
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
        private static QuteDataSpeed read(JsonNode source, JsonSource convert) {
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

    /** Return a {@link QuteDataFrequency} read from {@code source}, or null. */
    default QuteDataFrequency getFrequencyFrom(JsonNode source, JsonSource convert) {
        return getObjectFrom(source).map(n -> Pf2eFrequency.read(n, convert)).orElse(null);
    }

    /** Return a {@link QuteDataDefenses} read from this field in {@code source}, or null. */
    default QuteDataDefenses getDefensesFrom(JsonNode source, Pf2eTypeReader convert) {
        return getObjectFrom(source).map(n -> Pf2eDefenses.readDefenses(n, convert)).orElse(null);
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
     * <p>Or, with a custom unit:</p>
     * <pre>
     *     "customUnit": "gem",
     *     "number": 1,
     *     "recurs": true,
     *     "overcharge": true,
     *     "interval": 2,
     * </pre>
     *
     * <p>Or, for a special frequency with a custom string:</p>
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
        private static QuteDataFrequency read(JsonNode node, JsonSource convert) {
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
        private static QuteDataDefenses readDefenses(JsonNode source, Pf2eTypeReader convert) {
            if (notes.existsIn(source)) {
                convert.tui().warnf("Defenses has notes: %s", source.toString());
            }

            Map<String, QuteDataHpHardnessBt> hpHardnessBt = readHpHardnessBt(source, convert);

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
                    resistances.getWeakResistFrom(source, convert),
                    weaknesses.getWeakResistFrom(source, convert));
        }

        /**
         * Returns a map of names to {@link QuteDataHpHardnessBt} read from {@code source}. This can read data from
         * creatures or from hazards.
         *
         * <p>Example input JSON for a hazard:</p>
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
         * <p>Broken threshold is only valid for hazards. Example input JSON for a creature:</p>
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
        private static Map<String, QuteDataHpHardnessBt> readHpHardnessBt(JsonNode source, Pf2eTypeReader convert) {
            Map<String, QuteDataHpHardnessBt.HpStat> hpStats = hp.getHpFrom(source, convert);
            JsonNode btNode = bt.getFromOrEmptyObjectNode(source);
            JsonNode hardnessNode = hardness.getFromOrEmptyObjectNode(source);
            Map<String, String> hardnessNotes = notes.getMapOfStrings(hardnessNode, convert.tui());

            // Collect distinct names from the field names of the stat objects
            return Stream.of(
                    hpStats.keySet().stream(),
                    bt.streamPropsExcluding(source, abilities, notes).map(Map.Entry::getKey),
                    hardness.streamPropsExcluding(source, abilities, notes).map(Map.Entry::getKey))
                .flatMap(Function.identity())
                .distinct()
                // Map each known name to the known stats for that name
                .collect(Collectors.toMap(
                    String::trim,
                    k -> new QuteDataHpHardnessBt(
                            hpStats.getOrDefault(k, null),
                            hardnessNode.has(k)
                                    ? new QuteDataGenericStat.SimpleStat(
                                            hardnessNode.get(k).asInt(), convert.replaceText(hardnessNotes.get(k)))
                                    : null,
                            btNode.has(k) ? btNode.get(k).asInt() : null)));
        }

        private List<String> getWeakResistFrom(JsonNode source, Pf2eTypeReader convert) {
            List<String> items = new ArrayList<>();
            for (JsonNode wr : iterateArrayFrom(source)) {
                Pf2eTypeReader.NameAmountNote nmn = convert.tui().readJsonValue(wr, Pf2eTypeReader.NameAmountNote.class);
                items.add(nmn.flatten(convert));
            }
            return items;
        }

        /**
         * Return {@link QuteDataDefenses.QuteSavingThrows} read from this field in {@code source}, or null.
         *
         * <pre>
         *     "fort": {
         *         "std": 10,
         *         "abilities": ["some fort ability"]
         *     },
         *     "ref": {
         *         "std": 12,
         *         "abilities": ["some ref ability"]
         *     },
         *     "will": {
         *         "std": 14,
         *         "abilities": ["some will ability"]
         *     },
         *     "abilities": ["some saving throw ability"]
         * </pre>
         */
        private QuteDataDefenses.QuteSavingThrows getSavingThrowsFrom(JsonNode source, Pf2eTypeReader convert) {
            JsonNode stNode = getObjectFrom(source).orElse(null);
            if (stNode == null) {
                return null;
            }
            QuteDataDefenses.QuteSavingThrows svt = new QuteDataDefenses.QuteSavingThrows();
            for (Map.Entry<String, JsonNode> e : convert.iterableFields(stNode)) {
                int sv = std.intOrDefault(e.getValue(), 0);
                List<String> svValue = new ArrayList<>();
                svValue.add((sv >= 0 ? "+" : "") + sv);

                if (e.getValue().size() > 1) {
                    for (Map.Entry<String, JsonNode> extra : convert.iterableFields(e.getValue())) {
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

        /**
         * Returns a map of names to {@link QuteDataHpHardnessBt.HpStat} from this field of {@code source}, or an empty
         * map.
         */
        private Map<String, QuteDataHpHardnessBt.HpStat> getHpFrom(JsonNode source, Pf2eTypeReader convert) {
            // We need to do HP mapping separately because creature and hazard HP are structured differently
            return isArrayIn(source)
                ? Pf2eHpStat.readFromArray(ensureArrayIn(source), convert)
                : Pf2eHpStat.readFromObject(getFromOrEmptyObjectNode(source), convert);
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
        private static Map<String, QuteDataHpHardnessBt.HpStat> readFromArray(
                JsonNode source, Pf2eTypeReader convert) {
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
        private static Map<String, QuteDataHpHardnessBt.HpStat> readFromObject(
                JsonNode source, Pf2eTypeReader convert) {
            JsonNode notesNode = notes.getFromOrEmptyObjectNode(source);
            return convert.streamPropsExcluding(convert.ensureObjectNode(source), notes).collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> new QuteDataHpHardnessBt.HpStat(
                    e.getValue().asInt(),
                    notesNode.has(e.getKey()) ? convert.replaceText(notesNode.get(e.getKey())) : null)));
        }
    }
}
