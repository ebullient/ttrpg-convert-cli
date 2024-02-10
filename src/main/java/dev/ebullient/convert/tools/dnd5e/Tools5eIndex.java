package dev.ebullient.convert.tools.dnd5e;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.SourceAndPage;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.MarkdownConverter;
import dev.ebullient.convert.tools.ToolsIndex;
import dev.ebullient.convert.tools.dnd5e.ItemProperty.CustomItemProperty;
import dev.ebullient.convert.tools.dnd5e.ItemProperty.PropertyEnum;
import dev.ebullient.convert.tools.dnd5e.ItemType.CustomItemType;
import dev.ebullient.convert.tools.dnd5e.ItemType.ItemEnum;
import dev.ebullient.convert.tools.dnd5e.Json2QuteClass.ClassFields;
import dev.ebullient.convert.tools.dnd5e.Json2QuteRace.RaceFields;
import dev.ebullient.convert.tools.dnd5e.PsionicType.CustomPsionicType;
import dev.ebullient.convert.tools.dnd5e.SkillOrAbility.CustomSkillOrAbility;
import dev.ebullient.convert.tools.dnd5e.SpellSchool.CustomSpellSchool;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

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

    private final Map<String, JsonNode> nodeIndex = new HashMap<>();
    private Map<String, JsonNode> variantIndex = null;
    private Map<String, JsonNode> filteredIndex = null;

    private final Map<String, OptionalFeatureType> optFeatureIndex = new HashMap<>();
    private final Map<String, HomebrewMetaTypes> homebrewMetaTypes = new HashMap<>();
    private final Map<String, Set<JsonNode>> subraceIndex = new HashMap<>();
    private final Map<SourceAndPage, List<JsonNode>> tableIndex = new HashMap<>();
    private final Map<String, String> aliases = new HashMap<>();
    private final Map<String, String> classRoot = new HashMap<>();
    private final Map<String, Set<String>> spellClassIndex = new HashMap<>();
    private final Map<String, String> nameToLink = new HashMap<>();

    private final Set<String> srdKeys = new HashSet<>();
    private final Set<String> familiarKeys = new HashSet<>();

    final JsonSourceCopier copier = new JsonSourceCopier(this);

    Pattern classFeaturePattern;
    Pattern subclassFeaturePattern;

    // index state
    HomebrewMetaTypes homebrew = null;

    public Tools5eIndex(CompendiumConfig config) {
        this.config = config;
        instance = this;
    }

    private void indexTypes(String filename, JsonNode node) {

        // Reference/Internal Types

        Tools5eIndexType.backgroundFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.conditionFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.featFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.itemFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.monsterFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.objectFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.raceFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.spellFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.vehicleFluff.withArrayFrom(node, this::addToIndex);

        Tools5eIndexType.language.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.citation.withArrayFrom(node, this::addToIndex);

        Tools5eIndexType.itemEntry.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.itemGroup.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.itemTypeAdditionalEntries.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.card.withArrayFrom(node, this::addToIndex);

        Tools5eIndexType.magicvariant.withArrayFrom(node, this::addMagicVariantToIndex);
        Tools5eIndexType.subrace.withArrayFrom(node, this::addToSubraceIndex);

        Tools5eIndexType.monsterTemplate.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.subclass.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.classfeature.withArrayFrom(node, "classFeature", this::addToIndex);
        Tools5eIndexType.subclassFeature.withArrayFrom(node, "subclassFeature", this::addToIndex);

        // Output Types

        Tools5eIndexType.action.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.condition.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.disease.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.itemMastery.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.itemProperty.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.itemType.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.sense.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.skill.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.status.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.variantrule.withArrayFrom(node, this::addToIndex);

        Tools5eIndexType.psionic.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.legendaryGroup.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.optionalfeature.withArrayFrom(node, this::addToIndex);

        // tables

        Tools5eIndexType.table.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.tableGroup.withArrayFrom(node, this::addToIndex);

        // templated types

        Tools5eIndexType.background.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.classtype.withArrayFrom(node, "class", this::addToIndex);
        Tools5eIndexType.deck.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.deity.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.feat.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.hazard.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.item.withArrayFrom(node, "baseitem", this::addToIndex);
        Tools5eIndexType.item.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.monster.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.object.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.race.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.reward.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.spell.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.trap.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.vehicle.withArrayFrom(node, this::addToIndex);

        Tools5eIndexType.adventure.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.book.withArrayFrom(node, this::addToIndex);

        // 5e tools book/adventure data
        if (node.has("data") && !filename.isEmpty()) {
            int slash = filename.indexOf('/');
            int dot = filename.indexOf('.');
            String basename = filename.substring(slash < 0 ? 0 : slash + 1, dot < 0 ? filename.length() : dot);
            String id = basename.replace("book-", "").replace("adventure-", "");
            TtrpgConfig.includeAdditionalSource(id);
            ((ObjectNode) node).put("id", id);
            addToIndex(basename.startsWith("book") ? Tools5eIndexType.bookData : Tools5eIndexType.adventureData,
                    node);
        }
    }

    public Tools5eIndex importTree(String filename, JsonNode node) {
        if (!node.isObject() || addHomebrewSourcesIfPresent(filename, node)) {
            return this;
        }
        // user configuration
        config.readConfigurationIfPresent(node);

        // Index content types
        indexTypes(filename, node);

        // base items are special: add an additional flag
        Tools5eIndexType.item.withArrayFrom(node, "baseitem", (type, x) -> {
            TtrpgValue.indexBaseItem.setIn(x, BooleanNode.TRUE);
        });

        return this;
    }

    private void importHomebrewTree(HomebrewMetaTypes homebrew) {
        this.homebrew = homebrew;
        try {
            // Index content types
            indexTypes(homebrew.filename, homebrew.homebrewNode);
            Tools5eIndexType.adventureData.withArrayFrom(homebrew.homebrewNode, this::addToIndex); // homebrew
            Tools5eIndexType.bookData.withArrayFrom(homebrew.homebrewNode, this::addToIndex); // homebrew
        } finally {
            this.homebrew = null;
        }
    }

    private boolean addHomebrewSourcesIfPresent(String filename, JsonNode node) {
        JsonNode sources = SourceField._meta.getFieldFrom(node, HomebrewFields.sources);
        if (sources == null || sources.size() == 0) {
            return false;
        }
        String json = HomebrewFields.json.getTextOrNull(sources.get(0));
        if (json == null) {
            tui().errorf("Source does not define json id: %s", sources.get(0));
            return false;
        }
        TtrpgConfig.includeAdditionalSource(json);

        HomebrewMetaTypes metaTypes = new HomebrewMetaTypes(json, filename, node);
        for (JsonNode source : iterableElements(sources)) {
            String fullName = HomebrewFields.full.getTextOrEmpty(source);
            String abbreviation = HomebrewFields.abbreviation.getTextOrEmpty(source);
            json = HomebrewFields.json.getTextOrEmpty(source);
            if (fullName == null) {
                tui().warnf("Homebrew source %s missing full name: %s", json, fullName);
            }
            // add homebrew to known sources
            if (TtrpgConfig.addHomebrewSource(fullName, json, abbreviation)) {
                // one homebrew file may include multiple sources, the same mapping applies to
                // all
                HomebrewMetaTypes old = homebrewMetaTypes.put(json, metaTypes);
                if (old != null) {
                    tui().errorf("Shared homebrew id: %s and %s", old.filename, metaTypes.filename);
                }
            } else {
                tui().errorf("🍺 Skipping homebrew id %s from %s; duplicate source id", json, metaTypes.filename);
            }
        }

        JsonNode featureTypes = SourceField._meta.getFieldFrom(node, HomebrewFields.optionalFeatureTypes);
        JsonNode spellSchools = SourceField._meta.getFieldFrom(node, HomebrewFields.spellSchools);
        JsonNode psionicTypes = SourceField._meta.getFieldFrom(node, HomebrewFields.psionicTypes);
        JsonNode skillTypes = HomebrewFields.skill.getFrom(node);
        if (featureTypes != null || spellSchools != null || psionicTypes != null || skillTypes != null) {
            for (Entry<String, JsonNode> entry : iterableFields(featureTypes)) {
                metaTypes.setOptionalFeatureType(entry.getKey(), entry.getValue().asText());
            }
            // ignoring short names for spell schools and psionic types
            for (Entry<String, JsonNode> entry : iterableFields(spellSchools)) {
                metaTypes.setSpellSchool(entry.getKey(),
                        new CustomSpellSchool(HomebrewFields.full.getTextOrEmpty(entry.getValue())));
            }
            for (Entry<String, JsonNode> entry : iterableFields(psionicTypes)) {
                metaTypes.setPsionicType(entry.getKey(),
                        tui().readJsonValue(entry.getValue(), CustomPsionicType.class));
            }
            for (JsonNode skill : iterableElements(skillTypes)) {
                String skillName = SourceField.name.getTextOrEmpty(skill);
                if (skillName == null) {
                    tui().warnf("Homebrew skill type missing name: %s", skill);
                    continue;
                }
                metaTypes.setSkillType(skillName, skill);
            }
        }
        Tools5eSources.addFonts(SourceField._meta.getFrom(node), HomebrewFields.fonts);
        return true;
    }

    void addToSubraceIndex(Tools5eIndexType type, JsonNode node) {
        String raceName = RaceFields.raceName.getTextOrThrow(node);
        String raceSource = RaceFields.raceSource.getTextOrThrow(node);
        String raceKey = Tools5eIndexType.race.createKey(raceName, raceSource);
        subraceIndex.computeIfAbsent(raceKey, k -> new HashSet<>()).add(node);
    }

    void addMagicVariantToIndex(Tools5eIndexType type, JsonNode node) {
        MagicVariant.INSTANCE.populateGenericVariant(node);
        addToIndex(type, node);
    }

    void addToIndex(Tools5eIndexType type, JsonNode node) {
        String key = type.createKey(node);
        if (nodeIndex.containsKey(key)) {
            return;
        }
        nodeIndex.put(key, node);
        TtrpgValue.indexInputType.setIn(node, type.name());
        TtrpgValue.indexKey.setIn(node, key);
        TtrpgValue.isHomebrew.setIn(node, homebrew != null);

        if (type == Tools5eIndexType.classtype
                && !booleanOrDefault(node, "isReprinted", false)) {
            String[] parts = key.split("\\|");
            if (!parts[2].contains("ua")) {
                String lookupKey = String.format("%s|%s|", parts[0], parts[1]);
                classRoot.put(lookupKey, key);
            }
        } else if (type == Tools5eIndexType.itemProperty && homebrew != null) {
            // lookup by abbreviation
            String[] parts = key.split("\\|");
            homebrew.itemProperties.put(parts[1].toLowerCase(), new CustomItemProperty(node));
        } else if (type == Tools5eIndexType.itemType && homebrew != null) {
            // lookup by abbreviation
            String[] parts = key.split("\\|");
            if (SourceField.name.existsIn(node)) {
                homebrew.itemTypes.put(parts[1].toLowerCase(), new CustomItemType(node));
            } else {
                tui().errorf("Item type %s does not specify name: %s", key, node);
            }
        } else if (type == Tools5eIndexType.optionalfeature) {
            String lookup = null;
            for (String ft : toListOfStrings(node.get("featureType"))) {
                try {
                    boolean homebrewType = homebrew != null && homebrew.getOptionalFeatureType(ft) != null;
                    // scope the optional feature key (homebrew may conflict)
                    String featKey = (homebrewType
                            ? ft + "-" + homebrew.jsonKey
                            : ft).toLowerCase();

                    optFeatureIndex.computeIfAbsent(featKey, k -> new OptionalFeatureType(ft, k, homebrew, index())).add(node);
                    lookup = lookup == null ? featKey : lookup;
                } catch (IllegalArgumentException e) {
                    tui().error(e, "Unable to define optional feature");
                }
            }
            if (lookup != null) {
                ((ObjectNode) node).put("typeLookup", lookup);
            }
        } else if (type == Tools5eIndexType.subclass) {
            String lookupKey = Tools5eIndexType.getSubclassKey(
                    getTextOrEmpty(node, "className"), getTextOrEmpty(node, "classSource"),
                    getTextOrEmpty(node, "shortName"), getTextOrEmpty(node, "source"));
            // add subclass to alias. Referenced from spells
            addAlias(lookupKey, key);
        } else if (type == Tools5eIndexType.table || type == Tools5eIndexType.tableGroup) {
            SourceAndPage sp = new SourceAndPage(node);
            tableIndex.computeIfAbsent(sp, k -> new ArrayList<>()).add(node);
        } else if (type == Tools5eIndexType.language && HomebrewFields.fonts.existsIn(node)) {
            Tools5eSources.addFonts(node, HomebrewFields.fonts);
        } else if (type == Tools5eIndexType.book || type == Tools5eIndexType.adventure) {
            String id = SourceField.id.getTextOrEmpty(node);
            String source = SourceField.source.getTextOrEmpty(node);
            if (!id.equals(source)) {
                TtrpgConfig.sourceToIdMapping(source, id);
            }
        }

        if (node.has("srd")) {
            JsonNode srd = node.get("srd");
            if (srd.isTextual()) {
                String srdKey = key.replace(SourceField.name.getTextOrThrow(node).toLowerCase(),
                        Tools5eFields.srd.getTextOrThrow(node).toLowerCase());
                addAlias(srdKey, key);
                srdKeys.add(srdKey);
            } else {
                srdKeys.add(key);
            }
        }
        if (node.has("familiar")) {
            familiarKeys.add(key);
        }
    }

    void addAlias(String key, String alias) {
        if (key.equals(alias)) {
            return;
        }
        String old = aliases.putIfAbsent(key, alias);
        if (old != null && !old.equals(alias)) {
            tui().errorf("Oops! Duplicate simple key: %s -> %s", key, alias);
        }
    }

    List<String> getAliasesTo(String targetKey) {
        return aliases.entrySet().stream()
                .filter(e -> e.getValue().equals(targetKey))
                .map(Entry::getKey)
                .collect(Collectors.toList());
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
        // Add missing/frequently-used aliases
        TtrpgConfig.addDefaultAliases(aliases);

        // Find subrace variants (add to index)
        findRaceVariants();

        // Properly import homebrew sources
        for (HomebrewMetaTypes homebrew : homebrewMetaTypes.values()) {
            importHomebrewTree(homebrew);
        }

        variantIndex = new HashMap<>();

        setClassFeaturePatterns();

        for (Entry<String, JsonNode> entry : nodeIndex.entrySet()) {
            String key = entry.getKey();
            JsonNode jsonSource = entry.getValue();

            // check for / manage copies first.
            Tools5eIndexType type = Tools5eIndexType.getTypeFromKey(key);
            jsonSource = copier.handleCopy(type, jsonSource);
            entry.setValue(jsonSource); // update with resolved copy

            if (type == Tools5eIndexType.adventureData || type == Tools5eIndexType.bookData) {
                copySources(type, jsonSource);
            }

            TtrpgValue.indexKey.setIn(jsonSource, key);
            Tools5eSources sources = Tools5eSources.constructSources(jsonSource);

            if (type == Tools5eIndexType.monsterTemplate ||
                    type == Tools5eIndexType.deity) {
                // traits (templates) groups are pulled in my monsters
                // deities are a hot mess
                continue;
            }

            // Find variants
            List<Tuple> variants = findVariants(key, jsonSource);
            if (variants.size() > 1) {
                tui().debugf("%s variants found for %s", variants.size(), key);
            }
            variants.forEach(v -> {
                if (variants.size() > 1) {
                    tui().debugf("\t%s", v.key);
                }
                // store unique key / construct sources for variants
                TtrpgValue.indexKey.setIn(v.node, v.key);
                Tools5eSources.constructSources(v.node);

                JsonNode old = variantIndex.put(v.key, v.node);
                if (old != null && !old.equals(v.node)) {
                    tui().errorf("Duplicate key: %s%nold: %s%nnew: %s", v.key, old, v.node);
                }
            });

            if (type == Tools5eIndexType.classtype || type == Tools5eIndexType.subclass) {
                for (JsonNode ofp : iterableElements(ClassFields.optionalfeatureProgression.getFrom(jsonSource))) {
                    for (String featureType : Tools5eFields.featureType.getListOfStrings(ofp, tui())) {
                        OptionalFeatureType oft = getOptionalFeatureType(featureType, sources.primarySource());
                        if (oft != null) {
                            oft.appendSources(sources);
                        }
                    }
                }
            }
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
        filteredIndex = new TreeMap<>();
        variantIndex.entrySet().stream()
                .filter(e -> !isReprinted(e.getKey(), e.getValue()))
                .filter(e -> keyIsIncluded(e.getKey(), e.getValue()))
                .forEach(e -> filteredIndex.put(e.getKey(), e.getValue()));

        // And finally, create an index of classes/subclasses/feats for spells
        // based on included sources & avaiable spells.
        buildSpellSourceIndex();
    }

    public void findRaceVariants() {
        for (Entry<String, Set<JsonNode>> entry : subraceIndex.entrySet()) {
            String raceKey = entry.getKey();
            JsonNode jsonSource = nodeIndex.get(raceKey);

            Set<JsonNode> inputSubraces = entry.getValue();
            List<JsonNode> subraces = new ArrayList<>();

            Json2QuteRace.prepareBaseRace(this, jsonSource, inputSubraces);

            if (inputSubraces.size() > 1) {
                tui().debugf("%s subraces found for %s", inputSubraces.size(), raceKey);
            }

            for (JsonNode sr : inputSubraces) {
                sr = copier.mergeSubrace(sr, jsonSource);
                String srKey = Tools5eIndexType.subrace.createKey(sr);
                TtrpgValue.indexInputType.setIn(sr, Tools5eIndexType.subrace.name());
                TtrpgValue.indexKey.setIn(sr, srKey);

                nodeIndex.put(srKey, sr);
                subraces.add(sr);

                // Add expected alias:  {@race Aasimar (Fallen)|VGM}
                String[] parts = srKey.split("\\|");
                String source = SourceField.source.getTextOrThrow(sr);
                final String lookupKey;
                if (parts[1].contains("(")) { // "subrace|dwarf (duergar)|dwarf|phb|mtf"
                    lookupKey = String.format("race|%s|%s", parts[1], source).toLowerCase();
                } else { // "subrace|half-elf|half-elf|phb"
                    if (parts[1].equals(parts[2])) {
                        lookupKey = String.format("race|%s|%s", parts[1], source).toLowerCase();
                    } else {
                        lookupKey = String.format("race|%s (%s)|%s", parts[2], parts[1], source).toLowerCase();
                    }
                }
                addAlias(lookupKey, srKey);
                tui().debugf("\t%s :: %s", lookupKey, srKey);
            }

            Json2QuteRace.updateBaseRace(this, jsonSource, inputSubraces, subraces);
        }
        subraceIndex.clear();
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
                    // skip it. It has already been reprinted as a new thing
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
        if (type == Tools5eIndexType.itemGroup) {
            return Json2QuteItem.findGroupVariant(instance, type, key, jsonSource, copier);
        } else if (type == Tools5eIndexType.magicvariant) {
            return MagicVariant.INSTANCE.findSpecificVariants(this, type, key, jsonSource, copier);
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
        // "spell source" -> "spell" -> ("class"|"subclass"|"feat")  (walk your way through key construction.. )
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
                        String classKey = index()
                                .getAliasOrDefault(Tools5eIndexType.classtype.createKey(className, classSource));
                        if (isIncluded(classKey)) {
                            spellClassList.add(classKey);
                        }
                    }
                }
                for (Entry<String, JsonNode> sourceClassSubclassMap : iterableFields(spellMap.get("subclass"))) {
                    String classSource = sourceClassSubclassMap.getKey(); // PHB, XGE, etc
                    if (!sourceIncluded(classSource)) {
                        // skip it
                        continue;
                    }
                    for (Entry<String, JsonNode> classMap : iterableFields(sourceClassSubclassMap.getValue())) {
                        String className = classMap.getKey(); // Bard, Cleric, etc
                        for (Entry<String, JsonNode> sourceSubclassMap : iterableFields(classMap.getValue())) {
                            String subclassSource = sourceSubclassMap.getKey(); // PHB, XGE, etc
                            if (!sourceIncluded(subclassSource)) {
                                // skip it
                                continue;
                            }
                            for (Entry<String, JsonNode> subclassMap : iterableFields(sourceSubclassMap.getValue())) {
                                String subclassName = subclassMap.getKey(); // College of Lore, etc
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
                    tui().debugf("📰 Skipping %s; Reprinted as %s", finalKey, reprintKey);
                    // the reprint will be used instead (exclude this one)
                    // include an alias mapping the old key to the reprinted key
                    addAlias(finalKey, reprintKey);
                    return true;
                }
            }
        }
        // This true/false flag tends to comes from UA resources (when printed in official source)
        if (booleanOrDefault(jsonSource, "isReprinted", false)) {
            tui().debugf("🗞️ Skipping %s (has been reprinted)", finalKey);
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
        return nodesMatching(pattern);
    }

    public List<JsonNode> elementsMatching(Tools5eIndexType type, String middle) {
        String pattern = String.format("%s\\|%s\\|.*", type, middle)
                .toLowerCase();
        return nodesMatching(pattern);
    }

    private List<JsonNode> nodesMatching(String pattern) {
        return filteredIndex.entrySet().stream()
                .filter(e -> e.getKey().matches(pattern))
                .map(Entry::getValue)
                .collect(Collectors.toList());
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

    public boolean isHomebrew() {
        return homebrew != null;
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

    public ItemProperty findItemProperty(String abbreviation, Tools5eSources sources) {
        if (abbreviation == null || abbreviation.isEmpty()) {
            return null;
        }
        HomebrewMetaTypes meta = homebrewMetaTypes.get(sources.primarySource());
        ItemProperty prop = PropertyEnum.fromEncodedType(abbreviation);
        if (prop == null && meta != null) {
            prop = meta.getItemProperty(abbreviation);
        }
        if (prop == null) {
            tui().errorf("Unknown property %s for %s", abbreviation, sources);
            return new CustomItemProperty(abbreviation);
        }
        return prop;
    }

    public ItemType findItemType(String abbreviation, Tools5eSources sources) {
        if (abbreviation == null || abbreviation.isEmpty()) {
            return null;
        }
        HomebrewMetaTypes meta = homebrewMetaTypes.get(sources.primarySource());
        ItemType itemType = ItemEnum.fromEncodedValue(abbreviation);
        if (itemType == null && meta != null) {
            itemType = meta.getItemType(abbreviation);
        }
        if (itemType == null) {
            tui().errorf("Unknown item type %s", abbreviation);
            return new CustomItemType(abbreviation);
        }
        return itemType;
    }

    public HomebrewMetaTypes getHomebrewMetaTypes(Tools5eSources sources) {
        return homebrewMetaTypes.get(sources.primarySource());
    }

    public SkillOrAbility findSkillOrAbility(String key, Tools5eSources sources) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        HomebrewMetaTypes meta = homebrewMetaTypes.get(sources.primarySource());
        SkillOrAbility skill = SkillOrAbility.fromTextValue(key);
        if (skill == null && meta != null) {
            skill = meta.getSkillType(key);
        }
        if (skill == null) {
            tui().errorf("Unknown skill or ability %s in %s", key, sources);
            return new CustomSkillOrAbility(key);
        }
        return skill;
    }

    public SpellSchool findSpellSchool(String abbreviation, Tools5eSources sources) {
        if (abbreviation == null || abbreviation.isEmpty()) {
            return null;
        }
        HomebrewMetaTypes meta = homebrewMetaTypes.get(sources.primarySource());
        SpellSchool school = SpellSchool.fromEncodedValue(abbreviation);
        if (school == null && meta != null) {
            school = meta.getSpellSchool(abbreviation);
        }
        if (school == null) {
            tui().errorf("Unknown spell school %s in %s", abbreviation, sources);
            return new CustomSpellSchool(abbreviation);
        }
        return school;
    }

    public JsonNode findTable(SourceAndPage sourceAndPage, String rowData) {
        List<JsonNode> tables = tableIndex.get(sourceAndPage);
        if (tables != null) {
            if (tables.size() == 1) {
                Optional<JsonNode> table = matchTable(rowData, tables.get(0));
                return table.orElse(null);
            }
            for (JsonNode table : tables) {
                Optional<JsonNode> match = matchTable(rowData, table);
                if (match.isPresent()) {
                    return match.get();
                }
            }
        }
        return null;
    }

    private Optional<JsonNode> matchTable(String rowData, JsonNode table) {
        String matchData = TableFields.getFirstRow(table);
        if (rowData.equals(matchData)) {
            return Optional.of(table);
        }
        return Optional.empty();
    }

    public List<JsonNode> originNodesMatching(Function<JsonNode, Boolean> filter) {
        return nodeIndex.entrySet().stream()
                .filter(e -> filter.apply(e.getValue()))
                .map(Entry::getValue)
                .collect(Collectors.toList());
    }

    public JsonNode getOriginNoFallback(String finalKey) {
        JsonNode result = nodeIndex.get(finalKey);
        if (result == null) {
            result = variantIndex.get(finalKey);
        }
        return result;
    }

    public JsonNode getOrigin(String finalKey) {
        JsonNode result = nodeIndex.get(finalKey);
        if (result == null) {
            result = variantIndex.get(finalKey);
        }
        if (result == null) {
            List<String> target = variantIndex.keySet().stream()
                    .filter(k -> k.startsWith(finalKey))
                    .collect(Collectors.toList());
            if (target.size() == 1) {
                String lookup = target.get(0);
                addAlias(finalKey, lookup);
                result = nodeIndex.get(lookup);
            } else if (target.size() > 1) {
                List<String> reduce = target.stream()
                        .filter(x -> !x.matches(".*\\|ua[^|]*$"))
                        .filter(x -> !x.contains("|dmg"))
                        .distinct()
                        .collect(Collectors.toList());
                if (reduce.size() > 1) {
                    tui().debugf("Found several elements for %s: %s", finalKey, reduce);
                    return null;
                } else if (reduce.size() == 1) {
                    String lookup = reduce.get(0);
                    result = nodeIndex.get(lookup);
                    addAlias(finalKey, lookup);
                }
            }
        }
        return result;
    }

    public JsonNode getOrigin(Tools5eIndexType type, String name, String source) {
        String key = type.createKey(name, source);
        return getOrigin(key);
    }

    public JsonNode getOrigin(Tools5eIndexType type, JsonNode x) {
        if (x == null) {
            return null;
        }
        String key = type.createKey(x);
        return getOrigin(key);
    }

    public String linkifyByName(Tools5eIndexType type, String name) {
        String prefix = String.format("%s|%s|", type, name).toLowerCase();

        return nameToLink.computeIfAbsent(prefix, p -> {
            List<String> target = variantIndex.keySet().stream()
                    .filter(k -> k.startsWith(prefix))
                    .collect(Collectors.toList());
            if (target.isEmpty()) {
                target = aliases.keySet().stream()
                        .filter(k -> k.startsWith(prefix))
                        .collect(Collectors.toList());
            }

            if (target.isEmpty()) {
                tui().debugf("🫥 Did not find element for \"%s\" using [%s]", name, prefix);
                return name;
            } else if (target.size() > 1) {
                List<String> reduce = target.stream()
                        .filter(x -> !x.matches(".*\\|ua[^|]*$"))
                        .map(x -> getAliasOrDefault(x))
                        .distinct()
                        .collect(Collectors.toList());
                if (reduce.size() > 1) {
                    tui().debugf("Found several elements for %s using [%s]: %s", name, prefix, target);
                    return name;
                } else if (reduce.size() == 1) {
                    target = reduce;
                }
            }

            String key = getAliasOrDefault(target.get(0));
            JsonNode node = filteredIndex.get(key); // only included items
            return node == null ? name : type.linkify(this, node);
        });
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

    private boolean keyIsIncluded(String key, JsonNode node) {

        // Check against include/exclude rules (config: included/excluded/all)
        Optional<Boolean> rulesAllow = config.keyIsIncluded(key);
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
        return cfg().sourceIncluded(sources);
    }

    boolean isIncluded(String key) {
        String alias = getAlias(key);
        return filteredIndex.containsKey(key) || (alias != null && filteredIndex.containsKey(alias));
    }

    public boolean isExcluded(String key) {
        return !isIncluded(key);
    }

    public boolean differentSource(Tools5eSources sources, String source) {
        String primarySource = sources == null ? null : sources.primarySource();
        if (primarySource == null || source == null) {
            return false;
        }
        return !primarySource.equals(source);
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
        // TODO: Handle copies or other fill-in / fluff?
        return featureNode;
    }

    public Collection<String> classesForSpell(String spellKey) {
        return spellClassIndex.get(spellKey);
    }

    public OptionalFeatureType getOptionalFeatureType(JsonNode node) {
        String lookup = Tools5eFields.typeLookup.getTextOrDefault(node, SourceField.name.getTextOrEmpty(node));
        return optFeatureIndex.get(lookup);
    }

    public OptionalFeatureType getOptionalFeatureType(String ft, String source) {
        if (ft == null) {
            return null;
        }
        HomebrewMetaTypes metaTypes = homebrewMetaTypes.get(source);
        boolean homebrewType = metaTypes != null && metaTypes.getOptionalFeatureType(ft) != null;

        OptionalFeatureType oft = optFeatureIndex.get(ft.toLowerCase());
        if (homebrewType) {
            String homebrewScoped = ft + "-" + metaTypes.jsonKey;
            OptionalFeatureType homebrewOft = optFeatureIndex.get(homebrewScoped.toLowerCase());
            return homebrewOft == null
                    ? oft
                    : homebrewOft;
        }
        return oft;
    }

    @Override
    public void writeFullIndex(Path outputFile) throws IOException {
        if (notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing indexes");
        }
        Map<String, Object> allKeys = new TreeMap<>();
        allKeys.put("keys", new TreeSet<>(variantIndex.keySet()));
        allKeys.put("mapping", new TreeMap<>(aliases));
        allKeys.put("srdKeys", new TreeSet<>(srdKeys));
        tui().writeJsonFile(outputFile, allKeys);
    }

    @Override
    public void writeFilteredIndex(Path outputFile) throws IOException {
        if (notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing files");
        }
        tui().writeJsonFile(outputFile, Map.of("keys", filteredIndex.keySet()));
    }

    @Override
    public JsonNode getAdventure(String id) {
        String finalKey = Tools5eIndexType.adventure.createKey("adventure", id);
        return getOrigin(finalKey);
    }

    @Override
    public JsonNode getBook(String id) {
        String finalKey = Tools5eIndexType.book.createKey("book", id);
        return getOrigin(finalKey);
    }

    void copySources(Tools5eIndexType type, JsonNode dataNode) {
        String id = dataNode.get("id").asText();
        JsonNode fromNode = type == Tools5eIndexType.adventureData
                ? getAdventure(id)
                : getBook(id);

        // Adventures and Books have metadata in a different entry.
        SourceField.name.copy(fromNode, dataNode);
        SourceField.source.copy(fromNode, dataNode);
        SourceField.page.copy(fromNode, dataNode);
        Tools5eFields.otherSources.copy(fromNode, dataNode);
        Tools5eFields.additionalSources.copy(fromNode, dataNode);
    }

    @Override
    public MarkdownConverter markdownConverter(MarkdownWriter writer) {
        return new Tools5eMarkdownConverter(this, writer);
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

    static class OptionalFeatureType {
        final String lookupKey;
        final String abbreviation;
        final HomebrewMetaTypes homebrewMeta;
        final String title;
        final String source;
        final ObjectNode featureTypeNode;
        final List<JsonNode> nodes = new ArrayList<>();

        OptionalFeatureType(String abbreviation, String scopedAbv, HomebrewMetaTypes homebrewMeta, Tools5eIndex index) {
            this.abbreviation = abbreviation;
            this.lookupKey = scopedAbv;
            this.homebrewMeta = homebrewMeta;
            String tmpTitle = null;
            if (homebrewMeta != null) {
                tmpTitle = homebrewMeta.getOptionalFeatureType(abbreviation);
            }
            if (tmpTitle == null) {
                tmpTitle = switch (abbreviation) {
                    case "AI" -> "Artificer Infusion";
                    case "ED" -> "Elemental Discipline";
                    case "EI" -> "Eldritch Invocation";
                    case "MM" -> "Metamagic";
                    case "MV" -> "Maneuver";
                    case "MV:B" -> "Maneuver, Battle Master";
                    case "MV:C2-UA" -> "Maneuver, Cavalier V2 (UA)";
                    case "AS:V1-UA" -> "Arcane Shot, V1 (UA)";
                    case "AS:V2-UA" -> "Arcane Shot, V2 (UA)";
                    case "AS" -> "Arcane Shot";
                    case "OTH" -> "Other";
                    case "FS:F" -> "Fighting Style, Fighter";
                    case "FS:B" -> "Fighting Style, Bard";
                    case "FS:P" -> "Fighting Style, Paladin";
                    case "FS:R" -> "Fighting Style, Ranger";
                    case "PB" -> "Pact Boon";
                    case "OR" -> "Onomancy Resonant";
                    case "RN" -> "Rune Knight Rune";
                    case "AF" -> "Alchemical Formula";
                    default -> null;
                };
            }
            if (tmpTitle == null) {
                index.tui().warnf("Could not find title for OptionalFeatureType: %s from %s",
                        abbreviation, homebrewMeta == null ? "unknown/core" : homebrewMeta.filename);
                tmpTitle = abbreviation;
            }
            title = tmpTitle;
            source = getSource(homebrewMeta);

            featureTypeNode = Tui.MAPPER.createObjectNode();
            featureTypeNode.put("name", scopedAbv);
            featureTypeNode.put("source", source);
            if (inSRD(abbreviation)) {
                featureTypeNode.put("srd", true);
            }
            index.addToIndex(Tools5eIndexType.optionalFeatureTypes, featureTypeNode);
        }

        public void appendSources(Tools5eSources otherSources) {
            // Update sources from those of a consuming/using class or subclass
            Tools5eSources mySources = Tools5eSources.constructSources(featureTypeNode);
            if (otherSources.contains(mySources)) {
                mySources.amendSources(otherSources);
            }
        }

        public void add(JsonNode node) {
            nodes.add(node);
        }

        public String getFilename() {
            return "list-" + Tools5eQuteBase.fixFileName(title, source, Tools5eIndexType.optionalFeatureTypes);
        }

        private String getSource(HomebrewMetaTypes homebrewMeta) {
            if (homebrewMeta != null) {
                return homebrewMeta.jsonKey;
            }
            return switch (abbreviation) {
                case "AF" -> "UAA";
                case "AI", "RN" -> "TCE";
                case "AS", "FS:B" -> "XGE";
                case "AS:V1-UA" -> "UAF";
                case "AS:V2-UA" -> "UARSC";
                case "MV:C2-UA" -> "UARCO";
                case "OR" -> "UACDW";
                default -> "PHB";
            };
        }

        private boolean inSRD(String abbreviation) {
            return switch (abbreviation) {
                case "EI", "FS:F", "FS:R", "FS:P", "MM", "PB" -> true;
                default -> false;
            };
        }

        String getKey() {
            return TtrpgValue.indexKey.getTextOrNull(featureTypeNode);
        }
    }

    static class HomebrewMetaTypes {
        final String jsonKey;
        final String filename;
        final JsonNode homebrewNode;
        // name, long name
        final Map<String, String> optionalFeatureTypes = new HashMap<>();
        final Map<String, PsionicType> psionicTypes = new HashMap<>();
        final Map<String, SkillOrAbility> skillOrAbility = new HashMap<>();
        final Map<String, CustomSpellSchool> spellSchoolTypes = new HashMap<>();
        final Map<String, CustomItemType> itemTypes = new HashMap<>();
        final Map<String, CustomItemProperty> itemProperties = new HashMap<>();

        HomebrewMetaTypes(String jsonKey, String filename, JsonNode homebrewNode) {
            this.jsonKey = jsonKey;
            this.filename = filename;
            this.homebrewNode = homebrewNode;
        }

        public String getOptionalFeatureType(String key) {
            return optionalFeatureTypes.get(key.toLowerCase());
        }

        public void setOptionalFeatureType(String key, String value) {
            optionalFeatureTypes.put(key.toLowerCase(), value);
        }

        public PsionicType getPsionicType(String key) {
            return psionicTypes.get(key.toLowerCase());
        }

        public void setPsionicType(String key, PsionicType value) {
            psionicTypes.put(key.toLowerCase(), value);
        }

        public SkillOrAbility getSkillType(String key) {
            return skillOrAbility.get(key.toLowerCase());
        }

        public void setSkillType(String key, JsonNode skill) {
            skillOrAbility.put(key.toLowerCase(), new CustomSkillOrAbility(skill));
        }

        public SpellSchool getSpellSchool(String key) {
            return spellSchoolTypes.get(key.toLowerCase());
        }

        public void setSpellSchool(String key, CustomSpellSchool value) {
            spellSchoolTypes.put(key.toLowerCase(), value);
        }

        public ItemType getItemType(String key) {
            return itemTypes.get(key.toLowerCase());
        }

        public void setItemType(String key, CustomItemType value) {
            itemTypes.put(key.toLowerCase(), value);
        }

        public ItemProperty getItemProperty(String key) {
            return itemProperties.get(key.toLowerCase());
        }

        public void setItemProperty(String key, CustomItemProperty value) {
            itemProperties.put(key.toLowerCase(), value);
        }
    }

    enum HomebrewFields implements JsonNodeReader {
        abbreviation,
        fonts,
        full,
        json,
        optionalFeatureTypes,
        psionicTypes,
        skill,
        sources,
        spellSchools,
        spellDistanceUnits
    }
}
