package dev.ebullient.convert.tools.pf2e;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.IndexType;
import io.quarkus.qute.TemplateData;

@TemplateData
public class Pf2eSources extends CompendiumSources {

    private static final Map<String, Pf2eSources> keyToSources = new HashMap<>();

    public static Pf2eSources findSources(String key) {
        return keyToSources.get(key);
    }

    public static Pf2eSources findSources(JsonNode node) {
        String key = JsonSource.TtrpgValue.indexKey.getFromNode(node);
        return keyToSources.get(key);
    }

    public static Pf2eSources constructSources(Pf2eIndexType type, JsonNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Must pass a JsonNode");
        }
        String key = JsonSource.TtrpgValue.indexKey.getFromNode(node);
        return keyToSources.computeIfAbsent(key, k -> {
            Pf2eSources s = new Pf2eSources(type, key, node);
            s.checkKnown();
            return s;
        });
    }

    public static Pf2eSources constructSyntheticSource(String name) {
        String key = Pf2eIndexType.syntheticGroup.createKey(name, "mixed");
        return new Pf2eSources(Pf2eIndexType.syntheticGroup, key, null);
    }

    public static Pf2eSources createEmbeddedSource(JsonNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Must pass a JsonNode");
        }
        String key = Pf2eIndexType.bookReference.createKey(node);
        return new Pf2eSources(Pf2eIndexType.bookReference, key, node);
    }

    public static Pf2eSources findOrTemporary(Pf2eIndexType type, JsonNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Must pass a JsonNode");
        }
        String key = JsonSource.TtrpgValue.indexKey.getFromNode(node);
        if (key == null) {
            key = type.createKey(node);
        }
        Pf2eSources sources = findSources(key);
        return sources == null
                ? new Pf2eSources(type, key, node)
                : sources;
    }

    Pf2eIndexType type;

    private Pf2eSources(Pf2eIndexType type, String key, JsonNode node) {
        super(type, key, node);
        this.type = type;
    }

    protected String findName(IndexType type, JsonNode node) {
        if (type == Pf2eIndexType.syntheticGroup || type == Pf2eIndexType.bookReference) {
            return this.key.replaceAll("/.*\\|(.*)\\|/", "$1");
        }
        String name = JsonSource.Field.name.getTextOrNull(node);
        if (name == null) {
            throw new IllegalArgumentException("Unknown element, has no name: " + node.toString());
        }
        return name;
    }

    @Override
    protected String findSourceText(IndexType type, JsonNode jsonElement) {
        if (type == Pf2eIndexType.syntheticGroup) {
            return this.key.replaceAll("/.*\\|([^|]+)$/", "$1");
        }
        return super.findSourceText(type, jsonElement);
    }

    @Override
    public Pf2eIndexType getType() {
        return type;
    }

    public boolean fromDefaultSource() {
        if (type == Pf2eIndexType.data) {
            return true;
        }
        DefaultSource ds = type.defaultSource();
        return ds.name().equals(primarySource().toLowerCase());
    }

    public enum DefaultSource {
        apg,
        b1,
        crb,
        gmg,
        locg,
        lotg,
        som;

        public boolean sameSource(String source) {
            return this.name().equalsIgnoreCase(source);
        }
    }
}
