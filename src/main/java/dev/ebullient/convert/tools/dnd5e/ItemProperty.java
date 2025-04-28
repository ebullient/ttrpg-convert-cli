package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.toAnchorTag;
import static dev.ebullient.convert.StringUtil.valueOrDefault;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonTextConverter.SourceField;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.dnd5e.JsonSource.Tools5eFields;

record ItemProperty(
        String name,
        String abbreviation,
        String indexKey,
        String sectionName,
        String tag) {

    public String toString() {
        return "Property: " + name;
    }

    public String linkify() {
        return linkify(null);
    }

    public String linkify(String linkText) {
        Tools5eIndex index = Tools5eIndex.instance();
        linkText = isPresent(linkText) ? linkText : name;

        boolean included = isPresent(indexKey)
                ? index.isIncluded(indexKey)
                : index.customContentIncluded();

        return included
                ? "[%s](%sitem-properties.md#%s)".formatted(
                        linkText, index.rulesVaultRoot(),
                        toAnchorTag(isPresent(sectionName) ? sectionName : name))
                : linkText;
    }

    public static final Comparator<ItemProperty> comparator = Comparator.comparing(ItemProperty::name);
    public static final Map<String, ItemProperty> propertyMap = new HashMap<>();

    public static final ItemProperty CURSED = ItemProperty.customProperty("Cursed", "Cursed Items", "=");
    public static final ItemProperty SILVERED = ItemProperty.customProperty("Silvered", "Silvered Weapons", "=");
    public static final ItemProperty POISON = ItemProperty.customProperty("Poison", "=");

    public static ItemProperty forKey(String key) {
        if (!isPresent(key)) {
            return null;
        }
        return propertyMap.get(key);
    }

    public static ItemProperty fromNode(JsonNode property) {
        if (property == null) {
            return null;
        }
        String key = TtrpgValue.indexKey.getTextOrEmpty(property);
        if (key.isEmpty()) {
            Tui.instance().warnf(Msg.NOT_SET.wrap("Index key not found for property %s"), property);
            return null;
        }
        // Create the ItemType object once
        return ItemProperty.propertyMap.computeIfAbsent(key, k -> {
            String abbreviation = Tools5eFields.abbreviation.getTextOrEmpty(property);
            String sectionName = null;

            String name = SourceField.name.getTextOrNull(property);
            if (name == null) {
                JsonNode firstEntry = SourceField.entries.getFirstFromArray(property);
                if (firstEntry == null) {
                    firstEntry = Tools5eFields.entriesTemplate.getFirstFromArray(property);
                }
                if (firstEntry != null) {
                    name = SourceField.name.getTextOrNull(firstEntry);
                }
                if (name == null) {
                    Tui.instance().warnf(Msg.NOT_SET.wrap("Name not found for property %s"), key);
                    name = abbreviation;
                }
                // we've fished it out. remember it.
                SourceField.name.setIn(property, name);
            } else if ("AF".equals(abbreviation)) {
                // Special case for firearms to distinguish from regular ammunition
                name = "Ammunition (Firearm)";
            } else if ("S".equals(abbreviation)) {
                // Special, as the name, doesn't match the property/rules section
                sectionName = "Special Weapons";
            }

            // item property tag w/ a few fixes
            String tag = ItemTag.property.build(name
                    .toLowerCase()
                    .replace("extended reach", "reach/extended"));

            return new ItemProperty(
                    name,
                    abbreviation,
                    key,
                    sectionName,
                    tag);
        });
    }

    public static List<String> asLinks(Collection<ItemProperty> properties) {
        return properties.stream()
                .map(ItemProperty::linkify)
                .toList();
    }

    /**
     * Invented properties. No relevance to source material, but useful for
     * links to rules, e.g. Poison.
     *
     * @param name
     * @param sectionName Section heading in rules
     * @param abbreviation
     */
    public static ItemProperty customProperty(String name, String sectionName, String abbreviation) {
        return propertyMap.computeIfAbsent(sectionName, k -> {
            return new ItemProperty(
                    name,
                    abbreviation,
                    "",
                    sectionName,
                    ItemTag.property.build(name));
        });
    }

    /**
     * Invented properties. No relevance to source material, but useful for
     * links to rules, e.g. Poison.
     *
     * @param name
     * @param abbreviation
     * @return
     */
    public static ItemProperty customProperty(String name, String abbreviation) {
        return ItemProperty.customProperty(name, name, abbreviation);
    }

    public static void clear() {
        propertyMap.clear();
    }

    public static String refTagToKey(String text) {
        String[] parts = text.split("\\|");
        String abv = parts[0].trim();
        String source = defaultItemSource(abv,
                valueOrDefault(parts, 1, "PHB"));

        return Tools5eIndexType.itemProperty.createKey(abv, source);
    }

    private static String defaultItemSource(String code, String source) {
        boolean xphbAvailable = TtrpgConfig.getConfig().sourceIncluded("XPHB") || Tools5eSources.has2024basicSrd();
        boolean xdmgAvailable = TtrpgConfig.getConfig().sourceIncluded("XDMG") || Tools5eSources.has2024basicSrd();
        // reprints work mostly, but a few changed default between phb and dmg
        return switch (code.toUpperCase()) {
            // PHB <-> XPHB
            case "2H", // two-handed
                    "A", // ammunition
                    "F", // finesse
                    "H", // heavy
                    "L", // light
                    "LD", // loading
                    "R", // reach
                    "T", // thrown
                    "V" // versatile
                -> xphbAvailable ? "XPHB" : "PHB";

            // DMG <-> XDMG
            case "AF", // ammunition (futuristic)
                    "BF", // burst fire
                    "RLD" // reload
                -> xdmgAvailable ? "XDMG" : "DMG";

            case "ER" -> "TDCSR"; // extended reach
            case "S" -> "PHB"; // special
            case "VST" -> "TDCSR"; // vestige of divergence
            default -> source;
        };
    }
}

// Parser.ITM_PROP_ABV__TWO_HANDED = "2H";
// Parser.ITM_PROP_ABV__AMMUNITION = "A";
// Parser.ITM_PROP_ABV__AMMUNITION_FUTURISTIC = "AF";
// Parser.ITM_PROP_ABV__BURST_FIRE = "BF";
// Parser.ITM_PROP_ABV__EXTENDED_REACH = "ER";
// Parser.ITM_PROP_ABV__FINESSE = "F";
// Parser.ITM_PROP_ABV__HEAVY = "H";
// Parser.ITM_PROP_ABV__LIGHT = "L";
// Parser.ITM_PROP_ABV__LOADING = "LD";
// Parser.ITM_PROP_ABV__OTHER = "OTH";
// Parser.ITM_PROP_ABV__REACH = "R";
// Parser.ITM_PROP_ABV__RELOAD = "RLD";
// Parser.ITM_PROP_ABV__SPECIAL = "S";
// Parser.ITM_PROP_ABV__THROWN = "T";
// Parser.ITM_PROP_ABV__VERSATILE = "V";
// Parser.ITM_PROP_ABV__VESTIGE_OF_DIVERGENCE = "Vst";

// Parser.ITM_PROP__TWO_HANDED = "2H";
// Parser.ITM_PROP__AMMUNITION = "A";
// Parser.ITM_PROP__AMMUNITION_FUTURISTIC = "AF|DMG";
// Parser.ITM_PROP__BURST_FIRE = "BF|DMG";
// Parser.ITM_PROP__EXTENDED_REACH = "ER|TDCSR";
// Parser.ITM_PROP__FINESSE = "F";
// Parser.ITM_PROP__HEAVY = "H";
// Parser.ITM_PROP__LIGHT = "L";
// Parser.ITM_PROP__LOADING = "LD";
// Parser.ITM_PROP__OTHER = "OTH";
// Parser.ITM_PROP__REACH = "R";
// Parser.ITM_PROP__RELOAD = "RLD|DMG";
// Parser.ITM_PROP__SPECIAL = "S";
// Parser.ITM_PROP__THROWN = "T";
// Parser.ITM_PROP__VERSATILE = "V";
// Parser.ITM_PROP__VESTIGE_OF_DIVERGENCE = "Vst|TDCSR";

// Parser.ITM_PROP__ODND_TWO_HANDED = "2H|XPHB";
// Parser.ITM_PROP__ODND_AMMUNITION = "A|XPHB";
// Parser.ITM_PROP__ODND_FINESSE = "F|XPHB";
// Parser.ITM_PROP__ODND_HEAVY = "H|XPHB";
// Parser.ITM_PROP__ODND_LIGHT = "L|XPHB";
// Parser.ITM_PROP__ODND_LOADING = "LD|XPHB";
// Parser.ITM_PROP__ODND_REACH = "R|XPHB";
// Parser.ITM_PROP__ODND_THROWN = "T|XPHB";
// Parser.ITM_PROP__ODND_VERSATILE = "V|XPHB";
