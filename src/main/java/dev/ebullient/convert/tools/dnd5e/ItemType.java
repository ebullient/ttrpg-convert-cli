package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.isPresent;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonTextConverter.SourceField;
import dev.ebullient.convert.tools.dnd5e.Json2QuteItem.ItemTag;
import dev.ebullient.convert.tools.dnd5e.JsonSource.Tools5eFields;

/**
 * @param name Item type name.
 * @param lowercaseName Item type name in lowercase (for comparison)
 * @param abbreviation Item type abbreviation.
 * @param link Markdown link to definition of this type or name if source is not included.
 * @param group Item type group. Optional.
 */
public record ItemType(
        String name,
        String lowercaseName,
        String abbreviation,
        String indexKey,
        ItemTypeGroup group) {

    public String toString() {
        return "Type: " + name;
    }

    public String linkify() {
        return linkify(null);
    }

    public String linkify(String linkText) {
        Tools5eIndex index = Tools5eIndex.getInstance();
        linkText = isPresent(linkText) ? linkText : name;

        boolean included = isPresent(indexKey)
                ? index.isIncluded(indexKey)
                : index.customRulesIncluded();

        return included
                ? "[%s](%sitem-types.md#%s)".formatted(
                        linkText, index.rulesVaultRoot(), Tui.toAnchorTag(name))
                : linkText;
    }

    public static final Map<String, ItemType> typeMap = new HashMap<>();

    public static ItemType fromKey(String key, Tools5eIndex index) {
        String finalKey = index.getAliasOrDefault(key);
        JsonNode node = index.getNode(finalKey);
        return node == null ? null : fromNode(finalKey, node);
    }

    public static ItemType fromNode(String typeKey, JsonNode typeNode) {
        // Create the ItemType object once
        return typeMap.computeIfAbsent(typeKey, k -> {
            String abbreviation = Tools5eFields.abbreviation.getTextOrEmpty(typeNode);
            String name = SourceField.name.getTextOrEmpty(typeNode);
            if (!isPresent(name)) {
                Tui.instance().warnf(Msg.NOT_SET.wrap("Name not found for type %s"), typeKey);
                name = abbreviation;
            }
            name = fixName(abbreviation, name);
            String lower = name.toLowerCase();
            ItemTypeGroup group = mapGroup(abbreviation, lower, typeNode);

            return new ItemType(
                    name,
                    lower,
                    abbreviation,
                    typeKey,
                    group);
        });
    }

    public static String tagForType(ItemType type, Tui tui) {
        String lower = type.lowercaseName();
        if (type.group() == ItemTypeGroup.armor) {
            return ItemTag.armor.build(lower.replaceAll("\\s*armor\\s*", ""));
        }
        if (type.group() == ItemTypeGroup.shield) {
            return ItemTag.shield.build(lower.replaceAll("\\s*shield\\s*", ""));
        }
        if (type.group() == ItemTypeGroup.vehicle) {
            return ItemTag.vehicle.build(lower.replaceAll("\\s*vehicle\\s*", ""));
        }
        if (type.group() == ItemTypeGroup.wondrous) {
            return ItemTag.wondrous.build(lower.replaceAll("\\s*wondrous( item)?\\s*", ""));
        }
        if (type.group() == ItemTypeGroup.weapon) {
            return ItemTag.weapon.build(""
                    + (lower.contains("firearm") ? "firearm/" : "")
                    + (lower.contains("ammunition") || lower.contains("ammo") ? "ammunition/" : "")
                    + (lower.contains("explosive") ? "explosive/" : "")
                    + lower.replaceAll("\\s*(ammo|ammunition|explosive|firearm|weapon)\\s*", ""));
        }
        return ItemTag.gear.build(lower.replaceAll("\\s*(adventuring|gear)\\s*", ""));
    }

    private static String fixName(String abbreviation, String name) {
        return switch (abbreviation) {
            case "AF" -> "Ammunition (Firearm)";
            case "AIR" -> "Airship, Vehicle (air)";
            case "SHP" -> "Ship, Vehicle (water)";
            case "SPC" -> "Spelljammer, Vehicle (space)";
            case "VEH" -> "Vehicle (land)";
            default -> name;
        };
    }

    private static ItemTypeGroup mapGroup(String abbreviation, String lowercase, JsonNode itemType) {
        if (abbreviation.contains("$") || lowercase.contains("treasure")) {
            return ItemTypeGroup.treasure;
        }
        if (lowercase.contains("ammunition")) {
            return ItemTypeGroup.ammunition;
        }
        if (lowercase.contains("armor")) {
            return ItemTypeGroup.armor;
        }
        if (lowercase.contains("shield")) {
            return ItemTypeGroup.shield;
        }
        if (lowercase.contains("vehicle")) {
            return ItemTypeGroup.vehicle;
        }
        if (lowercase.contains("weapon")
                || lowercase.contains("explosive")
                || lowercase.contains("firearm")) {
            return ItemTypeGroup.weapon;
        }
        if (lowercase.contains("wondrous")) {
            return ItemTypeGroup.wondrous;
        }
        return switch (abbreviation) {
            // generic variant, other
            // potion, rod, ring, scroll, staff, wand, wondrous
            case "GV", "MR", "OTH", "P", "RD", "RG", "SC", "ST", "WD", "W" -> ItemTypeGroup.wondrous;
            // artisan tool, food & drink, gear, gaming set, illegal drug, musical instrument,
            // mount, spellcasting focus, tools, tack & harness, trade good,
            case "AT", "FD", "G", "GS", "IDG", "INS", "MNT", "SCF", "T", "TAH", "TG" -> ItemTypeGroup.gear;
            default -> {
                // Homebrew won't always have a type assigned. Poke around.
                String nodeString = itemType.toString();
                if (nodeString.contains("this weapon")) {
                    yield ItemTypeGroup.weapon;
                }
                if (lowercase.contains("magic")
                        || lowercase.contains("rune")
                        || nodeString.contains("magic")) {
                    yield ItemTypeGroup.wondrous;
                }
                yield ItemTypeGroup.gear;
            }
        };
    }
}

// Parser.ITM_TYP_ABV__TREASURE = "$";
// Parser.ITM_TYP_ABV__TREASURE_ART_OBJECT = "$A";
// Parser.ITM_TYP_ABV__TREASURE_COINAGE = "$C";
// Parser.ITM_TYP_ABV__TREASURE_GEMSTONE = "$G";
// Parser.ITM_TYP_ABV__AMMUNITION = "A";
// Parser.ITM_TYP_ABV__AMMUNITION_FUTURISTIC = "AF";
// Parser.ITM_TYP_ABV__VEHICLE_AIR = "AIR";
// Parser.ITM_TYP_ABV__ARTISAN_TOOL = "AT";
// Parser.ITM_TYP_ABV__EXPLOSIVE = "EXP";
// Parser.ITM_TYP_ABV__FOOD_AND_DRINK = "FD";
// Parser.ITM_TYP_ABV__ADVENTURING_GEAR = "G";
// Parser.ITM_TYP_ABV__GAMING_SET = "GS";
// Parser.ITM_TYP_ABV__GENERIC_VARIANT = "GV";
// Parser.ITM_TYP_ABV__HEAVY_ARMOR = "HA";
// Parser.ITM_TYP_ABV__ILLEGAL_DRUG = "IDG";
// Parser.ITM_TYP_ABV__INSTRUMENT = "INS";
// Parser.ITM_TYP_ABV__LIGHT_ARMOR = "LA";
// Parser.ITM_TYP_ABV__MELEE_WEAPON = "M";
// Parser.ITM_TYP_ABV__MEDIUM_ARMOR = "MA";
// Parser.ITM_TYP_ABV__MOUNT = "MNT";
// Parser.ITM_TYP_ABV__OTHER = "OTH";
// Parser.ITM_TYP_ABV__POTION = "P";
// Parser.ITM_TYP_ABV__RANGED_WEAPON = "R";
// Parser.ITM_TYP_ABV__ROD = "RD";
// Parser.ITM_TYP_ABV__RING = "RG";
// Parser.ITM_TYP_ABV__SHIELD = "S";
// Parser.ITM_TYP_ABV__SCROLL = "SC";
// Parser.ITM_TYP_ABV__SPELLCASTING_FOCUS = "SCF";
// Parser.ITM_TYP_ABV__VEHICLE_WATER = "SHP";
// Parser.ITM_TYP_ABV__VEHICLE_SPACE = "SPC";
// Parser.ITM_TYP_ABV__TOOL = "T";
// Parser.ITM_TYP_ABV__TACK_AND_HARNESS = "TAH";
// Parser.ITM_TYP_ABV__TRADE_GOOD = "TG";
// Parser.ITM_TYP_ABV__VEHICLE_LAND = "VEH";
// Parser.ITM_TYP_ABV__WAND = "WD";

// Parser.ITM_TYP__TREASURE = "$|DMG";
// Parser.ITM_TYP__TREASURE_ART_OBJECT = "$A|DMG";
// Parser.ITM_TYP__TREASURE_COINAGE = "$C";
// Parser.ITM_TYP__TREASURE_GEMSTONE = "$G|DMG";
// Parser.ITM_TYP__AMMUNITION = "A";
// Parser.ITM_TYP__AMMUNITION_FUTURISTIC = "AF|DMG";
// Parser.ITM_TYP__VEHICLE_AIR = "AIR|DMG";
// Parser.ITM_TYP__ARTISAN_TOOL = "AT";
// Parser.ITM_TYP__EXPLOSIVE = "EXP|DMG";
// Parser.ITM_TYP__FOOD_AND_DRINK = "FD";
// Parser.ITM_TYP__ADVENTURING_GEAR = "G";
// Parser.ITM_TYP__GAMING_SET = "GS";
// Parser.ITM_TYP__GENERIC_VARIANT = "GV|DMG";
// Parser.ITM_TYP__HEAVY_ARMOR = "HA";
// Parser.ITM_TYP__ILLEGAL_DRUG = "IDG|TDCSR";
// Parser.ITM_TYP__INSTRUMENT = "INS";
// Parser.ITM_TYP__LIGHT_ARMOR = "LA";
// Parser.ITM_TYP__MELEE_WEAPON = "M";
// Parser.ITM_TYP__MEDIUM_ARMOR = "MA";
// Parser.ITM_TYP__MOUNT = "MNT";
// Parser.ITM_TYP__OTHER = "OTH";
// Parser.ITM_TYP__POTION = "P";
// Parser.ITM_TYP__RANGED_WEAPON = "R";
// Parser.ITM_TYP__ROD = "RD|DMG";
// Parser.ITM_TYP__RING = "RG|DMG";
// Parser.ITM_TYP__SHIELD = "S";
// Parser.ITM_TYP__SCROLL = "SC|DMG";
// Parser.ITM_TYP__SPELLCASTING_FOCUS = "SCF";
// Parser.ITM_TYP__VEHICLE_WATER = "SHP";
// Parser.ITM_TYP__VEHICLE_SPACE = "SPC|AAG";
// Parser.ITM_TYP__TOOL = "T";
// Parser.ITM_TYP__TACK_AND_HARNESS = "TAH";
// Parser.ITM_TYP__TRADE_GOOD = "TG";
// Parser.ITM_TYP__VEHICLE_LAND = "VEH";
// Parser.ITM_TYP__WAND = "WD|DMG";

// Parser.ITM_TYP__ODND_TREASURE_ART_OBJECT = "$A|XDMG";
// Parser.ITM_TYP__ODND_TREASURE_COINAGE = "$C|XPHB";
// Parser.ITM_TYP__ODND_TREASURE_GEMSTONE = "$G|XDMG";
// Parser.ITM_TYP__ODND_AMMUNITION = "A|XPHB";
// Parser.ITM_TYP__ODND_AMMUNITION_FUTURISTIC = "AF|XDMG";
// Parser.ITM_TYP__ODND_VEHICLE_AIR = "AIR|XPHB";
// Parser.ITM_TYP__ODND_ARTISAN_TOOL = "AT|XPHB";
// Parser.ITM_TYP__ODND_EXPLOSIVE = "EXP|XDMG";
// Parser.ITM_TYP__ODND_FOOD_AND_DRINK = "FD|XPHB";
// Parser.ITM_TYP__ODND_ADVENTURING_GEAR = "G|XPHB";
// Parser.ITM_TYP__ODND_GAMING_SET = "GS|XPHB";
// Parser.ITM_TYP__ODND_GENERIC_VARIANT = "GV|XDMG";
// Parser.ITM_TYP__ODND_HEAVY_ARMOR = "HA|XPHB";
// Parser.ITM_TYP__ODND_INSTRUMENT = "INS|XPHB";
// Parser.ITM_TYP__ODND_LIGHT_ARMOR = "LA|XPHB";
// Parser.ITM_TYP__ODND_MELEE_WEAPON = "M|XPHB";
// Parser.ITM_TYP__ODND_MEDIUM_ARMOR = "MA|XPHB";
// Parser.ITM_TYP__ODND_MOUNT = "MNT|XPHB";
// Parser.ITM_TYP__ODND_POTION = "P|XPHB";
// Parser.ITM_TYP__ODND_RANGED_WEAPON = "R|XPHB";
// Parser.ITM_TYP__ODND_ROD = "RD|XDMG";
// Parser.ITM_TYP__ODND_RING = "RG|XDMG";
// Parser.ITM_TYP__ODND_SHIELD = "S|XPHB";
// Parser.ITM_TYP__ODND_SCROLL = "SC|XPHB";
// Parser.ITM_TYP__ODND_SPELLCASTING_FOCUS = "SCF|XPHB";
// Parser.ITM_TYP__ODND_VEHICLE_WATER = "SHP|XPHB";
// Parser.ITM_TYP__ODND_TOOL = "T|XPHB";
// Parser.ITM_TYP__ODND_TACK_AND_HARNESS = "TAH|XPHB";
// Parser.ITM_TYP__ODND_TRADE_GOOD = "TG|XDMG";
// Parser.ITM_TYP__ODND_VEHICLE_LAND = "VEH|XPHB";
// Parser.ITM_TYP__ODND_WAND = "WD|XDMG";
