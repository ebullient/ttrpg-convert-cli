package dev.ebullient.convert.tools.dnd5e;

import java.util.Collection;
import java.util.Locale;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;

public interface ItemType {

    boolean isWeapon();

    String getSpecializedType();

    String getItemTag(Collection<ItemProperty> itemProperties, Tui tui);

    class CustomItemType implements ItemType {
        final String name;
        final String lower;

        public CustomItemType(JsonNode typeNode) {
            this.name = typeNode.get("name").asText();
            this.lower = name.toLowerCase(Locale.ROOT);
        }

        @Override
        public boolean isWeapon() {
            return lower.contains("weapon")
                    || lower.contains("firearm")
                    || lower.contains("explosive")
                    || lower.contains("ammo");
        }

        @Override
        public String getItemTag(Collection<ItemProperty> itemProperties, Tui tui) {
            if (lower.contains("armor")) {
                return "armor/" + Tui.slugify(lower.replaceAll("\\s*armor\\s*", ""));
            }
            if (lower.contains("vehicle")) {
                return "vehicle/" + Tui.slugify(lower.replaceAll("\\s*vehicle\\s*", ""));
            }
            if (lower.contains("wondrous")) {
                return "wondrous/" + Tui.slugify(lower.replaceAll("\\s*wondrous( item)?\\s*", ""));
            }

            if (lower.contains("ammunition")) {
                return "weapon/"
                        + "ammunition/" + Tui.slugify(lower.replaceAll("\\s*ammunition\\s*", ""));
            }
            if (lower.contains("ammo")) {
                return "weapon/"
                        + "ammunition/" + Tui.slugify(lower.replaceAll("\\s*ammo\\s*", ""));
            }
            if (lower.contains("explosive")) {
                return "weapon/"
                        + "explosive/" + Tui.slugify(lower.replaceAll("\\s*explosive\\s*", ""));
            }
            if (lower.contains("firearm")) {
                return "weapon/"
                        + (itemProperties.contains(ItemProperty.PropertyEnum.MARTIAL) ? "martial/" : "simple/")
                        + "firearm/" + Tui.slugify(lower.replaceAll("\\s*firearm\\s*", ""));
            }
            if (lower.contains("weapon")) {
                return "weapon/"
                        + (itemProperties.contains(ItemProperty.PropertyEnum.MARTIAL) ? "martial/" : "simple/")
                        + Tui.slugify(lower.replaceAll("\\s*weapon\\s*", ""));
            }

            return "gear"
                    + (itemProperties.contains(ItemProperty.PropertyEnum.POISON) ? "/poison" : "")
                    + (itemProperties.contains(ItemProperty.PropertyEnum.CURSED) ? "/cursed" : "")
                    + "/" + Tui.slugify(lower.replaceAll("\\s*gear\\s*", ""));
        }

        @Override
        public String getSpecializedType() {
            return name;
        }
    }

    enum ItemEnum implements ItemType {

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
        IDG("Adventuring Gear", "IDG", "Illegal Drug"),

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
                return null;
            }
            for (ItemEnum i : ItemEnum.values()) {
                if (i.encodedValue.equals(v)) {
                    return i;
                }
            }
            return null;
        }

        public boolean isWeapon() {
            int x = this.ordinal();
            return x >= MELEE_WEAPON.ordinal() && x <= AMMUNITION_FIREARM.ordinal();
        }

        public boolean isArmor() {
            int x = this.ordinal();
            return x >= LIGHT_ARMOR.ordinal() && x <= SHIELD.ordinal();
        }

        public boolean isGear() {
            int x = this.ordinal();
            return x >= ARTISANS_TOOLS.ordinal() && x <= GEAR.ordinal();
        }

        public boolean isMoney() {
            return this == WEALTH;
        }

        public boolean isVehicle() {
            int x = this.ordinal();
            return x >= AIRSHIP.ordinal() && x <= VEHICLE.ordinal();
        }

        public boolean isWondrousItem() {
            int x = this.ordinal();
            return x >= ROD.ordinal() && x <= WONDROUS.ordinal();
        }

        public String getItemTag(Collection<ItemProperty> properties, Tui tui) {
            StringBuilder tag = new StringBuilder();
            if (isArmor()) {
                tag.append("armor/");
                tag.append(lower.replace(" armor", ""));
            } else if (isWeapon()) {
                tag.append("weapon/");
                if (this == MELEE_WEAPON) {
                    tag.append(properties.contains(ItemProperty.PropertyEnum.MARTIAL) ? "martial/" : "simple/");
                    tag.append("melee");
                } else if (this == RANGED_WEAPON) {
                    tag.append(properties.contains(ItemProperty.PropertyEnum.MARTIAL) ? "martial/" : "simple/");
                    tag.append("ranged");
                } else if (this == EXPLOSIVE) {
                    tag.append("explosive");
                } else if (this == AMMUNITION) {
                    tag.append("ammunition");
                } else if (this == AMMUNITION_FIREARM) {
                    tag.append("ammunition/firearm");
                }
            } else if (isVehicle()) {
                tag.append("vehicle");
                if (this == SPELLJAMMER) {
                    tag.append("/spelljammer");
                } else if (this == AIRSHIP) {
                    tag.append("/airship");
                } else if (this == SHIP) {
                    tag.append("/ship");
                }
            } else if (isGear()) {
                tag.append("gear");
                if (!this.specializedType.isEmpty()) {
                    tag.append("/").append(Tui.slugify(this.specializedType));
                }
                if (properties.contains(ItemProperty.PropertyEnum.POISON)) {
                    tag.append("/poison");
                } else if (properties.contains(ItemProperty.PropertyEnum.CURSED)) {
                    tag.append("/cursed");
                }
            } else if (isWondrousItem()) {
                tag.append("wondrous");
                if (this != WONDROUS) {
                    tag.append("/").append(Tui.slugify(genericType));
                }
            } else if (isMoney()) {
                tag.append("wealth");
            }
            return tag.toString();
        }
    }
}
