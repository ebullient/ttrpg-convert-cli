package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.joinConjunct;
import static dev.ebullient.convert.StringUtil.uppercaseFirst;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.QuteItem;
import dev.ebullient.convert.tools.dnd5e.qute.QuteItem.Variant;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QuteItem extends Json2QuteCommon {
    static final List<String> hiddenRarity = List.of("none", "unknown", "unknown (magic)", "varies");

    Json2QuteItem(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {

        Tags tags = new Tags(getSources());
        Variant rootVariant = createVariant(rootNode, tags);

        List<Variant> variants = new ArrayList<>();
        if (ItemField._variants.existsIn(rootNode)) {
            for (JsonNode variantNode : iterableElements(ItemField._variants.getFrom(rootNode))) {
                Variant variant = createVariant(variantNode, tags);
                variants.add(variant);
            }
        }

        List<ImageRef> fluffImages = new ArrayList<>();
        String text = itemText(fluffImages);

        if (type == Tools5eIndexType.itemGroup) {
            List<String> itemLinks = ItemField.items.linkifyListFrom(rootNode, Tools5eIndexType.item, this);
            text = (text == null ? "" : text + "\n\n")
                    + "**Items in this group:**\n\n- " + String.join("\n- ", itemLinks);
        }

        return new QuteItem(sources,
                getSourceText(sources),
                rootVariant,
                variants,
                fluffImages,
                text,
                tags);
    }

    private Variant createVariant(JsonNode variantNode, Tags tags) {
        Tools5eSources variantSources = Tools5eSources.findOrTemporary(variantNode);
        boolean pushed = parseState().push(variantNode);
        try {
            ItemType itemType = getItemType(variantSources, variantNode, ItemField.type);
            ItemType itemTypeAlt = getItemType(variantSources, variantNode, ItemField.typeAlt);

            Set<ItemProperty> itemProperties = new TreeSet<>(ItemProperty.comparator);
            findProperties(variantSources, variantNode, itemProperties, itemType, itemTypeAlt);

            Set<ItemMastery> itemMasteries = new TreeSet<>(ItemMastery.comparator);
            findMastery(variantSources, variantNode, itemMasteries);

            String damage = null;
            String damage2h = null;
            if (ItemField.dmgType.existsIn(variantNode)) {
                String dmg1 = ItemField.dmg1.getTextOrEmpty(variantNode);
                String dmg2 = ItemField.dmg2.getTextOrEmpty(variantNode);
                String dmgType = damageTypeToFull(ItemField.dmgType.getTextOrEmpty(variantNode));
                damage = dmg1 + " " + dmgType;
                if (dmg2 != null && !dmg2.isBlank()) {
                    damage2h = dmg2 + " " + dmgType;
                }
            }

            String baseItemKey = ItemField.baseItem.getTextOrEmpty(variantNode);
            String baseItem = linkify(Tools5eIndexType.item, baseItemKey);
            boolean baseItemIncluded = false;

            boolean ammo = ItemField.ammo.booleanOrDefault(variantNode, false);
            boolean cursed = ItemField.curse.booleanOrDefault(variantNode, false);
            boolean firearm = ItemField.firearm.booleanOrDefault(variantNode, false);
            boolean poison = ItemField.poison.booleanOrDefault(variantNode, false);
            boolean staff = ItemField.staff.booleanOrDefault(variantNode, false);
            boolean tattoo = ItemField.tattoo.booleanOrDefault(variantNode, false);
            boolean wondrous = ItemField.wondrous.booleanOrDefault(variantNode, false);

            boolean focus = ItemField.focus.existsIn(variantNode)
                    || ItemField.scfType.existsIn(variantNode);

            String age = ItemField.age.getTextOrEmpty(variantNode);
            String weaponCategory = ItemField.weaponCategory.getTextOrEmpty(variantNode);

            String attunement = attunement(variantNode);
            String rarity = ItemField.rarity.getTextOrEmpty(variantNode);
            String tier = ItemField.tier.getTextOrEmpty(variantNode);

            String poisonTypes = poison
                    ? joinConjunct(" or ", ItemField.poisonTypes.getListOfStrings(variantNode, tui()))
                    : null;

            // -- render.js -------------------------
            // const [typeListText, typeHtml, subTypeHtml] =
            // Renderer.item.getHtmlAndTextTypes(item);
            // Building typeDescription and subtypeDescription in a stable order
            List<String> typeDescription = new ArrayList<>();
            List<String> subTypeDescription = new ArrayList<>();

            if (wondrous) {
                typeDescription.add("wondrous item" + (tattoo ? " (tattoo)" : ""));
                if (tattoo) {
                    ItemTag.wondrous.add(tags, "tattoo");
                }
            }
            if (staff) {
                typeDescription.add("staff");
            }
            if (ammo) {
                typeDescription.add("ammunition");
            }
            if (isPresent(age)) {
                ItemTag.age.add(tags, age);
                subTypeDescription.add(age);
            }
            if (isPresent(weaponCategory)) {
                ItemTag.weapon.add(tags, weaponCategory);
                baseItemIncluded = isPresent(baseItem);
                typeDescription.add("weapon"
                        + (baseItemIncluded ? " (" + baseItem + ")" : ""));
                subTypeDescription.add(weaponCategory + " weapon");
            }
            if (staff && (EncodedType.M.typeIn(itemType, itemTypeAlt))) {
                // "M" --> Type: Melee weapon
                // DMG p140: "Unless a staff's description says otherwise, a staff can be used
                // as a quarterstaff."
                subTypeDescription.add("melee weapon");
            }
            if (itemType != null) {
                tags.addRaw(ItemType.tagForType(itemType, tui()));
                processType(itemType, typeDescription, subTypeDescription, baseItem, baseItemIncluded);
                subTypeDescription.add(itemType.linkify());
            }
            if (itemTypeAlt != null) {
                tags.addRaw(ItemType.tagForType(itemTypeAlt, tui()));
                processType(itemTypeAlt, typeDescription, subTypeDescription, baseItem, baseItemIncluded);
                subTypeDescription.add(itemTypeAlt.linkify());
            }
            if (firearm) {
                subTypeDescription.add("firearm");
            }
            if (poison) {
                itemProperties.add(ItemProperty.POISON);
                typeDescription.add("poison" + (isPresent(poisonTypes) ? " (" + poisonTypes + ")" : ""));
            }
            if (cursed) {
                itemProperties.add(ItemProperty.CURSED);
                typeDescription.add("cursed item");
            }

            // Begin creation of detail string;
            // render.js getAttunementAndAttunementCatText(item);
            // getTypeRarityAndAttunementText(item);
            // getTypeRarityAndAttunementHtml
            String detail = join(", ", typeDescription);
            if ("other".equals(detail)) {
                detail = "";
            }

            if (isPresent(tier)) {
                ItemTag.tier.add(tags, tier);
                detail += (detail.isBlank() ? "" : ", ") + tier;
            }
            if (isPresent(rarity)) {
                ItemTag.rarity.add(tags, rarity
                        .replace("very rare", "very-rare")
                        .replaceAll("[()]", "") // unknown (magic) -> unknown magic
                        .split(" "));
                if (!hiddenRarity.contains(rarity)) {
                    detail += (detail.isBlank() ? "" : ", ") + rarity;
                }
            }
            if (isPresent(attunement)) {
                ItemTag.attunement.add(tags,
                        attunement.equals("optional") ? "optional" : "required");

                detail += (detail.isBlank() ? "" : " ")
                        + switch (attunement) {
                            case "required" -> "(requires attunement)";
                            case "optional" -> "(attunement optional)";
                            default -> "(requires attunement " + attunement + ")";
                        };
            }

            return new Variant(
                    itemName(variantNode),
                    uppercaseFirst(detail),
                    uppercaseFirst(join(", ", subTypeDescription)),
                    baseItem,
                    itemType == null ? "" : itemType.name(),
                    itemTypeAlt == null ? "" : itemTypeAlt.name(),
                    ItemProperty.asLinks(itemProperties),
                    ItemMastery.asLinks(itemMasteries),
                    armorClass(variantNode, itemType, itemTypeAlt),
                    weaponCategory,
                    damage,
                    damage2h,
                    ItemField.range.getTextOrNull(variantNode),
                    ItemField.strength.intOrNull(variantNode),
                    ItemField.stealth.booleanOrDefault(variantNode, false),
                    listPrerequisites(variantNode),
                    age,
                    coinValue(variantNode),
                    ItemField.value.intOrNull(variantNode),
                    ItemField.weight.doubleOrNull(variantNode),
                    rarity,
                    tier,
                    attunement,
                    ammo,
                    firearm,
                    cursed,
                    focus,
                    focus ? focusType(variantNode) : "",
                    poison,
                    poisonTypes,
                    staff,
                    tattoo,
                    wondrous);
        } finally {
            parseState().pop(pushed);
        }
    }

    // render.js _getHtmlAndTextTypes_type
    private void processType(ItemType type,
            List<String> typeDescription, List<String> subTypeDescription,
            String baseItem, boolean baseItemIncluded) {

        String allTypes = typeDescription.toString();
        String fullType = type.lowercaseName();

        boolean isSubType = (type.group() == ItemTypeGroup.weapon && allTypes.contains("weapon"))
                || (type.group() == ItemTypeGroup.armor && allTypes.contains("armor"));
        List<String> target = isSubType ? subTypeDescription : typeDescription;

        if (EncodedType.S.typeIn(type, null)) {
            target.add("armor (" + linkify(Tools5eIndexType.item, "shield|phb") + ")");
        } else if (!baseItemIncluded && isPresent(baseItem)) {
            target.add(fullType + " (" + baseItem + ")");
        } else if (EncodedType.GV.not(type)) {
            target.add(fullType);
        }
    }

    private String attunement(JsonNode variantNode) {
        // render.js -- getAttunementAndAttunementCatText
        String attunement = ItemField.reqAttune.getTextOrEmpty(variantNode);
        if (!isPresent(attunement)) {
            attunement = ItemField.reqAttuneAlt.getTextOrEmpty(variantNode);
        }
        return switch (attunement) {
            case "", "false" -> "";
            case "true" -> "required";
            case "optional" -> "optional";
            default -> replaceText(attunement);
        };
    }

    private String focusType(JsonNode variantNode) {
        List<String> focusTypes = new ArrayList<>();
        JsonNode focusNode = ItemField.focus.getFrom(variantNode);
        if (focusNode != null && focusNode.isArray()) {
            focusNode.forEach(x -> focusTypes.add(
                    linkify(Tools5eIndexType.classtype, x.asText())));
        }
        String scfType = ItemField.scfType.getTextOrEmpty(variantNode);
        return scfType
                + (!isPresent(scfType) || focusTypes.isEmpty() ? "" : "; ")
                + join(", ", focusTypes);
    }

    private String coinValue(JsonNode variantNode) {
        Integer value = ItemField.value.intOrNull(variantNode);
        return value == null ? null : convertCurrency(value);
    }

    String itemName(JsonNode variantNode) {
        Tools5eSources vSources = Tools5eSources.findSources(variantNode);
        if (Tools5eSources.isSrd(variantNode)) {
            if (index().sourceIncluded(vSources.primarySource())) {
                return vSources.getName();
            }
            String srdName = Tools5eSources.srdName(variantNode);
            if (srdName != null) {
                return srdName;
            }
        }
        return vSources.getName();
    }

    String itemText(List<ImageRef> imageRef) {
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

        return text.isEmpty() ? null : String.join("\n", text);
    }

    void insertItemRefText(List<String> text, String input) {
        String finalKey = Tools5eIndexType.itemEntry.fromTagReference(input.replaceAll("\\{#itemEntry (.*)}", "$1"));
        if (finalKey == null || index.isExcluded(finalKey)) {
            return;
        }
        JsonNode ref = index.getNode(finalKey);
        if (ref == null) {
            tui().errorf("Could not find %s from %s", finalKey, getSources());
        } else if (index.sourceIncluded(ref.get("source").asText())) {
            try {
                String entriesTemplate = mapper().writeValueAsString(ref.get("entriesTemplate"));
                if (ItemField.detail1.existsIn(rootNode)) {
                    entriesTemplate = entriesTemplate.replaceAll("\\{\\{item.detail1}}",
                            ItemField.detail1.getTextOrEmpty(rootNode));
                }
                if (ItemField.resist.existsIn(rootNode)) {
                    entriesTemplate = entriesTemplate.replaceAll("\\{\\{(getFullImmRes\\s)?item.resist}}",
                            joinAndReplace(ItemField.resist.readArrayFrom(rootNode)));
                }
                appendToText(text, mapper().readTree(entriesTemplate), "##");
            } catch (JsonProcessingException e) {
                tui().errorf(e, "Unable to insert item element text for %s from %s", input, getSources());
            }
        }
    }

    String armorClass(JsonNode variantNode, ItemType type, ItemType typeAlt) {
        String ac = ItemField.ac.getTextOrEmpty(variantNode);
        if (!isPresent(ac)) {
            return null;
        }
        // - If you wear light armor, you add your Dexterity modifier to the base number
        // from your armor type to determine your Armor Class.
        // - If you wear medium armor, you add your Dexterity modifier, to a maximum of
        // +2,
        // to the base number from your armor type to determine your Armor Class.
        // - Heavy armor does not let you add your Dexterity modifier to your Armor
        // Class,
        // but it also does not penalize you if your Dexterity modifier is negative.
        if (EncodedType.LA.typeIn(type, typeAlt)) {
            ac += " + Dex modifier";
        } else if (EncodedType.MA.typeIn(type, typeAlt)) {
            ac += " + Dex modifier (max of +2)";
        }
        return ac;
    }

    ItemType getItemType(Tools5eSources variantSources, JsonNode node, ItemField typeField) {
        String abbv = typeField.getTextOrEmpty(node);
        return index.findItemType(abbv, variantSources);
    }

    void findProperties(Tools5eSources variantSources, JsonNode variantNode,
            Set<ItemProperty> itemProperties, ItemType type, ItemType typeAlt) {

        JsonNode propertyList = ItemField.property.getFrom(variantNode);
        if (propertyList != null && propertyList.isArray()) {
            // List of properties: abbreviation, or abbreviation|source
            for (JsonNode x : iterableElements(propertyList)) {
                ItemProperty p = index.findItemProperty(x.asText(), variantSources);
                if (p != null) {
                    itemProperties.add(p);
                }
            }
        }

        String lowerName = SourceField.name.getTextOrEmpty(variantNode).toLowerCase();
        if ((ItemTypeGroup.weapon.hasGroup(type, typeAlt)) && lowerName.contains("silvered")) {
            // Add property to link to section on silvered weapons
            itemProperties.add(ItemProperty.SILVERED);
        }
    }

    void findMastery(Tools5eSources variantSources, JsonNode variantNode,
            Set<ItemMastery> itemMasteries) {
        JsonNode masteryList = ItemField.mastery.getFrom(variantNode);
        if (masteryList != null && masteryList.isArray()) {
            for (JsonNode x : iterableElements(masteryList)) {
                ItemMastery mastery = index.findItemMastery(x.asText(), variantSources);
                if (mastery != null) {
                    itemMasteries.add(mastery);
                }
            }
        }
    }

    enum EncodedType {
        GV, // generic variant
        LA, // light armor
        MA, // medium armor
        M, // melee weapon
        S, // shield
        ;

        boolean typeIn(ItemType type, ItemType typeAlt) {
            return (type != null && name().equalsIgnoreCase(type.abbreviation()))
                    || (typeAlt != null && name().equalsIgnoreCase(typeAlt.abbreviation()));
        }

        boolean not(ItemType type) {
            return type == null || !name().equalsIgnoreCase(type.abbreviation());
        }
    }

    enum ItemField implements JsonNodeReader {
        _variants,
        ac,
        age,
        ammo,
        attunement,
        baseItem,
        curse,
        detail1,
        dmg1,
        dmg2,
        dmgType,
        firearm,
        focus,
        hasFluff,
        hasFluffImages,
        items,
        mastery,
        packContents,
        poison,
        poisonTypes,
        property,
        range,
        rarity,
        reqAttune,
        reqAttuneAlt,
        resist,
        scfType,
        sentient,
        staff,
        stealth,
        strength,
        tattoo,
        tier,
        type,
        typeAlt,
        value,
        weaponCategory,
        weight,
        wondrous,
        ;
    }
}
