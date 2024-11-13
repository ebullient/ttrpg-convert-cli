package dev.ebullient.convert.tools.dnd5e;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonTextConverter.SourceField;
import dev.ebullient.convert.tools.dnd5e.JsonSource.Tools5eFields;
import io.quarkus.qute.TemplateData;

public interface ItemMastery {
    static final Map<ItemMastery, String> masteryToLink = new HashMap<>();

    Comparator<ItemMastery> comparator = Comparator.comparing(ItemMastery::value);

    String getMarkdownLink(Tools5eIndex index);

    String tagValue();

    String value();

    class CustomItemMastery implements ItemMastery {
        final String name;
        final String tag;

        public CustomItemMastery(JsonNode mastery) {
            String name = SourceField.name.getTextOrNull(mastery);
            if (name == null) {
                JsonNode entries = SourceField.entries.existsIn(mastery)
                        ? SourceField.entries.getFrom(mastery)
                        : Tools5eFields.entriesTemplate.getFrom(mastery);

                if (entries != null && entries.size() > 0) {
                    JsonNode firstEntry = entries.get(0);
                    if (firstEntry.has("name")) {
                        name = firstEntry.get("name").asText();
                    }
                }

                if (name == null) {
                    throw new IllegalArgumentException("Unable to get name for Mastery: " + mastery.toPrettyString());
                }
            }

            this.name = name;
            this.tag = "mastery/" + Tui.slugify(name);
        }

        public CustomItemMastery(String name) {
            this.name = name;
            this.tag = "mastery/" + Tui.slugify(name);
        }

        @Override
        public String getMarkdownLink(Tools5eIndex index) {
            return masteryToLink.computeIfAbsent(this, p -> {
                List<JsonNode> targets = index.elementsMatching(Tools5eIndexType.itemMastery, name.toLowerCase());
                if (targets.isEmpty() || targets.size() > 1) {
                    return name;
                }
                String key = Tools5eIndexType.itemMastery.createKey(targets.get(0));

                return index.isIncluded(key)
                        ? String.format("[%s](%s)", name,
                                index.rulesVaultRoot() + "item-mastery.md#" + index.toAnchorTag(name))
                        : name;
            });
        }

        @Override
        public String tagValue() {
            return tag;
        }

        @Override
        public String value() {
            return name;
        }
    }

    @TemplateData
    enum MasteryEnum implements ItemMastery {
        CLEAVE("Cleave", "mastery/cleave"),
        GRAZE("Graze", "mastery/graze"),
        NICK("Nick", "mastery/nick"),
        PUSH("Push", "mastery/push"),
        SAP("Sap", "mastery/sap"),
        SLOW("Slow", "mastery/slow"),
        TOPPLE("Topple", "mastery/topple"),
        VEX("Vex", "mastery/vex");

        public final String name;
        private final String tagValue;

        MasteryEnum(String name, String tagValue) {
            this.name = name;
            this.tagValue = tagValue;
        }

        private static final List<MasteryEnum> knownProperties = Stream.of(MasteryEnum.values())
                .collect(Collectors.toList());

        public static boolean homebrewMastery(ItemMastery p) {
            return !knownProperties.contains(p);
        }

        public String value() {
            return name.toLowerCase();
        }

        public String tagValue() {
            return tagValue;
        }

        public String getMarkdownLink(Tools5eIndex index) {
            return masteryToLink.computeIfAbsent(this, p -> {
                List<JsonNode> targets = index.elementsMatching(Tools5eIndexType.itemMastery, name.toLowerCase());
                if (targets.isEmpty() || targets.size() > 1) {
                    return name;
                }
                String key = Tools5eIndexType.itemMastery.createKey(targets.get(0));

                return index.isIncluded(key)
                        ? String.format("[%s](%s)", name,
                                index.rulesVaultRoot() + "item-mastery.md#" + index.toAnchorTag(name))
                        : name;
            });
        }

        public static MasteryEnum fromValue(String v) {
            if (v == null || v.isBlank()) {
                return null;
            }
            String key = v.toLowerCase();
            for (MasteryEnum p : MasteryEnum.values()) {
                if (p.name.toLowerCase().equals(key)) {
                    return p;
                }
            }
            Tui.instance().errorf("Invalid/Unknown mastery %s", v);
            return null;
        }

        public static MasteryEnum fromName(String v) {
            if (v == null || v.isBlank()) {
                return null;
            }
            for (MasteryEnum p : MasteryEnum.values()) {
                if (p.name.equals(v) || p.name.toLowerCase().equals(v.toLowerCase())) {
                    return p;
                }
            }
            return null;
        }
    }
}
