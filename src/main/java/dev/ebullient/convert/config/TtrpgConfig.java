package dev.ebullient.convert.config;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import dev.ebullient.convert.config.CompendiumConfig.Configurator;
import dev.ebullient.convert.config.UserConfig.ImageOptions;
import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonNodeReader;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class TtrpgConfig {

    public static final String DEFAULT_IMG_ROOT = "imgRoot";

    static final Set<String> missingSourceName = new HashSet<>();

    private static Datasource datasource;
    private static CompendiumConfig activeConfig = null;
    private static DatasourceConfig datasourceConfig = null;
    private static Tui tui;
    private static ImageRoot internalImageRoot;
    private static Path toolsPath;

    public static void init(Tui tui) {
        init(tui, null);
    }

    public static void init(Tui tui, Datasource datasource) {
        TtrpgConfig.tui = tui;
        TtrpgConfig.internalImageRoot = null;
        TtrpgConfig.toolsPath = null;
        TtrpgConfig.activeConfig = null;
        TtrpgConfig.datasource = datasource == null ? Datasource.tools5e : datasource;
        TtrpgConfig.datasourceConfig = new DatasourceConfig();
        TtrpgConfig.missingSourceName.clear();
        readSystemConfig();
    }

    public static CompendiumConfig getConfig() {
        if (activeConfig == null) {
            activeConfig = new CompendiumConfig(TtrpgConfig.datasource, tui);
        }
        return activeConfig;
    }

    public static String getConstant(String key) {
        return activeDSConfig().constants.get(key);
    }

    public static void setToolsPath(Path toolsPath) {
        TtrpgConfig.toolsPath = toolsPath;
    }

    private static DatasourceConfig activeDSConfig() {
        return datasourceConfig;
    }

    public static List<Fix> getFixes(String filepath) {
        return activeDSConfig().findFixesFor(filepath.replace('\\', '/'));
    }

    public static String sourceToLongName(String src) {
        String abbreviation = sourceToAbbreviation(src).toLowerCase();
        SourceReference ref = activeDSConfig().reference.get(abbreviation);
        return ref == null ? src : ref.name;
    }

    public static String sourceToAbbreviation(String src) {
        return activeDSConfig().longToAbv.getOrDefault(src.toLowerCase(), src);
    }

    public static String sourcePublicationDate(String src) {
        String abbreviation = sourceToAbbreviation(src).toLowerCase();
        SourceReference ref = activeDSConfig().reference.get(abbreviation);
        return ref == null || ref.date == null ? "1970-01-01" : ref.date; // utils.json: ascSortDateString
    }

    public static Collection<String> getTemplateKeys() {
        return activeDSConfig().templateKeys;
    }

    public static boolean addHomebrewSource(String name, String abv, String longAbv) {
        return activeDSConfig().addSource(name, abv, longAbv);
    }

    public static void sourceToIdMapping(String id, String src) {
        Configurator config = new Configurator(getConfig());
        config.setSourceIdAlias(id, src);
    }

    public static void includeAdditionalSource(String src) {
        CompendiumConfig config = getConfig();
        config.addSource(src);
        // Books and Adventures use an id in the file name that may not
        // match the source abbreviation. When we add a source this way,
        // see if that mapping exists, and allow both.
        for (Entry<String, String> entry : config.sourceIdAlias.entrySet()) {
            if (entry.getValue().equals(src)) {
                config.addSource(entry.getKey());
            }
        }
    }

    public static void addReferenceEntries(Consumer<JsonNode> callback) {
        if (datasource == Datasource.tools5e) {
            JsonNode srdEntries = TtrpgConfig.activeGlobalConfig("srdEntries");
            for (JsonNode property : ConfigKeys.properties.iterateArrayFrom(srdEntries)) {
                callback.accept(property);
            }
        }
    }

    public static class ImageRoot {
        final String internalImageRoot;
        final boolean copyInternal;
        final boolean copyExternal;
        final Map<String, String> fallbackPaths;

        private ImageRoot(String cfgRoot, ImageOptions options) {
            this.copyExternal = options.copyExternal();
            this.fallbackPaths = options.fallbackPaths();

            if (cfgRoot == null) {
                this.internalImageRoot = "";
                this.copyInternal = false;
            } else {
                if (cfgRoot.startsWith("http") || cfgRoot.startsWith("file:")) {
                    this.internalImageRoot = endWithSlash(cfgRoot);
                    this.copyInternal = options.copyInternal();
                } else {
                    Path imgPath = Path.of("").resolve(cfgRoot).normalize().toAbsolutePath();
                    if (!imgPath.toFile().exists()) {
                        tui.errorf("Image root %s does not exist", imgPath);
                        this.internalImageRoot = "";
                        this.copyInternal = false;
                        return;
                    }
                    this.internalImageRoot = endWithSlash(imgPath.toString());
                    this.copyInternal = true;
                }
                Tui.instance().infof("Using %s as the source for remote images (copyInternal=%s)",
                        this.internalImageRoot, this.copyInternal);
            }
        }

        public String getRootPath() {
            return internalImageRoot;
        }

        public boolean copyInternalToVault() {
            return copyInternal;
        }

        public boolean copyExternalToVault() {
            return copyExternal;
        }

        public String getFallbackPath(String key) {
            return fallbackPaths.getOrDefault(key, key);
        }
    }

    public static ImageRoot internalImageRoot() {
        ImageRoot root = internalImageRoot;
        if (root == null) {
            ImageOptions options = getConfig().imageOptions();
            String cfg = options.internalRoot;
            if (cfg == null) {
                String imgRoot = activeDSConfig().constants.get(DEFAULT_IMG_ROOT);
                if (imgRoot == null && toolsPath != null && datasource == Datasource.toolsPf2e) {
                    cfg = toolsPath.resolve("..").normalize().toString();
                } else if (imgRoot == null) {
                    cfg = "";
                } else {
                    cfg = imgRoot;
                }
            }
            internalImageRoot = root = new ImageRoot(cfg, options);
        }
        return root;
    }

    private static String endWithSlash(String path) {
        if (path == null) {
            return "";
        }
        return path.endsWith("/") ? path : path + "/";
    }

    public static JsonNode readIndex(String key) {
        String file = activeDSConfig().indexes.get(key);
        Optional<Path> root = file == null ? Optional.empty() : tui.resolvePath(Path.of(file));
        if (root.isEmpty()) {
            return NullNode.getInstance();
        }
        File indexFile = root.get().resolve(file).toFile();
        try {
            return Tui.MAPPER.readTree(indexFile);
        } catch (Exception e) {
            tui.errorf("Failed to read index file %s: %s", indexFile, e.getMessage());
            return NullNode.getInstance();
        }
    }

    public static JsonNode activeGlobalConfig(String key) {
        return activeDSConfig().data.get(key);
    }

    public static void checkKnown(Collection<String> bookSources) {
        DatasourceConfig activeConfig = activeDSConfig();
        bookSources.forEach(s -> {
            String check = s.toLowerCase();
            if (activeConfig.reference.containsKey(check)) {
                return;
            }
            String alternate = activeConfig.longToAbv.get(check);
            if (alternate != null) {
                return;
            }
            if (missingSourceName.add(check)) {
                tui.warnf(Msg.SOURCE, "Source %s is unknown", s);
            }
        });
    }

    public static Collection<String> getMarkerFiles() {
        DatasourceConfig activeConfig = activeDSConfig();
        return Collections.unmodifiableSet(activeConfig.markerFiles);
    }

    public static Collection<String> getFileSources() {
        DatasourceConfig activeConfig = activeDSConfig();
        return Collections.unmodifiableSet(activeConfig.sources);
    }

    public static void addDefaultAliases(Map<String, String> aliases) {
        DatasourceConfig activeConfig = activeDSConfig();
        activeConfig.aliases.forEach((k, v) -> aliases.putIfAbsent(k, v));
    }

    private static void readSystemConfig() {
        JsonNode node = Tui.readTreeFromResource("/convertData.json");
        readSystemConfig(node);

        node = Tui.readTreeFromResource("/sourceMap.yaml");
        readSystemConfig(node);
    }

    // Global config: path mapping for missing images
    protected static void readSystemConfig(JsonNode node) {
        if (datasource == Datasource.tools5e) {
            JsonNode config5e = ConfigKeys.config5e.getFrom(node);
            if (config5e != null) {
                JsonNode srdEntries = ConfigKeys.srdEntries.getFrom(config5e);
                if (srdEntries != null) {
                    datasourceConfig.data.put(ConfigKeys.srdEntries.name(), srdEntries);
                }
                JsonNode basicRules = ConfigKeys.basicRules.getFrom(config5e);
                if (basicRules != null) {
                    datasourceConfig.data.put(ConfigKeys.basicRules.name(), basicRules);
                }
                JsonNode freeRules2024 = ConfigKeys.freeRules2024.getFrom(config5e);
                if (freeRules2024 != null) {
                    datasourceConfig.data.put(ConfigKeys.freeRules2024.name(), freeRules2024);
                }
                readCommonSystemConfig(config5e);
            }
        }
        if (datasource == Datasource.toolsPf2e) {
            JsonNode configPf2e = ConfigKeys.configPf2e.getFrom(node);
            if (configPf2e != null) {
                readCommonSystemConfig(configPf2e);
            }
        }
    }

    protected static void readCommonSystemConfig(JsonNode source) {
        datasourceConfig.constants.putAll(ConfigKeys.constants.getAsMap(source));
        datasourceConfig.aliases.putAll(ConfigKeys.aliases.getAsMap(source));
        datasourceConfig.reference.putAll(ConfigKeys.reference.getAsKeyLowerRefMap(source));
        datasourceConfig.longToAbv.putAll(ConfigKeys.longToAbv.getAsKeyLowerMap(source));
        datasourceConfig.fallbackImagePaths.putAll(ConfigKeys.fallbackImage.getAsMap(source));
        datasourceConfig.markerFiles.addAll(ConfigKeys.markerFiles.getAsList(source));
        datasourceConfig.sources.addAll(ConfigKeys.sources.getAsList(source));
        datasourceConfig.indexes.putAll(ConfigKeys.indexes.getAsKeyLowerMap(source));
        datasourceConfig.templateKeys.addAll(ConfigKeys.templateKeys.getAsList(source));

        Map<String, List<Fix>> fixes = ConfigKeys.fixes.getAs(source, FIXES);
        if (fixes != null) {
            datasourceConfig.fixes.putAll(fixes);
        }
    }

    static class DatasourceConfig {
        final Map<String, JsonNode> data = new HashMap<>();
        final Map<String, String> constants = new HashMap<>();
        final Map<String, String> aliases = new HashMap<>();
        final Map<String, SourceReference> reference = new HashMap<>();
        final Map<String, String> longToAbv = new HashMap<>();
        final Map<String, String> fallbackImagePaths = new HashMap<>();
        final Map<String, List<Fix>> fixes = new HashMap<>();
        final Map<String, String> indexes = new HashMap<>();
        final Set<String> sources = new HashSet<>();
        final Set<String> markerFiles = new HashSet<>();
        final Set<String> templateKeys = new TreeSet<>();

        public List<Fix> findFixesFor(String filepath) {
            for (Map.Entry<String, List<Fix>> entry : fixes.entrySet()) {
                if (filepath.endsWith(entry.getKey())) {
                    return entry.getValue();
                }
            }
            return List.of();
        }

        public boolean addSource(String name, String abv, String longAbv) {
            String key = abv.toLowerCase();
            if (reference.containsKey(key)) {
                tui.errorf("Duplicate source abbreviation %s for %s", abv, name);
                return false;
            }
            reference.put(key, new SourceReference(name));

            if (longAbv != null) {
                String longKey = longAbv.toLowerCase();
                if (!key.equals(longKey)) {
                    if (longToAbv.containsKey(longKey)) {
                        tui.errorf("Duplicate source key %s for %s -> %s", longKey, abv, name);
                    } else {
                        longToAbv.put(longKey, key);
                    }
                }
            }
            return true;
        }
    }

    public final static TypeReference<Map<String, SourceReference>> MAP_REFERENCE = new TypeReference<>() {
    };

    public final static TypeReference<Map<String, List<Fix>>> FIXES = new TypeReference<>() {
    };

    @RegisterForReflection
    static class SourceReference {
        String name;
        String type;
        String date;

        SourceReference() {
        }

        SourceReference(String name) {
            this.name = name;
        }
    }

    @RegisterForReflection
    public static class Fix {
        public String _comment;
        public String match;
        public String replace;
    }

    enum ConfigKeys implements JsonNodeReader {
        aliases,
        abvToName,
        basicRules, // 5e
        freeRules2024, // 5e
        config5e,
        configPf2e,
        constants,
        fallbackImage,
        internalImageRoot,
        fixes,
        indexes,
        longToAbv,
        markerFiles,
        properties,
        reference,
        sources,
        srdEntries,
        templateKeys,
        ;

        public <T> T getAs(JsonNode node, TypeReference<T> ref) {
            JsonNode obj = node.get(this.name());
            return obj == null
                    ? null
                    : Tui.MAPPER.convertValue(obj, ref);
        }

        Map<String, String> getAsMap(JsonNode node) {
            JsonNode map = node.get(this.name());
            return map == null
                    ? Map.of()
                    : Tui.MAPPER.convertValue(map, Tui.MAP_STRING_STRING);
        }

        Map<String, String> getAsKeyLowerMap(JsonNode node) {
            JsonNode map = node.get(this.name());
            if (map == null) {
                return Map.of();
            }
            Map<String, String> result = new HashMap<>();
            map.fields().forEachRemaining(e -> result.put(e.getKey().toLowerCase(), e.getValue().asText()));
            return result;
        }

        Map<String, SourceReference> getAsKeyLowerRefMap(JsonNode node) {
            JsonNode map = node.get(this.name());
            if (map == null) {
                return Map.of();
            }
            Map<String, SourceReference> result = new HashMap<>();
            map.fields().forEachRemaining(e -> {
                String key = e.getKey().toLowerCase();
                SourceReference ref = Tui.MAPPER.convertValue(e.getValue(), SourceReference.class);
                result.put(key, ref);
            });
            return result;
        }

        List<String> getAsList(JsonNode node) {
            JsonNode list = node.get(this.name());
            return list == null
                    ? List.of()
                    : Tui.MAPPER.convertValue(list, Tui.LIST_STRING);
        }
    }
}
