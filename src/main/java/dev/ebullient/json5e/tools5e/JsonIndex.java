package dev.ebullient.json5e.tools5e;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.json5e.io.Json5eTui;

public class JsonIndex implements JsonSource {

    // classfeature|ability score improvement|monk|phb|12
    static final String classFeature_1 = "classfeature\\|[^|]+\\|[^|]+\\|";
    static final String classFeature_2 = "\\|\\d+\\|?";
    // subclassfeature|blessed strikes|cleric|phb|death|dmg|8|uaclassfeaturevariants
    static final String subclassFeature_1 = "subclassfeature\\|[^|]+\\|[^|]+\\|";
    static final String subclassFeature_2 = "\\|[^|]+\\|";
    static final String subclassFeature_3 = "\\|\\d+\\|?";

    final Json5eTui tui;
    private final boolean allSources;
    private final Map<String, JsonNode> rules = new HashMap<>();
    private final Set<String> allowedSources = new HashSet<>();
    private final Set<String> excludedKeys = new HashSet<>();
    private final Set<Pattern> excludedPatterns = new HashSet<>();

    private final Map<String, JsonNode> nodeIndex = new HashMap<>();
    private final Map<String, String> aliases = new HashMap<>();
    private Map<String, JsonNode> variantIndex = null;
    private Map<String, JsonNode> filteredIndex = null;

    private final Set<String> srdKeys = new HashSet<>();
    private final Set<String> familiarKeys = new HashSet<>();
    private final Set<String> includeGroups = new HashSet<>();
    private final Set<String> missingSourceName = new HashSet<>();

    private String rulesPath = "/rules/";
    private String compendiumPath = "/compendium/";

    final JsonSourceCopier copier = new JsonSourceCopier(this);

    Pattern classFeaturePattern;
    Pattern subclassFeaturePattern;

    public JsonIndex(List<String> sources, Json5eTui tui) {
        this.tui = tui;
        this.allowedSources.addAll(sources.stream().map(String::toLowerCase).collect(Collectors.toList()));
        this.allSources = allowedSources.contains("*");

        setClassFeaturePatterns();
    }

    public void readFile(Path p) throws IOException {
        File f = p.toFile();
        JsonNode node = Json5eTui.MAPPER.readTree(f);
        importTree(f.getName(), node);
        tui().verbosef("🔖 Finished reading %s", p);
    }

    public void readDirectory(Path dir) {
        String basename = dir.getFileName().toString();
        tui().debugf("📁 %s\n", dir);
        try (Stream<Path> stream = Files.list(dir)) {
            stream.forEach(p -> {
                File f = p.toFile();
                String name = p.getFileName().toString();
                if (f.isDirectory()) {
                    try {
                        readDirectory(p);
                    } catch (Exception e) {
                        tui().errorf(e, "Error reading directory %s", p.toString());
                    }
                } else if ((name.startsWith("fluff") || name.startsWith(basename)) && name.endsWith(".json")) {
                    try {
                        readFile(p);
                    } catch (Exception e) {
                        tui().errorf(e, "Error parsing file %s", p.toString());
                    }
                }
            });
        } catch (Exception e) {
            tui().errorf(e, "Error parsing %s", dir.toString());
        }
    }

    public JsonIndex importTree(String filename, JsonNode node) {
        if (!node.isObject()) {
            return this;
        }

        addConfigIfPresent(node);

        addRulesIfPresent(node, "action");
        addRulesIfPresent(node, "artObjects");
        addRulesIfPresent(node, "condition");
        addRulesIfPresent(node, "disease");
        addRulesIfPresent(node, "gems");
        addRulesIfPresent(node, "itemProperty");
        addRulesIfPresent(node, "itemType");
        addRulesIfPresent(node, "itemTypeAdditionalEntry");
        addRulesIfPresent(node, "magicItems");
        addRulesIfPresent(node, "sense");
        addRulesIfPresent(node, "skill");
        addRulesIfPresent(node, "status");
        addRulesIfPresent(node, "table");
        addRulesIfPresent(node, "variantrule");

        // Reference/Internal Types
        node.withArray("backgroundFluff").forEach(x -> addToIndex(IndexType.backgroundfluff, x));
        node.withArray("itemEntry").forEach(x -> addToIndex(IndexType.itementry, x));
        node.withArray("itemFluff").forEach(x -> addToIndex(IndexType.itemfluff, x));
        node.withArray("monsterFluff").forEach(x -> addToIndex(IndexType.monsterfluff, x));
        node.withArray("raceFluff").forEach(x -> addToIndex(IndexType.racefluff, x));
        node.withArray("spellFluff").forEach(x -> addToIndex(IndexType.spellfluff, x));
        node.withArray("subrace").forEach(x -> addToIndex(IndexType.subrace, x));
        node.withArray("trait").forEach(x -> addToIndex(IndexType.trait, x));
        node.withArray("legendaryGroup").forEach(x -> addToIndex(IndexType.legendarygroup, x));
        node.withArray("subclass").forEach(x -> addToIndex(IndexType.subclass, x));
        node.withArray("classFeature").forEach(x -> addToIndex(IndexType.classfeature, x));
        node.withArray("optionalfeature").forEach(x -> addToIndex(IndexType.optionalfeature, x));
        node.withArray("subclassFeature").forEach(x -> addToIndex(IndexType.subclassfeature, x));
        // TODO: node.withArray("variant").forEach(x -> addToIndex(IndexType.itemvariant, x));

        // Output Types
        node.withArray("background").forEach(x -> addToIndex(IndexType.background, x));
        node.withArray("class").forEach(x -> addToIndex(IndexType.classtype, x));
        node.withArray("feat").forEach(x -> addToIndex(IndexType.feat, x));
        node.withArray("baseitem").forEach(x -> addToIndex(IndexType.item, x));
        node.withArray("item").forEach(x -> addToIndex(IndexType.item, x));
        // TODO: node.withArray("object").forEach(x -> addToIndex(IndexType.item, x));
        // TODO: node.withArray("vehicle").forEach(x -> addToIndex(IndexType.item, x));
        node.withArray("monster").forEach(x -> addToIndex(IndexType.monster, x));
        node.withArray("race").forEach(x -> addToIndex(IndexType.race, x));
        node.withArray("spell").forEach(x -> addToIndex(IndexType.spell, x));

        if (node.has("name") && node.get("name").isArray()) {
            ArrayNode names = node.withArray("name");
            if (names.get(0).isObject() && names.get(0).has("tables")) {
                names.forEach(nt -> rules.put("names-" + slugify(nt.get("name").asText()), nt));
            }
        }

        node.withArray("adventure").forEach(x -> addReferenceToIndex(x, "adventure"));
        node.withArray("book").forEach(x -> addReferenceToIndex(x, "book"));
        if (node.has("data") && !filename.isEmpty()) {
            int slash = filename.indexOf('/');
            int dot = filename.indexOf('.');
            rules.put(filename.substring(slash < 0 ? 0 : slash + 1, dot < 0 ? filename.length() : dot), node);
        }

        return this;
    }

    private void addReferenceToIndex(JsonNode node, String type) {
        String key = getDataKey(type, node.get("id").asText());
        nodeIndex.put(key, node);
    }

    void addToIndex(IndexType type, JsonNode node) {
        String key = getKey(type, node);
        nodeIndex.put(key, node);
        if (type == IndexType.subclass) {
            String lookupKey = getSubclassKey(node.get("shortName").asText().trim(),
                    node.get("className").asText(), node.get("classSource").asText());
            // add subclass to alias. Referenced from spells
            addAlias(lookupKey, key);
        }
        if (type == IndexType.subrace) {
            // {@race Aasimar (Fallen)|VGM}
            String[] parts = key.split("\\|");
            String lookupKey = String.format("race|%s (%s)|%s",
                    parts[2], parts[1], parts[3]).toLowerCase();
            addAlias(lookupKey, key);
        }

        if (node.has("srd")) {
            srdKeys.add(key);
        }
        if (node.has("familiar")) {
            familiarKeys.add(key);
        }
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

    void addConfigIfPresent(JsonNode node) {
        node.withArray("from").forEach(x -> updateSources(x.asText().toLowerCase()));
        node.withArray("includeGroups").forEach(x -> includeGroups.add(x.asText()));
        node.withArray("exclude").forEach(x -> excludedKeys.add(x.asText().toLowerCase()));
        node.withArray("excludePattern").forEach(x -> addExcludePattern(x.asText().toLowerCase()));

        if (node.has("paths")) {
            node.get("paths").fields().forEachRemaining(e -> {
                switch (e.getKey()) {
                    case "rules":
                        rulesPath = e.getValue().asText();
                        if (!rulesPath.endsWith("/")) {
                            rulesPath += "/";
                        }
                        break;
                    case "compendium":
                        compendiumPath = e.getValue().asText();
                        if (!compendiumPath.endsWith("/")) {
                            compendiumPath += "/";
                        }
                        break;
                }
            });
        }
    }

    void addAlias(String key, String alias) {
        if (key.equals(alias)) {
            return;
        }
        String old = aliases.put(key, alias);
        if (old != null && !alias.equals(old)) {
            tui().errorf("Oops! Duplicate simple key: %s -> %s", key, alias);
        }
    }

    void addRulesIfPresent(JsonNode node, String rule) {
        if (node.has(rule)) {
            rules.put(rule, node.get(rule));
        }
    }

    void updateSources(String x) {
        allowedSources.add(x);
        setClassFeaturePatterns();
    }

    void setClassFeaturePatterns() {
        String allowed = allowedSources.contains("*") ? "([^|]+)" : "(" + String.join("|", allowedSources) + ")";
        classFeaturePattern = Pattern.compile(classFeature_1 + allowed + classFeature_2 + allowed + "?");
        subclassFeaturePattern = Pattern
                .compile(subclassFeature_1 + allowed + subclassFeature_2 + allowed + subclassFeature_3 + allowed + "?");
    }

    public void prepare() {
        if (variantIndex != null || filteredIndex != null) {
            return;
        }
        variantIndex = new HashMap<>();

        nodeIndex.forEach((key, node) -> {
            IndexType type = IndexType.getTypeFromKey(key);
            JsonNode jsonSource = copier.handleCopy(type, node);

            if (type == IndexType.trait || type == IndexType.subrace || type == IndexType.legendarygroup) {
                // subraces are pulled in by races
                // traits and legendary groups are pulled in my monsters
                // check for / manage copies first.
                return;
            }

            // Find variants
            List<Tuple> variants = findVariants(key, jsonSource);
            if (variants.size() > 1) {
                tui.debugf("%s variants found for %s", variants.size(), key);
                variants.forEach(x -> tui.debugf("\t%s", x.key));
            }
            variants.forEach(v -> {
                JsonNode old = variantIndex.put(v.key, v.node);
                if (old != null) {
                    tui.errorf("Duplicate key: %s", v.key);
                }
            });
        });

        // Exclude items after we've created variants and handled copies
        filteredIndex = variantIndex.entrySet().stream()
                .filter(e -> !isReprinted(e.getKey(), e.getValue()))
                .filter(e -> keyIsIncluded(e.getKey(), e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    List<Tuple> findVariants(String key, JsonNode jsonSource) {
        IndexType type = IndexType.getTypeFromKey(key);
        if (type == IndexType.race) {
            return Json2QuteRace.findRaceVariants(this, type, key, jsonSource);
        } else if (type == IndexType.monster && jsonSource.has("summonedBySpellLevel")) {
            return Json2QuteMonster.findConjuredMonsterVariants(this, type, key, jsonSource);
        } else if (key.contains("splugoth the returned")) {
            // Fix.
            ObjectNode copy = (ObjectNode) copier.copyNode(jsonSource);
            copy.put("isNpc", true);
            return List.of(new Tuple(key, copy));
        }
        return List.of(new Tuple(key, jsonSource));
    }

    boolean isReprinted(String finalKey, JsonNode jsonSource) {
        if (jsonSource.has("reprintedAs")) {
            // "reprintedAs": [ "Deep Gnome|MPMM" ]
            // If any reprinted source is included, skip this in favor of the reprint
            for (Iterator<JsonNode> i = jsonSource.withArray("reprintedAs").elements(); i.hasNext();) {
                String reprint = i.next().asText();
                String[] ra = reprint.split("\\|");
                if (sourceIncluded(ra[1])) {
                    IndexType type = IndexType.getTypeFromKey(finalKey);
                    String primarySource = jsonSource.get("source").asText().toLowerCase();
                    String reprintKey = type + "|" + reprint.toLowerCase();
                    if (type == IndexType.subrace && !variantIndex.containsKey(reprintKey)) {
                        reprintKey = IndexType.race + "|" + reprint.toLowerCase();
                        if (!variantIndex.containsKey(reprintKey)) {
                            reprintKey = finalKey.replace(primarySource, ra[1]).toLowerCase();
                        }
                    }
                    if (!variantIndex.containsKey(reprintKey)) {
                        reprintKey = aliases.get(reprintKey);
                        if (reprintKey == null) {
                            tui().errorf("Unable to find reprint of %s: %s", finalKey, reprint);
                            return false;
                        }
                    }
                    tui().debugf("Skipping %s; Reprinted as %s", finalKey, reprintKey);
                    // the reprint will be used instead (exclude this one)
                    // include an alias mapping the old key to the reprinted key
                    addAlias(finalKey, reprintKey);
                    return true;
                }
            }
        }
        // This true/false flag tends to comes from UA resources (when printed in official source
        if (booleanOrDefault(jsonSource, "isReprinted", false)) {
            tui().debugf("Skipping %s (has been reprinted)", finalKey);
            return true; // the reprint will be used instead of this one.
        }
        return false;
    }

    public boolean notPrepared() {
        return filteredIndex == null || variantIndex == null;
    }

    public Stream<JsonNode> classElementsMatching(IndexType type, String className) {
        String pattern = String.format("%s\\|[^|]+\\|%s\\|.*", type, className)
                .toLowerCase();
        return filteredIndex.entrySet().stream()
                .filter(e -> e.getKey().matches(pattern))
                .map(Entry::getValue);
    }

    public String getClassKey(String className, String classSource) {
        return String.format("%s|%s|%s",
                IndexType.classtype, className, classSource).toLowerCase();
    }

    public String getSubclassKey(String name, String className, String classSource) {
        return String.format("%s|%s|%s|%s|",
                IndexType.subclass, name, className, classSource).toLowerCase();
    }

    public CompendiumSources constructSources(IndexType type, JsonNode x) {
        CompendiumSources sources = new CompendiumSources(type, getKey(type, x), x);
        sources.checkKnown(tui, missingSourceName);
        return sources;
    }

    public String getKey(IndexType type, JsonNode x) {
        switch (type) {
            case subclass:
                return String.format("%s|%s|%s|%s|",
                        type,
                        getTextOrEmpty(x, "name"),
                        getTextOrEmpty(x, "className"),
                        getTextOrEmpty(x, "classSource"))
                        .toLowerCase();
            case subrace:
                return String.format("%s|%s|%s|%s",
                        type,
                        getTextOrEmpty(x, "name"),
                        getTextOrEmpty(x, "raceName"),
                        getTextOrEmpty(x, "raceSource"))
                        .toLowerCase();
            case classfeature: {
                String featureSource = getOrEmptyIfEqual(x, "source",
                        getTextOrDefault(x, "classSource", "PHB"));
                return String.format("%s|%s|%s|%s|%s%s",
                        type,
                        getTextOrEmpty(x, "name"),
                        getTextOrEmpty(x, "className"),
                        getOrEmptyIfEqual(x, "classSource", "PHB"),
                        getTextOrEmpty(x, "level"),
                        featureSource.isBlank() ? "" : "|" + featureSource)
                        .toLowerCase();
            }
            case subclassfeature: {
                String scSource = getOrEmptyIfEqual(x, "subclassSource", "PHB");
                String scFeatureSource = getOrEmptyIfEqual(x, "source", "PHB");
                return String.format("%s|%s|%s|%s|%s|%s|%s%s",
                        type,
                        getTextOrEmpty(x, "name"),
                        getTextOrEmpty(x, "className"),
                        getOrEmptyIfEqual(x, "classSource", "PHB"),
                        getTextOrEmpty(x, "subclassShortName"),
                        scSource,
                        getTextOrEmpty(x, "level"),
                        scFeatureSource.equals(scSource) ? "" : "|" + scFeatureSource)
                        .toLowerCase();
            }
            case itementry: {
                String itEntrySource = getOrEmptyIfEqual(x, "source", "DMG");
                return String.format("%s|%s%s",
                        type,
                        getTextOrEmpty(x, "name"),
                        itEntrySource.isBlank() ? "" : "|" + itEntrySource)
                        .toLowerCase();
            }
            case optionalfeature: {
                String opFeatureSource = getOrEmptyIfEqual(x, "source", "PHB");
                return String.format("%s|%s%s",
                        type,
                        getTextOrEmpty(x, "name"),
                        opFeatureSource.isBlank() ? "" : "|" + opFeatureSource)
                        .toLowerCase();
            }
            default:
                String name = x.get("name").asText();
                String source = x.get("source").asText();
                return createSimpleKey(type, name, source);
        }
    }

    public String createSimpleKey(IndexType type, String name, String source) {
        return String.format("%s|%s|%s", type, name, source).toLowerCase();
    }

    public String getRefKey(IndexType type, String crossRef) {
        return String.format("%s|%s", type, crossRef).toLowerCase()
                // NOTE: correct reference inconsistencies in the original data
                .replaceAll("\\|phb\\|", "||")
                .replaceAll("\\|tce\\|8\\|tce", "|tce|8");
    }

    public String getDataKey(String value) {
        return String.format("%s|%s", IndexType.reference, value)
                .toLowerCase();
    }

    public String getDataKey(String type, String id) {
        return String.format("%s|%s-%s",
                IndexType.reference, type, id)
                .toLowerCase();
    }

    public String getAlias(String key) {
        return aliases.get(key);
    }

    /**
     * For subclasses, class features, and subclass features,
     * cross references come directly from the class definition
     * (as a lookup for additional json sources).
     *
     * @param finalKey Pre-created cross reference string (including type)
     * @return referenced JsonNode or null
     */
    public JsonNode getNode(String finalKey) {
        if (finalKey == null) {
            return null;
        }
        return filteredIndex.get(finalKey);
    }

    public JsonNode getOrigin(String finalKey) {
        return nodeIndex.get(finalKey);
    }

    public JsonNode getOrigin(IndexType type, String name, String source) {
        String key = String.format("%s|%s|%s", type, name, source)
                .toLowerCase();
        return nodeIndex.get(key);
    }

    /**
     * Find the full JsonNode based on information from the node
     * passed in. Used for fluff nodes, and to find the original node
     * for a copy.
     *
     * @param type Type of object
     * @param x JsonNode providing lookup elements (name, source)
     * @return JsonNode or null
     */
    public JsonNode getNode(IndexType type, JsonNode x) {
        if (x == null) {
            return null;
        }
        return filteredIndex.get(getKey(type, x));
    }

    public JsonNode getOrigin(IndexType type, JsonNode x) {
        if (x == null) {
            return null;
        }
        return nodeIndex.get(getKey(type, x));
    }

    public Stream<JsonNode> subraces(CompendiumSources sources) {
        String raceName = sources.getName();
        String raceSource = String.join("|", sources.bookSources);
        String pattern = String.format("%s\\|[^|]+\\|%s\\|(%s)", IndexType.subrace, raceName, raceSource)
                .toLowerCase();
        return filteredIndex.entrySet().stream()
                .filter(e -> e.getKey().matches(pattern))
                .map(Entry::getValue);
    }

    public Stream<JsonNode> originSubraces(CompendiumSources sources) {
        String raceName = sources.getName();
        String raceSource = String.join("|", sources.bookSources);
        String pattern = String.format("%s\\|[^|]+\\|%s\\|(%s)", IndexType.subrace, raceName, raceSource)
                .toLowerCase();
        return nodeIndex.entrySet().stream()
                .filter(e -> e.getKey().matches(pattern))
                .map(Entry::getValue);
    }

    public String lookupName(IndexType type, String name) {
        String prefix = String.format("%s|%s|", type, name).toLowerCase();
        List<String> target = filteredIndex.keySet().stream()
                .filter(k -> k.startsWith(prefix))
                .collect(Collectors.toList());

        if (target.isEmpty()) {
            tui.debugf("Did not find element for %s", name);
            return name;
        } else if (target.size() > 1) {
            tui.debugf("Found several elements for %s: %s", name, target);
        }
        return filteredIndex.get(target.get(0)).get("name").asText();
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

    private boolean keyIsIncluded(String key, JsonNode node) {
        if (excludedKeys.contains(key) ||
                excludedPatterns.stream().anyMatch(x -> x.matcher(key).matches())) {
            return false;
        }
        if (allSources) {
            return true;
        }
        if (allowedSources.isEmpty()) {
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
        CompendiumSources sources = constructSources(IndexType.getTypeFromKey(key), node);
        for (String s : sources.bookSources) {
            if (allowedSources.contains(s.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    boolean isIncluded(String key) {
        String alias = getAlias(key);
        return filteredIndex.containsKey(key) || (alias != null && filteredIndex.containsKey(alias));
    }

    public boolean isExcluded(String key) {
        return !isIncluded(key);
    }

    public Set<Entry<String, JsonNode>> includedEntries() {
        if (notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing indexes");
        }
        return filteredIndex.entrySet();
    }

    public JsonNode resolveClassFeatureNode(String finalKey, String originClassKey) {
        JsonNode featureNode = getNode(finalKey);
        if (featureNode == null) {
            tui.debugf("%s: %s not found or excluded", originClassKey, finalKey);
            return null; // skip this
        }
        return resolveClassFeatureNode(finalKey, featureNode);
    }

    public JsonNode resolveClassFeatureNode(String finalKey, JsonNode featureNode) {
        if (isExcluded(finalKey)) {
            return null; // skip this
        }
        // TODO: Handle copies or other fill-in / fluff?
        return featureNode;
    }

    public String rulesRoot() {
        return rulesPath;
    }

    public String compendiumRoot() {
        return compendiumPath;
    }

    public void writeIndex(Path outputFile) throws IOException {
        if (notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing indexes");
        }
        Map<String, Object> allKeys = new HashMap<>();
        List<String> keys = new ArrayList<>(variantIndex.keySet());
        Collections.sort(keys);
        allKeys.put("keys", keys);
        allKeys.put("mapping", aliases);
        writeFile(outputFile, allKeys);
    }

    public void writeSourceIndex(Path outputFile) throws IOException {
        if (notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing files");
        }
        List<String> keys = new ArrayList<>(filteredIndex.keySet());
        Collections.sort(keys);
        writeFile(outputFile, Map.of("keys", keys));
    }

    private void writeFile(Path outputFile, Map<String, Object> keys) throws IOException {
        DefaultPrettyPrinter pp = new DefaultPrettyPrinter();
        pp.indentArraysWith(DefaultIndenter.SYSTEM_LINEFEED_INSTANCE);
        Json5eTui.MAPPER.writer()
                .with(pp)
                .writeValue(outputFile.toFile(), keys);
    }

    @Override
    public JsonIndex index() {
        return this;
    }

    @Override
    public CompendiumSources getSources() {
        return null;
    }

    public Map<String, JsonNode> getRules() {
        return rules;
    }

    static class Tuple {
        final String key;
        final JsonNode node;

        public Tuple(String key, JsonNode node) {
            this.key = key;
            this.node = node;
        }
    }
}
