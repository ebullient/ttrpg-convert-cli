package dev.ebullient.convert.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class CompendiumConfig {
    final static Path CWD = Path.of(".");

    final Tui tui;
    final Datasource datasource;

    String tagPrefix = ""; // TODO: empty or ends with '/'
    PathAttributes paths;
    boolean allSources = false;
    boolean useDiceRoller = false;
    final Set<String> allowedSources = new HashSet<>();
    final Set<String> includedKeys = new HashSet<>();
    final Set<String> includedGroups = new HashSet<>();
    final Set<String> excludedKeys = new HashSet<>();
    final Set<Pattern> excludedPatterns = new HashSet<>();
    final Set<String> adventures = new HashSet<>();
    final Set<String> books = new HashSet<>();
    final Map<String, Path> customTemplates = new HashMap<>();

    CompendiumConfig(Datasource src, Tui tui) {
        this.tui = tui;
        this.datasource = src;
    }

    public Tui tui() {
        return tui;
    }

    public Datasource datasource() {
        return datasource;
    }

    public boolean alwaysUseDiceRoller() {
        return useDiceRoller;
    }

    public boolean allSources() {
        return allSources;
    }

    public boolean noSources() {
        return allowedSources.isEmpty();
    }

    public String getAllowedSourcePattern() {
        return allSources ? "([^|]+)" : "(" + String.join("|", allowedSources) + ")";
    }

    public boolean sourceIncluded(String source) {
        return allSources || allowedSources.contains(source.toLowerCase());
    }

    public boolean excludeItem(JsonNode sourceNode, boolean allowWhenEmpty) {
        if (allSources) {
            return false;
        }
        if (allowedSources.isEmpty()) {
            // skip item when no sources are defined
            return !allowWhenEmpty;
        }
        if (sourceNode == null || !sourceNode.isTextual()) {
            // unlikely, but skip items if we can't check their source
            return true;
        }
        // skip item if the source isn't in allowed sources
        return !allowedSources.contains(sourceNode.asText().toLowerCase());
    }

    public Optional<Boolean> keyIsIncluded(String key, JsonNode node) {
        if (includedKeys.contains(key)) {
            return Optional.of(true);
        }
        if (excludedKeys.contains(key) ||
                excludedPatterns.stream().anyMatch(x -> x.matcher(key).matches())) {
            return Optional.of(false);
        }
        if (allSources) {
            return Optional.of(true);
        }
        return Optional.empty();
    }

    public boolean groupIsIncluded(String group) {
        return includedGroups.contains(group);
    }

    public String rulesVaultRoot() {
        return getPaths().rulesVaultRoot;
    }

    public String compendiumVaultRoot() {
        return getPaths().compendiumVaultRoot;
    }

    public Path rulesFilePath() {
        return getPaths().rulesFilePath;
    }

    public Path compendiumFilePath() {
        return getPaths().compendiumFilePath;
    }

    public String tagOf(String... tag) {
        return tagPrefix + Arrays.stream(tag)
                .map(s -> Tui.slugify(s))
                .collect(Collectors.joining("/"));
    }

    public List<String> getBooks() {
        // works for 5eTools and pf2eTools
        return books.stream()
                .map(b -> {
                    if (b.endsWith(".json")) {
                        return b;
                    }
                    return "book/book-" + b.toLowerCase() + ".json";
                })
                .collect(Collectors.toList());
    }

    public List<String> getAdventures() {
        // works for 5eTools and pf2eTools
        return adventures.stream()
                .map(a -> {
                    if (a.endsWith(".json")) {
                        return a;
                    }
                    return "adventure/adventure-" + a.toLowerCase() + ".json";
                })
                .collect(Collectors.toList());
    }

    public Path getCustomTemplate(String id) {
        return customTemplates.get(id);
    }

    public void readConfigurationIfPresent(JsonNode node) {
        if (userConfigPresent(node)) {
            Configurator c = new Configurator(this);
            c.readConfigIfPresent(node);
        }
    }

    private void addSources(List<String> sources) {
        if (sources == null || sources.isEmpty()) {
            return;
        }
        allowedSources.addAll(sources.stream()
                .map(String::toLowerCase)
                .map(s -> "all".equals(s) ? "*" : s)
                .collect(Collectors.toList()));
        allSources = allowedSources.contains("*");
    }

    private void addExcludePattern(String value) {
        String[] split = value.split("\\|");
        if (split.length > 1) {
            for (int i = 0; i < split.length - 1; i++) {
                if (!split[i].endsWith("\\")) {
                    split[i] += "\\";
                }
            }
        }
        excludedPatterns.add(Pattern.compile(String.join("|", split)));
    }

    private PathAttributes getPaths() {
        if (paths == null) {
            return paths = new PathAttributes();
        }
        return paths;
    }

    /**
     * Create / populate CompendiumConfig in TtrpgConfig
     */
    public static class Configurator {

        protected Tui tui;

        public Configurator(Tui tui) {
            this.tui = tui;
        }

        public Configurator(CompendiumConfig compendiumConfig) {
            this.tui = compendiumConfig.tui;
        }

        /** 1.x sources from command line */
        public void setSources(List<String> source) {
            CompendiumConfig cfg = TtrpgConfig.getConfig();
            cfg.addSources(source);
        }

        public void setTemplatePaths(TemplatePaths templatePaths) {
            CompendiumConfig cfg = TtrpgConfig.getConfig();
            cfg.customTemplates.putAll(templatePaths.customTemplates);
            templatePaths.verify(tui);
        }

        public void setAlwaysUseDiceRoller(boolean useDiceRoller) {
            CompendiumConfig cfg = TtrpgConfig.getConfig();
            cfg.useDiceRoller = useDiceRoller;
        }

        /** Parse the config file at the given path */
        public boolean readConfiguration(Path configPath) {
            try {
                if (configPath != null) {
                    JsonNode node = Tui.mapper(configPath).readTree(configPath.toFile());
                    readConfigIfPresent(node);
                } else {
                    tui.errorf("Unknown configuration file: %s",
                            configPath);
                    return false;
                }
            } catch (IOException e) {
                tui.errorf(e, "Error parsing configuration file (%s): %s",
                        configPath, e.getMessage());
                return false;
            }
            return true;
        }

        /**
         * Reads contents of JsonNode. If TTRPG/Compendium
         * configuration is present, it will create the
         * CompendiumConfig for it, and set that on
         * {@link TtrpgConfig} (as default, or with
         * appropriate key).
         *
         * @param node
         */
        public void readConfigIfPresent(JsonNode node) {
            JsonNode ttrpgNode = ConfigKeys.ttrpg.get(node);
            if (ttrpgNode != null) {
                for (Iterator<Entry<String, JsonNode>> i = ttrpgNode.fields(); i.hasNext();) {
                    Entry<String, JsonNode> e = i.next();
                    Datasource source = Datasource.matchDatasource(e.getKey());
                    CompendiumConfig cfg = TtrpgConfig.getConfig(source);
                    readConfig(cfg, e.getValue());
                }
            } else if (userConfigPresent(node)) {
                CompendiumConfig cfg = TtrpgConfig.getConfig();
                readConfig(cfg, node);
            }
        }

        private void readConfig(CompendiumConfig config, JsonNode node) {
            InputConfig input = Tui.MAPPER.convertValue(node, InputConfig.class);

            config.addSources(input.from);
            config.useDiceRoller |= input.useDiceRoller;

            input.include.forEach(s -> config.includedKeys.add(s.toLowerCase()));
            input.includeGroup.forEach(s -> config.includedGroups.add(s.toLowerCase()));
            input.exclude.forEach(s -> config.excludedKeys.add(s.toLowerCase()));
            input.excludePattern.forEach(s -> config.addExcludePattern(s.toLowerCase()));

            config.books.addAll(input.fullSource.book);
            config.adventures.addAll(input.fullSource.adventure);

            config.paths = new PathAttributes(config.paths, input.paths);

            if (!input.template.isEmpty()) {
                TemplatePaths tplPaths = new TemplatePaths();
                input.template.entrySet().forEach(e -> tplPaths.setCustomTemplate(e.getKey(), Path.of(e.getValue())));
                tplPaths.verify(tui);
                config.customTemplates.putAll(tplPaths.customTemplates);
            }
        }
    }

    private static boolean userConfigPresent(JsonNode node) {
        return Stream.of(ConfigKeys.values())
                .anyMatch((k) -> k.get(node) != null);
    }

    private static class PathAttributes {
        String rulesVaultRoot = "/rules/";
        String compendiumVaultRoot = "/compendium/";

        Path rulesFilePath = Path.of("rules/");
        Path compendiumFilePath = Path.of("compendium/");

        PathAttributes() {
        }

        public PathAttributes(PathAttributes old, InputPaths paths) {
            String root;
            if (paths.rules != null) {
                root = toRoot(paths.rules);
                rulesFilePath = toFilesystemRoot(root);
                rulesVaultRoot = toVaultRoot(root);
            } else if (old != null) {
                rulesFilePath = old.rulesFilePath;
                rulesVaultRoot = old.rulesVaultRoot;
            }
            if (paths.compendium != null) {
                root = toRoot(paths.compendium);
                compendiumFilePath = toFilesystemRoot(root);
                compendiumVaultRoot = toVaultRoot(root);
            } else if (old != null) {
                compendiumFilePath = old.compendiumFilePath;
                compendiumVaultRoot = old.compendiumVaultRoot;
            }
        }

        private static String toRoot(String value) {
            return ('/' + value + '/')
                    .replace('\\', '/')
                    .replaceAll("/+", "/");
        }

        private static Path toFilesystemRoot(String root) {
            if (root.equals("/")) {
                return CWD;
            }
            return Path.of(root.substring(1));
        }

        private static String toVaultRoot(String root) {
            return root.replaceAll(" ", "%20");
        }
    }

    private enum ConfigKeys {
        useDiceRoller,
        exclude,
        excludePattern,
        from,
        fullSource(List.of("convert", "full-source")),
        include,
        includeGroups,
        paths,
        template,
        ttrpg;

        final List<String> aliases;

        ConfigKeys() {
            aliases = List.of();
        }

        ConfigKeys(List<String> aliases) {
            this.aliases = aliases;
        }

        JsonNode get(JsonNode node) {
            JsonNode child = node.get(this.name());
            if (child == null) {
                Optional<JsonNode> y = aliases.stream()
                        .map(x -> node.get(x))
                        .filter(x -> x != null)
                        .findFirst();
                return y.orElse(null);
            }
            return child;
        }
    }

    @RegisterForReflection
    public static class InputConfig {
        @JsonProperty(required = false)
        List<String> from = new ArrayList<>();

        @JsonProperty(required = false)
        InputPaths paths = new InputPaths();

        @JsonProperty(required = false)
        List<String> include = new ArrayList<>();

        @JsonProperty(required = false)
        List<String> includeGroup = new ArrayList<>();

        @JsonProperty(required = false)
        List<String> exclude = new ArrayList<>();

        @JsonProperty(required = false)
        List<String> excludePattern = new ArrayList<>();

        @JsonProperty(required = false)
        Map<String, String> template = new HashMap<>();

        @JsonProperty(required = false)
        boolean useDiceRoller = false;

        @JsonAlias({ "convert" })
        @JsonProperty(value = "full-source", required = false)
        FullSource fullSource = new FullSource();
    }

    @RegisterForReflection
    static class FullSource {
        @JsonProperty(required = false)
        List<String> book = new ArrayList<>();

        @JsonProperty(required = false)
        List<String> adventure = new ArrayList<>();
    }

    @RegisterForReflection
    static class InputPaths {
        @JsonProperty(required = false)
        String compendium;

        @JsonProperty(required = false)
        String rules;
    }
}
