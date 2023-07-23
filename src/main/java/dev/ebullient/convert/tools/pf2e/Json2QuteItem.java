package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataArmorClass;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataHpHardness;
import dev.ebullient.convert.tools.pf2e.qute.QuteItem;
import dev.ebullient.convert.tools.pf2e.qute.QuteItem.QuteItemActivate;
import dev.ebullient.convert.tools.pf2e.qute.QuteItem.QuteItemArmorData;
import dev.ebullient.convert.tools.pf2e.qute.QuteItem.QuteItemShieldData;
import dev.ebullient.convert.tools.pf2e.qute.QuteItem.QuteItemWeaponData;

public class Json2QuteItem extends Json2QuteBase {
    static final String ITEM_TAG = "item";

    public Json2QuteItem(Pf2eIndex index, JsonNode rootNode) {
        super(index, Pf2eIndexType.item, rootNode);
    }

    @Override
    protected Pf2eQuteBase buildQuteResource() {
        Tags tags = new Tags(sources);
        List<String> text = new ArrayList<>();
        List<String> aliases = new ArrayList<>(Field.alias.replaceTextFromList(rootNode, this));
        Set<String> traits = collectTraitsFrom(rootNode, tags);

        appendToText(text, SourceField.entries.getFrom(rootNode), "##");
        appendFootnotes(text, 0);

        String duration = Pf2eItem.duration.existsIn(rootNode)
                ? SourceField.entry.getTextOrNull(Pf2eItem.duration.getFrom(rootNode))
                : null;

        return new QuteItem(sources, text, tags, traits, aliases,
                buildActivate(),
                getPrice(rootNode),
                join(", ", Pf2eItem.ammunition.linkifyListFrom(rootNode, Pf2eIndexType.item, this)),
                Pf2eItem.level.getTextOrDefault(rootNode, "0"),
                Pf2eItem.onset.transformTextFrom(rootNode, ", ", this),
                replaceText(Pf2eItem.access.getTextOrNull(rootNode)),
                duration,
                getCategory(tags),
                linkify(Pf2eIndexType.group, getGroup()),
                Pf2eItem.hands.getTextOrNull(rootNode),
                keysToMap(List.of(Pf2eItem.usage, Pf2eItem.bulk)),
                getContract(tags),
                getShieldData(),
                getArmorData(),
                getWeaponData(tags),
                getVariants(tags),
                Pf2eItem.craftReq.transformTextFrom(rootNode, "; ", this));
    }

    private String getPrice(JsonNode rootNode) {
        if (Pf2eItem.price.existsIn(rootNode)) {
            return String.format("%s %s",
                    replaceText(Pf2eItem.price.getFieldFrom(rootNode, Pf2eItem.amount)),
                    replaceText(Pf2eItem.price.getFieldFrom(rootNode, Pf2eItem.coin)));
        }
        return null;
    }

    private QuteItemShieldData getShieldData() {
        JsonNode shieldDataNode = Pf2eItem.shieldData.getFrom(rootNode);
        if (shieldDataNode == null) {
            return null;
        }
        QuteItemShieldData shieldData = new QuteItemShieldData();

        String ac = Pf2eItem.ac.bonusOrNull(shieldDataNode);
        String ac2 = Pf2eItem.ac.bonusOrNull(shieldDataNode);
        String dexCap = Pf2eItem.dexCap.bonusOrNull(shieldDataNode);
        if (ac != null || dexCap != null) {
            shieldData.ac = new QuteDataArmorClass();
            shieldData.ac.armorClass = new LinkedHashMap<>();
            shieldData.ac.armorClass.put("AC Bonus", ac + (ac2 == null ? "" : ("/" + ac2)));
            if (dexCap != null) {
                shieldData.ac.armorClass.put("Dex Cap", dexCap);
            }
        }

        QuteDataHpHardness hpHardness = new QuteDataHpHardness();
        hpHardness.hpValue = Pf2eItem.hp.getTextOrNull(shieldDataNode);
        hpHardness.brokenThreshold = Pf2eItem.bt.getTextOrNull(shieldDataNode);
        hpHardness.hardnessValue = Pf2eItem.hardness.getTextOrNull(shieldDataNode);
        if (hpHardness.hpValue != null || hpHardness.hardnessValue != null || hpHardness.brokenThreshold != null) {
            shieldData.hpHardness = hpHardness;
        }

        String speedPen = Pf2eItem.speedPen.getTextOrNull(shieldDataNode);
        if (speedPen != null) {
            shieldData.speedPenalty = "0".equals(speedPen)
                    ? "\u2014"
                    : String.format("-%s ft.", speedPen);
        }

        return shieldData;
    }

    private QuteItemArmorData getArmorData() {
        JsonNode armorDataNode = Pf2eItem.armorData.getFrom(rootNode);
        if (armorDataNode == null) {
            return null;
        }

        QuteItemArmorData armorData = new QuteItemArmorData();
        String ac = Pf2eItem.ac.bonusOrNull(armorDataNode);
        String dexCap = Pf2eItem.dexCap.bonusOrNull(armorDataNode);
        if (ac != null || dexCap != null) {
            armorData.ac = new QuteDataArmorClass();
            armorData.ac.armorClass = new LinkedHashMap<>();
            armorData.ac.armorClass.put("AC Bonus", ac);
            if (dexCap != null) {
                armorData.ac.armorClass.put("Dex Cap", dexCap);
            }
        }

        armorData.strength = Pf2eItem.str.getTextOrDefault(armorDataNode, "\u2014");

        String checkPen = Pf2eItem.checkPen.getTextOrDefault(armorDataNode, null);
        armorData.checkPenalty = checkPen == null
                ? "\u2014"
                : "-" + checkPen;

        String speedPen = Pf2eItem.speedPen.getTextOrDefault(armorDataNode, null);
        armorData.speedPenalty = "0".equals(speedPen)
                ? "\u2014"
                : "-" + speedPen + " ft.";

        return armorData;
    }

    private List<QuteItemWeaponData> getWeaponData(Tags tags) {
        JsonNode weaponDataNode = Pf2eItem.weaponData.getFrom(rootNode);
        if (weaponDataNode == null) {
            return null;
        }
        List<QuteItemWeaponData> weaponDataList = new ArrayList<>();
        weaponDataList.add(Pf2eWeaponData.buildWeaponData(weaponDataNode, this, tags));

        JsonNode comboWeaponData = Pf2eItem.comboWeaponData.getFrom(rootNode);
        if (comboWeaponData != null) {
            weaponDataList.add(Pf2eWeaponData.buildWeaponData(comboWeaponData, this, tags));
        }

        return weaponDataList;
    }

    private List<QuteItem.QuteItemVariant> getVariants(Tags tags) {

        JsonNode variantsNode = Pf2eItem.variants.getFrom(rootNode);
        if (variantsNode == null)
            return null;

        List<QuteItem.QuteItemVariant> variantList = new ArrayList<>();

        variantsNode.forEach(e -> {
            QuteItem.QuteItemVariant variant = new QuteItem.QuteItemVariant();
            variant.level = Pf2eItemVariant.level.intOrDefault(e, 0);
            variant.variantType = Pf2eItemVariant.variantType.getTextOrEmpty(e);
            variant.price = getPrice(e);
            variant.entries = new ArrayList<>();
            appendToText(variant.entries, SourceField.entries.getFrom(e), null);
            variant.craftReq = new ArrayList<>();
            appendToText(variant.craftReq, Pf2eItemVariant.craftReq.getFrom(e), null);

            variantList.add(variant);
        });

        return variantList;
    }

    private Map<String, String> getContract(Tags tags) {
        JsonNode contractNode = Pf2eItem.contract.getFrom(rootNode);
        if (contractNode == null) {
            return null;
        }
        Map<String, String> contract = new LinkedHashMap<>();
        contractNode.fields().forEachRemaining(e -> {
            if (e.getKey().equals("decipher")) {
                List<String> writing = toListOfStrings(e.getValue()).stream()
                        .map(s -> linkify(Pf2eIndexType.skill, s))
                        .collect(Collectors.toList());
                contract.put("Decipher Writing", join(", ", writing));
            } else {
                contract.put(toTitleCase(e.getKey()), replaceText(e.getValue()));
            }
        });

        return contract;
    }

    Map<String, String> keysToMap(List<Pf2eItem> keys) {
        Map<String, String> map = new LinkedHashMap<>();
        for (Pf2eItem k : keys) {
            String value = k.getTextOrNull(rootNode);
            if (value != null) {
                map.put(k.properName(this), replaceText(value));
            }
        }
        return map.isEmpty() ? null : map;
    }

    QuteItemActivate buildActivate() {
        JsonNode activateNode = Pf2eItem.activate.getFrom(rootNode);
        if (activateNode == null) {
            return null;
        }

        QuteItemActivate activate = new QuteItemActivate();
        activate.activity = Pf2eTypeReader.getQuteActivity(rootNode, Pf2eItem.activity, this);
        activate.components = Pf2eItem.components.transformTextFrom(activateNode, ", ", this);
        activate.requirements = Field.requirements.replaceTextFrom(activateNode, this);
        activate.frequency = getFrequency(activateNode);
        activate.trigger = Pf2eItem.trigger.transformTextFrom(activateNode, ", ", this);
        activate.requirements = Pf2eItem.requirements.transformTextFrom(activateNode, ", ", this);
        return activate;
    }

    private String getGroup() {
        // If weaponData and !comboWeaponData, use weaponData group.
        // If not, use armorData group.
        // If not, check if it's a Shield, then make it a Shield. If not, use the item.group.
        if (Pf2eItem.weaponData.existsIn(rootNode) && !Pf2eItem.comboWeaponData.existsIn(rootNode)) {
            return Pf2eWeaponData.group.getTextOrNull(Pf2eItem.weaponData.getFrom(rootNode));
        }
        if (Pf2eItem.armorData.existsIn(rootNode)) {
            return Pf2eWeaponData.group.getTextOrNull(Pf2eItem.armorData.getFrom(rootNode));
        }
        String category = Pf2eItem.category.getTextOrNull(rootNode);
        if ("Shield".equals(category)) {
            return "Shield";
        }
        return Pf2eWeaponData.group.getTextOrNull(rootNode);
    }

    String getCategory(Tags tags) {
        String category = Pf2eItem.category.getTextOrNull(rootNode);
        String subcategory = Pf2eItem.subCategory.getTextOrNull(rootNode);
        if (category == null) {
            return null;
        }
        if (subcategory == null) {
            tags.add(ITEM_TAG, "category", category);
            return category;
        }
        tags.add(ITEM_TAG, "category", category, subcategory);
        return subcategory;
    }

    enum Pf2eItem implements JsonNodeReader {
        ac, // shieldData
        ac2, // shieldData
        access,
        activate,
        activity,
        ammunition,
        amount, // price
        armorData,
        bt, // shieldData
        bulk,
        category,
        checkPen, // armorData
        coin, // price
        comboWeaponData,
        components,
        contract,
        craftReq,
        dexCap, // shieldData
        duration,
        frequency,
        hands,
        hp, // shieldData
        hardness, // shieldData
        level,
        onset,
        price,
        requirements,
        shieldData,
        speedPen, // shieldData, armorData
        str, // armorData
        subCategory,
        trigger,
        usage,
        variants,
        weaponData;

        String properName(Pf2eTypeReader convert) {
            return convert.toTitleCase(this.nodeName());
        }
    }

    enum Pf2eItemVariant implements JsonNodeReader {
        level,
        price,
        entries,
        variantType,
        craftReq
    }
}
