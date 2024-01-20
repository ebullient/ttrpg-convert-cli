package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.dnd5e.ItemProperty.PropertyEnum;
import dev.ebullient.convert.tools.dnd5e.ItemType.ItemEnum;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndex.Tuple;
import dev.ebullient.convert.tools.dnd5e.qute.QuteItem;
import dev.ebullient.convert.tools.dnd5e.qute.QuteItem.Variant;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QuteItem extends Json2QuteCommon {

    final ItemType itemType;

    Json2QuteItem(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
        itemType = getItemType();
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {
        Set<ItemProperty> itemProperties = new TreeSet<>(ItemProperty.comparator); // stable order

        Variant rootVariant = createVariant(rootNode, itemProperties);
        List<ImageRef> fluffImages = new ArrayList<>();
        String text = itemText(itemProperties, fluffImages);
        String detail = itemDetail(itemProperties);

        Tags tags = new Tags(getSources());
        tags.addRaw("item", itemType.getItemTag(itemProperties, tui()));
        for (ItemProperty p : itemProperties) {
            tags.addRaw("item", p.tagValue());
        }

        List<Variant> variants = new ArrayList<>();
        if (ItemFields._variants.existsIn(rootNode)) {
            for (JsonNode variantNode : iterableElements(ItemFields._variants.getFrom(rootNode))) {
                variants.add(createVariant(variantNode, new TreeSet<>(ItemProperty.comparator)));
            }
        }

        return new QuteItem(sources,
                rootVariant.name,
                getSourceText(sources),
                itemType.getSpecializedType() + (detail.isBlank() ? "" : ", " + detail),
                rootVariant.armorClass,
                rootVariant.damage,
                rootVariant.damage2h,
                rootVariant.range,
                rootVariant.properties,
                rootVariant.strengthRequirement,
                rootVariant.stealthPenalty,
                rootVariant.cost,
                rootVariant.costCp,
                rootVariant.weight,
                rootVariant.prerequisite,
                text,
                fluffImages,
                variants,
                tags);
    }

    private Variant createVariant(JsonNode variantNode, Set<ItemProperty> itemProperties) {
        findProperties(itemProperties);

        String properties = itemProperties.stream()
                .filter(PropertyEnum::mundaneProperty)
                .map(x -> x.getMarkdownLink(index))
                .collect(Collectors.joining(", "));

        Integer strength = variantNode.has("strength")
                ? variantNode.get("strength").asInt()
                : null;
        Double weight = variantNode.has("weight")
                ? variantNode.get("weight").asDouble()
                : null;
        String range = variantNode.has("range")
                ? variantNode.get("range").asText()
                : null;

        String damage = null;
        String damage2h = null;
        if (variantNode.has("dmgType")) {
            String dmg1 = getTextOrDefault(variantNode, "dmg1", null);
            String dmg2 = getTextOrDefault(variantNode, "dmg2", null);
            String dmgType = getTextOrDefault(variantNode, "dmgType", null);
            damage = dmg1 + " " + dmgType;
            if (dmg2 != null && !dmg2.isBlank()) {
                damage2h = dmg2 + " " + dmgType;
            }
        }

        return new Variant(
                itemName(variantNode),
                armorClass(variantNode),
                damage,
                damage2h,
                range,
                properties,
                strength,
                booleanOrDefault(variantNode, "stealth", false),
                coinValue(variantNode),
                ItemFields.value.getIntFrom(variantNode).orElse(null), // cpValue
                weight,
                listPrerequisites(variantNode));
    }

    private String coinValue(JsonNode variantNode) {
        if (variantNode.has("value")) {
            return convertCurrency(variantNode.get("value").asInt());
        }
        return null;
    }

    String itemName(JsonNode variantNode) {
        JsonNode srd = variantNode.get("srd");
        Tools5eSources vSources = Tools5eSources.findSources(variantNode);
        if (srd != null) {
            if (index().sourceIncluded(vSources.primarySource())) {
                return vSources.getName();
            }
            if (srd.isTextual()) {
                return srd.asText();
            }
        }
        return vSources.getName();
    }

    String itemText(Collection<ItemProperty> propertyEnums, List<ImageRef> imageRef) {
        List<String> text = getFluff(Tools5eIndexType.itemFluff, "##", imageRef);

        if (rootNode.has("entries")) {
            maybeAddBlankLine(text);
            for (JsonNode entry : iterableEntries(rootNode)) {
                if (entry.isTextual()) {
                    String input = entry.asText();
                    if (input.startsWith("{#itemEntry ")) {
                        insertItemRefText(text, input);
                    } else {
                        maybeAddBlankLine(text);
                        text.add(replaceText(input));
                    }
                } else {
                    appendToText(text, entry, "##");
                }
            }
        }
        PropertyEnum.findAdditionalProperties(getName(),
                itemType, propertyEnums, s -> text.stream().anyMatch(l -> l.matches(s)));

        return text.isEmpty() ? null : String.join("\n", text);
    }

    void insertItemRefText(List<String> text, String input) {
        String finalKey = Tools5eIndexType.itemEntry.fromRawKey(input.replaceAll("\\{#itemEntry (.*)}", "$1"));
        if (finalKey == null || index.isExcluded(finalKey)) {
            return;
        }
        JsonNode ref = index.getNode(finalKey);
        if (ref == null) {
            tui().errorf("Could not find %s from %s", finalKey, getSources());
        } else if (index.sourceIncluded(ref.get("source").asText())) {
            try {
                String entriesTemplate = mapper().writeValueAsString(ref.get("entriesTemplate"));
                if (rootNode.has("detail1")) {
                    entriesTemplate = entriesTemplate.replaceAll("\\{\\{item.detail1}}",
                            rootNode.get("detail1").asText());
                }
                if (rootNode.has("resist")) {
                    entriesTemplate = entriesTemplate.replaceAll("\\{\\{item.resist}}",
                            joinAndReplace(rootNode, "resist"));
                }
                appendToText(text, mapper().readTree(entriesTemplate), "##");
            } catch (JsonProcessingException e) {
                tui().errorf(e, "Unable to insert item element text for %s from %s", input, getSources());
            }
        }
    }

    String armorClass(JsonNode variantNode) {
        if (!variantNode.has("ac")) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        result.append(variantNode.get("ac").asText());
        // - If you wear light armor, you add your Dexterity modifier to the base number from your armor type to determine your Armor Class.
        // - If you wear medium armor, you add your Dexterity modifier, to a maximum of +2, to the base number from your armor type to determine your Armor Class.
        // - Heavy armor does not let you add your Dexterity modifier to your Armor Class, but it also does not penalize you if your Dexterity modifier is negative.
        if (itemType == ItemEnum.LIGHT_ARMOR) {
            result.append(" + DEX");
        } else if (itemType == ItemEnum.MEDIUM_ARMOR) {
            result.append(" + DEX (max of +2)");
        }
        return result.toString();
    }

    ItemType getItemType() {
        try {
            String type = getTextOrDefault(rootNode, "type", "");
            if (type.isEmpty()) {
                if (booleanOrDefault(rootNode, "staff", false)) {
                    return ItemEnum.STAFF;
                }
                if (booleanOrDefault(rootNode, "poison", false)) {
                    return ItemEnum.GEAR;
                }
                if (booleanOrDefault(rootNode, "wondrous", false)
                        || booleanOrDefault(rootNode, "sentient", false)) {
                    return ItemEnum.WONDROUS;
                }
                if (rootNode.has("rarity")) {
                    return ItemEnum.WONDROUS;
                }
            }
            return index.findItemType(type, getSources());
        } catch (IllegalArgumentException e) {
            tui().errorf(e, "Unable to parse text for item %s", getSources());
            throw e;
        }
    }

    void findProperties(Collection<ItemProperty> itemProperties) {
        JsonNode property = rootNode.get("property");
        if (property != null && property.isArray()) {
            for (JsonNode x : iterableElements(property)) {
                ItemProperty prop = index.findItemProperty(x.asText(), getSources());
                if (prop != null) {
                    itemProperties.add(prop);
                }
            }
        }
        String category = getTextOrEmpty(rootNode, "weaponCategory");
        if ("martial".equals(category)) {
            itemProperties.add(PropertyEnum.MARTIAL);
        }
    }

    /**
     * @param itemProperties Item properties -- ensure non-null & modifiable: side-effect, will set magic properties
     * @return String containing formatted item text
     */
    String itemDetail(Collection<ItemProperty> itemProperties) {
        String tier = getTextOrDefault(rootNode, "tier", "");
        if (!tier.isEmpty()) {
            ItemProperty p = index.findItemProperty(tier, getSources());
            if (p != null) {
                itemProperties.add(p);
            }
        }
        String rarity = rootNode.has("rarity")
                ? rootNode.get("rarity").asText()
                : "";
        if (!rarity.isEmpty() && !"none".equals(rarity)) {
            ItemProperty p = index.findItemProperty(rarity, getSources());
            if (p != null) {
                itemProperties.add(p);
            }
        }
        String attunement = getTextOrDefault(rootNode, "reqAttune", "");
        String detail = createDetail(attunement, itemProperties);
        return replaceText(detail);
    }

    /**
     * @param attunement blank if false, "true" for default string, "optional" if attunement is optional, or some other specific
     *        string
     * @param properties Item properties -- ensure non-null & modifiable: side-effect, will set magic properties
     * @return detail string
     */
    String createDetail(String attunement, Collection<ItemProperty> properties) {
        StringBuilder replacement = new StringBuilder();

        PropertyEnum.tierProperties.forEach(p -> {
            if (properties.contains(p)) {
                if (replacement.length() > 0) {
                    replacement.append(", ");
                }
                replacement.append(p.value());
            }
        });
        PropertyEnum.rarityProperties.forEach(p -> {
            if (properties.contains(p)) {
                if (replacement.length() > 0) {
                    replacement.append(", ");
                }
                replacement.append(p.value());
            }
        });

        properties.stream().filter(PropertyEnum::homebrewProperty).forEach(p -> {
            if (replacement.length() > 0) {
                replacement.append(", ");
            }
            replacement.append(p.value());
        });

        if (properties.contains(PropertyEnum.POISON)) {
            if (replacement.length() > 0) {
                replacement.append(", ");
            }
            replacement.append(PropertyEnum.POISON.value());
        }
        if (properties.contains(PropertyEnum.CURSED)) {
            if (replacement.length() > 0) {
                replacement.append(", ");
            }
            replacement.append(PropertyEnum.CURSED.value());
        }

        switch (attunement) {
            case "":
            case "false":
                break;
            case "true":
                properties.add(PropertyEnum.REQ_ATTUNEMENT);
                replacement.append(" (requires attunement)");
                break;
            case "optional":
                properties.add(PropertyEnum.OPT_ATTUNEMENT);
                replacement.append(" (attunement optional)");
                break;
            default:
                properties.add(PropertyEnum.REQ_ATTUNEMENT);
                replacement.append(" (requires attunement ")
                        .append(attunement).append(")");
                break;
        }
        return replacement.toString();
    }

    /** Update / replace item with variants (where appropriate) */
    public static List<Tuple> findGroupVariant(Tools5eIndex index, Tools5eIndexType type,
            String key, JsonNode itemGroup, JsonSourceCopier copier) {

        // Update type & key for the new item
        final JsonNode item = copier.copyNode(itemGroup);
        String newKey = Tools5eIndexType.item.createKey(item);
        index.addAlias(key, newKey);
        TtrpgValue.indexInputType.setIn(item, Tools5eIndexType.item.name());
        TtrpgValue.indexKey.setIn(item, newKey);
        Tools5eSources.constructSources(item);
        return List.of(new Tuple(newKey, item));
    }

    enum ItemFields implements JsonNodeReader {
        _variants,
        baseItem,
        hasFluff,
        hasFluffImages,
        property,
        type,
        value,
        weight,
    }
}
