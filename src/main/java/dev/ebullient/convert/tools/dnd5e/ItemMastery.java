package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.toAnchorTag;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonTextConverter.SourceField;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;

public record ItemMastery(
        String name,
        String indexKey,
        String tag) {

    public String toString() {
        return name + " Mastery";
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
                ? "[%s](%sitem-mastery.md#%s)".formatted(
                        linkText, index.rulesVaultRoot(), toAnchorTag(name))
                : linkText;
    }

    public static final Comparator<ItemMastery> comparator = Comparator.comparing(ItemMastery::name);
    private static final Map<String, ItemMastery> masteryMap = new HashMap<>();

    public static ItemMastery forKey(String key) {
        if (!isPresent(key)) {
            return null;
        }
        return masteryMap.get(key);
    }

    public static ItemMastery fromNode(JsonNode mastery) {
        String key = TtrpgValue.indexKey.getTextOrEmpty(mastery);
        // Create the ItemType object once
        return masteryMap.computeIfAbsent(key, k -> {
            String name = SourceField.name.getTextOrEmpty(mastery);
            if (name == null) {
                throw new IllegalArgumentException("Unable to get name for Mastery: " + mastery.toPrettyString());
            }

            return new ItemMastery(
                    name,
                    key,
                    "item/mastery/" + Tui.slugify(name));
        });
    }

    public static List<String> asLinks(Collection<ItemMastery> itemMasteries) {
        return itemMasteries.stream()
                .map(ItemMastery::linkify)
                .toList();
    }

    public static void clear() {
        masteryMap.clear();
    }
}
