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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;

import dev.ebullient.convert.config.CompendiumConfig.Configurator;
import dev.ebullient.convert.config.CompendiumConfig.ImageOptions;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonNodeReader;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class TtrpgConfig {

    static final Map<Datasource, DatasourceConfig> globalConfig = new HashMap<>();
    static final Map<Datasource, CompendiumConfig> userConfig = new HashMap<>();
    static final Set<String> missingSourceName = new HashSet<>();

    private static Datasource datasource = Datasource.tools5e;
    private static Tui tui;
    private static ImageRoot internalImageRoot;

    public static void init(Tui tui, Datasource datasource) {
        userConfig.clear();
        internalImageRoot = null;
        TtrpgConfig.tui = tui;
        TtrpgConfig.datasource = datasource;
        readSystemConfig();
    }

    public static CompendiumConfig getConfig() {
        return getConfig(datasource == null ? Datasource.tools5e : datasource);
    }

    public static CompendiumConfig getConfig(Datasource datasource) {
        return userConfig.computeIfAbsent(datasource, (k) -> new CompendiumConfig(datasource, tui));
    }

    private static DatasourceConfig activeDSConfig() {
        return globalConfig.computeIfAbsent(datasource, (k) -> new DatasourceConfig());
    }

    public static List<Fix> getFixes(String filepath) {
        return activeDSConfig().findFixesFor(filepath.replace('\\', '/'));
    }

    public static String sourceToLongName(String src) {
        return activeDSConfig().abvToName.getOrDefault(sourceToAbbreviation(src).toLowerCase(), src);
    }

    public static String sourceToAbbreviation(String src) {
        return activeDSConfig().longToAbv.getOrDefault(src.toLowerCase(), src);
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

    public static class ImageRoot {
        final String internalImageRoot;
        final boolean copyRemote;

        private ImageRoot(String cfgRoot, ImageOptions options) {
            if (cfgRoot == null) {
                this.internalImageRoot = "";
                this.copyRemote = false;
            } else {
                if (cfgRoot.startsWith("http") || !cfgRoot.startsWith("file:")) {
                    this.internalImageRoot = endWithSlash(cfgRoot);
                } else {
                    Path imgPath = Path.of("").resolve(cfgRoot).normalize().toAbsolutePath();
                    if (!imgPath.toFile().exists()) {
                        tui.errorf("Image root %s does not exist", imgPath);
                        this.internalImageRoot = "";
                        this.copyRemote = false;
                        return;
                    }
                    this.internalImageRoot = endWithSlash(imgPath.toString());
                }
                this.copyRemote = options.copyRemote();
                Tui.instance().printlnf("🖼️ Using %s as the source for remote images (copyRemote=%s)",
                        this.internalImageRoot, this.copyRemote);
            }
        }

        public String getRootPath() {
            return internalImageRoot;
        }

        public boolean copyToVault() {
            return copyRemote;
        }

        private String endWithSlash(String path) {
            if (path == null) {
                return "";
            }
            return path.endsWith("/") ? path : path + "/";
        }
    }

    public static ImageRoot internalImageRoot() {
        ImageRoot root = internalImageRoot;
        if (root == null) {
            ImageOptions options = getConfig().imageOptions();
            String cfg = options.internalRoot;
            if (cfg == null) {
                cfg = activeDSConfig().constants.get("internalImageRoot");
            }
            internalImageRoot = root = new ImageRoot(cfg, options);
        }
        return root;
    }

    public static Map<String, String> imageFallbackPaths() {
        return activeDSConfig().fallbackImagePaths;
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
            if (activeConfig.abvToName.containsKey(check)) {
                return;
            }
            String alternate = activeConfig.longToAbv.get(check);
            if (alternate != null) {
                return;
            }
            if (missingSourceName.add(check)) {
                tui.warnf("Source %s is unknown", s);
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

        node = Tui.readTreeFromResource("/sourceMap.json");
        readSystemConfig(node);
    }

    // Global config: path mapping for missing images
    protected static void readSystemConfig(JsonNode node) {
        DatasourceConfig config = globalConfig.computeIfAbsent(datasource, k -> new DatasourceConfig());

        if (datasource == Datasource.tools5e) {
            JsonNode config5e = ConfigKeys.config5e.get(node);
            if (config5e != null) {
                JsonNode srdEntries = ConfigKeys.srdEntries.get(config5e);
                if (srdEntries != null) {
                    config.data.put(ConfigKeys.srdEntries.name(), srdEntries);
                }
                config.constants.putAll(ConfigKeys.constants.getAsMap(config5e));
                config.aliases.putAll(ConfigKeys.aliases.getAsMap(config5e));
                config.abvToName.putAll(ConfigKeys.abvToName.getAsKeyLowerMap(config5e));
                config.longToAbv.putAll(ConfigKeys.longToAbv.getAsKeyLowerMap(config5e));
                config.fallbackImagePaths.putAll(ConfigKeys.fallbackImage.getAsMap(config5e));
                config.markerFiles.addAll(ConfigKeys.markerFiles.getAsList(config5e));
                config.sources.addAll(ConfigKeys.sources.getAsList(config5e));
                config.indexes.putAll(ConfigKeys.indexes.getAsKeyLowerMap(config5e));
                config.templateKeys.addAll(ConfigKeys.templateKeys.getAsList(config5e));

                Map<String, List<Fix>> fixes = ConfigKeys.fixes.getAs(config5e, FIXES);
                if (fixes != null) {
                    config.fixes.putAll(fixes);
                }
            }
        }
        if (datasource == Datasource.toolsPf2e) {
            JsonNode configPf2e = ConfigKeys.configPf2e.get(node);
            if (configPf2e != null) {
                config.abvToName.putAll(ConfigKeys.abvToName.getAsKeyLowerMap(configPf2e));
                config.longToAbv.putAll(ConfigKeys.longToAbv.getAsKeyLowerMap(configPf2e));
                config.fallbackImagePaths.putAll(ConfigKeys.fallbackImage.getAsMap(configPf2e));
                config.markerFiles.addAll(ConfigKeys.markerFiles.getAsList(configPf2e));
                config.sources.addAll(ConfigKeys.sources.getAsList(configPf2e));
                config.indexes.putAll(ConfigKeys.indexes.getAsKeyLowerMap(configPf2e));
                config.templateKeys.addAll(ConfigKeys.templateKeys.getAsList(configPf2e));

                Map<String, List<Fix>> fixes = ConfigKeys.fixes.getAs(configPf2e, FIXES);
                if (fixes != null) {
                    config.fixes.putAll(fixes);
                }
            }
        }
    }

    static class DatasourceConfig {
        final Map<String, JsonNode> data = new HashMap<>();
        final Map<String, String> constants = new HashMap<>();
        final Map<String, String> aliases = new HashMap<>();
        final Map<String, String> abvToName = new HashMap<>();
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
            if (abvToName.containsKey(key)) {
                tui.errorf("Duplicate source abbreviation %s for %s", abv, name);
                return false;
            }
            abvToName.put(key, name);

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

    public final static TypeReference<Map<String, List<Fix>>> FIXES = new TypeReference<>() {
    };

    @RegisterForReflection
    public static class Fix {
        public String match;
        public String replace;
    }

    enum ConfigKeys implements JsonNodeReader {
        aliases,
        abvToName,
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
        sources,
        srdEntries,
        templateKeys;

        JsonNode get(JsonNode node) {
            return node.get(this.name());
        }

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

        List<String> getAsList(JsonNode node) {
            JsonNode list = node.get(this.name());
            return list == null
                    ? List.of()
                    : Tui.MAPPER.convertValue(list, Tui.LIST_STRING);
        }
    }
}
