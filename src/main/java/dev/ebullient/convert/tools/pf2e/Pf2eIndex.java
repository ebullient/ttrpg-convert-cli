package dev.ebullient.convert.tools.pf2e;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.MarkdownConverter;
import dev.ebullient.convert.tools.ToolsIndex;

public class Pf2eIndex implements ToolsIndex, Pf2eTypeReader {
    static final String CORE_RULES_KEY = "book|book-crb";
    final CompendiumConfig config;

    private static final Map<String, JsonNode> imported = new HashMap<>();

    private final Map<String, String> alias = new HashMap<>();
    private final Map<String, JsonNode> filteredIndex = new TreeMap<>();

    private final Map<String, String> traitToSource = new HashMap<>();
    private final Map<String, Collection<String>> categoryToTraits = new TreeMap<>();
    private final Map<String, Set<String>> archetypeToFeats = new TreeMap<>();
    private final Map<String, Set<String>> domainToSpells = new TreeMap<>();

    final JsonSourceCopier copier = new JsonSourceCopier(this);

    public Pf2eIndex(CompendiumConfig config) {
        this.config = config;
    }

    @Override
    public boolean notPrepared() {
        return filteredIndex.isEmpty();
    }

    @Override
    public Pf2eIndex importTree(String filename, JsonNode node) {
        if (!node.isObject()) {
            return this;
        }

        // user configuration
        config.readConfigurationIfPresent(node);

        // data ingest. Minimal processing.
        Pf2eIndexType.ability.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.action.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.archetype.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.background.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.creature.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.curse.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.condition.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.deity.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.disease.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.domain.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.feat.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.hazard.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.baseitem.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.item.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.ritual.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.skill.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.spell.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.table.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.trait.withArrayFrom(node, this::addToIndex);

        Pf2eIndexType.adventure.withArrayFrom(node, this::addToIndex);
        Pf2eIndexType.book.withArrayFrom(node, this::addToIndex);

        addDataToIndex(Pf2eIndexType.data.getFrom(node), filename);

        return this;
    }

    void addToIndex(Pf2eIndexType type, JsonNode node) {
        if (type == Pf2eIndexType.baseitem) {
            // always use item (baseitem is a detail that we have remembered if we need it)
            type = Pf2eIndexType.item;
        }
        TtrpgValue.indexInputType.setIn(node, type.name());
        // TODO: Variants? Reprints?
        String key = type.createKey(node);
        String hash = Field.add_hash.getTextOrNull(node);
        if (type == Pf2eIndexType.trait) {
            key = prepareTrait(key, node);
        } else if (hash != null) {
            String name = SourceField.name.getTextOrEmpty(node);
            name += " (" + hash + ")";
            key = replaceName(type, name, key, node, false);
        }

        if (type == Pf2eIndexType.book || type == Pf2eIndexType.adventure) {
            String id = SourceField.id.getTextOrEmpty(node);
            String source = SourceField.source.getTextOrEmpty(node);
            if (!id.equals(source)) {
                TtrpgConfig.sourceToIdMapping(source, id);
            }
        }

        // Add the node + key to the index, and store the key in the node
        JsonNode previous = imported.get(key);
        if (previous != null) {
            // We include the CRB by default, otherwise, say something about skipping duplicates
            if (!"book|book-crb".equals(key) &&
                    (!SourceField.name.valueEquals(previous, node) || !SourceField.source.valueEquals(previous, node)
                            || !SourceField.page.valueEquals(previous, node))) {
                tui().debugf("Skipping %s, already indexed", key);
            }
            return;
        }
        imported.put(key, node);
        TtrpgValue.indexKey.setIn(node, key);
    }

    String prepareTrait(String key, JsonNode node) {
        String name = SourceField.name.getTextOrEmpty(node);
        Pf2eAlignmentValue alignment = Pf2eAlignmentValue.fromString(name);

        // Change the indexed name for [...] traits
        if (name.startsWith("[") || alignment != null) {
            // Update name & object node
            name = alignment == null
                    ? name.replaceAll("\\[(.*)]", "Any $1")
                    : alignment.longName;
            key = replaceName(Pf2eIndexType.trait, name, key, node, true);
        }

        // Quick lookup for traits
        String source = SourceField.source.getTextOrDefault(node, Pf2eIndexType.trait.defaultSourceString());
        String oldSource = traitToSource.put(name.toLowerCase(), source);
        if (oldSource != null && !oldSource.equals(source)) {
            tui().warnf("Duplicate trait name %s, from source %s and %s",
                    name, source, oldSource);
        }
        return key;
    }

    private String replaceName(Pf2eIndexType type, String newName, String oldKey, JsonNode node, boolean makeAlias) {
        ((ObjectNode) node).put("name", newName);

        // Create new key, add alias from old key
        String key = type.createKey(node);
        if (makeAlias) {
            alias.put(oldKey, key);
        }
        return key;
    }

    void addDataToIndex(JsonNode data, String filename) {
        if (data == null || filename.isEmpty()) {
            return;
        }
        int slash = filename.indexOf('/');
        int dot = filename.indexOf('.');
        String name = filename.substring(slash < 0 ? 0 : slash + 1, dot < 0 ? filename.length() : dot);
        String key = Pf2eIndexType.data.createKey(name, null); // e.g. data|book-crb
        if (imported.containsKey(key)) {
            return;
        }

        // synthetic node
        ObjectNode newNode = Tui.MAPPER.createObjectNode();
        newNode.put("name", name);
        newNode.put("filename", filename);
        newNode.set("data", data);

        int dash = name.lastIndexOf("-");
        if (dash >= 0) {
            newNode.put("source", name.substring(dash + 1));
        }
        TtrpgValue.indexKey.setIn(newNode, key); // backlink
        imported.put(key, newNode);
    }

    @Override
    public void prepare() {
        if (!this.filteredIndex.isEmpty()) {
            return;
        }

        imported.forEach((key, node) -> {
            Pf2eIndexType type = Pf2eIndexType.getTypeFromKey(key);

            if (type.checkCopiesAndReprints()) {
                // check for / manage copies first (creatures, fluff)
                node = copier.handleCopy(type, node);
            }
            Pf2eSources sources = Pf2eSources.constructSources(type, node); // pre-construct sources

            if (type == Pf2eIndexType.feat && keyIsIncluded(key, node)) {
                createArchetypeReference(key, node, sources);
            } else if (type == Pf2eIndexType.spell && keyIsIncluded(key, node)) {
                createDomainReference(key, node);
            } else if (type == Pf2eIndexType.trait) {
                createTraitReference(key, node, sources);
            }
        });

        imported.entrySet().stream()
                .filter(e -> keyIsIncluded(e.getKey(), e.getValue()))
                .forEach(e -> filteredIndex.put(e.getKey(), e.getValue()));
    }

    private void createTraitReference(String key, JsonNode node, Pf2eSources sources) {
        // Precreate category mapping for traits
        String name = SourceField.name.getTextOrEmpty(node);
        String traitLink = linkifyTrait(node, name);

        Field.categories.getListOfStrings(node, tui()).stream()
                .filter(c -> !c.equalsIgnoreCase("_alignAbv"))
                .forEach(c -> categoryToTraits.computeIfAbsent(c, k -> new TreeSet<>())
                        .add(traitLink));
    }

    void createArchetypeReference(String key, JsonNode node, Pf2eSources sources) {
        JsonNode featType = Json2QuteFeat.Pf2eFeat.featType.getFrom(node);
        if (featType != null) {
            List<String> archetype = Json2QuteFeat.Pf2eFeat.archetype.getListOfStrings(featType, tui());
            archetype.forEach(a -> {
                String aKey = Pf2eIndexType.archetype.createKey(a, sources.primarySource());
                archetypeToFeats.computeIfAbsent(aKey, k -> new HashSet<>())
                        .add(key);
            });
        }
    }

    void createDomainReference(String key, JsonNode node) {
        Json2QuteSpell.Pf2eSpell.domains.getListOfStrings(node, tui())
                .forEach(d -> domainToSpells.computeIfAbsent(d.toLowerCase(), k -> new HashSet<>())
                        .add(key));
    }

    boolean keyIsIncluded(String key, JsonNode node) {
        Pf2eIndexType type = Pf2eIndexType.getTypeFromKey(key);
        if (type.alwaysInclude()) {
            return true;
        }
        // Check against include/exclude rules (config: included/excluded/all)
        Optional<Boolean> rulesAllow = config.keyIsIncluded(key);
        if (rulesAllow.isPresent()) {
            return rulesAllow.get();
        }
        if (CORE_RULES_KEY.equals(key)) { // include core rules unless turned off
            return true;
        }
        Pf2eSources sources = Pf2eSources.findSources(key);
        if (config.noSources()) {
            return sources.fromDefaultSource();
        }
        return sources != null && sources.getSources().stream().anyMatch((s) -> config.sourceIncluded(s));
    }

    public boolean isIncluded(String key) {
        if (filteredIndex.isEmpty()) {
            return keyIsIncluded(key, null);
        }
        return filteredIndex.containsKey(aliasOrDefault(key));
    }

    // --------- Node retrieval --------

    /** Used for source/page lookup during rendering */
    public static JsonNode findNode(Pf2eSources sources) {
        return imported.get(sources.getKey());
    }

    public String aliasOrDefault(String key) {
        return alias.getOrDefault(key, key);
    }

    public JsonNode getIncludedNode(String key) {
        return filteredIndex.get(aliasOrDefault(key));
    }

    public Set<String> featKeys(String archetypeKey) {
        Set<String> feats = archetypeToFeats.get(archetypeKey);
        return feats == null ? Set.of() : feats;
    }

    public Set<String> domainSpells(String domain) {
        Set<String> spells = domainToSpells.get(domain.toLowerCase());
        return spells == null ? Set.of() : spells;
    }

    public String traitToSource(String trait) {
        return traitToSource.get(trait.toLowerCase());
    }

    @Override
    public JsonNode getAdventure(String a) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public JsonNode getBook(String b) {
        // TODO Auto-generated method stub
        return null;
    }

    // --------- Write indexes ---------

    @Override
    public MarkdownConverter markdownConverter(MarkdownWriter writer) {
        return new Pf2eMarkdown(this, writer);
    }

    @Override
    public void writeFullIndex(Path outputFile) throws IOException {
        if (notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing indexes");
        }
        Map<String, Object> allKeys = new HashMap<>();
        List<String> keys = new ArrayList<>(imported.keySet());
        Collections.sort(keys);
        allKeys.put("keys", keys);
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

    public Set<Map.Entry<String, JsonNode>> filteredEntries() {
        if (notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing indexes");
        }
        return filteredIndex.entrySet();
    }

    public Map<String, Collection<String>> categoryTraitMap() {
        return categoryToTraits;
    }

    // ---- JsonSource overrides ------

    @Override
    public CompendiumConfig cfg() {
        return config;
    }

    @Override
    public Pf2eIndex index() {
        return this;
    }

    @Override
    public Pf2eSources getSources() {
        return null;
    }

}
