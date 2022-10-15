package dev.ebullient.json5e.tools5e;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.json5e.qute.QuteItem;
import dev.ebullient.json5e.qute.QuteSource;

public class Json2QuteItem extends Json2QuteCommon {

    final ItemEnum itemType;

    Json2QuteItem(JsonIndex index, IndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
        itemType = getItemType();
    }

    @Override
    public QuteSource build() {
        List<PropertyEnum> propertyEnums = new ArrayList<>();
        findProperties(propertyEnums);
        String text = itemText(propertyEnums);

        String detail = itemDetail(propertyEnums);
        String properties = propertyEnums.stream()
                .map(x -> x.getMarkdownLink(index))
                .collect(Collectors.joining(", "));

        List<String> tags = new ArrayList<>(sources.getSourceTags());

        tags.add(itemType.getItemTag(propertyEnums, tui()));
        for (PropertyEnum p : propertyEnums) {
            tags.add("item/" + p.tagValue());
        }

        Integer strength = node.has("strength")
                ? node.get("strength").asInt()
                : null;
        Double weight = node.has("weight")
                ? node.get("weight").asDouble()
                : null;
        String range = node.has("range")
                ? node.get("range").asText()
                : null;
        boolean stealthPenalty = booleanOrDefault(node, "stealth", false);

        String damage = null;
        String damage2h = null;
        if (node.has("dmgType")) {
            String dmg1 = getTextOrDefault(node, "dmg1", null);
            String dmg2 = getTextOrDefault(node, "dmg2", null);
            String dmgType = getTextOrDefault(node, "dmgType", null);
            damage = dmg1 + " " + dmgType;
            if (dmg2 != null && !dmg2.isBlank()) {
                damage2h = dmg2 + " " + dmgType;
            }
        }

        return new QuteItem(sources,
                itemName(),
                sources.getSourceText(index.srdOnly()),
                itemType.getSpecializedType() + (detail.isBlank() ? "" : ", " + detail),
                armorClass(),
                damage, damage2h,
                range, properties,
                strength, stealthPenalty, gpValue(), weight,
                text,
                tags);
    }

    private String gpValue() {
        if (node.has("value")) {
            return Currency.coinValue(node.get("value").asInt());
        }
        return null;
    }

    String itemName() {
        JsonNode srd = node.get("srd");
        if (srd != null) {
            if (index().sourceIncluded(getSources().primarySource())) {
                return getSources().getName();
            }
            if (srd.isTextual()) {
                return srd.asText();
            }
        }
        return decoratedUaName(getSources().getName(), getSources());
    }

    String itemText(List<PropertyEnum> propertyEnums) {
        List<String> text = new ArrayList<>(getFluff(IndexType.itemfluff, "##"));
        if (node.has("entries")) {
            maybeAddBlankLine(text);
            node.withArray("entries").forEach(entry -> {
                if (entry.isTextual()) {
                    String input = entry.asText();
                    if (input.startsWith("{#itemEntry ")) {
                        insertItemRefText(text, input);
                    } else {
                        maybeAddBlankLine(text);
                        text.add(replaceText(input));
                    }
                } else {
                    appendEntryToText(text, entry, "##");
                }
            });
        }
        PropertyEnum.findAdditionalProperties(getName(),
                itemType, propertyEnums, s -> text.stream().anyMatch(l -> l.matches(s)));

        return text.isEmpty() ? null : String.join("\n", text);
    }

    void insertItemRefText(List<String> text, String input) {
        String finalKey = index.getRefKey(IndexType.itementry, input.replaceAll("\\{#itemEntry (.*)}", "$1"));
        if (index.isExcluded(finalKey)) {
            return;
        }
        JsonNode ref = index.getNode(finalKey);
        if (ref == null) {
            tui().errorf("Could not find %s from %s", finalKey, getSources());
        } else if (index.sourceIncluded(ref.get("source").asText())) {
            try {
                String entriesTemplate = mapper().writeValueAsString(ref.get("entriesTemplate"));
                if (node.has("detail1")) {
                    entriesTemplate = entriesTemplate.replaceAll("\\{\\{item.detail1}}",
                            node.get("detail1").asText());
                }
                if (node.has("resist")) {
                    entriesTemplate = entriesTemplate.replaceAll("\\{\\{item.resist}}",
                            joinAndReplace(node, "resist"));
                }
                appendEntryToText(text, mapper().readTree(entriesTemplate), "##");
            } catch (JsonProcessingException e) {
                tui().errorf(e, "Unable to insert item element text for %s from %s", input, getSources());
            }
        }
    }

    String armorClass() {
        if (!node.has("ac")) {
            return null;
        }

        StringBuilder result = new StringBuilder();
        result.append(node.get("ac").asText());
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

    ItemEnum getItemType() {
        try {
            String type = getTextOrDefault(node, "type", "");
            if (!type.isEmpty()) {
                return ItemEnum.fromEncodedValue(type);
            }
            if (booleanOrDefault(node, "staff", false)) {
                return ItemEnum.STAFF;
            }
            if (booleanOrDefault(node, "poison", false)) {
                return ItemEnum.GEAR;
            }
            if (booleanOrDefault(node, "wondrous", false)
                    || booleanOrDefault(node, "sentient", false)) {
                return ItemEnum.WONDROUS;
            }
            if (node.has("rarity")) {
                return ItemEnum.WONDROUS;
            }
            throw new IllegalArgumentException("Unknown type");
        } catch (IllegalArgumentException e) {
            tui().errorf(e, "Unable to parse text for item %s", getSources());
            throw e;
        }
    }

    void findProperties(List<PropertyEnum> propertyEnums) {
        JsonNode property = node.get("property");
        if (property != null && property.isArray()) {
            property.forEach(x -> propertyEnums.add(PropertyEnum.fromEncodedType(x.asText())));
        }
        String category = getTextOrEmpty(node, "weaponCategory");
        if ("martial".equals(category)) {
            propertyEnums.add(PropertyEnum.MARTIAL);
        }
    }

    /**
     * @param propertyEnums Item properties -- ensure non-null & modifiable: side-effect, will set magic properties
     * @return String containing formatted item text
     */
    String itemDetail(List<PropertyEnum> propertyEnums) {
        String tier = getTextOrDefault(node, "tier", "");
        if (!tier.isEmpty()) {
            propertyEnums.add(PropertyEnum.fromValue(tier));
        }
        String rarity = node.has("rarity")
                ? node.get("rarity").asText()
                : "";
        if (!rarity.isEmpty() && !"none".equals(rarity)) {
            propertyEnums.add(PropertyEnum.fromValue(rarity));
        }
        String attunement = getTextOrDefault(node, "reqAttune", "");
        String detail = createDetail(attunement, propertyEnums);
        return replaceText(detail);
    }

    /**
     * @param attunement blank if false, "true" for default string, "optional" if attunement is optional, or some other specific
     *        string
     * @param properties Item properties -- ensure non-null & modifiable: side-effect, will set magic properties
     * @return detail string
     */
    String createDetail(String attunement, List<PropertyEnum> properties) {
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

}
