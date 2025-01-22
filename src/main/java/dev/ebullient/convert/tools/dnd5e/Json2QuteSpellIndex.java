package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import dev.ebullient.convert.qute.QuteNote;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteNote;

/**
 * Read the spell index: create a variety of lists and indexes for spells
 */
public class Json2QuteSpellIndex extends Json2QuteCommon {
    static final List<String> SPELL_LEVELS = List.of(
            "Cantrip", "1st Level", "2nd Level", "3rd Level", "4th Level", "5th Level", "6th Level", "7th Level", "8th Level",
            "9th Level");

    final SpellIndex spellIndex;

    Json2QuteSpellIndex(Tools5eIndex index) {
        super(index, Tools5eIndexType.spellIndex, null);
        this.spellIndex = index.getSpellIndex();
    }

    public Collection<? extends QuteNote> buildNotes() {
        List<QuteNote> notes = new ArrayList<>();

        SpellByLevel spellsByLevel = new SpellByLevel();
        Map<String, SpellByLevel> spellsByClass = new HashMap<>();
        Map<String, SpellRefByLevel> spellsByOther = new HashMap<>();
        Map<SpellSchool, SpellByLevel> spellsBySchool = new HashMap<>();

        // Spells by all the things.
        for (var entry : spellIndex.spellsByKey.values()) {
            spellsByLevel.add(entry);
            spellsBySchool.computeIfAbsent(entry.school, k -> new SpellByLevel()).add(entry);

            for (var name : entry.classes) {
                spellsByClass.computeIfAbsent(name, k -> new SpellByLevel()).add(entry);
            }

            for (var ref : entry.references.values()) {
                if (ref.refererType == Tools5eIndexType.classtype) {
                    continue;
                }
                spellsByOther.computeIfAbsent(ref.refererKey, k -> new SpellRefByLevel(ref))
                        .add(entry);
            }

            for (var ref : entry.expandedList.values()) {
                if (ref.refererType == Tools5eIndexType.classtype) {
                    continue;
                }
                spellsByOther.computeIfAbsent(ref.refererKey, k -> new SpellRefByLevel(ref))
                        .add(entry);
            }
        }

        // Create school spell list
        for (var schoolList : spellsBySchool.entrySet()) {
            QuteNote note = createSchoolList(schoolList.getKey(), schoolList.getValue());
            if (note != null) {
                notes.add(note);
            }
        }

        // Create class spell list
        for (var classList : spellsByClass.entrySet()) {
            QuteNote note = createClassList(classList.getKey(), classList.getValue());
            if (note != null) {
                notes.add(note);
            }
        }

        // Create other spell list
        for (var otherList : spellsByOther.entrySet()) {
            QuteNote note = createOtherList(otherList.getKey(), otherList.getValue());
            if (note != null) {
                notes.add(note);
            }
        }

        return notes;
    }

    private QuteNote createClassList(String className, SpellByLevel spellsByClass) {
        if (spellsByClass.isEmpty()) {
            return null;
        }

        Tags tags = new Tags();
        tags.add("spell", "list", "class", className);
        List<String> text = new ArrayList<>();

        for (int i = 0; i < SPELL_LEVELS.size(); i++) {
            Set<SpellEntry> levelSpells = spellsByClass.getLevel(String.valueOf(i));
            if (levelSpells.isEmpty()) {
                continue;
            }
            maybeAddBlankLine(text);
            String levelHeading = SPELL_LEVELS.get(i);
            text.add("## " + levelHeading);
            text.add("");
            for (var entry : levelSpells) {
                text.add("- " + entry.linkify() + (entry.isExpanded(className) ? " (\\*)" : ""));
            }
        }
        maybeAddBlankLine(text);

        return new Tools5eQuteNote(toTitleCase(className) + " Spells", "", text, tags)
                .withTargetFile(Tools5eQuteBase.getClassSpellList(className))
                .withTargetPath(Tools5eIndexType.spellIndex.getRelativePath());
    }

    private QuteNote createSchoolList(SpellSchool spellSchool, SpellByLevel spellsByClass) {
        if (spellsByClass.isEmpty()) {
            return null;
        }

        Tags tags = new Tags();
        tags.add("spell", "list", "school", spellSchool.name());
        List<String> text = new ArrayList<>();

        for (int i = 0; i < SPELL_LEVELS.size(); i++) {
            Set<SpellEntry> levelSpells = spellsByClass.getLevel(String.valueOf(i));
            if (levelSpells.isEmpty()) {
                continue;
            }
            maybeAddBlankLine(text);
            String levelHeading = SPELL_LEVELS.get(i);
            text.add("## " + levelHeading);
            text.add("");
            for (var entry : levelSpells) {
                text.add("- " + entry.linkify());
            }
        }
        maybeAddBlankLine(text);

        return new Tools5eQuteNote(spellSchool.name() + " Spells", "", text, tags)
                .withTargetFile("list-spells-school-" + spellSchool.name())
                .withTargetPath(Tools5eIndexType.spellIndex.getRelativePath());
    }

    private QuteNote createOtherList(String key, SpellRefByLevel spellsByOther) {
        if (spellsByOther.isEmpty()) {
            return null;
        }
        Tools5eIndexType type = Tools5eIndexType.getTypeFromKey(key);
        String name = type.decoratedName(spellsByOther.reference.refererNode);

        Tags tags = new Tags();
        tags.add("spell", "list", type.name(), name);
        List<String> text = new ArrayList<>();

        for (int i = 0; i < SPELL_LEVELS.size(); i++) {
            Set<SpellEntry> levelSpells = spellsByOther.getLevel(String.valueOf(i));
            if (levelSpells.isEmpty()) {
                continue;
            }
            maybeAddBlankLine(text);
            String levelHeading = SPELL_LEVELS.get(i);
            text.add("## " + levelHeading);
            text.add("");
            for (var entry : levelSpells) {
                text.add("- " + entry.linkify() + " " + spellsByOther.reference.describe());
            }
        }
        maybeAddBlankLine(text);

        return new Tools5eQuteNote("Spells for " + name, "", text, tags)
                .withTargetFile(spellsByOther.reference.listFileName())
                .withTargetPath(Tools5eIndexType.spellIndex.getRelativePath());
    }

    private class SpellRefByLevel extends SpellByLevel {
        final SpellEntry.SpellReference reference;

        SpellRefByLevel(SpellEntry.SpellReference reference) {
            this.reference = reference;
        }

        boolean isEmpty() {
            return spellsByLevel.isEmpty();
        }
    }

    private class SpellByLevel {
        final Map<String, Set<SpellEntry>> spellsByLevel = new HashMap<>();

        void add(SpellEntry entry) {
            spellsByLevel.computeIfAbsent(entry.getLevel(),
                    k -> new TreeSet<>(Comparator.comparing(SpellEntry::getName))).add(entry);
        }

        Set<SpellEntry> getLevel(String level) {
            Set<SpellEntry> spells = spellsByLevel.get(level);
            return spells == null ? Set.of() : spells;
        }

        boolean isEmpty() {
            return spellsByLevel.isEmpty();
        }
    }

}
