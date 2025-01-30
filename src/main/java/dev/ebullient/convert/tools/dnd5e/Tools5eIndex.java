package dev.ebullient.convert.tools.dnd5e;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.BiConsumer;
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
import dev.ebullient.convert.tools.dnd5e.HomebrewIndex.HomebrewFields;
import dev.ebullient.convert.tools.dnd5e.HomebrewIndex.HomebrewMetaTypes;
import dev.ebullient.convert.tools.dnd5e.Json2QuteClass.SubclassFeatureKeyData;
import dev.ebullient.convert.tools.dnd5e.Json2QuteItem.ItemField;
import dev.ebullient.convert.tools.dnd5e.Json2QuteRace.RaceFields;
import dev.ebullient.convert.tools.dnd5e.OptionalFeatureIndex.OptionalFeatureType;
import dev.ebullient.convert.tools.dnd5e.SkillOrAbility.CustomSkillOrAbility;
import dev.ebullient.convert.tools.dnd5e.SpellSchool.CustomSpellSchool;

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
    private final Map<String, JsonNode> nodeIndex = new TreeMap<>(); // --index
    private Map<String, JsonNode> filteredIndex = null;

    private final Map<String, Set<JsonNode>> subraceIndex = new HashMap<>(); // --index
    private final Map<SourceAndPage, List<JsonNode>> tableIndex = new HashMap<>();

    private final Map<String, String> aliases = new TreeMap<>(); // --index
    private final Map<String, String> reprints = new TreeMap<>(); // --index
    private final Map<String, String> subraceMap = new TreeMap<>(); // --index
    private final Map<String, String> nameToLink = new HashMap<>();

    // Class feature, Subclass, and Subclass Feature nonsense
    private final Map<String, Set<String>> classFeatures = new TreeMap<>(); // --index
    private final Map<String, Set<String>> subclassMap = new TreeMap<>(); // --index

    private final Set<String> srdKeys = new HashSet<>();

    final Tools5eJsonSourceCopier copier = new Tools5eJsonSourceCopier(this);
    final OptionalFeatureIndex optFeatureIndex = new OptionalFeatureIndex(this);
    final HomebrewIndex homebrewIndex = new HomebrewIndex(this);
    final SpellIndex spellIndex = new SpellIndex(this);

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
        Tools5eIndexType.rewardFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.spellFluff.withArrayFrom(node, this::addToIndex);
        Tools5eIndexType.subclassFluff.withArrayFrom(node, this::addToIndex);
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
            homebrew.addCrossReference(type, key, node);
        }

        switch (type) {
            case optfeature -> {
                // add while we're ingesting (homebrew or not)
                // will create/register an optionalFeatureType node
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
                classFeatures.put(key, new HashSet<>());
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
                String parentSource = Tools5eFields.parentSource.getTextOrNull(node);
                if (parentSource != null && TtrpgConfig.getConfig().sourceIncluded(source)) {
                    // include the parent source if you include an adventure (related rules)
                    tui().debugf(Msg.SOURCE, "including %s due to %s", parentSource, source);
                    TtrpgConfig.includeAdditionalSource(parentSource);
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
        TtrpgConfig.addReferenceEntries((n) -> addToIndex(Tools5eIndexType.reference, n));

        // Properly import homebrew sources
        tui().progressf("Importing homebrew sources");
        homebrewIndex.importBrew(this::importHomebrewTree);

        tui().debugf("Preparing index using configuration:\n%s", Tui.jsonStringify(config));

        // Add subraces to index
        defineSubraces();

        tui().progressf("Resolving copies and linking sources");

        // Find remaining/included base items
        List<JsonNode> baseItems = nodeIndex.values().stream()
                .filter(n -> TtrpgValue.indexBaseItem.booleanOrDefault(n, false))
                .filter(n -> !ItemField.packContents.existsIn(n))
                .toList();

        List<String> keys = new ArrayList<>(nodeIndex.keySet());
        List<Tuple> deities = new ArrayList<>();

        // For each node: handle copies, link sources
        for (String key : keys) {
            JsonNode jsonSource = nodeIndex.get(key);

            // check for / manage copies first.
            Tools5eIndexType type = Tools5eIndexType.getTypeFromKey(key);
            jsonSource = copier.handleCopy(type, jsonSource);
            nodeIndex.put(key, jsonSource); // update value with resolved/copied node

            // Pre-creation of sources..
            switch (type) {
                case adventureData, bookData -> linkSources(type, jsonSource);
                default -> {
                }
            }

            Tools5eSources.constructSources(key, jsonSource);

            if (type == Tools5eIndexType.deity) {
                deities.add(new Tuple(key, jsonSource));
                continue; // deal with these later.
            }

            // Reprints follow specialized variants, so we need to find the variants
            // now (and will filter them out based on rules later...)
            if (type.hasVariants()) {
                List<JsonNode> variants = findVariants(key, jsonSource, baseItems);
                for (JsonNode variant : variants) {
                    String variantKey = TtrpgValue.indexKey.getTextOrThrow(variant);
                    Tools5eSources.constructSources(variantKey, variant);
                    JsonNode old = nodeIndex.put(variantKey, variant);
                    if (old != null && !old.equals(variant)) {
                        tui().errorf("Duplicate key: %s%nold: %s%nnew: %s", variantKey, old, variant);
                    }
                }
            }

            // Post-creation of sources..
            switch (type) {
                case classtype, subclass -> optFeatureIndex.amendSources(key, jsonSource);
                case optfeature -> optFeatureIndex.amendSources(key, jsonSource);
                default -> {
                }
            }
        } // end for each entry

        tui().progressf("Applying source filters");
        filteredIndex = new HashMap<>(nodeIndex.size());

        BiConsumer<Msg, String> logThis = (msgType, msg) -> {
            if (msgType == Msg.TARGET) {
                tui().debugf(msgType, msg);
            } else {
                tui().logf(msgType, msg);
            }
        };

        // Let's create a list of interesting keys
        List<String> interestingKeys = new ArrayList<>(nodeIndex.size());
        for (var e : nodeIndex.entrySet()) {
            String key = e.getKey();
            JsonNode jsonSource = e.getValue();
            Tools5eIndexType type = Tools5eIndexType.getTypeFromKey(key);
            if (false
                    // Fluff types can continue to live only in the origin/nodeIndex
                    || type.isFluffType()
                    // Checking for reprints has aliasing knock-ons.
                    || isReprinted(key, jsonSource)
                    // While Deities are interesting, their handling is unique and done later
                    || type == Tools5eIndexType.deity
                    // Subclasses are also handled backwards (filled in by subclass features)
                    || type == Tools5eIndexType.subclass) {
                // Theses are uninteresting.
            } else {
                interestingKeys.add(key);
            }
        }

        // Apply include/exclude rules & source filters
        // to add included elements to the filter index
        for (String key : interestingKeys) {
            JsonNode jsonSource = getOriginNoFallback(key);
            Tools5eIndexType type = Tools5eIndexType.getTypeFromKey(key);
            Tools5eSources sources = Tools5eSources.findSources(key);
            if (sources == null) {
                // This is programmer error.
                tui().logf(Msg.SOURCE, "No sources found for %s", key);
                continue;
            }
            Msg msgType = sources.filterRuleApplied() ? Msg.TARGET : Msg.FILTER;

            if (type.isDependentType()) {
                // dependent types: don't keep if parent is excluded/missing
                if (processDependentType(key)) {
                    logThis.accept(msgType, " ----  " + key);
                    filteredIndex.put(key, jsonSource);
                } else {
                    logThis.accept(msgType, "(drop) " + key);
                }
            } else if (sources.includedByConfig()) {
                // key is included (by a specific rule, or because the source is included)
                filteredIndex.put(key, jsonSource);
                logThis.accept(msgType, " ----  " + key);

                if (type == Tools5eIndexType.spell) {
                    // Create a spell entry for included spell
                    spellIndex.addSpell(key, jsonSource);
                }
            } else {
                // source is not included, item is dropped
                logThis.accept(msgType, "(drop) " + key);
            }
        }

        // classFeatures contains both features and subclass features
        for (var entry : classFeatures.entrySet()) {
            String scKey = entry.getKey();
            if (scKey.startsWith("subclass")) {
                if (entry.getValue().isEmpty()) {
                    // no features associated with this subclass
                    logThis.accept(Msg.CLASSES, "(drop | no subclass features) " + scKey);
                } else {
                    logThis.accept(Msg.CLASSES, " ----  " + scKey);
                    filteredIndex.put(scKey, nodeIndex.get(scKey));
                }
            }
        }

        // Remove unused optional features from the optional feature index
        optFeatureIndex.removeUnusedOptionalFeatures(
                (k) -> filteredIndex.containsKey(k),
                (k) -> {
                    Tools5eSources sources = Tools5eSources.findSources(k);
                    if (sources.filterRuleApplied()) {
                        return; // keep because a rule says so (we already logged these)
                    }
                    logThis.accept(Msg.FEATURETYPE, "(drop) " + k);
                    filteredIndex.remove(k);
                });

        // Deities have their own glorious reprint mess, which we only need to deal with
        // when we aren't hoarding all the things.
        tui().progressf("Dealing with deities");
        // Find deities that have not been superceded by a reprint
        Json2QuteDeity.findDeities(deities).forEach(k -> {
            filteredIndex.put(k, nodeIndex.get(k));
        });

        // And finally, create an index of classes/subclasses/feats for spells
        // based on included sources & avaiable spells.
        spellIndex.buildSpellIndex(filteredIndex.values());
    }

    private void defineSubraces() {
        tui().progressf("Adding subraces");
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

    List<JsonNode> findVariants(String key, JsonNode jsonSource, List<JsonNode> baseItems) {
        Tools5eIndexType type = Tools5eIndexType.getTypeFromKey(key);
        if (type == Tools5eIndexType.magicvariant) {
            return MagicVariant.findSpecificVariants(this, type, key, jsonSource, copier, baseItems);
        } else if (type == Tools5eIndexType.monster) {
            return Json2QuteMonster.findMonsterVariants(this, type, key, jsonSource);
        }
        return List.of(jsonSource);
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
                    String lookupKey = finalKey.replace(sources.primarySource().toLowerCase(), "");
                    String versionKey = TtrpgValue.indexVersionKeys.streamFrom(reprint)
                            .map(x -> x.asText())
                            .filter(x -> x.startsWith(lookupKey))
                            .findFirst().orElse(null);
                    if (versionKey != null) {
                        reprintKey = versionKey; // more specific version/variant for redirect
                    }

                    // Otherwise, we have a "newer" reprint that should be used instead
                    tui().logf(Msg.REPRINT, "(--->| reprinted) %s ==> %s", finalKey, reprintKey);
                    // 1) create an alias mapping the old key to the reprinted key
                    reprints.put(finalKey, reprintKey);
                    // 2) add the sources of the reprint to the sources of the original (for later linking)
                    reprintSources.addReprint(sources);
                    return true;
                }
            }
        }
        if (SourceField.isReprinted.booleanOrDefault(jsonSource, false)) {
            tui().logf(Msg.REPRINT, "(--->| isReprint) %s", finalKey);
            return true; // this is a reprint, but we have no alias..
        }
        return false; // keep
    }

    /**
     * Filter sub-resources based on the inclusion of the parent resource.
     *
     * @return true if resource should be kept (not used in a filter)
     */
    private boolean processDependentType(final String key) {
        Tools5eIndexType type = Tools5eIndexType.getTypeFromKey(key);
        switch (type) {
            case optionalFeatureTypes -> {
                // optionalFeatureTypes are always included
                return true;
            }
            case classfeature -> {
                // classfeature is reliably tied to the class
                //   classfeature|ability score improvement|barbarian|phb|8|phb
                //   classfeature|ability score improvement|barbarian|xphb|12|xphb
                String classKey = Tools5eIndexType.classtype.fromChildKey(key);
                boolean reprinted = reprints.containsKey(classKey);
                if (!reprinted && Tools5eSources.includedByConfig(classKey)) {
                    // Only keep the class feature if the parent class is not a reprint.
                    classFeatures.computeIfAbsent(classKey, k -> new HashSet<>()).add(key);
                    return true; // keep it
                }
            }
            case subclassFeature -> {
                // This is where things go sideways
                // For example, these two versions of a subclass feature exists:
                //   subclassfeature|zealous presence|barbarian|phb|zealot|xge|10|xge
                //   subclassfeature|zealous presence|barbarian|xphb|zealot|xphb|10|xphb
                // usually reachable through the matching subclass
                //   subclass|path of the zealot|barbarian|phb|xge
                //   subclass|path of the zealot|barbarian|xphb|xphb
                // which relies on reprint behavior to resolve, if xphb is around
                //   subclass|path of the zealot|barbarian|phb|xge -> subclass|path of the zealot|barbarian|xphb|xphb
                //   subclass|path of the zealot|barbarian|xphb|xge -> subclass|path of the zealot|barbarian|xphb|xphb
                String scfKey = key;
                SubclassFeatureKeyData keyData = new SubclassFeatureKeyData(key);

                // does the subclass exist or is it a reprint
                String scKey = getSubclassKey(keyData.toSubclassKey());
                boolean scIncluded = Tools5eSources.includedByConfig(scKey);
                boolean scReprint = reprints.containsKey(scKey);

                if (scReprint) {
                    // the subclass (including its features) was reprinted.
                    return false; // remove it
                }

                // does the parent class exist or is it a reprint
                String classKey = keyData.toClassKey();
                boolean classIncluded = Tools5eSources.includedByConfig(classKey);
                String classReprint = reprints.get(classKey);

                tui().debugf(Msg.CLASSES, "%s\n\t(%5s) %s -> %s\n\t(%5s) %s -> %s", key,
                        classReprint, classKey, classReprint,
                        scReprint, scKey, reprints.get(scKey));

                if (classReprint != null) {
                    // This is the most common case: PHB -> XPHB
                    // the reprint behavior will handle this
                    Tools5eSources altSources = Tools5eSources.findSources(classReprint);
                    classIncluded = altSources != null && altSources.includedByConfig();
                    if (!classIncluded) {
                        return false; // remove it, can't fix it
                    }

                    // We found the class reprint.
                    // The reprinted class is the new resource anchor for generated notes
                    // Change the class source for the subclass feature
                    keyData.classSource = altSources.primarySource();

                    // is there a subclass key with this new class source?
                    var altScKey = getSubclassKey(keyData.toSubclassKey());
                    boolean altScPresent = Tools5eSources.includedByConfig(altScKey);
                    if (altScPresent) {
                        // This is the sometimes-covered case:
                        //   subclass|path of wild magic|barbarian|xphb|tce
                        // reset all the things to hit the happy path below
                        tui().debugf("subclassFeature subclass: %s -> %s", scKey, altScKey);
                        scfKey = keyData.toKey();
                        scKey = altScKey;
                        classKey = classReprint;
                        scIncluded = altScPresent;
                    } else {
                        // There are times this case is not covered, especially in homebrew, for example:
                        //   subclassfeature|adamantine hide|druid|phb|forged|exploringeberron|10|exploringeberron
                        // this subclassfeature is included, but there is no mapping to an xphb version of the subclass.
                        //
                        // The reset classKey will force the issue. If the subclass is also present/included,
                        // then it will be added to the adjusted class.
                        tui().debugf("subclassFeature oddball: %s -> %s", scKey, altScKey);
                    }
                }

                if (classIncluded && scIncluded) {
                    // keep the subclass feature if both the class and subclass are included
                    subclassMap.computeIfAbsent(classKey, k -> new HashSet<>()).add(scKey);
                    classFeatures.computeIfAbsent(scKey, k -> new HashSet<>()).add(scfKey);
                    return true;
                }
            }
            default -> {
                // no-op
            }
        }
        return false; // remove it!
    }

    public boolean notPrepared() {
        return filteredIndex == null;
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

    public String getSubclassKey(String targetKey) {
        // short name to long name without following reprints.
        return getAliasOrDefault(targetKey, false);
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

    public Collection<HomebrewMetaTypes> getHomebrewMetaTypes(Tools5eSources activeSources) {
        return homebrewIndex.getHomebrewMetaTypes(activeSources);
    }

    public ItemProperty findItemProperty(String key, Tools5eSources sources) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        // now a mix of with and without sources
        if (!Tools5eIndexType.itemProperty.isKey(key)) {
            key = Tools5eIndexType.itemProperty.fromTagReference(key);
        }
        JsonNode propertyNode = findTypePropertyNode(Tools5eIndexType.itemProperty, key, sources); // check alias & phb/xphb
        if (propertyNode != null) {
            return ItemProperty.fromNode(propertyNode);
        }
        // try homebrew (normalize from key)
        String[] parts = key.split("\\|");
        return homebrewIndex.findHomebrewProperty(parts[1], sources);
    }

    public ItemType findItemType(String key, Tools5eSources sources) {
        if (key == null || key.isEmpty()) {
            return null;
        }
        // now a mix of with and without sources
        if (!Tools5eIndexType.itemType.isKey(key)) {
            key = Tools5eIndexType.itemType.fromTagReference(key);
        }
        JsonNode typeNode = findTypePropertyNode(Tools5eIndexType.itemType, key, sources); // check alias & phb/xphb
        if (typeNode != null) {
            return ItemType.fromNode(typeNode);
        }
        // try homebrew (normalize from key)
        String[] parts = key.split("\\|");
        return homebrewIndex.findHomebrewType(parts[1], sources);
    }

    public ItemMastery findItemMastery(String tagReference, Tools5eSources sources) {
        if (tagReference == null || tagReference.isEmpty()) {
            return null;
        }
        // This is always a tag: name|source
        String key = Tools5eIndexType.itemMastery.fromTagReference(tagReference);
        JsonNode masteryNode = getOriginNoFallback(getAliasOrDefault(key));
        if (masteryNode != null) {
            return ItemMastery.fromNode(masteryNode);
        }
        // try homebrew (normalize from key)
        String[] parts = key.split("\\|");
        return homebrewIndex.findHomebrewMastery(parts[1], sources);
    }

    private JsonNode findTypePropertyNode(Tools5eIndexType type, String key, Tools5eSources sources) {
        String aliasKey = getAliasOrDefault(key);
        JsonNode node = getOriginNoFallback(aliasKey);
        if (node == null && aliasKey.endsWith("phb")) {
            aliasKey = aliasKey.contains("|xphb")
                    ? aliasKey.replace("|xphb", "|phb")
                    : aliasKey.replace("|phb", "|xphb");
            node = getOriginNoFallback(aliasKey);
            if (node != null) {
                addAlias(key, aliasKey);
            }
        }
        return node;
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

    public SpellSchool findSpellSchool(String code, Tools5eSources sources) {
        if (code == null || code.isEmpty()) {
            return SpellSchool.SchoolEnum.None;
        }
        SpellSchool school = SpellSchool.fromEncodedValue(code);
        if (school == null) {
            school = homebrewIndex.findHomebrewSpellSchool(code, sources);
        }
        if (school == null) {
            tui().warnf(Msg.UNKNOWN, "Unknown spell school %s in %s", code, sources);
            return new CustomSpellSchool(code, code);
        }
        return school;
    }

    public Set<String> findSubclasses(String classKey) {
        return subclassMap.getOrDefault(classKey, Set.of());
    }

    public Set<String> findClassFeatures(String classOrSubclassKey) {
        return classFeatures.getOrDefault(classOrSubclassKey, Set.of());
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
            if (result == null) {
                tui().logf(Msg.UNRESOLVED, "No element found for %s", finalKey);
            }
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

    public boolean customContentIncluded() {
        // The biggest hack of all time (not really).
        // I have some custom content for types/property/mastery that
        // should be included, but only if some combination of
        // basic/free rules, srd, phb or dmg is included
        return config.noSources()
                || config.sourcesIncluded(List.of(
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

    public OptionalFeatureType getOptionalFeatureType(JsonNode optfeatureNode) {
        return optFeatureIndex.get(optfeatureNode);
    }

    public OptionalFeatureType getOptionalFeatureType(String featureType) {
        if (featureType == null) {
            return null;
        }
        return optFeatureIndex.get(featureType);
    }

    @Override
    public void writeFullIndex(Path outputFile) throws IOException {
        if (notPrepared()) {
            throw new IllegalStateException("Index must be prepared before writing indexes");
        }
        Map<String, Object> allKeys = new LinkedHashMap<>();
        allKeys.put("keys", nodeIndex.keySet());
        allKeys.put("mapping", aliases);
        allKeys.put("reprints", reprints);
        allKeys.put("subraceMap", subraceMap);
        allKeys.put("subclassMap", subclassMap);
        allKeys.put("classFeatures", classFeatures);
        allKeys.put("optionalFeatures", optFeatureIndex.getMap());
        allKeys.put("srdKeys", srdKeys);
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

    public SpellIndex getSpellIndex() {
        return spellIndex;
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

        spellIndex.clear();
        srdKeys.clear();

        optFeatureIndex.clear();
        homebrewIndex.clear();

        // affiliated sources cache, too
        Tools5eSources.clear();
        ItemMastery.clear();
        ItemProperty.clear();
        ItemType.clear();
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
