package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.isPresent;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonTextConverter.SourceField;
import dev.ebullient.convert.tools.dnd5e.SpellIndex.SpellIndexFields;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class SpellEntry {
    final String level;
    final String spellKey;
    final Map<String, SpellReference> references = new TreeMap<>();
    final Map<String, SpellReference> expandedList = new TreeMap<>();
    final Set<String> classes = new HashSet<>();
    final Set<String> classExpanded = new HashSet<>();
    final SpellSchool school;
    final boolean ritual;
    final List<String> components;
    final List<String> spellAttack;

    @JsonIgnore
    final JsonNode spellNode;

    /**
     * Created as spells are read from index
     * Needed for filtering spells for indexes
     */
    public SpellEntry(String key, JsonNode spellNode) {
        this.spellKey = key;
        this.spellNode = spellNode;
        this.level = SpellIndexFields.level.getTextOrEmpty(spellNode);
        this.ritual = spellIsRitual(spellNode);
        this.components = spellComponents(spellNode);
        this.spellAttack = spellAttack(spellNode);
        this.school = spellSchool(spellNode);
    }

    public String getLevelText() {
        return JsonSource.spellLevelToText(level);
    }

    public String getLevel() {
        return level;
    }

    public String getName() {
        return Tools5eIndexType.spell.decoratedName(spellNode);
    }

    /**
     * Added when reading legacy spell definitions from the spell node
     * Considered an expansion if the variantNode is present
     * (i.e. TCE expands the spell list for ranger... )
     */
    public SpellReference addSpellReference(String refererKey, boolean expanded) {
        SpellReference ref = new SpellReference(refererKey, expanded);
        if (expanded) {
            expandedList.put(refererKey, ref);
        } else {
            references.put(refererKey, ref);
        }
        if (ref.refererType == Tools5eIndexType.classtype) {
            // Create class index without source (Wizard) for filters -> spellEntry
            String className = SourceField.name.getTextOrEmpty(ref.refererNode).toLowerCase();
            classes.add(className);
            if (expanded) {
                classExpanded.add(className);
            }
        }
        return ref;
    }

    public SpellReference addReference(String refererKey, String constraint, String asLevel, boolean expanded) {
        SpellReference ref = new SpellReference(refererKey, constraint, asLevel, expanded);
        return addReference(ref);
    }

    /**
     * Add a reference to this spell from the `additionalSpells` attribute.
     * There is more information here: is there a class-level requirement, or
     * a spell-slot level requirement; does this expand the class spell list; etc.
     * Given these have more detail (and are newer), they may replace a more basic
     * reference.
     *
     * @param ref
     */
    public SpellReference addReference(SpellReference ref) {
        return addOrReplace(ref, ref.expanded ? expandedList : references);
    }

    private SpellReference addOrReplace(SpellReference spellRef, Map<String, SpellReference> set) {
        return set.compute(spellRef.refererKey, (k, existingRef) -> {
            if (existingRef != null && existingRef.isSpecific()) {
                return existingRef; // Keep the existing specific reference
            }
            return spellRef; // Add new or replace non-specific
        });
    }

    private boolean spellIsRitual(JsonNode spellNode) {
        boolean ritual = false;
        JsonNode meta = SpellIndexFields.meta.getFrom(spellNode);
        if (meta != null) {
            ritual = SpellIndexFields.ritual.booleanOrDefault(meta, false);
        }
        return ritual;
    }

    private List<String> spellComponents(JsonNode spellNode) {
        JsonSource converter = Tools5eIndex.getInstance();
        List<String> list = new ArrayList<>();
        for (Entry<String, JsonNode> f : SpellIndexFields.components.iterateFieldsFrom(spellNode)) {
            switch (f.getKey().toLowerCase()) {
                case "v" -> list.add("V");
                case "s" -> list.add("S");
                case "m" -> {
                    if (f.getValue().isObject()) {
                        list.add(SpellIndexFields.text.replaceTextFrom(f.getValue(), converter));
                    } else {
                        list.add(converter.replaceText(f.getValue()));
                    }
                }
                case "r" -> list.add("R"); // Royalty. Acquisitions Incorporated
            }
        }
        return list;
    }

    private List<String> spellAttack(JsonNode spellNode) {
        List<String> list = new ArrayList<>();
        for (Entry<String, JsonNode> f : SpellIndexFields.spellAttack.iterateFieldsFrom(spellNode)) {
            switch (f.getKey().toLowerCase()) {
                case "m" -> list.add("M"); // melee
                case "r" -> list.add("R"); // ranged
                case "o" -> list.add("O"); // other/unknown
                default -> {
                }
            }
        }
        return list;
    }

    private SpellSchool spellSchool(JsonNode spellNode) {
        String school = SpellIndexFields.school.getTextOrEmpty(spellNode);
        return Tools5eIndex.getInstance().findSpellSchool(school, Tools5eSources.findSources(spellNode));
    }

    public SpellReference getReference(String key) {
        SpellReference ref = references.get(key);
        if (ref == null) {
            return expandedList.get(key);
        }
        return ref;
    }

    public boolean inClassList(String x) {
        return classes.contains(x.toLowerCase());
    }

    public boolean isExpanded(String className) {
        return classExpanded.contains(className.toLowerCase());
    }

    public String linkify() {
        Tools5eSources sources = Tools5eSources.findSources(spellNode);
        String name = Tools5eIndexType.spell.decoratedName(spellNode);
        String resource = Tools5eQuteBase.fixFileName(name, sources);
        return "[%s](%s \"%s\")".formatted(name, resource, sources.primarySource());
    }

    @Override
    public String toString() {
        return "spellEntry[" + spellKey + ";l=" + level + ";classes=" + classes + ";classExp=" + classExpanded + "]";
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((spellKey == null) ? 0 : spellKey.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        SpellEntry other = (SpellEntry) obj;
        if (spellKey == null) {
            if (other.spellKey != null) {
                return false;
            }
        } else if (!spellKey.equals(other.spellKey)) {
            return false;
        }
        return true;
    }

    public static class SpellReference {
        final Tools5eIndexType refererType;
        final JsonNode refererNode;
        final String refererName;

        final String refererKey;
        final String classLevel;
        final String spellLevel;
        final String asLevel; // special case for known spells castable as cantrips
        final boolean expanded;

        public SpellReference(String key, boolean expanded) {
            this(key, "", null, expanded);
        }

        public SpellReference(String key, String constraint, String asLevel, boolean expanded) {
            this.refererKey = key;
            this.asLevel = asLevel;
            this.expanded = expanded;
            if (constraint.contains("_")) {
                this.classLevel = null;
                this.spellLevel = null;
            } else if (constraint.matches("^\\d+")) {
                this.classLevel = constraint;
                this.spellLevel = null;
            } else if (constraint.matches("^s\\d+")) {
                this.classLevel = null;
                this.spellLevel = constraint.substring(1);
            } else {
                if (isPresent(constraint)) {
                    Tui.instance().logf(Msg.UNKNOWN, "%s: Unknown constraint [%s]", key, constraint);
                }
                this.classLevel = null;
                this.spellLevel = null;
            }
            this.refererType = Tools5eIndexType.getTypeFromKey(key);
            this.refererNode = Tools5eIndex.getInstance().getOriginNoFallback(key);
            this.refererName = refererType.decoratedName(refererNode);
        }

        boolean isSpecific() {
            return classLevel != null
                    || spellLevel != null
                    || asLevel != null;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((refererKey == null) ? 0 : refererKey.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            SpellReference other = (SpellReference) obj;
            if (refererKey == null) {
                if (other.refererKey != null) {
                    return false;
                }
            } else if (!refererKey.equals(other.refererKey)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            return refererKey + ";c:" + classLevel + ";s:" + spellLevel + ";" + asLevel;
        }

        public String tagifyReference() {
            String type = refererType.name().replace("type", "");
            return Stream.of("spell", type, refererName)
                    .map(Tui::slugify)
                    .collect(Collectors.joining("/"));
        }

        public String listFileName() {
            return Tools5eQuteBase.getSpellList(refererName, Tools5eSources.findSources(refererNode));
        }

        public String linkifyReference() {
            Tools5eSources sources = Tools5eSources.findSources(refererNode);
            Tools5eIndex index = Tools5eIndex.getInstance();
            String name = refererType.decoratedName(refererNode);
            String resource = Tools5eQuteBase.getSpellList(name, sources);
            if (refererType == Tools5eIndexType.subclass) {
                String classKey = Tools5eIndexType.classtype.fromChildKey(refererKey);
                JsonNode classNode = index.getOriginNoFallback(classKey);
                String className = Tools5eIndexType.classtype.decoratedName(classNode);
                name = "%s (%s)".formatted(className, name);
            }
            Tools5eIndexType.spellIndex.defaultSourceString();
            return "[%s](%s%s/%s.md)".formatted(name,
                    Tools5eIndex.getInstance().compendiumVaultRoot(),
                    Tools5eIndexType.spellIndex.getRelativePath(),
                    resource);
        }

        public String describe() {
            List<String> append = new ArrayList<>();
            if (isPresent(asLevel)) {
                append.add("as " + JsonSource.spellLevelToText(asLevel));
            } else if (isPresent(spellLevel)) {
                String display = JsonSource.spellLevelToText(spellLevel);
                if ("cantrip".equals(display)) {
                    display = "cantrips";
                } else {
                    display += " spells";
                }
                append.add("with access to " + display);
            }
            if (isPresent(classLevel) && !"1".equals(classLevel)) {
                append.add("at class level " + classLevel);
            }
            return String.join(", ", append);
        }
    }
}
