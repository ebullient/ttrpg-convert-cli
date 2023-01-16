package dev.ebullient.convert.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;

public class CompendiumConfig {
    final static Path CWD = Path.of(".");

    final Tui tui;
    final Datasource datasource;

    PathAttributes paths;
    boolean allSources = false;
    final Set<String> allowedSources = new HashSet<>();
    final Set<String> includedKeys = new HashSet<>();
    final Set<String> includeGroups = new HashSet<>();
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
        return includeGroups.contains(group);
    }

    public String rulesRoot() {
        return getPaths().rulesRoot;
    }

    public String compendiumRoot() {
        return getPaths().compendiumRoot;
    }

    public Path rulesPath() {
        return getPaths().rulesPath;
    }

    public Path compendiumPath() {
        return getPaths().compendiumPath;
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
            JsonNode from = ConfigKeys.from.get(node);
            if (from != null) {
                List<String> src = Tui.MAPPER.convertValue(from, Tui.LIST_STRING);
                config.addSources(src);
            }

            ConfigKeys.include.forEach(node, (x) -> config.includedKeys.add(x.asText()));
            ConfigKeys.includeGroups.forEach(node, (x) -> config.includeGroups.add(x.asText()));
            ConfigKeys.exclude.forEach(node, (x) -> config.excludedKeys.add(x.asText().toLowerCase()));
            ConfigKeys.excludePattern.forEach(node, (x) -> config.addExcludePattern(x.asText().toLowerCase()));

            JsonNode fullSource = ConfigKeys.fullSource.get(node);
            if (fullSource != null) {
                JsonNode adventure = ConfigKeys.adventure.get(fullSource);
                if (adventure != null) {
                    List<String> a = Tui.MAPPER.convertValue(adventure, Tui.LIST_STRING);
                    config.adventures.addAll(a);
                }

                JsonNode book = ConfigKeys.book.get(fullSource);
                if (book != null) {
                    List<String> b = Tui.MAPPER.convertValue(book, Tui.LIST_STRING);
                    config.books.addAll(b);
                }
            }

            JsonNode paths = ConfigKeys.paths.get(node);
            if (paths != null) {
                config.paths = new PathAttributes(paths);
            }

            JsonNode templates = ConfigKeys.template.get(node);
            if (templates != null) {
                TemplatePaths tplPaths = new TemplatePaths();
                Map<String, String> tplString = Tui.MAPPER.convertValue(templates, Tui.MAP_STRING_STRING);
                for (Entry<String, String> entry : tplString.entrySet()) {
                    tplPaths.setCustomTemplate(entry.getKey(), Path.of(entry.getValue()));
                }
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
        String rulesRoot = "/rules/";
        String compendiumRoot = "/compendium/";

        Path rulesPath = Path.of("rules/");
        Path compendiumPath = Path.of("compendium/");

        PathAttributes() {
        }

        PathAttributes(JsonNode paths) {
            paths.fields().forEachRemaining(e -> {
                String root;
                switch (e.getKey()) {
                    case "rules":
                        root = toRoot(e.getValue().asText());
                        rulesPath = rootToPath(root);
                        rulesRoot = rootToMarkdown(root);
                        break;
                    case "compendium":
                        root = toRoot(e.getValue().asText());
                        compendiumPath = rootToPath(root);
                        compendiumRoot = rootToMarkdown(root);
                        break;
                }
            });
        }

        private static String toRoot(String value) {
            return ('/' + value + '/')
                    .replace('\\', '/')
                    .replaceAll("/+", "/");
        }

        private static Path rootToPath(String root) {
            if (root.equals("/")) {
                return CWD;
            }
            return Path.of(root.substring(1));
        }

        private static String rootToMarkdown(String root) {
            return root.replaceAll(" ", "%20");
        }
    }

    private enum ConfigKeys {
        adventure,
        book,
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

        void forEach(JsonNode parent, Consumer<JsonNode> action) {
            JsonNode child = this.get(parent);
            if (child != null && child.isArray()) {
                child.forEach(x -> action.accept(x));
            }
        }
    }
}
