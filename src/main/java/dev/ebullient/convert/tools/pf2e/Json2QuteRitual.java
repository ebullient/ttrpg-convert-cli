package dev.ebullient.convert.tools.pf2e;

import static dev.ebullient.convert.StringUtil.joinConjunct;

import java.util.List;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataRange;
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
        String level = Pf2eSpell.level.getTextOrDefault(rootNode, "1");
        tags.add(RITUAL_TAG, level);

        return new QuteRitual(sources, entries, tags,
                level, "Ritual",
                traits,
                Field.alias.replaceTextFromList(rootNode, this),
                getQuteRitualCast(),
                getQuteRitualChecks(),
                getQuteRitualSpellTarget(),
                Field.requirements.transformTextFrom(rootNode, ", ", this),
                null,
                getHeightenedCast());
    }

    QuteSpellTarget getQuteRitualSpellTarget() {
        String targets = replaceText(Pf2eSpell.targets.getTextOrEmpty(rootNode));
        QuteDataRange range = Pf2eSpell.range.getRangeFrom(rootNode, this);
        SpellArea area = Pf2eSpell.area.fieldFromTo(rootNode, SpellArea.class, tui());
        if (targets == null && range == null && area == null) {
            return null;
        }

        QuteSpellTarget spellTarget = new QuteSpellTarget();
        if (targets != null) {
            spellTarget.targets = replaceText(targets);
        }
        spellTarget.range = range;
        if (area != null) {
            spellTarget.area = area.entry;
            area.types.forEach(t -> tags.add(RITUAL_TAG, "area", t));
        }
        return spellTarget;
    }

    QuteRitualCasting getQuteRitualCast() {
        RitualSecondaryCaster casters = Pf2eSpell.secondaryCasters.fieldFromTo(rootNode, RitualSecondaryCaster.class, tui());

        QuteRitualCasting quteCast = new QuteRitualCasting();
        quteCast.duration = Pf2eSpell.cast.getDurationFrom(rootNode, this);
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
                    mustBe == null ? "" : String.format("; you must be a %s", joinConjunct(" or ", mustBe)));
        }

        public String buildSecondaryString(JsonSource convert) {
            if (entry != null) {
                return convert.replaceText(entry);
            }
            return String.format("%s%s",
                    skillsToString(convert),
                    prof == null ? "" : String.format(" (%s)", prof));
        }

        // Compensate for Lore skills..
        // `${skill.includes("Lore") ? `${renderer.render(`{@skill Lore||${skill}}`)}`
        String skillsToString(JsonSource convert) {
            List<String> converted = skills.stream()
                    .map(s -> s.replaceAll("(.* Lore)", "Lore||$1"))
                    .map(s -> convert.linkify(Pf2eIndexType.skill, s))
                    .collect(Collectors.toList());
            return joinConjunct(" or ", converted);
        }
    }
}
