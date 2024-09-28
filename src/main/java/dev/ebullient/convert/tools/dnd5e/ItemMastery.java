package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.isPresent;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonTextConverter.SourceField;

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
        Tools5eIndex index = Tools5eIndex.getInstance();
        linkText = isPresent(linkText) ? linkText : name;

        boolean included = isPresent(indexKey)
                ? index.isIncluded(indexKey)
                : index.customRulesIncluded();

        return included
                ? "[%s](%sitem-mastery.md#%s)".formatted(
                        linkText, index.rulesVaultRoot(), Tui.toAnchorTag(name))
                : linkText;
    }

    public static final Comparator<ItemMastery> comparator = Comparator.comparing(ItemMastery::name);
    private static final Map<String, ItemMastery> masteryMap = new HashMap<>();

    public static ItemMastery fromKey(String key, Tools5eIndex index) {
        String finalKey = index.getAliasOrDefault(key);
        JsonNode node = index.getNode(finalKey);
        return node == null ? null : fromNode(finalKey, node);
    }

    public static ItemMastery fromNode(String key, JsonNode mastery) {
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
}
