package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools spell attributes ({@code spell2md.txt})
 *
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase}.
 */
@TemplateData
public class QuteSpell extends Tools5eQuteBase {

    /** Spell level */
    public final String level;
    /** Spell school */
    public final String school;
    /** true for ritual spells */
    public final boolean ritual;
    /** Formatted: casting time */
    public final String time;
    /** Formatted: spell range */
    public final String range;
    /** Formatted: spell components */
    public final String components;
    /** Formatted: spell range */
    public final String duration;
    /** Formatted: Ability checks */
    public final String abilityChecks;
    /** Formatted: Creature types */
    public final String affectsCreatureTypes;
    /** Formatted/mapped: Areas */
    public final String areaTags;
    /** Formatted: Condition immunities */
    public final String conditionImmune;
    /** Formatted: Conditions */
    public final String conditionInflict;
    /** Formatted: Damage immunities */
    public final String damageImmune;
    /** Formatted: Damage types */
    public final String damageInflict;
    /** Formatted: Damage resistances */
    public final String damageResist;
    /** Formatted: Damage vulnerabilities */
    public final String damageVulnerable;
    /** Formatted/mapped: Misc tags */
    public final String miscTags;
    /** Formatted: Saving throws */
    public final String savingThrows;
    /** Formatted: Scaling damage dice entries */
    public final String scalingLevelDice;
    /** Formatted: Spell attack forms */
    public final String spellAttacks;
    /** At higher levels text */
    public final String higherLevels;
    /** String: rendered list of links to classes that grant access to this spell. May be incomplete or empty. */
    public final String backgrounds;
    /** String: rendered list of links to classes that can use this spell. May be incomplete or empty. */
    public final String classes;
    /** String: rendered list of links to feats that grant acccess to this spell. May be incomplete or empty. */
    public final String feats;
    /** String: rendered list of links to optional features that grant access to this spell. May be incomplete or empty. */
    public final String optionalfeatures;
    /** String: rendered list of links to races that can use this spell. May be incomplete or empty. */
    public final String races;
    /** List of links to resources (classes, subclasses, feats, etc.) that have access to this spell */
    public final Collection<String> references;

    public QuteSpell(Tools5eSources sources, String name, String source, String level,
            String school, boolean ritual, String time, String range,
            String components, String duration,
            String abilityChecks, String affectsCreatureTypes,
            String areaTags, String conditionImmune,
            String conditionInflict, String damageImmune,
            String damageInflict, String damageResist,
            String damageVulnerable, String miscTags,
            String savingThrows, String scalingLevelDice,
            String spellAttacks,
            String higherLevels, Collection<String> references, List<ImageRef> images, String text, Tags tags) {
        super(sources, name, source, images, text, tags);

        this.level = level;
        this.school = school;
        this.ritual = ritual;
        this.time = time;
        this.range = range;
        this.components = components;
        this.duration = duration;
        this.abilityChecks = abilityChecks;
        this.affectsCreatureTypes = affectsCreatureTypes;
        this.areaTags = areaTags;
        this.conditionImmune = conditionImmune;
        this.conditionInflict = conditionInflict;
        this.damageImmune = damageImmune;
        this.damageInflict = damageInflict;
        this.damageResist = damageResist;
        this.damageVulnerable = damageVulnerable;
        this.miscTags = miscTags;
        this.savingThrows = savingThrows;
        this.scalingLevelDice = scalingLevelDice;
        this.spellAttacks = spellAttacks;
        this.higherLevels = higherLevels == null || higherLevels.isBlank() ? null : higherLevels;
        this.references = references;
        this.backgrounds = references.stream()
                .filter(s -> s.contains("background"))
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));
        this.classes = references.stream()
                .filter(s -> s.contains("class"))
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));
        this.feats = references.stream()
                .filter(s -> s.contains("feat"))
                .filter(s -> !s.contains("optional-feature"))
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));
        this.optionalfeatures = references.stream()
                .filter(s -> s.contains("optional-feature"))
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));
        this.races = references.stream()
                .filter(s -> s.contains("race"))
                .distinct()
                .sorted()
                .collect(Collectors.joining("; "));
    }

    /** List of class names (not links) that can use this spell. */
    public List<String> getClassList() {
        return references == null || references.isEmpty()
                ? List.of()
                : references.stream()
                        .filter(s -> s.contains("class"))
                        .map(s -> s.replaceAll("\\[(.*?)\\].*", "$1"))
                        .distinct()
                        .sorted()
                        .toList();
    }
}
