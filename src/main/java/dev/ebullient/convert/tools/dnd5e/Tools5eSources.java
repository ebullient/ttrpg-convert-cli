package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.isPresent;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.FontRef;
import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.JsonTextConverter.SourceField;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.dnd5e.JsonSource.JsonMediaHref;
import dev.ebullient.convert.tools.dnd5e.JsonSource.TableFields;
import io.quarkus.qute.TemplateData;

@TemplateData
public class Tools5eSources extends CompendiumSources {

    private static final Map<String, Tools5eSources> keyToSources = new HashMap<>();
    private static final Map<String, ImageRef> imageSourceToRef = new HashMap<>();
    private static final Map<String, FontRef> fontSourceToRef = new HashMap<>();
    private static final Map<String, List<QuteBase>> keyToInlineNotes = new HashMap<>();
    private static final Set<String> basicRulesKeys = new HashSet<>();
    private static final Set<String> freeRulesKeys = new HashSet<>();

    private static boolean isBasicRules(String key, JsonNode jsonElement) {
        if (basicRulesKeys.isEmpty()) {
            final JsonNode basicRules = TtrpgConfig.activeGlobalConfig("basicRules");
            basicRules.forEach(node -> basicRulesKeys.add(node.asText()));
        }
        return SourceAttributes.basicRules.coerceBooleanOrDefault(jsonElement, false)
                || basicRulesKeys.contains(key);
    }

    private static boolean isFreeRules2024(String key, JsonNode jsonElement) {
        if (freeRulesKeys.isEmpty()) {
            final JsonNode freeRules = TtrpgConfig.activeGlobalConfig("freeRules2024");
            freeRules.forEach(node -> freeRulesKeys.add(node.asText()));
        }
        return SourceAttributes.freeRules2024.coerceBooleanOrDefault(jsonElement, false)
                || freeRulesKeys.contains(key);
    }

    public static boolean has2024Content() {
        // return true if any of the 2024 core sources are enabled
        return List.of("XPHB", "XDMG", "XMM", "srd52", "freerules2024")
                .stream().anyMatch(TtrpgConfig.getConfig()::sourceIncluded);
    }

    public static boolean has2014Content() {
        // return true if any of the 2024 core sources are enabled
        return List.of("PHB", "DMG", "MM", "srd", "basicRules")
                .stream().anyMatch(TtrpgConfig.getConfig()::sourceIncluded);
    }

    public static boolean includedByConfig(String key) {
        Tools5eSources sources = findSources(key);
        return sources != null && sources.includedByConfig();
    }

    public static boolean excludedByConfig(String key) {
        return !includedByConfig(key);
    }

    public static boolean filterRuleApplied(String key) {
        Tools5eSources sources = findSources(key);
        return sources != null && sources.filterRule;
    }

    public static Tools5eSources findSources(String key) {
        if (key == null) {
            return null;
        }
        return keyToSources.get(key);
    }

    public static Tools5eSources findSources(JsonNode node) {
        String key = TtrpgValue.indexKey.getTextOrEmpty(node);
        return keyToSources.get(key);
    }

    public static Tools5eSources constructSources(String key, JsonNode node) {
        if (node == null) {
            throw new IllegalArgumentException("Must pass a JsonNode");
        }
        Tools5eIndexType type = Tools5eIndexType.getTypeFromKey(key);
        TtrpgValue.indexKey.setIn(node, key);
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
            TtrpgValue.indexInputType.setIn(node, type.name());
        }
        String key = TtrpgValue.indexKey.getTextOrNull(node);
        if (key == null) {
            key = type.createKey(node);
            TtrpgValue.indexKey.setIn(node, key);
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

    public static Collection<FontRef> getFonts() {
        return fontSourceToRef.values().stream()
                .filter(FontRef::hasTextReference)
                .toList();
    }

    public static void addFonts(JsonNode source, JsonNodeReader field) {
        if (field.isArrayIn(source)) {
            for (JsonNode font : field.iterateArrayFrom(source)) {
                addFont(font.asText());
            }
        } else if (field.isObjectIn(source)) {
            for (Entry<String, JsonNode> font : field.iterateFieldsFrom(source)) {
                addFont(font.getKey(), font.getValue().asText());
            }
        }
    }

    public static void addFont(String fontFamily, String fontString) {
        FontRef ref = FontRef.of(fontFamily, fontString);
        if (ref == null) {
            Tui.instance().warnf("Font '%s' is invalid, empty, or not found", fontString);
        } else {
            FontRef previous = fontSourceToRef.putIfAbsent(fontFamily, ref);
            if (previous != null) {
                Tui.instance().warnf("Font '%s' is already defined as '%s'", fontString, previous);
            }
        }
    }

    public static void addFont(String fontString) {
        String fontFamily = FontRef.fontFamily(fontString);
        addFont(fontFamily, fontString);
    }

    public static String getFontReference(String fontString) {
        String fontFamily = FontRef.fontFamily(fontString);
        FontRef ref = fontSourceToRef.get(fontFamily);
        if (ref == null) {
            return null;
        }
        ref.addTextReference();
        return fontFamily;
    }

    public static boolean isSrd(JsonNode node) {
        return SourceAttributes.srd.coerceBooleanOrDefault(node, false)
                || SourceAttributes.srd52.coerceBooleanOrDefault(node, false);
    }

    /** Return the srd name or null */
    public static String srdName(JsonNode node) {
        String name = SourceAttributes.srd52.getTextOrDefault(node, SourceAttributes.srd.getTextOrNull(node));
        return "true".equalsIgnoreCase(name) ? null : name;
    }

    private final boolean srd;
    private final boolean basicRules;
    private final boolean srd52;
    private final boolean freeRules2024;
    private final boolean includedWhenNoSource;

    private final Tools5eIndexType type;
    private final String edition;

    private boolean filterRule;
    private boolean cfgIncluded;

    private Tools5eSources(Tools5eIndexType type, String key, JsonNode jsonElement) {
        super(type, key, jsonElement);
        this.type = type;
        this.basicRules = isBasicRules(key, jsonElement);
        this.freeRules2024 = isFreeRules2024(key, jsonElement);
        this.srd = SourceAttributes.srd.coerceBooleanOrDefault(jsonElement, false);
        this.srd52 = SourceAttributes.srd52.coerceBooleanOrDefault(jsonElement, false);
        this.includedWhenNoSource = this.srd52 || this.freeRules2024; // just 2024 when nothing specified

        this.edition = SourceAttributes.edition.getTextOrEmpty(jsonElement);
        addBrewSource(TtrpgValue.homebrewSource, jsonElement);
        addBrewSource(TtrpgValue.homebrewBaseSource, jsonElement);
        testSourceRules();
    }

    private void addBrewSource(JsonNodeReader field, JsonNode jsonElement) {
        String source = field.getTextOrNull(jsonElement);
        if (isPresent(source)) {
            this.sources.add(source);
        }
    }

    public boolean isHomebrew() {
        JsonNode node = findNode();
        return TtrpgValue.homebrewSource.existsIn(node) || TtrpgValue.homebrewBaseSource.existsIn(node);
    }

    public boolean isSrdOrFreeRules() {
        return srd || basicRules || srd52 || freeRules2024;
    }

    /**
     * Is this included by configuration (source list, include/exclude rules)?
     * Content may be suppressed for other reasons (reprints)
     */
    public boolean includedByConfig() {
        return cfgIncluded;
    }

    /**
     * Was this targeted by an include/exclude rule?
     */
    public boolean filterRuleApplied() {
        return filterRule;
    }

    private void testSourceRules() {
        CompendiumConfig config = TtrpgConfig.getConfig();
        Optional<Boolean> rulesSpecify = config.keyIsIncluded(key);
        this.filterRule = rulesSpecify.isPresent();
        this.cfgIncluded = testSourceRules(config, rulesSpecify);
    }

    /**
     * Test if this source is included by the configuration
     */
    private boolean testSourceRules(CompendiumConfig config, Optional<Boolean> rulesSpecify) {
        if (rulesSpecify.isPresent()) {
            return rulesSpecify.get();
        }
        if (config.allSources()) {
            return true;
        }
        if (config.noSources()) {
            return this.includedWhenNoSource;
        } else if (Tools5eIndex.isSrdBasicFreeOnly()) {
            return testSrdRules2014(config)
                    || testSrdRules2024(config);
        }
        return testSourceIncluded(config)
                || testSrdRules2014(config)
                || testSrdRules2024(config);
    }

    private boolean testSourceIncluded(CompendiumConfig config) {
        // backgrounds don't nest. Check only primary source
        return type == Tools5eIndexType.background
                ? config.sourceIncluded(this.primarySource())
                : config.sourceIncluded(this);
    }

    private boolean testSrdRules2014(CompendiumConfig config) {
        if (has2014Content()) {
            return (config.sourceIncluded("srd") && this.srd)
                    || (config.sourceIncluded("basicrules") && this.basicRules);
        }
        return false;
    }

    private boolean testSrdRules2024(CompendiumConfig config) {
        if (has2024Content()) {
            return (config.sourceIncluded("srd52") && this.srd52)
                    || (config.sourceIncluded("freerules2024") && this.freeRules2024);
        }
        return false;
    }

    @Override
    public boolean includedBy(Set<String> sources) {
        CompendiumConfig config = TtrpgConfig.getConfig();
        if (config.noSources()) {
            return this.includedWhenNoSource;
        }
        return super.includedBy(sources)
                || (this.srd && sources.contains("srd"))
                || (this.srd52 && sources.contains("srd52"))
                || (this.basicRules && sources.contains("basicrules"))
                || (this.freeRules2024 && sources.contains("freerules2024"));
    }

    @Override
    public Tools5eIndexType getType() {
        return type;
    }

    @Override
    protected boolean isSynthetic() {
        return type == Tools5eIndexType.syntheticGroup;
    }

    public String edition() {
        return edition;
    }

    public boolean isClassic() {
        return "classic".equalsIgnoreCase(edition);
    }

    public String getSourceText() {
        if (Tools5eIndex.isSrdBasicFreeOnly()) {
            List<String> bits = new ArrayList<>();
            if (srd) {
                bits.add("SRD 5.1");
            } else if (srd52) {
                bits.add("SRD 5.2");
            }
            if (freeRules2024) {
                bits.add("the Free Rules (2024)");
            } else if (basicRules) {
                bits.add("the Basic Rules (2014)");
            }
            return String.join(" and ", bits);
        }
        return super.getSourceText();
    }

    public JsonNode findNode() {
        JsonNode result = Tools5eIndex.getInstance().getNode(key);
        if (result == null) {
            result = Tools5eIndex.getInstance().getOrigin(this.key);
        }
        return result;
    }

    protected String findName(IndexType type, JsonNode jsonElement) {
        if (type == Tools5eIndexType.syntheticGroup) {
            return this.key.replaceAll(".+?\\|([^|]+).*", "$1");
        }

        if (Tools5eIndex.isSrdBasicFreeOnly() && Tools5eSources.isSrd(jsonElement)) {
            String srdName = Tools5eSources.srdName(jsonElement);
            if (srdName != null) {
                return srdName;
            }
        }

        return SourceField.name.getTextOrDefault(jsonElement,
                SourceField.abbreviation.getTextOrDefault(jsonElement,
                        TableFields.caption.getTextOrDefault(jsonElement,
                                "unknown")));
    }

    @Override
    protected String findSourceText(IndexType type, JsonNode jsonElement) {
        if (type == Tools5eIndexType.syntheticGroup) {
            return this.key.replaceAll(".*\\|([^|]+)\\|", "$1");
        }
        if (type == Tools5eIndexType.reference) {
            return "";
        }
        if (jsonElement == null) {
            Tui.instance().logf(Msg.UNRESOLVED, "Resource %s has no jsonElement", this.key);
            return "";
        }
        String srcText = super.findSourceText(type, jsonElement);

        JsonNode basicRules = jsonElement.get("basicRules");
        JsonNode freeRules2024 = jsonElement.get("freeRules2024");
        JsonNode srd52 = jsonElement.get("srd52");
        JsonNode srd = jsonElement.get("srd");

        String srdText = "";
        if (srd52 != null) {
            srdText = "the <span title='Systems Reference Document (5.2)'>SRD</span>";
            if (srd52.isTextual()) {
                srdText += " (as \"" + srd52.asText() + "\")";
            }
        } else if (srd != null) {
            srdText = "the <span title='Systems Reference Document (5.1)'>SRD</span>";
            if (srd.isTextual()) {
                srdText += " (as \"" + srd.asText() + "\")";
            }
        }

        String basicRulesText = "";
        if (freeRules2024 != null) {
            basicRulesText = "the Free Rules (2024)";
            if (freeRules2024.isTextual()) {
                basicRulesText += " (as \"" + freeRules2024.asText() + "\")";
            }
        } else if (basicRules != null) {
            basicRulesText = "the Basic Rules (2014)";
            if (basicRules.isTextual()) {
                basicRulesText += " (as \"" + basicRules.asText() + "\")";
            }
        }

        String sourceText = String.join(", ", srcText);
        if (srdText.isBlank() && basicRulesText.isBlank()) {
            return sourceText;
        }
        String srdBasic = "Available in " + srdText;
        if (!srdText.isEmpty() && !basicRulesText.isEmpty()) {
            srdBasic += " and ";
        }
        srdBasic += basicRulesText;
        return sourceText.isEmpty()
                ? srdBasic
                : sourceText + ". " + srdBasic;
    }

    public Optional<String> uaSource() {
        Optional<String> source = sources.stream().filter(x -> x.contains("UA") && !x.equals("UAWGE")).findFirst();
        return source.map(TtrpgConfig::sourceToAbbreviation);
    }

    public ImageRef buildTokenImageRef(Tools5eIndex index, String sourcePath, Path target, boolean useCompendium) {
        String key = sourcePath.toString();
        ImageRef imageRef = new ImageRef.Builder()
                .setRelativePath(target)
                .setInternalPath(sourcePath)
                .setRootFilepath(useCompendium ? index.compendiumFilePath() : index.rulesFilePath())
                .setVaultRoot(useCompendium ? index.compendiumVaultRoot() : index.rulesVaultRoot())
                .build(imageSourceToRef.get(key));
        imageSourceToRef.putIfAbsent(key, imageRef);
        return imageRef;
    }

    public ImageRef buildImageRef(Tools5eIndex index, JsonMediaHref mediaHref, String imageBasePath, boolean useCompendium) {
        final String title = mediaHref.title == null ? "" : mediaHref.title;
        final String altText = mediaHref.altText == null ? title : mediaHref.altText;
        final String key = mediaHref.href.path == null
                ? mediaHref.href.url
                : mediaHref.href.path;

        if (mediaHref.href.url == null && mediaHref.href.path == null) {
            Tui.instance().errorf("We have an ImageRef (%s) with no path", mediaHref);
            ImageRef imageRef = new ImageRef.Builder()
                    .setTitle(index.replaceText(altText))
                    .build();
            return imageRef;
        }

        String fullPath = mediaHref.href.path == null
                ? mediaHref.href.url
                : mediaHref.href.path.replace("\\", "/");
        int pos = fullPath.lastIndexOf('/');
        // Replace %20 with space ahead of slugify if it is present
        String fileName = fullPath.substring(pos + 1).replace("%20", " ");

        int query = fileName.lastIndexOf('?');
        if (query >= 0) {
            fileName = fileName.substring(0, query);
        }

        if (type == Tools5eIndexType.deity || type == Tools5eIndexType.note || type == Tools5eIndexType.variantrule) {
            fileName = primarySource() + "-" + fileName;
        } else if (type == Tools5eIndexType.deck && !fullPath.contains("generic")) {
            int pos2 = fullPath.substring(0, pos).lastIndexOf('/');
            fileName = fullPath.substring(pos2 + 1, pos) + "-" + fileName;
        }

        int x = fileName.lastIndexOf('.');
        fileName = x < 0
                ? index.slugify(fileName)
                : index.slugify(fileName.substring(0, x)) + fileName.substring(x);
        Path target = Path.of(imageBasePath, "img", fileName);

        ImageRef.Builder builder = new ImageRef.Builder()
                .setWidth(mediaHref.width)
                .setTitle(index.replaceText(altText))
                .setRelativePath(target)
                .setRootFilepath(useCompendium ? index.compendiumFilePath() : index.rulesFilePath())
                .setVaultRoot(useCompendium ? index.compendiumVaultRoot() : index.rulesVaultRoot());

        if (mediaHref.href.path == null) {
            builder.setUrl(mediaHref.href.url);
        } else {
            builder.setInternalPath(mediaHref.href.path);
        }

        ImageRef imageRef = builder.build(imageSourceToRef.get(key));
        imageSourceToRef.putIfAbsent(key, imageRef);
        return imageRef;
    }

    /** Amend optionalfeaturetype with sources of related optional features */
    public void amendSources(Tools5eSources otherSources) {
        this.sources.addAll(otherSources.sources);
        this.bookRef.addAll(otherSources.bookRef);
        testSourceRules();
    }

    public void amendSources(Set<String> brewSources) {
        this.sources.addAll(brewSources);
        testSourceRules();
    }

    public void amendHomebrewSources(JsonNode homebrewElement) {
        addBrewSource(TtrpgValue.homebrewBaseSource, homebrewElement);
        addBrewSource(TtrpgValue.homebrewSource, homebrewElement);
        testSourceRules();
    }

    public boolean contains(Tools5eSources sources) {
        Collection<String> sourcesList = sources.getSources();
        return this.sources.stream().anyMatch(sourcesList::contains);
    }

    public enum SourceAttributes implements JsonNodeReader {
        srd,
        basicRules,
        srd52,
        freeRules2024,
        edition;
    }

    public static void clear() {
        keyToSources.clear();
        imageSourceToRef.clear();
        fontSourceToRef.clear();
        keyToInlineNotes.clear();
        basicRulesKeys.clear();
        freeRulesKeys.clear();
    }

    public static boolean isClassicEdition(JsonNode baseItem) {
        String edition = SourceAttributes.edition.getTextOrDefault(baseItem, "");
        return "classic".equalsIgnoreCase(edition);
    }
}
