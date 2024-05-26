package dev.ebullient.convert.tools.pf2e;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataArmorClass;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataGenericStat.SimpleStat;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataHpHardnessBt;
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

        String duration = Pf2eItem.duration.existsIn(rootNode)
                ? SourceField.entry.getTextOrEmpty(Pf2eItem.duration.getFrom(rootNode))
                : null;

        return new QuteItem(sources, text, tags, traits, aliases,
                buildActivate(),
                getPrice(rootNode),
                join(", ", Pf2eItem.ammunition.linkifyListFrom(rootNode, Pf2eIndexType.item, this)),
                Pf2eItem.level.getTextOrDefault(rootNode, "0"),
                Pf2eItem.onset.transformTextFrom(rootNode, ", ", this),
                replaceText(Pf2eItem.access.getTextOrEmpty(rootNode)),
                duration,
                getCategory(tags),
                linkify(Pf2eIndexType.group, getGroup()),
                Pf2eItem.hands.getTextOrEmpty(rootNode),
                keysToList(List.of(Pf2eItem.usage, Pf2eItem.bulk)),
                getContract(tags),
                getShieldData(),
                getArmorData(),
                getWeaponData(tags),
                getVariants(tags),
                Pf2eItem.craftReq.transformTextFrom(rootNode, "; ", this));
    }

    private String getPrice(JsonNode rootNode) {
        JsonNode price = Pf2eItem.price.getFrom(rootNode);
        if (price != null) {
            StringBuilder sb = new StringBuilder();
            String amount = Pf2eItem.amount.replaceTextFrom(price, this);
            if (amount != null) {
                sb.append(amount);
            }
            String coin = Pf2eItem.coin.replaceTextFrom(price, this);
            if (coin != null) {
                sb.append(" ").append(coin);
            }
            return sb.toString().trim();
        }
        return "";
    }

    /**
     * Example input JSON:
     *
     * <pre>
     *     "sheldData": {
     *         "ac": 2,
     *         "ac2": 3,
     *         "hardness": 5,
     *         "hp": 20,
     *         "bt": 10,
     *         "speedPen": 10
     *     }
     * </pre>
     *
     * <p>
     * `speedPen` is optional.
     * </p>
     */
    private QuteItemShieldData getShieldData() {
        JsonNode shieldNode = Pf2eItem.shieldData.getFrom(rootNode);
        return shieldNode == null ? null
                : new QuteItemShieldData(
                        new QuteDataArmorClass(
                                Pf2eItem.ac.getIntOrThrow(shieldNode),
                                Pf2eItem.ac2.getIntFrom(shieldNode).orElse(null)),
                        new QuteDataHpHardnessBt(
                                new QuteDataHpHardnessBt.HpStat(Pf2eItem.hp.getIntOrThrow(shieldNode)),
                                new SimpleStat(Pf2eItem.hardness.getIntOrThrow(shieldNode)),
                                Pf2eItem.bt.getIntOrThrow(shieldNode)),
                        penalty(Pf2eItem.speedPen.getTextOrEmpty(shieldNode), " ft."));
    }

    private QuteItemArmorData getArmorData() {
        JsonNode armorDataNode = Pf2eItem.armorData.getFrom(rootNode);
        if (armorDataNode == null) {
            return null;
        }

        QuteItemArmorData armorData = new QuteItemArmorData();
        Pf2eItem.ac.getIntFrom(armorDataNode).ifPresent(ac -> armorData.ac = new QuteDataArmorClass(ac));
        armorData.dexCap = Pf2eItem.dexCap.bonusOrNull(armorDataNode);

        armorData.strength = Pf2eItem.str.getTextOrDefault(armorDataNode, "—");

        String checkPen = Pf2eItem.checkPen.getTextOrDefault(armorDataNode, null);
        armorData.checkPenalty = penalty(checkPen, "");

        String speedPen = Pf2eItem.speedPen.getTextOrDefault(armorDataNode, null);
        armorData.speedPenalty = penalty(speedPen, " ft.");

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
            variant.variantType = Pf2eItemVariant.variantType.replaceTextFrom(e, this);
            variant.price = getPrice(e);
            variant.entries = new ArrayList<>();
            appendToText(variant.entries, SourceField.entries.getFrom(e), null);
            variant.craftReq = new ArrayList<>();
            appendToText(variant.craftReq, Pf2eItemVariant.craftReq.getFrom(e), null);

            variantList.add(variant);
        });

        return variantList;
    }

    private Collection<NamedText> getContract(Tags tags) {
        JsonNode contractNode = Pf2eItem.contract.getFrom(rootNode);
        if (contractNode == null) {
            return null;
        }

        NamedText.SortedBuilder namedText = new NamedText.SortedBuilder();
        for (Entry<String, JsonNode> e : iterableFields(contractNode)) {
            if (e.getKey().equals("decipher")) {
                List<String> writing = toListOfStrings(e.getValue()).stream()
                        .map(s -> linkify(Pf2eIndexType.skill, s))
                        .collect(Collectors.toList());
                namedText.add("Decipher Writing", join(", ", writing));
            } else {
                namedText.add(toTitleCase(e.getKey()), replaceText(e.getValue()));
            }
        }
        ;
        return namedText.build();
    }

    Collection<NamedText> keysToList(List<Pf2eItem> keys) {
        NamedText.SortedBuilder namedText = new NamedText.SortedBuilder();
        for (Pf2eItem k : keys) {
            String value = k.getTextOrEmpty(rootNode);
            if (!value.isEmpty()) {
                namedText.add(k.properName(this), replaceText(value));
            }
        }
        return namedText.isEmpty() ? List.of() : namedText.build();
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
        activate.frequency = Pf2eFrequency.getFrequency(Pf2eItem.frequency.getFrom(activateNode), this);
        activate.trigger = Pf2eItem.trigger.transformTextFrom(activateNode, ", ", this);
        activate.requirements = Pf2eItem.requirements.transformTextFrom(activateNode, ", ", this);
        return activate;
    }

    private String getGroup() {
        // If weaponData and !comboWeaponData, use weaponData group.
        // If not, use armorData group.
        // If not, check if it's a Shield, then make it a Shield. If not, use the item.group.
        if (Pf2eItem.weaponData.existsIn(rootNode) && !Pf2eItem.comboWeaponData.existsIn(rootNode)) {
            return Pf2eWeaponData.group.getTextOrEmpty(Pf2eItem.weaponData.getFrom(rootNode));
        }
        if (Pf2eItem.armorData.existsIn(rootNode)) {
            return Pf2eWeaponData.group.getTextOrEmpty(Pf2eItem.armorData.getFrom(rootNode));
        }
        String category = Pf2eItem.category.getTextOrEmpty(rootNode);
        if ("Shield".equals(category)) {
            return "Shield";
        }
        return Pf2eWeaponData.group.getTextOrEmpty(rootNode);
    }

    String getCategory(Tags tags) {
        String category = Pf2eItem.category.getTextOrEmpty(rootNode);
        String subcategory = Pf2eItem.subCategory.getTextOrEmpty(rootNode);
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

    private String penalty(String input, String suffix) {
        if (!isPresent(input) || "0".equals(input)) {
            return "—";
        }
        return (input.startsWith("-") ? input : ("-" + input)) + suffix;
    }

    enum Pf2eItem implements Pf2eJsonNodeReader {
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
            return toTitleCase(this.nodeName());
        }
    }

    enum Pf2eItemVariant implements Pf2eJsonNodeReader {
        level,
        price,
        entries,
        variantType,
        craftReq
    }
}
