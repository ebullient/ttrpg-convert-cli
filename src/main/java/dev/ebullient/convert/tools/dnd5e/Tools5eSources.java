package dev.ebullient.convert.tools.dnd5e;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import io.quarkus.qute.TemplateData;

@TemplateData
public class Tools5eSources extends CompendiumSources {

    private static final Map<String, Tools5eSources> keyToSources = new HashMap<>();
    private static final Map<String, ImageRef> imageSourceToRef = new HashMap<>();

    public static Tools5eSources findSources(String key) {
        return keyToSources.get(key);
    }

    public static Tools5eSources findSources(JsonNode node) {
        String key = TtrpgValue.indexKey.getFromNode(node);
        return keyToSources.get(key);
    }

    public static Tools5eSources constructSources(JsonNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Must pass a JsonNode");
        }
        String key = TtrpgValue.indexKey.getFromNode(node);
        if (key == null) {
            throw new IllegalArgumentException("Node has not been indexed (no key)");
        }
        Tools5eIndexType type = Tools5eIndexType.getTypeFromKey(key);
        return keyToSources.computeIfAbsent(key, k -> {
            Tools5eSources s = new Tools5eSources(type, key, node);
            s.checkKnown();
            return s;
        });
    }

    public static Tools5eSources findOrTemporary(Tools5eIndexType type, JsonNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Must pass a JsonNode");
        }
        String key = TtrpgValue.indexKey.getFromNode(node);
        if (key == null) {
            key = type.createKey(node);
        }
        Tools5eSources sources = findSources(key);
        return sources == null
                ? new Tools5eSources(type, key, node)
                : sources;
    }

    final boolean srd;
    final boolean basicRules;
    final Tools5eIndexType type;

    private Tools5eSources(Tools5eIndexType type, String key, JsonNode jsonElement) {
        super(type, key, jsonElement);
        this.type = type;
        this.basicRules = jsonElement.has("basicRules") && jsonElement.get("basicRules").asBoolean(false);
        this.srd = jsonElement.has("srd") && jsonElement.get("srd").asBoolean(false);
    }

    @Override
    public Tools5eIndexType getType() {
        return type;
    }

    public String getSourceText(boolean useSrd) {
        if (useSrd) {
            return "SRD / Basic Rules";
        }
        return sourceText;
    }

    public JsonNode findNode() {
        return Tools5eIndex.getInstance().getNode(this.key);
    }

    protected String findName(IndexType type, JsonNode jsonElement) {
        if (type == Tools5eIndexType.syntheticGroup) {
            return this.key.replaceAll(".+?\\|([^|]+).*", "$1");
        }
        return Fields.name.getTextOrDefault(jsonElement,
                Fields.abbreviation.getTextOrDefault(jsonElement,
                        "unknown"));
    }

    @Override
    protected String findSourceText(IndexType type, JsonNode jsonElement) {
        if (type == Tools5eIndexType.syntheticGroup) {
            return this.key.replaceAll(".*\\|([^|]+)$", "$1");
        }
        String srcText = super.findSourceText(type, jsonElement);

        String srdBasic = null;
        if (srd && basicRules) {
            srdBasic = "Available in the SRD and the Basic Rules.";
        } else if (srd) {
            srdBasic = "Available in the SRD.";
        } else if (basicRules) {
            srdBasic = "Available in the Basic Rules.";
        }

        String sourceText = String.join(", ", srcText);
        if (srdBasic != null) {
            return sourceText.isEmpty()
                    ? srdBasic
                    : sourceText + ". " + srdBasic;
        }

        return sourceText;
    }

    @Override
    protected boolean datasourceFilter(String source) {
        return !List.of("phb", "mm", "dmg").contains(source.toLowerCase());
    }

    public Optional<String> uaSource() {
        Optional<String> source = bookSources.stream().filter(x -> x.contains("UA") && !x.equals("UAWGE")).findFirst();
        return source.map(TtrpgConfig::sourceToAbbreviation);
    }

    public String alternateSource() {
        if (bookSources.size() < 2) {
            return null;
        }

        Iterator<String> i = bookSources.iterator();
        i.next(); // primary
        return i.next();
    }
}
