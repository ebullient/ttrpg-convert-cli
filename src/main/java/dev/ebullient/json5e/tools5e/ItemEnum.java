package dev.ebullient.json5e.tools5e;

import java.util.List;

import dev.ebullient.json5e.io.Json5eTui;

public enum ItemEnum {

    LIGHT_ARMOR("Light Armor", "LA", ""),
    MEDIUM_ARMOR("Medium Armor", "MA", ""),
    HEAVY_ARMOR("Heavy Armor", "HA", ""),
    SHIELD("Shield", "S", ""),

    MELEE_WEAPON("Melee Weapon", "M", ""),
    EXPLOSIVE("Ranged Weapon", "EXP", "Explosive"),
    RANGED_WEAPON("Ranged Weapon", "R", ""),
    AMMUNITION("Ammunition", "A", ""),
    AMMUNITION_FIREARM("Ammunition", "AF", "Ammunition (Firearm)"),

    ROD("Rod", "RD", ""),
    STAFF("Staff", "ST", ""),
    WAND("Wand", "WD", ""),
    RING("Ring", "RG", ""),
    POTION("Potion", "P", ""),
    SCROLL("Scroll", "SC", ""),
    ELDRITCH_MACHINE("Wondrous Item", "EM", "Eldritch Machine"),
    GENERIC_VARIANT("Wondrous Item", "GV", "Generic Variant"),
    MASTER_RUNE("Wondrous Item", "MR", "Master Rune"),
    OTHER("Wondrous Item", "OTH", "Other"),
    WONDROUS("Wondrous Item", "W", ""),

    ARTISANS_TOOLS("Adventuring Gear", "AT", "Artisan's Tools"),
    FOOD("Adventuring Gear", "FD", "Food and Drink"),
    GAMING_SET("Adventuring Gear", "GS", "Gaming Set"),
    INSTRUMENT("Adventuring Gear", "INS", "Instrument"),
    MOUNT("Adventuring Gear", "MNT", "Mount"),
    SPELLCASTING_FOCUS("Adventuring Gear", "SCF", "Spellcasting Focus"),
    TOOLS("Adventuring Gear", "T", "Tools"),
    TACK("Adventuring Gear", "TAH", "Tack and Harness"),
    TRADE_GOOD("Adventuring Gear", "TG", "Trade Good"),
    GEAR("Adventuring Gear", "G", ""),

    AIRSHIP("Vehicle", "AIR", "Airship, Vehicle (air)"),
    SHIP("Vehicle", "SHP", "Ship, Vehicle (water)"),
    SPELLJAMMER("Vehicle", "SPC", "Spelljammer, Vehicle (space)"),
    VEHICLE("Vehicle", "VEH", "Vehicle (land)"),

    WEALTH("Treasure", "$", ""),
    UNKNOWN("Unknown", "", "");

    private final String genericType;
    private final String lower;
    private final String encodedValue;
    private final String specializedType;

    ItemEnum(String genericType, String encodedValue, String specializedType) {
        this.genericType = genericType;
        this.lower = genericType.toLowerCase();
        this.encodedValue = encodedValue;
        this.specializedType = specializedType;
    }

    public String getSpecializedType() {
        return specializedType.isBlank() ? genericType : specializedType;
    }

    public String value() {
        return lower;
    }

    public static ItemEnum fromEncodedValue(String v) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Invalid/Empty item type");
        }
        for (ItemEnum i : ItemEnum.values()) {
            if (i.encodedValue.equals(v)) {
                return i;
            }
        }
        throw new IllegalArgumentException("Invalid/Unknown item type " + v);
    }

    public boolean isWeapon() {
        return this == RANGED_WEAPON
                || this == MELEE_WEAPON
                || this.lower.equals("ammunition");
    }

    public boolean isArmor() {
        return this == LIGHT_ARMOR || this == MEDIUM_ARMOR || this == HEAVY_ARMOR || this == SHIELD;
    }

    public boolean isGear() {
        return this.lower.equals("adventuring gear");
    }

    public boolean isMoney() {
        return this == WEALTH;
    }

    public boolean isVehicle() {
        return this.lower.equals("vehicle");
    }

    public boolean isWondrousItem() {
        return this == ROD
                || this == STAFF
                || this == WAND
                || this == RING
                || this == POTION
                || this == SCROLL
                || this == WONDROUS;
    }

    public String getItemTag(List<PropertyEnum> properties, Json5eTui tui) {
        StringBuilder tag = new StringBuilder();
        tag.append("item");
        if (isArmor()) {
            tag.append("/armor/");
            tag.append(lower.replace(" armor", ""));
        } else if (isWeapon()) {
            tag.append("/weapon/");
            tag.append(properties.contains(PropertyEnum.MARTIAL) ? "martial/" : "simple/");
            if (this == MELEE_WEAPON) {
                tag.append("melee");
            } else if (this == RANGED_WEAPON) {
                tag.append("ranged");
            } else if (this == EXPLOSIVE) {
                tag.append("ranged/explosive");
            } else if (this == AMMUNITION) {
                tag.append("ammunition");
            } else if (this == AMMUNITION_FIREARM) {
                tag.append("ammunition/firearm");
            }
        } else if (isVehicle()) {
            tag.append("/vehicle");
            if (this == SPELLJAMMER) {
                tag.append("/spelljammer");
            } else if (this == AIRSHIP) {
                tag.append("/airship");
            } else if (this == SHIP) {
                tag.append("/ship");
            }
        } else if (isGear()) {
            tag.append("/gear");
            if (!this.specializedType.isEmpty()) {
                tag.append("/").append(tui.slugify(this.specializedType));
            }
            if (properties.contains(PropertyEnum.POISON)) {
                tag.append("/poison");
            } else if (properties.contains(PropertyEnum.CURSED)) {
                tag.append("/cursed");
            }
        } else if (isWondrousItem()) {
            tag.append("/wondrous").append(this == WONDROUS ? "" : "/").append(tui.slugify(genericType));
        } else if (isMoney()) {
            tag.append("/wealth");
        }
        return tag.toString();
    }
}
