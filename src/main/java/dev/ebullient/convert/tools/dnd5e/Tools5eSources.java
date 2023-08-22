package dev.ebullient.convert.tools.dnd5e;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.JsonTextConverter.SourceField;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.dnd5e.JsonSource.JsonMediaHref;
import dev.ebullient.convert.tools.dnd5e.JsonSource.TableFields;
import io.quarkus.qute.TemplateData;

@TemplateData
public class Tools5eSources extends CompendiumSources {

    private static final Map<String, Tools5eSources> keyToSources = new HashMap<>();
    private static final Map<Path, ImageRef> imageSourceToRef = new HashMap<>();
    private static final Map<String, List<QuteBase>> keyToInlineNotes = new HashMap<>();

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

    public static Tools5eSources findOrTemporary(JsonNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Must pass a JsonNode");
        }
        Tools5eIndexType type = Tools5eIndexType.getTypeFromNode(node);
        if (type == null) {
            type = SourceField.source.existsIn(node)
                    ? Tools5eIndexType.reference
                    : Tools5eIndexType.syntheticGroup;
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

    public static Collection<ImageRef> getImages() {
        return imageSourceToRef.values();
    }

    public static Collection<QuteBase> getInlineNotes(String key) {
        return keyToInlineNotes.getOrDefault(key, List.of());
    }

    public void addInlineNote(QuteBase note) {
        keyToInlineNotes.computeIfAbsent(this.key, k -> new ArrayList<>()).add(note);
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
        return SourceField.name.getTextOrDefault(jsonElement,
                SourceField.abbreviation.getTextOrDefault(jsonElement,
                        TableFields.caption.getTextOrDefault(jsonElement,
                                "unknown")));
    }

    @Override
    protected String findSourceText(IndexType type, JsonNode jsonElement) {
        if (type == Tools5eIndexType.syntheticGroup) {
            return this.key.replaceAll(".*\\|([^|]+)$", "$1");
        }
        String srcText = super.findSourceText(type, jsonElement);

        boolean basicRules = jsonElement.has("basicRules") && jsonElement.get("basicRules").asBoolean(false);
        String value = jsonElement.has("srd") ? jsonElement.get("srd").asText() : null;
        boolean srd = !(value == null || "false".equals(value));
        String srdValue = srd && !"true".equals(value) ?  " (as '" + value + "')" : "";

        String srdBasic = "";
        if (srd && basicRules) {
            srdBasic = "Available in the SRD and the Basic Rules"+srdValue+".";
        } else if (srd) {
            srdBasic = "Available in the SRD"+srdValue+".";
        } else if (basicRules) {
            srdBasic = "Available in the Basic Rules"+srdValue+".";
        }

        String sourceText = String.join(", ", srcText);
        if (srdBasic.isBlank()) {
            return sourceText;
        }
        return sourceText.isEmpty()
                ? srdBasic
                : sourceText + ". " + srdBasic;
    }

    @Override
    protected boolean datasourceFilter(String source) {
        return !List.of("phb", "mm", "dmg").contains(source.toLowerCase());
    }

    public Optional<String> uaSource() {
        Optional<String> source = sources.stream().filter(x -> x.contains("UA") && !x.equals("UAWGE")).findFirst();
        return source.map(TtrpgConfig::sourceToAbbreviation);
    }

    public String alternateSource() {
        if (sources.size() < 2) {
            return null;
        }

        Iterator<String> i = sources.iterator();
        i.next(); // primary
        return i.next();
    }

    public ImageRef buildImageRef(String title, String hrefString) {
        return new ImageRef.Builder()
                .setTitle(title)
                .setUrl(hrefString)
                .build();
    }

    public ImageRef buildImageRef(Tools5eIndex index, Path sourcePath, Path target, boolean useCompendium) {
        ImageRef imageRef = new ImageRef.Builder()
                .setRelativePath(target)
                .setSourcePath(sourcePath)
                .setRootFilepath(useCompendium ? index.compendiumFilePath() : index.rulesFilePath())
                .setVaultRoot(useCompendium ? index.compendiumVaultRoot() : index.rulesVaultRoot())
                .build(imageSourceToRef.get(sourcePath));
        imageSourceToRef.putIfAbsent(sourcePath, imageRef);
        return imageRef;
    }

    public ImageRef buildImageRef(Tools5eIndex index, JsonMediaHref mediaHref, String imageBasePath, boolean useCompendium) {
        String title = mediaHref.title == null ? "" : mediaHref.title;
        String altText = mediaHref.altText == null ? title : mediaHref.altText;

        if ("external".equals(mediaHref.href.type)) {
            return new ImageRef.Builder()
                    .setTitle(index.replaceText(altText))
                    .setUrl(mediaHref.href.url)
                    .setWidth(mediaHref.width)
                    .build();
        }
        if (mediaHref.href.path != null) {
            Path sourcePath = Path.of("img", mediaHref.href.path);

            String fileName = sourcePath.getFileName().toString();
            if (type == Tools5eIndexType.deity || type == Tools5eIndexType.note || type == Tools5eIndexType.variantrule) {
                fileName = primarySource() + "-" + fileName;
            }

            int x = fileName.lastIndexOf('.');
            Path target = Path.of(imageBasePath, "img",
                    index.slugify(fileName.substring(0, x)) + fileName.substring(x));

            ImageRef imageRef = new ImageRef.Builder()
                    .setWidth(mediaHref.width)
                    .setTitle(index.replaceText(title))
                    .setRelativePath(target)
                    .setSourcePath(sourcePath)
                    .setRootFilepath(useCompendium ? index.compendiumFilePath() : index.rulesFilePath())
                    .setVaultRoot(useCompendium ? index.compendiumVaultRoot() : index.rulesVaultRoot())
                    .build(imageSourceToRef.get(sourcePath));

            imageSourceToRef.putIfAbsent(sourcePath, imageRef);
            return imageRef;
        } else {
            throw new IllegalArgumentException("We have an ImageRef with no path");
        }
    }
}
