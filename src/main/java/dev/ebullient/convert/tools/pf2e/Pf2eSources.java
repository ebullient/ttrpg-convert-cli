package dev.ebullient.convert.tools.pf2e;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.JsonTextConverter.SourceField;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import io.quarkus.qute.TemplateData;

@TemplateData
public class Pf2eSources extends CompendiumSources {

    private static final Map<String, Pf2eSources> keyToSources = new HashMap<>();
    private static final Map<String, ImageRef> imageSourceToRef = new HashMap<>();

    public static Pf2eSources findSources(String key) {
        return keyToSources.get(key);
    }

    public static Pf2eSources findSources(JsonNode node) {
        String key = TtrpgValue.indexKey.getTextOrEmpty(node);
        return keyToSources.get(key);
    }

    public static Pf2eSources constructSources(Pf2eIndexType type, JsonNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Must pass a JsonNode");
        }
        String key = TtrpgValue.indexKey.getTextOrNull(node);
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
        String key = TtrpgValue.indexKey.getTextOrNull(node);
        if (key == null) {
            key = type.createKey(node);
        }
        Pf2eSources sources = findSources(key);
        return sources == null
                ? new Pf2eSources(type, key, node)
                : sources;
    }

    public static ImageRef buildStreamImageRef(Pf2eIndex index, String sourcePath, Path relativeTarget, String title) {
        ImageRef imageRef = new ImageRef.Builder()
                .setStreamSource(sourcePath)
                .setRelativePath(Path.of("assets").resolve(relativeTarget))
                .setTitle(index.replaceText(title))
                .setRootFilepath(index.rulesFilePath())
                .setVaultRoot(index.rulesVaultRoot())
                .build(imageSourceToRef.get(sourcePath));
        imageSourceToRef.putIfAbsent(sourcePath, imageRef);
        return imageRef;
    }

    public static ImageRef buildImageRef(Pf2eIndexType type, Pf2eIndex index, Path sourcePath, String title) {
        return buildImageRef(type, index, sourcePath, sourcePath, title);
    }

    public static ImageRef buildImageRef(Pf2eIndexType type, Pf2eIndex index, Path sourcePath, Path relativeTarget,
            String title) {
        String key = sourcePath.toString();
        ImageRef imageRef = new ImageRef.Builder()
                .setSourcePath(sourcePath)
                .setRelativePath(Path.of("assets").resolve(relativeTarget))
                .setRootFilepath(type.getFilePath(index))
                .setVaultRoot(type.getVaultRoot(index))
                .setTitle(index.replaceText(title))
                .build(imageSourceToRef.get(key));
        imageSourceToRef.putIfAbsent(key, imageRef);
        return imageRef;
    }

    public static Collection<ImageRef> getImages() {
        return imageSourceToRef.values();
    }

    final Pf2eIndexType type;

    private Pf2eSources(Pf2eIndexType type, String key, JsonNode node) {
        super(type, key, node);
        this.type = type;
    }

    public JsonNode findNode() {
        return Pf2eIndex.findNode(this);
    }

    protected String findName(IndexType type, JsonNode node) {
        if (type == Pf2eIndexType.syntheticGroup || type == Pf2eIndexType.bookReference) {
            return this.key.replaceAll(".*\\|(.*)\\|", "$1");
        }
        String name = SourceField.name.getTextOrEmpty(node);
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Unknown element, has no name: " + node.toString());
        }
        return name;
    }

    @Override
    protected String findSourceText(IndexType type, JsonNode jsonElement) {
        if (type == Pf2eIndexType.syntheticGroup) {
            return this.key.replaceAll(".*\\|([^|]+)$", "$1");
        }
        return super.findSourceText(type, jsonElement);
    }

    @Override
    public Pf2eIndexType getType() {
        return type;
    }

    /** Documents that have no primary source (compositions) */
    protected boolean isSynthetic() {
        return type == Pf2eIndexType.syntheticGroup;
    }

    public boolean fromDefaultSource() {
        if (type == Pf2eIndexType.data) {
            return true;
        }
        return type.defaultSourceString().equals(primarySource().toLowerCase());
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
