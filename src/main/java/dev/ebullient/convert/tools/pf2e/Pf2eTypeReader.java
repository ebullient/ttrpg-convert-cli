package dev.ebullient.convert.tools.pf2e;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.pluralize;
import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataSkillBonus;
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
                    Optional.ofNullable(activity.getActivityFrom(node, convert))
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
