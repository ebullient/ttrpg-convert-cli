package dev.ebullient.convert.config;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class TtrpgConfig {

    static final Map<Datasource, DatasourceConfig> globalConfig = new HashMap<>();
    static final Map<Datasource, CompendiumConfig> userConfig = new HashMap<>();
    static final Set<String> missingSourceName = new HashSet<>();

    private static Datasource datasource = Datasource.tools5e;
    private static Tui tui;

    public static void init(Tui tui, Datasource datasource) {
        userConfig.clear();
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

    private static DatasourceConfig activeConfig() {
        return globalConfig.computeIfAbsent(datasource, (k) -> new DatasourceConfig());
    }

    public static List<Fix> getFixes(String filepath) {
        List<Fix> list = activeConfig().fixes.get(filepath);
        return list == null ? List.of() : list;
    }

    public static String sourceToLongName(String src) {
        return activeConfig().abvToName.getOrDefault(sourceToAbbreviation(src).toLowerCase(), src);
    }

    public static String sourceToAbbreviation(String src) {
        return activeConfig().longToAbv.getOrDefault(src.toLowerCase(), src);
    }

    public static Map<String, String> imageFallbackPaths() {
        return activeConfig().fallbackImagePaths;
    }

    public static JsonNode activeGlobalConfig(String key) {
        return activeConfig().data.get(key);
    }

    public static void checkKnown(Collection<String> bookSources) {
        DatasourceConfig activeConfig = activeConfig();
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

    public static List<String> getMarkerFiles() {
        DatasourceConfig activeConfig = activeConfig();
        return Collections.unmodifiableList(activeConfig.markerFiles);
    }

    public static List<String> getFileSources() {
        DatasourceConfig activeConfig = activeConfig();
        return Collections.unmodifiableList(activeConfig.sources);
    }

    private static void readSystemConfig() {
        try {
            JsonNode node = Tui.MAPPER.readTree(TtrpgConfig.class.getResourceAsStream("/convertData.json"));
            readSystemConfig(node);

            node = Tui.MAPPER.readTree(TtrpgConfig.class.getResourceAsStream("/sourceMap.json"));
            readSystemConfig(node);
        } catch (IOException e) {
            tui.error(e, "Error reading system config: /convertData.json");
        }
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
                config.abvToName.putAll(ConfigKeys.abvToName.getAsKeyLowerMap(config5e));
                config.longToAbv.putAll(ConfigKeys.longToAbv.getAsKeyLowerMap(config5e));
                config.fallbackImagePaths.putAll(ConfigKeys.fallbackImage.getAsMap(config5e));
                config.markerFiles.addAll(ConfigKeys.markerFiles.getAsList(config5e));
                config.sources.addAll(ConfigKeys.sources.getAsList(config5e));

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

                Map<String, List<Fix>> fixes = ConfigKeys.fixes.getAs(configPf2e, FIXES);
                if (fixes != null) {
                    config.fixes.putAll(fixes);
                }
            }
        }
    }

    static class DatasourceConfig {
        final Map<String, JsonNode> data = new HashMap<>();
        final Map<String, String> abvToName = new HashMap<>();
        final Map<String, String> longToAbv = new HashMap<>();
        final Map<String, String> fallbackImagePaths = new HashMap<>();
        final Map<String, List<Fix>> fixes = new HashMap<>();
        final List<String> sources = new ArrayList<>();
        final List<String> markerFiles = new ArrayList<>();
    }

    public final static TypeReference<Map<String, List<Fix>>> FIXES = new TypeReference<>() {
    };

    @RegisterForReflection
    public static class Fix {
        public String match;
        public String replace;
    }

    enum ConfigKeys {
        fallbackImage,
        config5e,
        configPf2e,
        fixes,
        markerFiles,
        srdEntries,
        properties,
        sources,
        abvToName,
        longToAbv;

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
