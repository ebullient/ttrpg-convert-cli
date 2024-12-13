package dev.ebullient.convert.tools.dnd5e;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.ReprintBehavior;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.MarkdownWriter;
import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.SourceAndPage;
import dev.ebullient.convert.tools.MarkdownConverter;
import dev.ebullient.convert.tools.ToolsIndex;
import dev.ebullient.convert.tools.dnd5e.Json2QuteItem.ItemField;
import dev.ebullient.convert.tools.dnd5e.Json2QuteRace.RaceFields;
import dev.ebullient.convert.tools.dnd5e.OptionalFeatureIndex.OptionalFeatureType;
import dev.ebullient.convert.tools.dnd5e.SkillOrAbility.CustomSkillOrAbility;
import dev.ebullient.convert.tools.dnd5e.SpellSchool.CustomSpellSchool;
import dev.ebullient.convert.tools.dnd5e.Tools5eHomebrewIndex.HomebrewFields;
import dev.ebullient.convert.tools.dnd5e.Tools5eHomebrewIndex.HomebrewMetaTypes;

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

    // Initialization
    private final Map<String, JsonNode> nodeIndex = new HashMap<>();
    private final Map<String, Set<JsonNode>> subraceIndex = new HashMap<>();
    private final Map<SourceAndPage, List<JsonNode>> tableIndex = new HashMap<>();

    private Map<String, JsonNode> filteredIndex = null;

    private final Map<String, String> aliases = new HashMap<>();
    private final Map<String, String> reprints = new HashMap<>();
    private final Map<String, String> subraceMap = new HashMap<>();
    private final Map<String, String> nameToLink = new HashMap<>();

    private final Map<String, Set<String>> spellClassIndex = new HashMap<>();

    private final Set<String> srdKeys = new HashSet<>();

    final Tools5eJsonSourceCopier copier = new Tools5eJsonSourceCopier(this);
    final OptionalFeatureIndex optFeatureIndex = new OptionalFeatureIndex(this);
    final Tools5eHomebrewIndex homebrewIndex = new Tools5eHomebrewIndex(this);

    // index state
    volatile HomebrewMetaTypes homebrew = null;

    public Tools5eIndex(CompendiumConfig config) {
        this.config = config;
        instance = this;
    }

    public Tools5eIndex importTree(String filename, JsonNode node) {
        if (!node.isObject() || homebrewIndex.addHomebrewSourcesIfPresent(filename, node)) {
            // defer reading contents of homebrew until after we've indexed the rest
            // see prepare()  / importHomebrewTree()
            return this;
        }

        // user configuration
        config.readConfigurationIfPresent(node);

        // Index content types
        indexTypes(filename, node);

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

    private void indexTypes(String filename, JsonNode node) {

        // Reference/Internal Types

        Tools5eIndexType.backgroundFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.classFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.conditionFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.facilityFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.featFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.itemFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.monsterFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.objectFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.optionalfeatureFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.raceFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.spellFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.trapFluff.withArrayFrom(node, this::addToIndex);
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

        // Class-scoped resources (if the class is left out, the resource is not included)

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
        Tools5eIndexType.optfeature.withArrayFrom(node, "optionalfeature", this::addToIndex);

        // tables

        Tools5eIndexType.table.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.tableGroup.withArrayFrom(node, this::addToIndex);

        // templated types

        Tools5eIndexType.background.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.classtype.withArrayFrom(node, "class", this::addToIndex);
        Tools5eIndexType.deck.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.deity.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.facility.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.feat.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.hazard.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.item.withArrayFrom(node, "baseitem", this::addBaseItemToIndex);
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

    void addToSubraceIndex(Tools5eIndexType type, JsonNode node) {
        String raceName = RaceFields.raceName.getTextOrThrow(node);
        String raceSource = RaceFields.raceSource.getTextOrThrow(node);
        String raceKey = Tools5eIndexType.race.createKey(raceName, raceSource);
        subraceIndex.computeIfAbsent(raceKey, k -> new HashSet<>()).add(node);
    }

    void addMagicVariantToIndex(Tools5eIndexType type, JsonNode node) {
        MagicVariant.populateGenericVariant(node);
        addToIndex(type, node);
    }

    void addBaseItemToIndex(Tools5eIndexType type, JsonNode node) {
        TtrpgValue.indexBaseItem.setIn(node, BooleanNode.TRUE);
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

        // Homebrew files are ingested in a lump:
        // if homebrew is set, then we're reading a homebrew file
        TtrpgValue.isHomebrew.setIn(node, homebrew != null);
        if (homebrew != null) {
            homebrew.addElement(type, key, node);
        }

        switch (type) {
            case optfeature -> {
                // add while we're ingesting (homebrew or not)
                optFeatureIndex.addOptionalFeature(key, node, homebrew);
            }
            case subclass -> {
                // add alias with subclass shortname
                String lookupKey = Tools5eIndexType.getSubclassKey(
                        Tools5eFields.className.getTextOrEmpty(node),
                        Tools5eFields.classSource.getTextOrEmpty(node),
                        Tools5eFields.shortName.getTextOrEmpty(node),
                        SourceField.source.getTextOrEmpty(node));
                addAlias(lookupKey, key);
            }
            case table, tableGroup -> {
                SourceAndPage sp = new SourceAndPage(node);
                tableIndex.computeIfAbsent(sp, k -> new ArrayList<>()).add(node);
            }
            case language -> {
                if (HomebrewFields.fonts.existsIn(node)) {
                    Tools5eSources.addFonts(node, HomebrewFields.fonts);
                }
            }
            case adventure, book -> {
                String id = SourceField.id.getTextOrEmpty(node);
                String source = SourceField.source.getTextOrEmpty(node);
                if (!id.equals(source) && type == Tools5eIndexType.book) {
                    // adventures can be subdivided from books. Don't map source/id for those
                    TtrpgConfig.sourceToIdMapping(source, id);
                }
            }
            case itemGroup -> {
                addAlias(key.replace("itemgroup|", "item|"), key);
            }
            default -> {
            }
        }

        addSrdEntry(key, node);
    }

    public void prepare() {
        if (filteredIndex != null) {
            return;
        }

        tui().progressf("Adding default aliases");

        // Add missing/frequently-used aliases
        TtrpgConfig.addDefaultAliases(aliases);

        tui().progressf("Importing homebrew sources");
        // Properly import homebrew sources
        homebrewIndex.importBrew(this::importHomebrewTree);

        tui().debugf("Preparing index using configuration:\n%s", Tui.jsonStringify(config));

        tui().progressf("Adding subraces (2014)");
        // Add subraces to index
        defineSubraces();

        tui().progressf("Resolving copies and link sources");

        // Find remaining/included base items
        List<JsonNode> baseItems = nodeIndex.values().stream()
                .filter(n -> TtrpgValue.indexBaseItem.booleanOrDefault(n, false))
                .filter(n -> !ItemField.packContents.existsIn(n))
                .toList();

        Map<String, JsonNode> variants = new HashMap<>();

        // For each node: handle copies, link sources
        for (Entry<String, JsonNode> entry : nodeIndex.entrySet()) {
            String key = entry.getKey();
            JsonNode jsonSource = entry.getValue();

            // check for / manage copies first.
            Tools5eIndexType type = Tools5eIndexType.getTypeFromKey(key);
            jsonSource = copier.handleCopy(type, jsonSource);

            // Pre-creation of sources..
            if (type == Tools5eIndexType.adventureData || type == Tools5eIndexType.bookData) {
                // changes name and things used when constructing sources
                linkSources(type, jsonSource);
            }

            Tools5eSources.constructSources(key, jsonSource);
            entry.setValue(jsonSource); // update with resolved copy

            // Post-creation of sources..
            switch (type) {
                case classtype, subclass -> optFeatureIndex.amendSources(key, jsonSource, homebrewIndex);
                case optfeature -> optFeatureIndex.amendSources(key, jsonSource, homebrewIndex);
                case classfeature -> {
                    String classKey = Tools5eIndexType.classtype.fromChildKey(key);
                    JsonNode classNode = nodeIndex.get(classKey);
                    if (classNode != null) {
                        JsonNode featureKeys = Tools5eFields.classFeatureKeys.ensureArrayIn(classNode).add(key);
                        Tools5eFields.classFeatureKeys.setIn(jsonSource, featureKeys);
                    }
                }
                case subclassFeature -> {
                    // don't follow reprints, just go from shortname to subclass name
                    String scKey = Tools5eIndexType.subclass.fromChildKey(key);
                    scKey = getAliasOrDefault(scKey, false);
                    JsonNode scNode = nodeIndex.get(scKey);
                    if (scNode != null) {
                        JsonNode featureKeys = Tools5eFields.classFeatureKeys.ensureArrayIn(scNode).add(key);
                        Tools5eFields.classFeatureKeys.setIn(jsonSource, featureKeys);
                    }
                }
                default -> {
                }
            }

            // Reprints do follow specialized variants, so we need to find the variants
            // now (and will filter them out based on rules later...)
            if (type.hasVariants()) {
                List<Tuple> variantList = findVariants(key, jsonSource, baseItems);
                for (Tuple variant : variantList) {
                    variants.put(variant.key, variant.node);
                }
            }
        } // end for each entry

        nodeIndex.putAll(variants);
        variants.clear();

        filteredIndex = new HashMap<>(nodeIndex.size());

        tui().progressf("Applying source filters");

        BiConsumer<Msg, String> logThis = (msgType, msg) -> {
            if (msgType == Msg.TARGET) {
                tui().debugf(msgType, msg);
            } else {
                tui().logf(msgType, msg);
            }
        };

        // Apply include/exclude rules & source filters
        for (var e : nodeIndex.entrySet()) {
            String key = e.getKey();
            Tools5eIndexType type = Tools5eIndexType.getTypeFromKey(key);
            // construct source if missing (which it may be for a variant)
            Tools5eSources sources = Tools5eSources.constructSources(key, e.getValue());
            Msg msgType = sources.filterRuleApplied() ? Msg.TARGET : Msg.FILTER;

            if (type.isFluffType()) {
                // no-op
            } else if (type.isDependentType() && msgType != Msg.TARGET) {
                // keep dependent types unless there is a specific rule
                filteredIndex.put(key, e.getValue());
            } else if (sources.includedByConfig()) {
                filteredIndex.put(key, e.getValue());
                logThis.accept(msgType, "(KEEP) " + key);
            } else if (type.isOutputType()) {
                logThis.accept(msgType, "(drop) " + key);
            }
        }

        // Remove reprints based on included sources (and reprint behavior)
        // If someone includes MM, but not MPMM, you want the MM version
        tui().progressf("Resolving reprints");
        filteredIndex.entrySet().removeIf(e -> isReprinted(e.getKey(), e.getValue()));

        // Follow inclusion of certain types to remove additional related elements
        tui().progressf("Removing dependent and dangling resources");
        filteredIndex.keySet().removeIf(k -> otherwiseExcluded(k));

        // Deities have their own glorious reprint mess, which we only need to deal with
        // when we aren't hoarding all the things.
        if (config.reprintBehavior() != ReprintBehavior.all) {
            tui().progressf("Dealing with deities");

            List<Tuple> allDeities = filteredIndex.entrySet().stream()
                    .filter(e -> Tools5eIndexType.getTypeFromKey(e.getKey()) == Tools5eIndexType.deity)
                    .map(e -> new Tuple(e.getKey(), e.getValue()))
                    .toList();

            // Remove deities that should be removed (superceded)
            Json2QuteDeity.findDeitiesToRemove(allDeities).forEach(k -> {
                tui().logf(Msg.DEITY, "(drop | superseded) %s", k);
                filteredIndex.remove(k);
            });
        }
    }

    private void defineSubraces() {
        for (Entry<String, Set<JsonNode>> entry : subraceIndex.entrySet()) {
            String raceKey = entry.getKey();
            JsonNode jsonSource = nodeIndex.get(raceKey);

            Set<JsonNode> inputSubraces = entry.getValue();
            List<JsonNode> subraces = new ArrayList<>();

            Json2QuteRace.prepareBaseRace(this, jsonSource, inputSubraces);

            if (inputSubraces.size() > 1) {
                tui().logf(Msg.RACES, "%s subraces found for %s", inputSubraces.size(), raceKey);
            }

            for (JsonNode sr : inputSubraces) {
                sr = copier.mergeSubrace(sr, jsonSource);
                String srKey = Tools5eIndexType.subrace.createKey(sr);
                TtrpgValue.indexInputType.setIn(sr, Tools5eIndexType.subrace.name());
                TtrpgValue.indexKey.setIn(sr, srKey);

                nodeIndex.put(srKey, sr);
                addSrdEntry(srKey, sr);
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
                // lookups from race to subrace are necessary, but can conflict with reprints/aliases
                // keep them separate (still used in getAliasOrDefault)
                subraceMap.put(lookupKey, srKey);
                tui().logf(Msg.RACES, "\t%s :: %s", lookupKey, srKey);
            }

            Json2QuteRace.updateBaseRace(this, jsonSource, inputSubraces, subraces);
        }
        subraceIndex.clear();
    }

    List<Tuple> findVariants(String key, JsonNode jsonSource, List<JsonNode> baseItems) {
        Tools5eIndexType type = Tools5eIndexType.getTypeFromKey(key);
        if (type == Tools5eIndexType.magicvariant) {
            return MagicVariant.findSpecificVariants(this, type, key, jsonSource, copier, baseItems);
        } else if (type == Tools5eIndexType.monster) {
            return Json2QuteMonster.findMonsterVariants(this, type, key, jsonSource);
        }
        return List.of(new Tuple(key, jsonSource));
    }

    /**
     * Behavior of looking for reprints is changed by reprint behavior defined in configuration.
     * The default is "newest", which will always collapse reprints into the newest source.
     *
     * @param finalKey
     * @param jsonSource
     * @return
     */
    private boolean isReprinted(String finalKey, JsonNode jsonSource) {
        // This method assumes that excluded sources are already filtered out
        if (config.reprintBehavior() == ReprintBehavior.all) {
            return false; // ignore reprints and include everything ;)
        }
        Tools5eSources sources = Tools5eSources.findSources(jsonSource);
        if (sources.filterRuleApplied()) {
            return false; // keep because a rule says so
        }

        if (SourceField.reprintedAs.existsIn(jsonSource)) {
            // This was reprinted in one or more other sources.
            // If any of those sources have been included, then skip this one
            // in favor of the (newer) reprint.
            // "reprintedAs": [ "Deep Gnome|MPMM" ]
            // "reprintedAs": [
            //   {
            //     "uid": "Unarmed Strike|XPHB",
            //     "tag": "variantrule"
            //   }
            // ],
            for (JsonNode reprintedAs : SourceField.reprintedAs.iterateArrayFrom(jsonSource)) {
                String rawKey = reprintedAs.isObject()
                        ? SourceField.uid.getTextOrThrow(reprintedAs)
                        : reprintedAs.asText();

                Tools5eIndexType type = Tools5eIndexType.getTypeFromKey(finalKey);
                if (reprintedAs.isObject()) {
                    String tag = SourceField.tag.getTextOrNull(reprintedAs);
                    if (tag != null) {
                        type = Tools5eIndexType.fromText(tag);
                    }
                }

                // Reprints can also be reprints; follow the alias/reprint chain
                String reprintKey = getAliasOrDefault(type.fromTagReference(rawKey));
                JsonNode reprint = nodeIndex.get(reprintKey);
                if (reprint == null) {
                    if (type == Tools5eIndexType.subrace) {
                        reprintKey = getAliasOrDefault(Tools5eIndexType.race.fromTagReference(rawKey));
                        reprint = nodeIndex.get(reprintKey);
                    }
                    if (reprint == null) {
                        tui().warnf(Msg.UNRESOLVED, "%s: unresolved reprint source %s", finalKey, reprintKey);
                        continue;
                    }
                }

                Tools5eSources reprintSources = Tools5eSources.findSources(reprint);
                if (reprintSources.includedByConfig()) {
                    if (config.reprintBehavior() == ReprintBehavior.edition) {
                        // Only follow the reprint chain if it's in the same edition
                        String sourceEdition = sources.edition();
                        String reprintEdition = reprintSources.edition();
                        if (reprintEdition != null && sourceEdition != null && !reprintEdition.equals(sourceEdition)) {
                            tui().logf(Msg.REPRINT, "(SKIP | edition)   %s: ignoring reprint as %s",
                                    finalKey, reprintKey);
                            continue;
                        }
                    }
                    // Otherwise, we have a "newer" reprint that should be used instead
                    tui().logf(Msg.REPRINT, "(drop | reprinted) %s ==> %s", finalKey, reprintKey);
                    // 1) create an alias mapping the old key to the reprinted key
                    reprints.put(finalKey, reprintKey);
                    // 2) add the sources of the reprint to the sources of the original (for later linking)
                    reprintSources.addReprint(sources);
                    return true;
                }
            }
        }
        if (SourceField.isReprinted.booleanOrDefault(jsonSource, false)) {
            tui().logf(Msg.REPRINT, "(drop | isReprint) %s", finalKey);
            return true; // the reprint will be used instead of this one.
        }
        return false; // keep
    }

    /**
     * Filter sub-resources based on the inclusion of the parent resource.
     *
     * @return true if resource has a parent, and that parent is excluded
     */
    private boolean otherwiseExcluded(String key) {
        // If a class is excluded, specific classfeatures, optional features,
        // subclasses, and subclassfeatures should also be removed
        // (unless a specific rule says otherwise).
        Tools5eSources sources = Tools5eSources.findSources(key);
        if (sources.filterRuleApplied()) {
            return false; // keep because a rule says so (we already logged these)
        }

        Tools5eIndexType type = Tools5eIndexType.getTypeFromKey(key);
        return switch (type) {
            case card -> removeIfParentExcluded(key, Tools5eIndexType.deck, Msg.DECK);
            case classfeature, subclassFeature -> removeIfParentExcluded(key, Tools5eIndexType.classtype,
                    Msg.CLASSES);
            case optfeature, optionalFeatureTypes -> removeUnusedOptionalFeatures(type, key);
            case subclass -> !sources.includedByConfig() || removeIfParentExcluded(key, Tools5eIndexType.classtype,
                    Msg.CLASSES);
            case subrace -> !sources.includedByConfig() || removeIfParentExcluded(key, Tools5eIndexType.race, Msg.RACES);
            default -> false; // does not have a parent
        };
    }

    private boolean removeIfParentExcluded(String key, Tools5eIndexType parentType, Msg msg) {
        String parentKey = parentType.fromChildKey(key);
        Tools5eSources parentSources = Tools5eSources.findSources(parentKey);
        if (parentSources == null) {
            tui().warnf(Msg.UNRESOLVED, "%35s :: unresolved parent of [%s]", parentKey, key);
            // allow for corrections (aliases), not reprints
            parentKey = getAliasOrDefault(parentKey, false);
            parentSources = Tools5eSources.findSources(parentKey);
            if (parentSources == null) {
                return true; // has a parent, it is missing (dangling resource)
            }
        }
        boolean included = parentSources.includedByConfig();
        if (!included) {
            tui().debugf(msg, "(drop) %43s :: %s", parentKey, key);
        }
        return !included;
    }

    private boolean removeUnusedOptionalFeatures(Tools5eIndexType type, String key) {
        OptionalFeatureType oft = optFeatureIndex.get(type, key);
        Tools5eSources oftSources = oft.getSources();

        // the feature type sources are amended by consuming classes/subclasses
        boolean included = oft.inUse() && oftSources.includedByConfig();
        var msgType = Msg.FEATURETYPE;

        if (included && type == Tools5eIndexType.optfeature) {
            msgType = Msg.FEATURE;
            // If an optional feature (rather than a type),
            // and the optional feature source is different from the parent source,
            // then we need to see if the feature source is included
            Tools5eSources ofSources = Tools5eSources.findSources(key);
            if (!ofSources.primarySource().equals(oftSources.primarySource())) {
                included = ofSources.includedByConfig();
            }
        }
        if (!included) {
            tui().debugf(msgType, "(drop) %43s :: %s", oft.getKey(), key);
        }
        return !included;
    }

    public boolean notPrepared() {
        return filteredIndex == null;
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

    private void addSrdEntry(String key, JsonNode node) {
        if (Tools5eSources.isSrd(node)) {
            String srdName = Tools5eSources.srdName(node);
            if (srdName != null) {
                // If there is a generic/SRD name, replace the specific name in the key
                String srdKey = key.replace(SourceField.name.getTextOrThrow(node).toLowerCase(),
                        srdName.toLowerCase());
                // Add an alias for the srd/generic-form of the name
                addAlias(srdKey, key);
                srdKeys.add(srdKey);
            } else {
                srdKeys.add(key);
            }
        }
    }

    void addAlias(String key, String alias) {
        if (key.equals(alias)) {
            return;
        }
        String old = aliases.putIfAbsent(key, alias);
        if (old != null && !old.equals(alias)) {
            tui().warnf("Oops! Duplicate simple key: %s; old: %s; new: %s", key, old, alias);
        }
    }

    public List<String> getAliasesFor(String targetKey) {
        return aliases.entrySet().stream()
                .filter(e -> e.getValue().equals(targetKey))
                .map(Entry::getKey)
                .collect(Collectors.toList());
    }

    public String getAliasOrDefault(String key) {
        return getAliasOrDefault(key, true);
    }

    public String getAliasOrDefault(String key, boolean includeReprints) {
        String previous;
        String value = key;
        do {
            previous = value;

            // race -> possible subrace alias
            String alias = value.startsWith("race")
                    ? subraceMap.get(previous)
                    : null;

            if (includeReprints) {
                String reprint = reprints.get(alias == null ? previous : alias);
                if (reprint != null) {
                    alias = reprint;
                }
            }

            value = alias == null
                    ? aliases.getOrDefault(previous, previous)
                    : alias;
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
        if (finalKey == null || finalKey.isEmpty()) {
            return null;
        }
        return filteredIndex.get(finalKey);
    }

    public ItemProperty findItemProperty(String fragment, Tools5eSources sources) {
        if (fragment == null || fragment.isEmpty()) {
            return null;
        }
        if (fragment.contains("|")) {
            String finalKey = Tools5eIndexType.itemProperty.fromTagReference(fragment);
            return ItemProperty.fromKey(finalKey, this);
        }

        // We could have a default property (phb), or we could have a homebrew property
        ItemProperty property = homebrewIndex.findHomebrewProperty(fragment, sources);
        if (property == null) {
            // Then we'll try the default source
            String key = Tools5eIndexType.itemProperty.fromTagReference(fragment);
            return ItemProperty.fromKey(key, this);
        }
        return property;
    }

    public ItemType findItemType(String fragment, Tools5eSources sources) {
        if (fragment == null || fragment.isEmpty()) {
            return null;
        }
        if (fragment.contains("|")) {
            String finalKey = Tools5eIndexType.itemType.fromTagReference(fragment);
            return ItemType.fromKey(finalKey, this);
        }
        // We could have a default property (phb), or we could have a homebrew property
        ItemType type = homebrewIndex.findHomebrewType(fragment, sources);
        if (type == null) {
            // Then we'll try the default source
            String key = Tools5eIndexType.itemType.fromTagReference(fragment);
            return ItemType.fromKey(key, this);
        }
        return type;
    }

    public ItemMastery findItemMastery(String fragment, Tools5eSources sources) {
        if (fragment == null || fragment.isEmpty()) {
            return null;
        }
        if (fragment.contains("|")) {
            String finalKey = Tools5eIndexType.itemMastery.fromTagReference(fragment);
            return ItemMastery.fromKey(finalKey, this);
        }
        // We could have a default property, or we could have a homebrew property
        ItemMastery mastery = homebrewIndex.findHomebrewMastery(fragment, sources);
        if (mastery == null) {
            // Then we'll try the default source
            String key = Tools5eIndexType.itemMastery.fromTagReference(fragment);
            return ItemMastery.fromKey(key, this);
        }
        return mastery;
    }

    public HomebrewMetaTypes getHomebrewMetaTypes(Tools5eSources sources) {
        return homebrewIndex.getHomebrewMetaTypes(sources);
    }

    public SkillOrAbility findSkillOrAbility(String key, Tools5eSources sources) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        SkillOrAbility skill = SkillOrAbility.fromTextValue(key);
        if (skill == null) {
            skill = homebrewIndex.findHomebrewSkillOrAbility(key, sources);
        }
        if (skill == null) {
            tui().warnf(Msg.UNKNOWN, "Unknown skill or ability %s in %s", key, sources);
            return new CustomSkillOrAbility(key);
        }
        return skill;
    }

    public SpellSchool findSpellSchool(String abbreviation, Tools5eSources sources) {
        if (abbreviation == null || abbreviation.isEmpty()) {
            return null;
        }
        SpellSchool school = SpellSchool.fromEncodedValue(abbreviation);
        if (school == null) {
            school = homebrewIndex.findHomebrewSpellSchool(abbreviation, sources);
        }
        if (school == null) {
            tui().warnf(Msg.UNKNOWN, "Unknown spell school %s in %s", abbreviation, sources);
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
        return result;
    }

    public JsonNode getOrigin(String finalKey) {
        JsonNode result = nodeIndex.get(finalKey);
        if (result == null) {
            List<String> target = nodeIndex.keySet().stream()
                    .filter(k -> k.startsWith(finalKey))
                    .collect(Collectors.toList());
            if (target.size() == 1) {
                String lookup = target.get(0);
                result = nodeIndex.get(lookup);
            } else if (target.size() > 1) {
                List<String> reduce = target.stream()
                        .filter(x -> !x.matches(".*\\|ua[^|]*$"))
                        .filter(x -> !x.contains("|dmg"))
                        .filter(x -> isIncluded(x))
                        .distinct()
                        .collect(Collectors.toList());
                if (reduce.size() > 1) {
                    tui().debugf(Msg.MULTIPLE, "Found several elements for %s: %s",
                            finalKey, reduce);
                    return null;
                } else if (reduce.size() == 1) {
                    String lookup = reduce.get(0);
                    result = nodeIndex.get(lookup);
                }
            }
        }
        if (result == null) {
            tui().debugf(Msg.UNRESOLVED, "No element found for %s",
                    finalKey);
        }
        return result;
    }

    public String linkifyByName(Tools5eIndexType type, String name) {
        String prefix = String.format("%s|%s|", type, name).toLowerCase();

        return nameToLink.computeIfAbsent(prefix, p -> {
            // Akin to getAliasOrDefault, but we have to filter by prefix
            List<String> target = List.of();

            if (type == Tools5eIndexType.subrace || type == Tools5eIndexType.race) {
                target = subraceMap.keySet().stream()
                        .filter(k -> k.startsWith(prefix))
                        .collect(Collectors.toList());
            }

            if (target.isEmpty()) {
                target = reprints.keySet().stream()
                        .filter(k -> k.startsWith(prefix))
                        .collect(Collectors.toList());
            }

            if (target.isEmpty()) {
                target = aliases.keySet().stream()
                        .filter(k -> k.startsWith(prefix))
                        .collect(Collectors.toList());
            }

            if (target.isEmpty()) {
                target = nodeIndex.keySet().stream()
                        .filter(k -> k.startsWith(prefix))
                        .collect(Collectors.toList());
            }

            if (target.isEmpty()) {
                tui().debugf(Msg.UNRESOLVED, "unresolved element for \"%s\" using [%s]", name, prefix);
                return name;
            } else if (target.size() > 1) {
                List<String> reduce = target.stream()
                        .filter(x -> !x.matches(".*\\|ua[^|]*$"))
                        .map(x -> getAliasOrDefault(x))
                        .filter(x -> isIncluded(x))
                        .distinct()
                        .collect(Collectors.toList());
                if (reduce.size() > 1) {
                    tui().debugf(Msg.MULTIPLE, "Found several elements for %s using [%s]: %s",
                            name, prefix, target);
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

    public boolean customRulesIncluded() {
        // The biggest hack of all time (not really).
        // I have some custom content for types/property/mastery that
        // should be included, but only if:
        // 1. No content is included (srdOnly)
        // 2. Some combination of basic rules and/or phb/dmg is included
        return srdOnly() ||
                config.sourcesIncluded(List.of(
                        "srd", "basicRules", "phb", "dmg",
                        "srd52", "freerules2024", "xphb", "xdmg"));
    }

    public boolean srdOnly() {
        return config.noSources();
    }

    public boolean sourceIncluded(String source) {
        return config.sourceIncluded(source);
    }

    public boolean isIncluded(String key) {
        String alias = getAliasOrDefault(key);
        return filteredIndex.containsKey(key) || filteredIndex.containsKey(alias);
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
            tui().debugf(Msg.UNRESOLVED, "unresolved class feature %s", finalKey);
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

    public OptionalFeatureType getOptionalFeatureType(JsonNode optfeatureNode) {
        return optFeatureIndex.get(optfeatureNode);
    }

    public OptionalFeatureType getOptionalFeatureType(String ft, String source) {
        if (ft == null) {
            return null;
        }
        return optFeatureIndex.get(ft, source, homebrewIndex);
    }

    @Override
    public void writeFullIndex(Path outputFile) throws IOException {
        if (notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing indexes");
        }
        Map<String, Object> allKeys = new TreeMap<>();
        allKeys.put("keys", new TreeSet<>(nodeIndex.keySet()));
        allKeys.put("mapping", new TreeMap<>(aliases));
        allKeys.put("reprints", new TreeMap<>(reprints));
        allKeys.put("srdKeys", new TreeSet<>(srdKeys));
        allKeys.put("subraceMap", new TreeMap<>(subraceMap));
        allKeys.put("optionalFeatures", optFeatureIndex.getMap());
        tui().writeJsonFile(outputFile, allKeys);
    }

    @Override
    public void writeFilteredIndex(Path outputFile) throws IOException {
        if (notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing files");
        }
        tui().writeJsonFile(outputFile, Map.of("keys", new TreeSet<>(filteredIndex.keySet())));
    }

    @Override
    public JsonNode getAdventure(String id) {
        String finalKey = Tools5eIndexType.adventure.createKey("adventure", id);
        return getNode(finalKey); // filtered
    }

    @Override
    public JsonNode getBook(String id) {
        String finalKey = Tools5eIndexType.book.createKey("book", id);
        return getNode(finalKey); // filtered
    }

    void linkSources(Tools5eIndexType type, JsonNode dataNode) {
        String id = dataNode.get("id").asText();

        String finalKey = type == Tools5eIndexType.adventureData
                ? Tools5eIndexType.adventure.createKey("adventure", id)
                : Tools5eIndexType.book.createKey("book", id);

        JsonNode fromNode = getOrigin(finalKey);

        // Adventures and Books have metadata in a different entry.
        SourceField.name.link(fromNode, dataNode);
        SourceField.source.link(fromNode, dataNode);
        SourceField.page.link(fromNode, dataNode);
        Tools5eFields.otherSources.link(fromNode, dataNode);
        Tools5eFields.additionalSources.link(fromNode, dataNode);
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

    public void cleanup() {
        if (instance == this) {
            instance = null;
        }
        nodeIndex.clear();
        subraceIndex.clear();
        tableIndex.clear();

        if (filteredIndex != null) {
            filteredIndex.clear();
        }

        aliases.clear();
        reprints.clear();
        subraceMap.clear();
        nameToLink.clear();

        spellClassIndex.clear();
        srdKeys.clear();

        optFeatureIndex.clear();
        homebrewIndex.clear();

        // affiliated sources cache, too
        Tools5eSources.clear();
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
