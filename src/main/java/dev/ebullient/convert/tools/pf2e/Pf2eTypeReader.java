package dev.ebullient.convert.tools.pf2e;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.pluralize;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.QuteItem.QuteItemWeaponData;
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
