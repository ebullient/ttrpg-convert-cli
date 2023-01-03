package dev.ebullient.json5e.tools5e;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import dev.ebullient.json5e.io.Json5eTui;

public class Json5eConfig {
    final static TypeReference<List<String>> LIST_STRING = new TypeReference<List<String>>() {
    };
    final static TypeReference<Map<String, String>> MAP_STRING_STRING = new TypeReference<Map<String, String>>() {
    };
    final static Path CWD = Path.of(".");
    final static String CONVERT = "convert";
    final static String BOOK = "book";
    final static String ADVENTURE = "adventure";
    final static String TEMPLATE = "template";
    final static String FROM = "from";
    final static String IMAGE_MAP = "fallback-image";

    private String rulesRoot = "/rules/";
    private Path rulesPath = Path.of("rules/");

    private String compendiumRoot = "/compendium/";
    private Path compendiumPath = Path.of("compendium/");

    final Map<String, String> fallbackImagePaths = new HashMap<>();
    final Map<String, String> templates = new HashMap<>();
    final Set<String> adventures = new HashSet<>();
    final Set<String> books = new HashSet<>();

    private final Set<String> allowedSources = new HashSet<>();
    private final Set<String> includedKeys = new HashSet<>();
    private final Set<String> includeGroups = new HashSet<>();
    private final Set<String> excludedKeys = new HashSet<>();
    private final Set<Pattern> excludedPatterns = new HashSet<>();

    private boolean allSources;

    final Json5eTui tui;

    public Json5eConfig(Json5eTui tui, List<String> initialSources) {
        this.tui = tui;
        this.addSources(initialSources);
    }

    public void readConfigIfPresent(ObjectMapper mapper, JsonNode node) {
        JsonNode fullSource = node.get(CONVERT);
        if (fullSource != null) {
            if (fullSource.has(ADVENTURE)) {
                List<String> a = mapper.convertValue(fullSource.get(ADVENTURE), LIST_STRING);
                adventures.addAll(a);
            }
            if (fullSource.has(BOOK)) {
                List<String> b = mapper.convertValue(fullSource.get(BOOK), LIST_STRING);
                books.addAll(b);
            }
        }
        if (node.has(TEMPLATE)) {
            Map<String, String> tpl = mapper.convertValue(node.get(TEMPLATE), MAP_STRING_STRING);
            templates.putAll(tpl);
        }
        if (node.has(FROM)) {
            List<String> src = mapper.convertValue(node.get(FROM), LIST_STRING);
            addSources(src);
        }

        node.withArray("include").forEach(x -> includedKeys.add(x.asText()));
        node.withArray("includeGroups").forEach(x -> includeGroups.add(x.asText()));
        node.withArray("exclude").forEach(x -> excludedKeys.add(x.asText().toLowerCase()));
        node.withArray("excludePattern").forEach(x -> addExcludePattern(x.asText().toLowerCase()));

        if (node.has("paths")) {
            node.get("paths").fields().forEachRemaining(e -> {
                switch (e.getKey()) {
                    case "rules":
                        rulesRoot = ('/' + e.getValue().asText() + '/')
                                .replace('\\', '/')
                                .replaceAll("/+", "/");
                        if (rulesRoot.equals("/")) {
                            rulesPath = CWD;
                        } else {
                            rulesPath = Path.of(rulesRoot.substring(1));
                        }
                        rulesRoot = rulesRoot.replaceAll(" ", "%20");
                        break;
                    case "compendium":
                        compendiumRoot = ('/' + e.getValue().asText() + '/')
                                .replace('\\', '/')
                                .replaceAll("/+", "/");

                        if (compendiumRoot.equals("/")) {
                            compendiumPath = CWD;
                        } else {
                            compendiumPath = Path.of(compendiumRoot.substring(1));
                        }
                        compendiumRoot = compendiumRoot.replaceAll(" ", "%20");
                        break;
                }
            });
        }
        if (node.has(IMAGE_MAP)) {
            fallbackImagePaths.putAll(mapper.convertValue(node.get(IMAGE_MAP), MAP_STRING_STRING));
        }
    }

    public String rulesRoot() {
        return rulesRoot;
    }

    public String compendiumRoot() {
        return compendiumRoot;
    }

    public Path rulesPath() {
        return rulesPath;
    }

    public Path compendiumPath() {
        return compendiumPath;
    }

    public Map<String, Path> getTemplates() {
        if (templates.isEmpty()) {
            return Map.of();
        }

        Map<String, Path> result = new HashMap<>();
        for (Entry<String, String> entry : templates.entrySet()) {
            result.put(entry.getKey(), Path.of(entry.getValue()));
        }
        return result;
    }

    public List<String> getBooks() {
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
        return adventures.stream()
                .map(a -> {
                    if (a.endsWith(".json")) {
                        return a;
                    }
                    return "adventure/adventure-" + a.toLowerCase() + ".json";
                })
                .collect(Collectors.toList());
    }

    public void addSources(List<String> sources) {
        if (sources == null || sources.isEmpty()) {
            return;
        }
        this.allowedSources.addAll(sources.stream()
                .map(String::toLowerCase)
                .map(s -> "all".equals(s) ? "*" : s)
                .collect(Collectors.toList()));
        this.allSources = allowedSources.contains("*");
    }

    void addExcludePattern(String value) {
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

    public String getAllowedSourcePattern() {
        return allSources ? "([^|]+)" : "(" + String.join("|", allowedSources) + ")";
    }

    public boolean allSources() {
        return allSources;
    }

    public boolean srdOnly() {
        return allowedSources.isEmpty();
    }

    public boolean sourceIncluded(String source) {
        return allSources || allowedSources.contains(source.toLowerCase());
    }

    public boolean excludeItem(JsonNode itemSource, boolean isSRD) {
        if (allSources) {
            return false;
        }
        if (allowedSources.isEmpty()) {
            return !isSRD; // exclude non-SRD sources when no filter is specified.
        }
        if (itemSource == null || !itemSource.isTextual()) {
            return true; // unlikely, but skip items if we can't check their source
        }
        return !allowedSources.contains(itemSource.asText().toLowerCase());
    }

    public boolean rulesSourceExcluded(JsonNode node, String name) {
        boolean isSRD = node.has("srd");
        JsonNode itemSource = node.get("source");
        if (excludeItem(itemSource, isSRD)) {
            // skip this item: not from a specified source
            tui.debugf("Skipped %s from %s (%s)", name, itemSource, isSRD);
            return true;
        }
        return false;
    }

    public boolean keyIsIncluded(String key, JsonNode node,
            Collection<String> srdKeys, Collection<String> familiarKeys,
            Pattern classFeaturePattern, Pattern subclassFeaturePattern,
            Supplier<CompendiumSources> supplier) {

        if (includedKeys.contains(key)) {
            return true;
        }
        if (excludedKeys.contains(key) ||
                excludedPatterns.stream().anyMatch(x -> x.matcher(key).matches())) {
            return false;
        }
        if (allSources) {
            return true;
        }
        if (srdOnly()) {
            return srdKeys.contains(key);
        }
        if (key.contains("classfeature|")) {
            // class features squish phb
            String featureKey = key.replace("||", "|phb|");
            return classFeaturePattern.matcher(featureKey).matches() || subclassFeaturePattern.matcher(featureKey).matches();
        }
        if (key.startsWith("monster|") && key.endsWith("mm")
                && includeGroups.contains("familiars") && familiarKeys.contains(key)) {
            return true;
        }
        CompendiumSources sources = supplier.get();
        for (String s : sources.bookSources) {
            if (allowedSources.contains(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public Map<String, String> getFallbackPaths() {
        return fallbackImagePaths;
    }
}
