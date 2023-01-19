package dev.ebullient.convert.config;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;

public class TtrpgConfig {

    static final Map<Datasource, DatasourceConfig> globalConfig = new HashMap<Datasource, DatasourceConfig>();
    static final Map<Datasource, CompendiumConfig> userConfig = new HashMap<Datasource, CompendiumConfig>();
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

    public static String sourceToLongName(String src) {
        return activeConfig().abvToName.getOrDefault(sourceToAbbreviation(src), src);
    }

    public static String sourceToAbbreviation(String src) {
        return activeConfig().longToAbv.getOrDefault(src, src);
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
            if (activeConfig.abvToName.containsKey(s)) {
                return;
            }
            String alternate = activeConfig.longToAbv.get(s);
            if (alternate != null) {
                return;
            }
            if (missingSourceName.add(s)) {
                tui.warnf("Source %s is unknown", s);
            }
        });
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
                config.abvToName.putAll(ConfigKeys.abvToName.getAsMap(config5e));
                config.longToAbv.putAll(ConfigKeys.longToAbv.getAsMap(config5e));
                config.fallbackImagePaths.putAll(ConfigKeys.fallbackImage.getAsMap(config5e));
            }
        }
        if (datasource == Datasource.toolsPf2e) {
            JsonNode configPf2e = ConfigKeys.configPf2e.get(node);
            if (configPf2e != null) {
                config.abvToName.putAll(ConfigKeys.abvToName.getAsMap(configPf2e));
                config.longToAbv.putAll(ConfigKeys.longToAbv.getAsMap(configPf2e));
                config.fallbackImagePaths.putAll(ConfigKeys.fallbackImage.getAsMap(configPf2e));
            }
        }
    }

    static class DatasourceConfig {
        final Map<String, JsonNode> data = new HashMap<>();
        final Map<String, String> abvToName = new HashMap<>();
        final Map<String, String> longToAbv = new HashMap<>();
        final Map<String, String> fallbackImagePaths = new HashMap<>();
    }

    enum ConfigKeys {
        fallbackImage,
        config5e,
        configPf2e,
        srdEntries,
        properties,
        abvToName,
        longToAbv;

        JsonNode get(JsonNode node) {
            return node.get(this.name());
        }

        Map<String, String> getAsMap(JsonNode node) {
            JsonNode map = node.get(this.name());
            return map == null
                    ? Map.of()
                    : Tui.MAPPER.convertValue(map, Tui.MAP_STRING_STRING);
        }
    }

}
