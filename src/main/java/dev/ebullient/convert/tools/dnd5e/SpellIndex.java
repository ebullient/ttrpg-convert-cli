package dev.ebullient.convert.tools.dnd5e;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.dnd5e.SpellEntry.SpellReference;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndexType.IndexFields;

public class SpellIndex implements JsonSource {
    private static final Set<String> skipReferences = Set.of(
            "subclass|college of lore|bard|phb|phb");

    final Map<String, SpellEntry> spellsByKey = new TreeMap<>();

    private final Tools5eIndex index;

    public SpellIndex(Tools5eIndex index) {
        this.index = index;
    }

    public void clear() {
        spellsByKey.clear();
    }

    public SpellEntry getSpellEntry(String key) {
        key = index.getAliasOrDefault(key);
        // getOrigin will log unresolved once.
        return index.getOrigin(key) != null
                ? spellsByKey.computeIfAbsent(key, k -> new SpellEntry(k, index.getOrigin(k)))
                : null;
    }

    /**
     * Add a spell to the index
     * (while iterating elements in prepare)
     *
     * @param key
     * @param spellNode
     */
    public SpellEntry addSpell(String key, JsonNode spellNode) {
        key = index.getAliasOrDefault(key);
        return spellsByKey.compute(key,
                (k, v) -> v == null ? new SpellEntry(k, spellNode) : v);
    }

    /**
     * Read spells/sources.json.
     * Called at the end of {@link Tools5eIndex#prepare()}
     * read additional spell sources to generate reference indexes of included
     * spells
     */
    public void buildSpellIndex(Collection<JsonNode> allNodes) {
        // Remove excluded spells ahead of any other iteration
        spellsByKey.entrySet().removeIf(e -> index.isExcluded(e.getKey()));

        // Read spells/sources.json; generate class index for filters
        readSpellSources();
        // now process additionalSpells nodes
        processAdditionalSpells(allNodes);
    }

    /**
     * Called from {@link #buildSpellIndex(Collection)} to read
     * the spells/sources.json file and create class indexes for filtering
     * by class.
     */
    private void readSpellSources() {
        // Iterate the contents of spells/sources.json
        // This file is organized source -> spellName -> "class" or "classVariant"->
        // array of { name, source }
        JsonNode spellClassMap = TtrpgConfig.readIndex("spell-source");
        // source -> spellName
        for (var sourceToSpells : iterableFields(spellClassMap)) {
            final String spellSource = sourceToSpells.getKey();
            // spellName -> "class" or "classVariant"
            for (var nameToReferences : iterableFields(sourceToSpells.getValue())) {
                final String spellName = nameToReferences.getKey();

                String spellKey = Tools5eIndexType.spell.createKey(spellName, spellSource);
                if (index.isExcluded(spellKey)) {
                    continue;
                }

                final JsonNode spellNode = index().getOriginNoFallback(spellKey);
                SpellEntry spellEntry = addSpell(spellKey, spellNode);

                final JsonNode referenceNode = nameToReferences.getValue();
                // "class" or "classVariant" -> { name, source }
                for (var typeToReference : iterableFields(referenceNode)) {
                    if (!typeToReference.getKey().matches("^class.*")) {
                        // maybe something besides class or classVariant..
                        // that would be unexpected
                        tui().logf(Msg.UNKNOWN, "Unknown reference type: %s", typeToReference.getKey());
                        continue;
                    }
                    Tools5eIndexType refType = Tools5eIndexType.classtype;
                    // "class" or "classVariant" -> array of { name, source }
                    for (var reference : iterableElements(typeToReference.getValue())) {
                        readClassType(spellEntry, reference, refType);
                    }
                }
            }
        }
    }

    /**
     * Called from {@link #buildSpellIndex(Collection)} to process
     * `additionalSpells`
     * nodes present in included content (can occur in a variety of types)
     */
    private void processAdditionalSpells(Collection<JsonNode> allNodes) {
        // iterate over all included nodes
        for (var node : allNodes) {
            Tools5eSources sources = Tools5eSources.findSources(node);
            final String nodeKey = sources.getKey();
            if (skipReferences.contains(nodeKey) || index.isExcluded(nodeKey)) {
                // beyond excluded classes, there are some hard-coded notes to skip...
                continue;
            }
            // get the type of the node (spell, race, class, feat, ...)
            final Tools5eIndexType type = sources.getType();
            if (type == Tools5eIndexType.spell) {
                // look for legacy specification of classes that can use a spell
                // this method is used by homebrew
                readClassesFromSpell(nodeKey, node);
            } else {
                // otherwise look for `additionalSpells` in the node
                readAdditionalSpells(nodeKey, node);
            }
        }
    }

    /**
     * Called from {@link #processAdditionalSpells(Collection)} to read
     *
     * @param spellKey the spell key
     * @param spellNode the spell node
     */
    private void readClassesFromSpell(String spellKey, JsonNode spellNode) {
        JsonNode classes = SpellIndexFields.classes.getFrom(spellNode);
        if (classes == null || classes.isNull()) {
            return;
        }
        // Find the created spellEntry (by key)
        SpellEntry spellEntry = getSpellEntry(spellKey);
        // Legacy / homebrew
        for (var n : SpellIndexFields.fromClassList.iterateArrayFrom(classes)) {
            tui().logf(Msg.SPELL, "readClasses/fromClassList: %s :: %s", spellKey, n);
            readClassType(spellEntry, n, Tools5eIndexType.classtype);
        }
        for (var n : SpellIndexFields.fromClassListVariant.iterateArrayFrom(classes)) {
            tui().logf(Msg.SPELL, "readClasses/fromClassList: %s :: %s", spellKey, n);
            readClassType(spellEntry, n, Tools5eIndexType.classtype);
        }
        for (var n : SpellIndexFields.fromSubclass.iterateArrayFrom(classes)) {
            tui().logf(Msg.SPELL, "readClasses/fromSubclass: %s :: %s", spellKey, n);
            JsonNode classNode = SpellIndexFields.classNode.getFrom(n);
            JsonNode subclassNode = SpellIndexFields.subclass.getFrom(n);
            // Add class attributes to the subclass node so the key can
            // be created as usual
            IndexFields.className.setIn(subclassNode,
                    SourceField.name.getTextOrNull(classNode));
            IndexFields.classSource.setIn(subclassNode,
                    SourceField.source.getTextOrNull(classNode));
            readClassType(spellEntry, subclassNode, Tools5eIndexType.subclass);
        }
    }

    /**
     * Called from {@link #readClassesFromSpell(String, JsonNode)}
     *
     * @param spellEntry the spell entry
     * @param reference the class or subclass node
     * @param refType Tools5eIndexType.classtype or Tools5eIndexType.subclass
     */
    private void readClassType(SpellEntry spellEntry, JsonNode reference, Tools5eIndexType refType) {
        final String refKey = refType.createKey(reference);

        // A book: TCE, for example, which made changes to Bard and Ranger..
        String variantSource = SpellIndexFields.definedInSource.getTextOrNull(reference);

        // skip (a) if reference is excluded, or
        // (b) this is a variant and the variant source is excluded
        if (index().isExcluded(refKey)
                || (variantSource != null && !index().sourceIncluded(variantSource))) {
            return;
        }

        spellEntry.addSpellReference(refKey, variantSource != null);
    }

    /**
     * Called from {@link #processAdditionalSpells(Collection)} to read
     *
     * from: "util-additionalspells.json"
     * schema: additionalSpellsArray
     *
     * @param refererKey the key of the referring node
     * @param refererNode the referring node
     */
    private void readAdditionalSpells(String refererKey, JsonNode refererNode) {
        final JsonNode additionalNode = SpellIndexFields.additionalSpells.getFrom(refererNode);
        if (index.isExcluded(refererKey) || additionalNode == null || additionalNode.isNull()) {
            // skip excluded nodes and nodes without an additionalSpells attribute
            return;
        }
        // Collect all spells referenced by this element
        for (var additionalSpells : iterableElements(additionalNode)) {
            gatherSpells(refererKey, SpellIndexFields.innate.getFrom(additionalSpells), false);
            gatherSpells(refererKey, SpellIndexFields.known.getFrom(additionalSpells), false);
            gatherSpells(refererKey, SpellIndexFields.prepared.getFrom(additionalSpells), false);
            gatherSpells(refererKey, SpellIndexFields.expanded.getFrom(additionalSpells), true);
        }
    }

    /**
     * Called from {@link #readAdditionalSpells(String, JsonNode)}
     * from: "util-additionalspells.json"
     * schema: _additionalSpellObject
     *
     * @param refererKey the key of the referring node
     * @param spellList the _additionalSpellObject data from the referring node
     * @param expanded true if this data expands/extends the class spell list
     */
    private void gatherSpells(String refererKey, JsonNode spellList, boolean expanded) {
        // This is a rough ride. We need to handle a variety of formats
        // Ultimately, we're just looking to find a list of touched/referenced spells
        for (var properties : iterableFields(spellList)) {
            toSpellList(refererKey, properties.getValue(), properties.getKey(), expanded);
        }
    }

    /**
     * Called from {@link #gatherSpells(String, JsonNode, boolean)}
     *
     * from: "util-additionalspells.json", schema noted below
     *
     * @param refererKey the key of the referring node
     * @param spellList list of nodes (of various types) that reference spells
     * @param constraint the constraint (the key: 1, s1, etc.)
     * @param expanded true if this data expands/extends the class spell list
     */
    private void toSpellList(String refererKey, JsonNode spellList, String constraint, boolean expanded) {
        if (spellList == null || spellList.isNull()) {
            return;
        }
        if (spellList.isObject()) {
            // _additionalSpellLevelObject -> _additionalSpellRechargeObject
            resolveRechargeSpells(refererKey, SpellIndexFields.rest.getFrom(spellList), constraint, expanded);
            resolveRechargeSpells(refererKey, SpellIndexFields.daily.getFrom(spellList), constraint, expanded);
            resolveRechargeSpells(refererKey, SpellIndexFields.resource.getFrom(spellList), constraint, expanded);

            // recurse: these keys hold arrays of spells:
            // _additionalSpellArrayOfStringOrFilterObject
            toSpellList(refererKey, SpellIndexFields.ritual.getFrom(spellList), constraint, expanded);
            toSpellList(refererKey, SpellIndexFields.will.getFrom(spellList), constraint, expanded);
            toSpellList(refererKey, SpellIndexFields.others.getFrom(spellList), constraint, expanded);
        } else if (spellList.isArray()) {
            // _additionalSpellArrayOfStringOrFilterObject
            for (var reference : iterableElements(spellList)) {
                if (reference.isTextual()) {
                    addFromText(refererKey, reference.asText(), constraint, expanded);
                } else if (reference.isObject()) {
                    // a filter defining referenced spells (where all would be included)
                    resolveFilter(refererKey, SpellIndexFields.all.getFrom(reference), constraint, expanded);

                    // a filter defining referenced spells (where some would be chosen)
                    JsonNode choose = SpellIndexFields.choose.getFrom(reference);
                    if (SpellIndexFields.from.existsIn(choose)) {
                        // choose from a list of spell reference tags..
                        for (var x : SpellIndexFields.from.iterateArrayFrom(choose)) {
                            addFromText(refererKey, x.asText(), constraint, expanded);
                        }
                    } else {
                        // handle the filter describing the spells to include
                        resolveFilter(refererKey, choose, constraint, expanded);
                    }
                }
            }
        }
    }

    /**
     * Called from {@link #toSpellList(String, JsonNode, String, boolean)}
     *
     * schema: _additionalSpellRechargeObject
     *
     * @param refererKey the key of the referring node
     * @param rechargeNode the _additionalSpellRechargeObject data from the
     *        referring node
     * @param constraint the constraint (the key: 1, s1, etc.)
     * @param expanded true if this data expands/extends the class spell list
     */
    public void resolveRechargeSpells(String refererKey, JsonNode rechargeNode, String constraint,
            boolean expanded) {
        if (rechargeNode == null || rechargeNode.isNull()) {
            return;
        }
        // we're ignoring the key here: 1, 2, 3, 4, ...; 1e, 2e, 3e...
        // the value is _additionalSpellArrayOfStringOrFilterObject
        for (var x : iterableFields(rechargeNode)) {
            toSpellList(refererKey, x.getValue(), constraint, expanded);
        }
    }

    /**
     * A range of spells to be added, formatted similarly to the options in a
     * {@literal {@filter ...}} tag. For example: {@code level=0|class=Wizard}
     *
     * @param refererKey the key of the referring node
     * @param filter the filter node (should be text)
     * @param constraint the constraint (the key: 1, s1, etc.)
     * @param expanded true if this data expands/extends the class spell list
     */
    public void resolveFilter(String refererKey, JsonNode filter, String constraint, boolean expanded) {
        if (filter == null || filter.isNull()) {
            return;
        }
        if (!filter.isTextual()) {
            tui().logf(Msg.UNKNOWN, "resolveFilter unknown value %s from %s", filter, refererKey);
            return;
        }
        tui().logf(Msg.SPELL, "resolveFilter (%2s) %s :: %s", constraint, refererKey, filter);
        // level=1;2;3;4;5|class=Cleric;Druid;Wizard|school=D
        String[] filterParts = filter.asText().split("\\|");
        FilterConditions filterConditions = new FilterConditions();

        for (String f : filterParts) {
            String[] parts = f.split("=");
            if (parts.length == 2) {
                switch (parts[0].toLowerCase()) {
                    case "class" -> {
                        filterConditions.setClasses(parts[1].split(";"));
                    }
                    case "level" -> {
                        filterConditions.setLevels(parts[1].split(";"));
                    }
                    case "school" -> {
                        filterConditions.setSchools(parts[1].split(";"));
                    }
                    case "source" -> {
                        filterConditions.setSources(parts[1].split(";"));
                    }
                    case "spell attack" -> {
                        filterConditions.setSpellAttack(parts[1].split(";"));
                    }
                    case "components & miscellaneous" -> {
                        filterConditions.setComponentsMisc(parts[1].split(";"));
                    }
                    default ->
                        tui().logf(Msg.UNKNOWN, "resolveFilter unknown part: %s", parts[0]);
                }
            }
        }
        for (SpellEntry spell : spellsByKey.values()) {
            if (filterConditions.matchAll(spell)) {
                spell.addReference(new SpellReference(refererKey, constraint, null, expanded));
            }
        }
    }

    /**
     * Called from {@link #toSpellList(String, JsonNode, String, boolean)}
     *
     * @param refererKey the key of the referring node
     * @param tag the spell reference tag
     * @param constraint the constraint (the key: 1, s1, etc.)
     * @param expanded true if this data expands/extends the class spell list
     */
    private void addFromText(String refererKey, String tag, String constraint, boolean expanded) {
        int pos = tag.indexOf("#");
        String asLevel = pos > 0 ? tag.substring(pos + 1) : null;
        tag = pos > 0 ? tag.substring(0, pos) : tag;

        String spellKey = Tools5eIndexType.spell.fromTagReference(tag);
        if (index.isExcluded(spellKey)) {
            return;
        }
        var spellEntry = getSpellEntry(spellKey);
        if (spellEntry != null) {
            spellEntry.addReference(refererKey, constraint, asLevel, expanded);
        }
    }

    static class FilterConditions {
        Set<String> classes = Set.of();
        Set<String> levels = Set.of();
        Set<String> schools = Set.of();
        Set<String> sources = Set.of();
        Set<String> spellAttack = Set.of();
        Set<String> componentsMisc = Set.of();

        /**
         * @param class the class to set
         */
        public void setClasses(String[] classList) {
            this.classes = Arrays.stream(classList)
                    .map(x -> x.toLowerCase())
                    .collect(Collectors.toSet());
        }

        /**
         * @param level the level to set
         */
        public void setLevels(String[] level) {
            this.levels = Set.of(level);
        }

        /**
         * @param school the school to set
         */
        public void setSchools(String[] school) {
            this.schools = Set.of(school);
        }

        /**
         * @param source the source to set
         */
        public void setSources(String[] source) {
            this.sources = Set.of(source);
        }

        /**
         * @param source the source to set
         */
        public void setSpellAttack(String[] source) {
            this.spellAttack = Arrays.stream(source)
                    .map(x -> x.toUpperCase())
                    .collect(Collectors.toSet());
        }

        public void setComponentsMisc(String[] components) {
            this.componentsMisc = Arrays.stream(components)
                    .map(x -> x.toLowerCase())
                    .collect(Collectors.toSet());
        }

        boolean matchAll(SpellEntry spell) {
            Tools5eSources spellSources = Tools5eSources.findSources(spell.spellNode);
            return testClasses(spell)
                    && testLevels(spell)
                    && testSchools(spell)
                    && testSources(spell, spellSources)
                    && testSpellAttack(spell)
                    && testSpellComponents(spell);
        }

        private boolean testClasses(SpellEntry spell) {
            return classes.isEmpty() || classes.stream().anyMatch(x -> spell.inClassList(x));
        }

        private boolean testLevels(SpellEntry spell) {
            return levels.isEmpty() || levels.contains(spell.level);
        }

        private boolean testSchools(SpellEntry spell) {
            return schools.isEmpty() || schools.contains(spell.school.code());
        }

        private boolean testSources(SpellEntry spell, Tools5eSources spellSources) {
            return sources.isEmpty() || spellSources.includedBy(sources);
        }

        private boolean testSpellAttack(SpellEntry spell) {
            return spellAttack.isEmpty()
                    || spell.spellAttack.stream().anyMatch(x -> spellAttack.contains(x.toUpperCase()));
        }

        private boolean testSpellComponents(SpellEntry spell) {
            if (componentsMisc.contains("ritual")) {
                return spell.ritual;
            } else if (!componentsMisc.isEmpty()) {
                Tui.instance().logf(Msg.UNKNOWN, "Unknown components & miscellaneous value: %s", componentsMisc);
            }
            return true;
        }
    }

    enum SpellIndexFields implements JsonNodeReader {
        ability,
        additionalSpells,
        all,
        choose,
        classes,
        classNode("class"),
        components,
        daily,
        definedInSource,
        expanded,
        from,
        fromClassList,
        fromClassListVariant,
        fromSubclass,
        innate,
        known,
        level,
        meta,
        others("_"),
        prepared,
        resource,
        rest,
        ritual,
        school,
        spellAttack,
        subclass,
        text,
        will,
        ;

        private final String nodeName;

        SpellIndexFields(String nodeName) {
            this.nodeName = nodeName;
        }

        SpellIndexFields() {
            this.nodeName = name();
        }

        @Override
        public String nodeName() {
            return nodeName;
        }
    }

    @Override
    public Tools5eIndex index() {
        return index;
    }

    @Override
    public Tools5eSources getSources() {
        return null;
    }
}
