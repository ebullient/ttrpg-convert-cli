package dev.ebullient.convert.config;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.TtrpgConfig.Fix;
import dev.ebullient.convert.config.UserConfig.ImageOptions;
import dev.ebullient.convert.config.UserConfig.VaultPaths;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.ParseState;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class CompendiumConfig {

    public enum DiceRoller {
        disabled,
        disabledUsingFS,
        enabled,
        enabledUsingFS;

        public boolean enabled() {
            return this == enabled || this == enabledUsingFS;
        }

        public boolean useFantasyStatblocks() {
            return this == enabledUsingFS || this == disabledUsingFS;
        }

        public boolean useDiceRolls(ParseState state) {
            return switch (this) {
                case disabledUsingFS, disabled -> false;
                case enabled -> true;
                case enabledUsingFS -> !state.inTrait();
            };
        }

        public boolean decorate(ParseState state) {
            return switch (this) {
                case enabled -> false;
                case disabled -> true;
                case enabledUsingFS, disabledUsingFS -> !state.inTrait();
            };
        }

        static DiceRoller fromAttributes(Boolean useDiceRoller, Boolean yamlStatblocks) {
            yamlStatblocks = yamlStatblocks == null ? false : yamlStatblocks;

            if (useDiceRoller == null || !useDiceRoller) {
                return yamlStatblocks ? disabledUsingFS : disabled;
            }
            return yamlStatblocks ? enabledUsingFS : enabled;
        }
    }

    final static Path CWD = Path.of(".");

    @JsonIgnore
    final Tui tui;

    Datasource datasource;

    @JsonIgnore
    final ParseState parseState = new ParseState();

    String tagPrefix = "";
    PathAttributes paths;
    ImageOptions images;
    boolean allSources = false;
    DiceRoller useDiceRoller = DiceRoller.disabled;
    ReprintBehavior reprintBehavior = ReprintBehavior.newest;
    final Set<String> allowedSources = new HashSet<>();
    final Set<String> includedKeys = new HashSet<>();
    final Set<String> includedGroups = new HashSet<>();
    final Set<String> excludedKeys = new HashSet<>();
    final Set<Pattern> excludedPatterns = new HashSet<>();
    final Set<String> homebrew = new HashSet<>();
    final Set<String> adventures = new HashSet<>();
    final Set<String> books = new HashSet<>();
    final Map<String, String> defaultSource = new HashMap<>();
    final Map<String, Path> customTemplates = new HashMap<>();
    final Map<String, String> sourceIdAlias = new HashMap<>();

    CompendiumConfig(Datasource datasource, Tui tui) {
        this.datasource = datasource;
        this.tui = tui;
    }

    public ParseState parseState() {
        return parseState;
    }

    public Tui tui() {
        return tui;
    }

    public Datasource datasource() {
        return datasource;
    }

    public DiceRoller useDiceRoller() {
        return useDiceRoller;
    }

    public ReprintBehavior reprintBehavior() {
        return reprintBehavior;
    }

    public boolean allSources() {
        return allSources;
    }

    public boolean noSources() {
        return allowedSources.isEmpty();
    }

    public boolean onlySources(List<String> sources) {
        return allowedSources.stream().allMatch(sources::contains);
    }

    public String getAllowedSourcePattern() {
        return allSources ? "([^|]+)" : "(" + String.join("|", allowedSources) + ")";
    }

    public boolean readSource(Path p, List<Fix> fixes, BiConsumer<String, JsonNode> callback) {
        return tui.readFile(p, fixes, callback);
    }

    public boolean sourceIncluded(String source) {
        if (allSources) {
            return true;
        }
        if (source == null || source.isEmpty()) {
            return false;
        }
        return allowedSources.contains(source.toLowerCase());
    }

    public boolean sourcesIncluded(List<String> sources) {
        if (allSources) {
            return true;
        }
        if (sources == null || sources.isEmpty()) {
            return false;
        }
        return sources.stream().anyMatch(x -> allowedSources.contains(x.toLowerCase()));
    }

    public boolean sourceIncluded(CompendiumSources source) {
        if (allSources) {
            return true;
        }
        return source.includedBy(allowedSources);
    }

    public Optional<Boolean> keyIsIncluded(String key) {
        if (includedKeys.contains(key)) {
            return Optional.of(true);
        }
        if (excludedKeys.contains(key) ||
                excludedPatterns.stream().anyMatch(x -> x.matcher(key).matches())) {
            return Optional.of(false);
        }
        return Optional.empty();
    }

    public boolean groupIsIncluded(String group) {
        return includedGroups.contains(group);
    }

    public String rulesVaultRoot() {
        return pathAttributes().rulesVaultRoot;
    }

    public String compendiumVaultRoot() {
        return pathAttributes().compendiumVaultRoot;
    }

    public Path rulesFilePath() {
        return pathAttributes().rulesFilePath;
    }

    public Path compendiumFilePath() {
        return pathAttributes().compendiumFilePath;
    }

    public String tagOf(String... tag) {
        return tagPrefix + Arrays.stream(tag)
                .map(Tui::slugify)
                .collect(Collectors.joining("/"));
    }

    public String tagOfRaw(String tag) {
        return tagPrefix + tag;
    }

    public List<String> resolveBooks() {
        // works for 5eTools and pf2eTools
        return books.stream()
                .map(b -> {
                    if (b.endsWith(".json")) {
                        return b;
                    }
                    String bl = b.toLowerCase();
                    allowSource(bl);
                    String id = sourceIdAlias.getOrDefault(bl, bl);
                    allowSource(id);
                    return "book/book-" + id + ".json";
                })
                .toList();
    }

    public List<String> resolveAdventures() {
        // works for 5eTools and pf2eTools
        return adventures.stream()
                .map(a -> {
                    if (a.endsWith(".json")) {
                        return a;
                    }
                    String al = a.toLowerCase();
                    allowSource(al);
                    String id = sourceIdAlias.getOrDefault(al, al);
                    allowSource(id);
                    return "adventure/adventure-" + id + ".json";
                })
                .toList();
    }

    public Collection<String> resolveHomebrew() {
        return Collections.unmodifiableCollection(homebrew);
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

    /** Package private: add source */
    void allowSource(String source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        String s = source.toLowerCase();
        allowedSources.add("all".equals(s) ? "*" : s);
        allSources = allowedSources.contains("*");

        if (!allSources) {
            // If this source maps to an abbreviation, include that, too
            // This also handles source renames (freeRules2024 -> basicRules2024)
            String abbv = TtrpgConfig.sourceToAbbreviation(s);
            allowedSources.add(abbv);
        }
    }

    /** Package private: add sources */
    void allowSources(List<String> sources) {
        if (sources == null) {
            return;
        }
        for (String s : sources) {
            allowSource(s);
        }
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

    private PathAttributes pathAttributes() {
        if (paths == null) {
            return paths = new PathAttributes();
        }
        return paths;
    }

    ImageOptions imageOptions() {
        if (images == null) {
            return images = new ImageOptions();
        }
        return images;
    }

    /**
     * Create / populate CompendiumConfig in TtrpgConfig
     */
    public static class Configurator {

        protected final Tui tui;

        public Configurator(Tui tui) {
            this.tui = tui;
        }

        public Configurator(CompendiumConfig compendiumConfig) {
            this(compendiumConfig.tui);
        }

        public void allowSource(String src) {
            CompendiumConfig cfg = TtrpgConfig.getConfig();
            cfg.allowSource(src);
        }

        public void setSourceIdAlias(String src, String id) {
            CompendiumConfig cfg = TtrpgConfig.getConfig();
            cfg.sourceIdAlias.put(src.toLowerCase(), id.toLowerCase());
        }

        public void setTemplatePaths(TemplatePaths templatePaths) {
            CompendiumConfig cfg = TtrpgConfig.getConfig();
            templatePaths.verify(tui);
            cfg.customTemplates.putAll(templatePaths.customTemplates);
        }

        public void setUseDiceRoller(DiceRoller useDiceRoller) {
            CompendiumConfig cfg = TtrpgConfig.getConfig();
            cfg.useDiceRoller = useDiceRoller;
        }

        /** Parse the config file at the given path */
        public boolean readConfiguration(Path configPath) {
            try {
                if (configPath != null && configPath.toFile().exists()) {
                    JsonNode node = Tui.mapper(configPath).readTree(configPath.toFile());
                    readConfigIfPresent(node);
                } else {
                    tui.errorf("Unknown configuration file: %s", configPath);
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
         * Reads contents of JsonNode.
         * Will read and process user configuration keys if they are
         * present.
         *
         * @param node
         */
        public void readConfigIfPresent(JsonNode node) {
            if (userConfigPresent(node)) {
                CompendiumConfig cfg = TtrpgConfig.getConfig();
                readConfig(cfg, node);
            }
        }

        private void readConfig(CompendiumConfig config, JsonNode node) {
            UserConfig input = Tui.MAPPER.convertValue(node, UserConfig.class);

            if (input.useDiceRoller != null || input.yamlStatblocks != null) {
                config.useDiceRoller = DiceRoller.fromAttributes(input.useDiceRoller, input.yamlStatblocks);
            }

            input.include.forEach(s -> config.includedKeys.add(s.toLowerCase()));
            input.includeGroup.forEach(s -> config.includedGroups.add(s.toLowerCase()));
            input.exclude.forEach(s -> config.excludedKeys.add(s.toLowerCase()));
            input.excludePattern.forEach(s -> config.addExcludePattern(s.toLowerCase()));

            config.allowSources(input.references()); // sources + from
            config.books.addAll(input.sources.book);
            config.adventures.addAll(input.sources.adventure);
            config.homebrew.addAll(input.sources.homebrew);

            // map: type to default source
            input.sources.defaultSource.entrySet().stream()
                    .forEach(e -> {
                        config.defaultSource.put(e.getKey().toLowerCase(), e.getValue());
                    });

            config.images = new ImageOptions(config.images, input.images);
            config.paths = new PathAttributes(config.paths, input.paths);

            if (input.tagPrefix != null && !input.tagPrefix.isEmpty()) {
                config.tagPrefix = input.tagPrefix;
                if (!config.tagPrefix.endsWith("/")) {
                    config.tagPrefix += "/";
                }
            }

            if (!input.template.isEmpty()) {
                TemplatePaths tplPaths = new TemplatePaths();
                input.template.forEach((key, value) -> tplPaths.setCustomTemplate(key, Path.of(value)));
                tplPaths.verify(tui);
                config.customTemplates.putAll(tplPaths.customTemplates);
            }

            config.reprintBehavior = input.reprintBehavior;
        }
    }

    private static boolean userConfigPresent(JsonNode node) {
        return Stream.of(UserConfig.ConfigKeys.values())
                .anyMatch((k) -> k.get(node) != null);
    }

    private static class PathAttributes {
        String rulesVaultRoot = "rules/";
        String compendiumVaultRoot = "compendium/";

        Path rulesFilePath = Path.of("rules/");
        Path compendiumFilePath = Path.of("compendium/");

        PathAttributes() {
        }

        public PathAttributes(PathAttributes old, VaultPaths paths) {
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
            if (value == null || value.isEmpty()) {
                return "";
            }
            return (value + '/')
                    .replace('\\', '/')
                    .replaceAll("/+", "/");
        }

        private static Path toFilesystemRoot(String root) {
            if (root.equals("/") || root.isBlank()) {
                return CWD;
            }
            return Path.of(root.startsWith("/") ? root.substring(1) : root);
        }

        private static String toVaultRoot(String root) {
            return root.replaceAll(" ", "%20");
        }
    }
}
