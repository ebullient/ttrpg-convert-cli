package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase;
import dev.ebullient.convert.tools.pf2e.qute.QuteRitual;
import dev.ebullient.convert.tools.pf2e.qute.QuteRitual.QuteRitualCasting;
import dev.ebullient.convert.tools.pf2e.qute.QuteRitual.QuteRitualChecks;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellTarget;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class Json2QuteRitual extends Json2QuteSpell {
    static final String RITUAL_TAG = "ritual";

    public Json2QuteRitual(Pf2eIndex index, JsonNode rootNode) {
        super(index, Pf2eIndexType.ritual, rootNode);
    }

    @Override
    protected Pf2eQuteBase buildQuteResource() {
        Set<String> tags = new TreeSet<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();

        appendEntryToText(text, Field.entries.getFrom(rootNode), "##");
        appendFootnotes(text, 0);

        String level = Pf2eSpell.level.getTextOrDefault(rootNode, "1");
        tags.add(cfg().tagOf(RITUAL_TAG, level));

        return new QuteRitual(sources, text, tags,
                level, "Ritual",
                collectTraitsFrom(rootNode, tags),
                Field.alias.replaceTextFromList(rootNode, this),
                getQuteRitualCast(),
                getQuteRitualChecks(),
                getQuteRitualSpellTarget(tags),
                Field.requirements.transformTextFrom(rootNode, ", ", this),
                null,
                getHeightenedCast());
    }

    QuteSpellTarget getQuteRitualSpellTarget(Collection<String> tags) {
        String targets = replaceText(Pf2eSpell.targets.getTextOrNull(rootNode));
        JsonNode rangeEntry = Pf2eSpell.range.getFieldFrom(rootNode, Field.entry);
        SpellArea area = Pf2eSpell.area.fieldFromTo(rootNode, SpellArea.class, tui());
        if (targets == null && rangeEntry == null && area == null) {
            return null;
        }

        QuteSpellTarget spellTarget = new QuteSpellTarget();
        if (targets != null) {
            spellTarget.targets = replaceText(targets);
        }
        if (rangeEntry != null) {
            spellTarget.range = replaceText(rangeEntry.asText());
        }
        if (area != null) {
            spellTarget.area = area.entry;
            area.types.forEach(t -> tags.add(cfg().tagOf(RITUAL_TAG, "area", t)));
        }
        return spellTarget;
    }

    QuteRitualCasting getQuteRitualCast() {
        NumberUnitEntry cast = Pf2eSpell.cast.fieldFromTo(rootNode, NumberUnitEntry.class, tui());
        RitualSecondaryCaster casters = Pf2eSpell.secondaryCasters.fieldFromTo(rootNode, RitualSecondaryCaster.class, tui());

        QuteRitualCasting quteCast = new QuteRitualCasting();
        quteCast.cast = cast.convertToDurationString(this);
        quteCast.cost = Pf2eSpell.cost.transformTextFrom(rootNode, ", ", this);
        if (casters != null) {
            quteCast.secondaryCasters = casters.buildString(this);
        }
        return quteCast;
    }

    QuteRitualChecks getQuteRitualChecks() {
        RitualCheck primary = Pf2eSpell.primaryCheck.fieldFromTo(rootNode, RitualCheck.class, tui());
        RitualCheck secondary = Pf2eSpell.secondaryCheck.fieldFromTo(rootNode, RitualCheck.class, tui());
        if (primary == null && secondary == null) {
            return null;
        }

        QuteRitualChecks checks = new QuteRitualChecks();

        checks.primaryChecks = primary.buildPrimaryString(this);
        if (secondary != null) {
            checks.secondaryChecks = secondary.buildSecondaryString(this);
        }

        return checks;
    }

    @RegisterForReflection
    public static class RitualSecondaryCaster {
        public String entry;
        public Integer number;
        public String note;

        public String buildString(JsonSource convert) {
            // ${ritual.secondaryCasters.entry ? ritual.secondaryCasters.entry       : ritual.secondaryCasters.number}
            // ${ritual.secondaryCasters.note  ? `, ${ritual.secondaryCasters.note}` : ""}
            return String.format("%s%s",
                    entry == null ? number : convert.replaceText(entry),
                    note == null ? "" : " " + note);
        }
    }

    @RegisterForReflection
    public static class RitualCheck {
        public String prof;
        public String entry;
        public List<String> skills;
        public List<String> mustBe;

        public String buildPrimaryString(JsonSource convert) {
            if (entry != null) {
                return convert.replaceText(entry);
            }
            return String.format("%s (%s%s)", skillsToString(convert), prof,
                    mustBe == null ? "" : String.format("; you must be a %s", convert.joinConjunct(" or ", mustBe)));
        }

        public String buildSecondaryString(JsonSource convert) {
            if (entry != null) {
                return convert.replaceText(entry);
            }
            return String.format("%s%s",
                    skillsToString(convert),
                    prof == null ? "" : String.format(" (%s)", prof));
        }

        String skillsToString(JsonSource convert) {
            List<String> converted = skills.stream()
                    .map(s -> convert.linkify(Pf2eIndexType.skill, s))
                    .collect(Collectors.toList());
            return convert.joinConjunct(" or ", converted);
        }
    }
}
