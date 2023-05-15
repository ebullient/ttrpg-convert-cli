package dev.ebullient.convert.tools.dnd5e;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.tools.MarkdownConverter;
import dev.ebullient.convert.tools.ToolsIndex;

public class Tools5eIndex implements JsonSource, ToolsIndex {
    private static Tools5eIndex instance;

    public static Tools5eIndex getInstance() {
        return instance;
    }

    // classfeature|ability score improvement|monk|phb|12
    static final String classFeature_1 = "classfeature\\|[^|]+\\|[^|]+\\|";
    static final String classFeature_2 = "\\|\\d+\\|?";
    // subclassfeature|blessed strikes|cleric|phb|death|dmg|8|uaclassfeaturevariants
    static final String subclassFeature_1 = "subclassfeature\\|[^|]+\\|[^|]+\\|";
    static final String subclassFeature_2 = "\\|[^|]+\\|";
    static final String subclassFeature_3 = "\\|\\d+\\|?";

    final CompendiumConfig config;

    private final Map<String, JsonNode> rules = new HashMap<>();

    private final Map<String, JsonNode> nodeIndex = new HashMap<>();
    private final Map<String, List<JsonNode>> optFeatureIndex = new HashMap<>();
    private final Map<String, String> aliases = new HashMap<>();
    private final Map<String, String> classRoot = new HashMap<>();
    private Map<String, JsonNode> variantIndex = null;
    private Map<String, JsonNode> filteredIndex = null;
    private Map<String, Set<String>> spellClassIndex = new HashMap<>();

    private final Set<String> srdKeys = new HashSet<>();
    private final Set<String> familiarKeys = new HashSet<>();

    final JsonSourceCopier copier = new JsonSourceCopier(this);

    Pattern classFeaturePattern;
    Pattern subclassFeaturePattern;

    public Tools5eIndex(CompendiumConfig config) {
        this.config = config;
        instance = this;
    }

    public Tools5eIndex importTree(String filename, JsonNode node) {
        if (!node.isObject()) {
            return this;
        }

        // user configuration
        config.readConfigurationIfPresent(node);

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
        Tools5eIndexType.backgroundfluff.withArrayFrom(node, "backgroundFluff", this::addToIndex);
        Tools5eIndexType.itementry.withArrayFrom(node, "itemEntry", this::addToIndex);
        Tools5eIndexType.itemfluff.withArrayFrom(node, "itemFluff", this::addToIndex);
        Tools5eIndexType.itemproperty.withArrayFrom(node, "itemProperty", this::addToIndex);
        Tools5eIndexType.magicvariant.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.monsterfluff.withArrayFrom(node, "monsterFluff", this::addToIndex);
        Tools5eIndexType.racefluff.withArrayFrom(node, "raceFluff", this::addToIndex);
        Tools5eIndexType.spellfluff.withArrayFrom(node, "spellFluff", this::addToIndex);
        Tools5eIndexType.subrace.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.trait.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.legendarygroup.withArrayFrom(node, "legendaryGroup", this::addToIndex);
        Tools5eIndexType.subclass.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.classfeature.withArrayFrom(node, "classFeature", this::addToIndex);
        Tools5eIndexType.optionalfeature.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.subclassfeature.withArrayFrom(node, "subclassFeature", this::addToIndex);

        // Output Types
        Tools5eIndexType.background.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.classtype.withArrayFrom(node, "class", this::addToIndex);
        Tools5eIndexType.deity.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.feat.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.item.withArrayFrom(node, "baseitem", this::addToIndex);
        Tools5eIndexType.item.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.monster.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.race.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.spell.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.vehicle.withArrayFrom(node, this::addToIndex);

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

    void addToIndex(Tools5eIndexType type, JsonNode node) {
        String key = type.createKey(node);
        if (nodeIndex.containsKey(key)) {
            return;
        }
        nodeIndex.put(key, node);
        TtrpgValue.indexInputType.addToNode(node, type.name());
        TtrpgValue.indexKey.addToNode(node, key);

        if (type == Tools5eIndexType.subclass) {
            String lookupKey = Tools5eIndexType.getSubclassKey(
                    getTextOrEmpty(node, "className"), getTextOrEmpty(node, "classSource"),
                    getTextOrEmpty(node, "shortName"), getTextOrEmpty(node, "source"));
            // add subclass to alias. Referenced from spells
            addAlias(lookupKey, key);
        }
        if (type == Tools5eIndexType.subrace) {
            // {@race Aasimar (Fallen)|VGM}
            String[] parts = key.split("\\|");
            String lookupKey = String.format("race|%s (%s)|%s",
                    parts[2], parts[1], parts[3]).toLowerCase();
            addAlias(lookupKey, key);
        }
        if (type == Tools5eIndexType.classtype
                && !booleanOrDefault(node, "isReprinted", false)) {
            String[] parts = key.split("\\|");
            if (!parts[2].contains("ua")) {
                String lookupKey = String.format("%s|%s|", parts[0], parts[1]);
                classRoot.put(lookupKey, key);
            }
        }
        if (type == Tools5eIndexType.optionalfeature) {
            for (String ft : toListOfStrings(node.get("featureType"))) {
                optFeatureIndex.computeIfAbsent(ft, k -> new ArrayList<>()).add(node);
            }
        }

        if (node.has("srd")) {
            srdKeys.add(key);
        }
        if (node.has("familiar")) {
            familiarKeys.add(key);
        }
    }

    public String getSourceText(JsonNode node) {
        return getSourceText(Tools5eSources.findOrTemporary(Tools5eIndexType.reference, node));
    }

    public String getSourceText(Tools5eSources currentSource) {
        return String.format("_Source: %s_", currentSource.getSourceText(index().srdOnly()));
    }

    public boolean differentSource(Tools5eSources sources, JsonNode node) {
        if (sources == null && node == null) {
            return false;
        } else if (node == null || !node.has("source")) {
            return false;
        } else if (sources == null) {
            return !(index().srdOnly() || node.has("srd") || node.has("basicRules"));
        }
        Tools5eSources currentSource = Tools5eSources.findOrTemporary(Tools5eIndexType.reference, node);
        return !currentSource.primarySource().equals(currentSource.primarySource());
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

    List<String> getAliasesTo(String targetKey) {
        return aliases.entrySet().stream()
                .filter(e -> e.getValue().equals(targetKey))
                .map(Entry::getKey)
                .collect(Collectors.toList());
    }

    void addRulesIfPresent(JsonNode node, String rule) {
        if (node.has(rule)) {
            rules.put(rule, node.get(rule));
        }
    }

    void setClassFeaturePatterns() {
        String allowed = config.getAllowedSourcePattern();
        classFeaturePattern = Pattern.compile(classFeature_1 + allowed + classFeature_2 + allowed + "?");
        subclassFeaturePattern = Pattern
                .compile(subclassFeature_1 + allowed + subclassFeature_2 + allowed + subclassFeature_3 + allowed + "?");
    }

    public void prepare() {
        if (variantIndex != null || filteredIndex != null) {
            return;
        }
        variantIndex = new HashMap<>();

        setClassFeaturePatterns();

        for (Entry<String, JsonNode> entry : nodeIndex.entrySet()) {
            String key = entry.getKey();
            JsonNode node = entry.getValue();

            // check for / manage copies first.
            Tools5eIndexType type = Tools5eIndexType.getTypeFromKey(key);
            JsonNode jsonSource = copier.handleCopy(type, node);
            TtrpgValue.indexKey.addToNode(node, key);
            Tools5eSources.constructSources(node);

            if (type == Tools5eIndexType.subrace ||
                    type == Tools5eIndexType.trait ||
                    type == Tools5eIndexType.legendarygroup ||
                    type == Tools5eIndexType.deity) {
                // subraces are pulled in by races
                // traits and legendary groups are pulled in my monsters
                // deities are a hot mess
                continue;
            }

            // Find variants
            List<Tuple> variants = findVariants(key, jsonSource);
            if (variants.size() > 1) {
                tui().debugf("%s variants found for %s", variants.size(), key);
                variants.forEach(x -> tui().debugf("\t%s", x.key));
            }
            variants.forEach(v -> {
                JsonNode old = variantIndex.put(v.key, v.node);
                if (old != null) {
                    tui().errorf("Duplicate key: %s", v.key);
                }
                // store unique key / construct sources for variants
                TtrpgValue.indexKey.addToNode(v.node, v.key);
                Tools5eSources.constructSources(v.node);
            });
        }

        // Find/Merge deities (this will also exclude based on sources)
        List<Tuple> deities = findDeities(nodeIndex.entrySet().stream()
                .filter(e -> Tools5eIndexType.getTypeFromKey(e.getKey()) == Tools5eIndexType.deity)
                .map(e -> new Tuple(e.getKey(), e.getValue(),
                        String.format("%s-%s",
                                e.getValue().get("name").asText(),
                                e.getValue().get("pantheon").asText())))
                .collect(Collectors.toList()));
        deities.forEach(v -> {
            JsonNode old = variantIndex.put(v.key, v.node);
            if (old != null) {
                tui().errorf("Duplicate key: %s", v.key);
            }
        });

        // Exclude items after we've created variants and handled copies
        filteredIndex = variantIndex.entrySet().stream()
                .filter(e -> !isReprinted(e.getKey(), e.getValue()))
                .filter(e -> keyIsIncluded(e.getKey(), e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        // And finally, create an index of classes/subclasses/feats for spells
        // based on included sources & avaiable spells.
        buildSpellSourceIndex();
    }

    private List<Tuple> findDeities(List<Tuple> allDeities) {
        List<String> reverseOrder = List.of("dsotdq", "erlw", "mtf", "vgm", "scag", "dmg", "phb");
        List<Tuple> result = new ArrayList<>();
        Map<String, List<Tuple>> booklist = new HashMap<>();

        // We have to build the reprint index ourselves in print order.
        // If it isn't in one of the printed sources that matters, it goes right to the outbox
        // otherwise, we put it in buckets by source (with aliases)
        for (Tuple t : allDeities) {
            if (keyIsIncluded(t.key, t.node)) {
                String src = t.getSource().toLowerCase();
                if (reverseOrder.contains(src)) {
                    booklist.computeIfAbsent(src, k -> new ArrayList<>()).add(t);
                } else {
                    result.add(t);
                }
            }
        }

        // Now go through buckets in reverse order.
        Map<String, Tuple> reprintIndex = new HashMap<>();
        for (String book : reverseOrder) {
            List<Tuple> deities = booklist.remove(book);
            if (deities == null || deities.isEmpty()) {
                continue;
            }
            if (reprintIndex.isEmpty()) { // most recent bucket. Keep all.
                deities.forEach(t -> reprintIndex.put(t.getName(), t));
                continue;
            }

            for (Tuple t : deities) {
                String lookup = t.node.has("reprintAlias")
                        ? t.node.get("reprintAlias").asText()
                        : t.getName();

                if (reprintIndex.containsKey(lookup)) {
                    // skip it. It has been reprinted as a new thing
                } else {
                    reprintIndex.put(lookup, t);
                }
            }
        }
        result.addAll(reprintIndex.values());
        return result;
    }

    List<Tuple> findVariants(String key, JsonNode jsonSource) {
        Tools5eIndexType type = Tools5eIndexType.getTypeFromKey(key);
        if (type == Tools5eIndexType.race) {
            return Json2QuteRace.findRaceVariants(this, type, key, jsonSource, copier);
        } else if (type == Tools5eIndexType.monster && jsonSource.has("summonedBySpellLevel")) {
            return Json2QuteMonster.findConjuredMonsterVariants(this, type, key, jsonSource);
        } else if (key.contains("splugoth the returned") || key.contains("prophetess dran")) {
            // Fix.
            ObjectNode copy = (ObjectNode) copier.copyNode(jsonSource);
            copy.put("isNpc", true);
            return List.of(new Tuple(key, copy));
        }
        return List.of(new Tuple(key, jsonSource));
    }

    private void buildSpellSourceIndex() {
        // index key in resources/convertData.json
        JsonNode indexNode = TtrpgConfig.readIndex("spell-source");
        if (indexNode.isNull()) {
            return;
        }
        // structure is interesting:
        // "spell source" -> "spell" -> "class", "subclass", "feat"  (walk your way through key construction.. )
        for (Entry<String, JsonNode> sourceSpellMap : iterableFields(indexNode)) {
            String spellSource = sourceSpellMap.getKey();
            for (Entry<String, JsonNode> spellAssociation : iterableFields(sourceSpellMap.getValue())) {
                String spellName = spellAssociation.getKey();
                String spellKey = Tools5eIndexType.spell.createKey(spellName, spellSource);
                if (!filteredIndex.containsKey(spellKey)) {
                    // Spell is excluded
                    continue;
                }
                Set<String> spellClassList = spellClassIndex.computeIfAbsent(spellKey, k -> new HashSet<>());
                JsonNode spellMap = spellAssociation.getValue();
                for (Entry<String, JsonNode> sourceClassMap : iterableFields(spellMap.get("class"))) {
                    String classSource = sourceClassMap.getKey();
                    for (String className : iterableFieldNames(sourceClassMap.getValue())) {
                        String classKey = Tools5eIndexType.classtype.createKey(className, classSource);
                        if (isIncluded(classKey)) {
                            spellClassList.add(classKey);
                        }
                    }
                }
                for (Entry<String, JsonNode> sourceClassSubclassMap : iterableFields(spellMap.get("subclass"))) {
                    String classSource = sourceClassSubclassMap.getKey();
                    for (Entry<String, JsonNode> classMap : iterableFields(sourceClassSubclassMap.getValue())) {
                        String className = classMap.getKey();
                        for (Entry<String, JsonNode> sourceSubclassMap : iterableFields(classMap.getValue())) {
                            String subclassSource = sourceSubclassMap.getKey();
                            for (Entry<String, JsonNode> subclassMap : iterableFields(sourceSubclassMap.getValue())) {
                                String subclassName = subclassMap.getKey();
                                String subclassKey = index()
                                        .getAliasOrDefault(Tools5eIndexType.getSubclassKey(
                                                className, classSource, subclassName, subclassSource));
                                if (isIncluded(subclassKey)) {
                                    spellClassList.add(subclassKey);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    boolean isReprinted(String finalKey, JsonNode jsonSource) {
        if (jsonSource.has("reprintedAs")) {
            // "reprintedAs": [ "Deep Gnome|MPMM" ]
            // If any reprinted source is included, skip this in favor of the reprint
            for (Iterator<JsonNode> i = jsonSource.withArray("reprintedAs").elements(); i.hasNext();) {
                String reprint = i.next().asText();
                String[] ra = reprint.split("\\|");
                if (sourceIncluded(ra[1])) {
                    Tools5eIndexType type = Tools5eIndexType.getTypeFromKey(finalKey);
                    String primarySource = jsonSource.get("source").asText().toLowerCase();
                    String reprintKey = type + "|" + reprint.toLowerCase();
                    if (type == Tools5eIndexType.subrace && !variantIndex.containsKey(reprintKey)) {
                        reprintKey = Tools5eIndexType.race + "|" + reprint.toLowerCase();
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
        // This true/false flag tends to comes from UA resources (when printed in official source)
        if (booleanOrDefault(jsonSource, "isReprinted", false)) {
            tui().debugf("Skipping %s (has been reprinted)", finalKey);
            if (finalKey.startsWith("classtype")) {
                String[] parts = finalKey.split("\\|");
                String lookupKey = String.format("%s|%s|", parts[0], parts[1]);
                String reprintKey = classRoot.get(lookupKey);
                if (reprintKey == null) {
                    lookupKey = String.format("%s|%s|", parts[0], parts[1].replaceAll("\\s*\\(.*", ""));
                    reprintKey = classRoot.get(lookupKey);
                }
                if (reprintKey != null) {
                    addAlias(finalKey, reprintKey);
                }
            }
            return true; // the reprint will be used instead of this one.
        }
        return false;
    }

    public boolean notPrepared() {
        return filteredIndex == null || variantIndex == null;
    }

    public List<JsonNode> classElementsMatching(Tools5eIndexType type, String className, String classSource) {
        String pattern = String.format("%s\\|[^|]+\\|%s\\|%s\\|.*", type, className, classSource)
                .toLowerCase();
        return filteredIndex.entrySet().stream()
                .filter(e -> e.getKey().matches(pattern))
                .map(Entry::getValue)
                .collect(Collectors.toList());
    }

    public String getDataKey(String value) {
        return Tools5eIndexType.reference.createKey(value, null);
    }

    public String getDataKey(String type, String id) {
        return Tools5eIndexType.reference.createKey(type, id);
    }

    public String getAlias(String key) {
        return aliases.get(key);
    }

    public String getAliasOrDefault(String key) {
        String previous;
        String value = key;
        do {
            previous = value;
            value = aliases.getOrDefault(previous, previous);
        } while (!value.equals(previous));
        return value;
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

    public JsonNode getOrigin(Tools5eIndexType type, String name, String source) {
        String key = String.format("%s|%s|%s", type, name, source)
                .toLowerCase();
        return nodeIndex.get(key);
    }

    public JsonNode getOrigin(Tools5eIndexType type, JsonNode x) {
        if (x == null) {
            return null;
        }
        return nodeIndex.get(type.createKey(x));
    }

    public Stream<JsonNode> originSubraces(Tools5eSources sources) {
        String raceName = sources.getName();
        String raceSource = String.join("|", sources.getBookSources());
        String pattern = String.format("%s\\|[^|]+\\|%s\\|(%s)", Tools5eIndexType.subrace, raceName, raceSource)
                .toLowerCase();
        return nodeIndex.entrySet().stream()
                .filter(e -> e.getKey().matches(pattern))
                .map(Entry::getValue);
    }

    public String lookupName(Tools5eIndexType type, String name) {
        String prefix = String.format("%s|%s|", type, name).toLowerCase();
        List<String> target = filteredIndex.keySet().stream()
                .filter(k -> k.startsWith(prefix))
                .collect(Collectors.toList());

        if (target.isEmpty()) {
            tui().debugf("Did not find element for %s", name);
            return name;
        } else if (target.size() > 1) {
            tui().debugf("Found several elements for %s: %s", name, target);
        }
        return filteredIndex.get(target.get(0)).get("name").asText();
    }

    public boolean srdOnly() {
        return config.noSources();
    }

    public boolean sourceIncluded(String source) {
        return config.sourceIncluded(source);
    }

    public boolean excludeItem(JsonNode itemSource, boolean isSRD) {
        return config.excludeItem(itemSource, isSRD);
    }

    public boolean rulesSourceExcluded(JsonNode node, String name) {
        boolean isSRD = node.has("srd");
        JsonNode itemSource = node.get("source");
        if (excludeItem(itemSource, isSRD)) {
            // skip this item: not from a specified source
            tui().debugf("Skipped %s from %s (%s)", name, itemSource, isSRD);
            return true;
        }
        return false;
    }

    private boolean keyIsIncluded(String key, JsonNode node) {

        // Check against include/exclude rules (srdKeys allowed when there are no sources)
        Optional<Boolean> rulesAllow = config.keyIsIncluded(key, node);
        if (rulesAllow.isPresent()) {
            return rulesAllow.get();
        }
        if (config.noSources()) {
            return srdKeys.contains(key);
        }

        // Special case for class features (match against constructed patterns)
        if (key.contains("classfeature|")) {
            String featureKey = key.replace("||", "|phb|");
            return classFeaturePattern.matcher(featureKey).matches() || subclassFeaturePattern.matcher(featureKey).matches();
        }
        // Familiars
        if (key.startsWith("monster|")
                && config.groupIsIncluded("familiars")
                && familiarKeys.contains(key)) {
            return true;
        }

        Tools5eSources sources = Tools5eSources.findSources(key);
        return sources.getBookSources().stream().anyMatch((s) -> config.sourceIncluded(s));
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

    public JsonNode resolveClassFeatureNode(String finalKey) {
        JsonNode featureNode = getOrigin(finalKey);
        if (featureNode == null) {
            tui().debugf("%s not found", finalKey);
            return null; // skip this
        }
        return resolveClassFeatureNode(finalKey, featureNode);
    }

    public JsonNode resolveClassFeatureNode(String finalKey, JsonNode featureNode) {
        if (isExcluded(finalKey)) {
            tui().debugf("excluded: %s", finalKey);
            return null; // skip this
        }
        // TODO: Handle copies or other fill-in / fluff?
        return featureNode;
    }

    public Collection<? extends JsonNode> findOptionalFeatures(String ft) {
        return optFeatureIndex.get(ft);
    }

    public Collection<String> classesForSpell(String spellKey) {
        return spellClassIndex.get(spellKey);
    }

    @Override
    public void writeFullIndex(Path outputFile) throws IOException {
        if (notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing indexes");
        }
        Map<String, Object> allKeys = new HashMap<>();
        List<String> keys = new ArrayList<>(variantIndex.keySet());
        Collections.sort(keys);
        allKeys.put("keys", keys);
        allKeys.put("mapping", aliases);
        tui().writeJsonFile(outputFile, allKeys);
    }

    @Override
    public void writeFilteredIndex(Path outputFile) throws IOException {
        if (notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing files");
        }
        List<String> keys = new ArrayList<>(filteredIndex.keySet());
        Collections.sort(keys);
        tui().writeJsonFile(outputFile, Map.of("keys", keys));
    }

    @Override
    public MarkdownConverter markdownConverter(MarkdownWriter writer, Map<String, String> imageFallbackPaths) {
        return new Json2MarkdownConverter(this, writer, imageFallbackPaths);
    }

    @Override
    public CompendiumConfig cfg() {
        return this.config;
    }

    @Override
    public Tools5eIndex index() {
        return this;
    }

    @Override
    public Tools5eSources getSources() {
        return null;
    }

    public Map<String, JsonNode> getRules() {
        return rules;
    }

    static class Tuple {
        final String key;
        final JsonNode node;

        public Tuple(String key, JsonNode node) {
            this(key, node, null);
        }

        public Tuple(String key, JsonNode node, String name) {
            this.key = key;
            this.node = node;
            this.name = name;
        }

        String name;

        public String getName() {
            if (name == null) {
                name = node.get("name").asText();
            }
            return name;
        }

        String source;

        public String getSource() {
            if (source == null) {
                source = node.get("source").asText();
            }
            return source;
        }
    }
}
