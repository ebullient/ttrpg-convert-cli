package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.NodeReader;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase;
import dev.ebullient.convert.tools.pf2e.qute.QuteItem;
import dev.ebullient.convert.tools.pf2e.qute.QuteItem.QuteItemActivate;

public class Json2QuteItem extends Json2QuteBase {
    static final String ITEM_TAG = "item";

    public Json2QuteItem(Pf2eIndex index, JsonNode rootNode) {
        super(index, Pf2eIndexType.item, rootNode);
    }

    @Override
    protected Pf2eQuteBase buildQuteResource() {
        List<String> tags = new ArrayList<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();
        List<String> aliases = new ArrayList<>(Field.alias.transformListFrom(rootNode, this));

        appendEntryToText(text, Field.entries.getFrom(rootNode), "##");
        appendFootnotes(text, 0);

        String duration = Pf2eItem.duration.existsIn(rootNode)
                ? Field.entry.getTextOrNull(Pf2eItem.duration.getFrom(rootNode))
                : null;

        return new QuteItem(sources, text, tags, collectTraitsFrom(rootNode, tags), aliases,
                buildActivate(),
                Pf2eItem.level.getTextOrDefault(rootNode, "1"),
                Pf2eItem.onset.transformTextFrom(rootNode, ", ", this),
                duration,
                getCategory(tags),
                linkify(Pf2eIndexType.group, getGroup()),
                Pf2eItem.hands.getTextOrNull(rootNode),
                keysToMap(List.of(Pf2eItem.usage, Pf2eItem.bulk)));
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
        NumberUnitEntry jsonActivity = Pf2eItem.activity.fieldFromTo(activateNode, NumberUnitEntry.class, tui());

        QuteItemActivate activate = new QuteItemActivate();
        activate.activity = jsonActivity == null ? null : jsonActivity.toQuteActivity(this);
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
            return Pf2eItem.group.getTextOrNull(Pf2eItem.weaponData.getFrom(rootNode));
        }
        if (Pf2eItem.armorData.existsIn(rootNode)) {
            return Pf2eItem.group.getTextOrNull(Pf2eItem.armorData.getFrom(rootNode));
        }
        String category = Pf2eItem.category.getTextOrNull(rootNode);
        if ("Shield".equals(category)) {
            return "Shield";
        }
        return Pf2eItem.group.getTextOrNull(rootNode);
    }

    String getCategory(List<String> tags) {
        String category = Pf2eItem.category.transformTextFrom(rootNode, ", ", this);
        String subcategory = Pf2eItem.subCategory.getTextOrNull(rootNode);
        if (category == null) {
            return null;
        }
        if (subcategory == null) {
            tags.add(cfg().tagOf(ITEM_TAG, "category", category));
            return category;
        }
        tags.add(cfg().tagOf(ITEM_TAG, "category", category, subcategory));
        return subcategory;
    }

    enum Pf2eItem implements NodeReader {
        activate,
        activity,
        armorData,
        bulk,
        category,
        comboWeaponData,
        components,
        duration,
        freuency,
        group,
        hands,
        level,
        onset,
        range,
        requirements,
        subCategory,
        trigger,
        usage,
        weaponData;

        String properName(Pf2eTypeReader convert) {
            return convert.toTitleCase(this.nodeName());
        }
    }
}
