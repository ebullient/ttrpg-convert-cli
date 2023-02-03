package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellAmp;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellCasting;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellSaveDuration;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellSubclass;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellTarget;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class Json2QuteSpell extends Json2QuteBase {
    static final String SPELL_TAG = "spell";

    public Json2QuteSpell(Pf2eIndex index, JsonNode rootNode) {
        this(index, Pf2eIndexType.spell, rootNode);
    }

    protected Json2QuteSpell(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode) {
        super(index, type, rootNode);
    }

    @Override
    protected Pf2eQuteBase buildQuteResource() {
        Set<String> tags = new TreeSet<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();

        appendEntryToText(text, Field.entries.getFrom(rootNode), "##");
        appendFootnotes(text, 0);

        Collection<String> traits = collectTraitsFrom(rootNode, tags);

        boolean focus = Pf2eSpell.focus.booleanOrDefault(rootNode, false);
        String level = Pf2eSpell.level.getTextOrDefault(rootNode, "1");
        String type = "spell";
        if (join("", traits).contains("cantrip")) {
            type = "cantrip";
            tags.add(cfg().tagOf(SPELL_TAG, type));
        } else if (focus) {
            type = "focus";
            tags.add(cfg().tagOf(SPELL_TAG, type, level));
        } else {
            tags.add(cfg().tagOf(SPELL_TAG, "level", level));
        }

        Pf2eIndexType.domain.getListOfStrings(rootNode, tui()).forEach(d -> tags.add(cfg().tagOf("domain", d, "spell")));

        // subclass --> link to subclass definition
        List<QuteSpellSubclass> subclass = new ArrayList<>();
        JsonNode scNode = Pf2eSpell.subclass.getFrom(rootNode);
        if (scNode != null) {
            scNode.fields().forEachRemaining(e -> {
                QuteSpellSubclass sc = new QuteSpellSubclass();
                String[] parts = e.getKey().split("\\|");
                sc.category = parts[0];

                String value = e.getValue().asText();
                String[] vParts = value.split("\\|");

                // Construct a proper subclass link. ugh.
                sc.text = linkify(Pf2eIndexType.classtype,
                        String.format("%s|%s|%s|%s",
                                parts[1],
                                parts.length > 2 ? parts[2] : Pf2eIndexType.classtype.defaultSource(),
                                vParts[0].toLowerCase(),
                                value));

                subclass.add(sc);
            });
        }

        return new QuteSpell(sources, text, tags,
                level, toTitleCase(type),
                traits,
                Field.alias.replaceTextFromList(rootNode, this),
                getQuteSpellCasting(),
                getQuteSpellTarget(tags),
                getQuteSaveDuration(),
                Pf2eSpell.domains.linkifyListFrom(rootNode, Pf2eIndexType.domain, this),
                Pf2eSpell.traditions.linkifyListFrom(rootNode, Pf2eIndexType.trait, this),
                Pf2eSpell.spellLists.getListOfStrings(rootNode, tui()),
                subclass,
                getHeightenedCast(),
                getAmpEffects());
    }

    QuteSpellCasting getQuteSpellCasting() {
        QuteSpellCasting quteCast = new QuteSpellCasting();

        NumberUnitEntry cast = Pf2eSpell.cast.fieldFromTo(rootNode, NumberUnitEntry.class, tui());
        quteCast.cast = cast.convertToDurationString(this);

        quteCast.components = Pf2eSpell.components.getNestedListOfStrings(rootNode, tui())
                .stream()
                .map(c -> Pf2eSpellComponent.valueFromEncoding(c).getRulesPath(cfg().rulesVaultRoot()))
                .collect(Collectors.toList());

        quteCast.cost = Pf2eSpell.cost.transformTextFrom(rootNode, ", ", this);
        quteCast.trigger = Pf2eSpell.trigger.transformTextFrom(rootNode, ", ", this);
        quteCast.requirements = Field.requirements.transformTextFrom(rootNode, ", ", this);

        return quteCast;
    }

    QuteSpellSaveDuration getQuteSaveDuration() {
        JsonNode savingThrow = Pf2eSpell.savingThrow.getFrom(rootNode);
        SpellDuration duration = Pf2eSpell.duration.fieldFromTo(rootNode, SpellDuration.class, tui());
        if (savingThrow == null && duration == null) {
            return null;
        }
        QuteSpellSaveDuration saveDuration = new QuteSpellSaveDuration();

        boolean hidden = Pf2eSpell.hidden.booleanOrDefault(savingThrow, false);
        String throwString = Pf2eSpell.type.getListOfStrings(savingThrow, tui())
                .stream()
                .map(t -> Pf2eSavingThrowType.valueFromEncoding(t))
                .map(t -> toTitleCase(t.name()))
                .collect(Collectors.joining(" or "));
        if (!hidden && !throwString.isEmpty()) {
            saveDuration.basic = Pf2eSpell.basic.booleanOrDefault(savingThrow, false);
            saveDuration.savingThrow = throwString;
        }
        if (duration != null) {
            saveDuration.duration = duration.convertToSpellDurationString(this);
        }

        return saveDuration;
    }

    QuteSpellTarget getQuteSpellTarget(Collection<String> tags) {
        String targets = replaceText(Pf2eSpell.targets.getTextOrNull(rootNode));
        NumberUnitEntry range = Pf2eSpell.range.fieldFromTo(rootNode, NumberUnitEntry.class, tui());
        SpellArea area = Pf2eSpell.area.fieldFromTo(rootNode, SpellArea.class, tui());
        if (targets == null && area == null && range == null) {
            return null;
        }
        QuteSpellTarget spellTarget = new QuteSpellTarget();
        if (targets != null) {
            spellTarget.targets = replaceText(targets);
        }
        if (range != null) {
            spellTarget.range = range.convertToRangeString(this);
        }
        if (area != null) {
            spellTarget.area = area.entry;
            area.types.forEach(t -> tags.add(cfg().tagOf(SPELL_TAG, "area", t)));
        }
        return spellTarget;
    }

    Map<String, String> getHeightenedCast() {
        JsonNode heightened = Pf2eSpell.heightened.getFrom(rootNode);
        if (heightened == null) {
            return null;
        }
        Map<String, String> heightenedCast = new LinkedHashMap<>();
        heightened.fields().forEachRemaining(e -> {
            JsonNode plusX = Pf2eSpell.plusX.getFrom(heightened);
            JsonNode X = Pf2eSpell.X.getFrom(heightened);
            if (plusX != null) {
                plusX.fields().forEachRemaining(x -> {
                    heightenedCast.put(
                            String.format("Heightened (+ %s)", x.getKey()),
                            getHeightenedValue(x.getValue()));
                });
            }
            if (X != null) {
                X.fields().forEachRemaining(x -> {
                    heightenedCast.put(
                            String.format("Heightened (%s)", getOrdinalForm(x.getKey())),
                            getHeightenedValue(x.getValue()));
                });
            }
        });
        return heightenedCast;
    }

    String getHeightenedValue(JsonNode value) {
        List<String> inner = new ArrayList<>();
        appendEntryToText(inner, value, null);
        return String.join("\n", inner);
    }

    QuteSpellAmp getAmpEffects() {
        JsonNode ampNode = Pf2eSpell.amp.getFrom(rootNode);
        if (ampNode == null) {
            return null;
        }

        QuteSpellAmp amp = new QuteSpellAmp();

        List<String> inner = new ArrayList<>();
        appendEntryToText(inner, Field.entries.getFrom(ampNode), null);
        appendEntryToText(inner, Field.entry.getFrom(ampNode), null);
        if (!inner.isEmpty()) {
            amp.text = String.join("\n", inner);
        }

        JsonNode heightened = Pf2eSpell.heightened.getFrom(ampNode);
        if (heightened != null) {
            amp.ampEffects = new LinkedHashMap<>();
            heightened.fields().forEachRemaining(e -> {
                JsonNode plusX = Pf2eSpell.plusX.getFrom(heightened);
                JsonNode X = Pf2eSpell.X.getFrom(heightened);
                if (plusX != null) {
                    plusX.fields().forEachRemaining(x -> {
                        amp.ampEffects.put(
                                String.format("Amp Heightened (+ %s)", x.getKey()),
                                getHeightenedValue(x.getValue()));
                    });
                }
                if (X != null) {
                    X.fields().forEachRemaining(x -> {
                        amp.ampEffects.put(
                                String.format("Amp Heightened (%s)", getOrdinalForm(x.getKey())),
                                getHeightenedValue(x.getValue()));
                    });
                }
            });
        }

        return amp;
    }

    @RegisterForReflection
    static class SpellDuration extends NumberUnitEntry {
        public Boolean sustained;
        public Boolean dismiss;

        public String convertToSpellDurationString(JsonSource convert) {
            if (entry != null) {
                return convert.replaceText(entry);
            }
            boolean isSustained = sustained != null && sustained;
            if (isSustained && "unlimited".equals(unit)) {
                return "sustained";
            }
            String rendered = number == null
                    ? ""
                    : String.format("%s %s%s", number, unit, (number > 1 ? "s" : ""));

            return isSustained
                    ? "sustained up to " + rendered
                    : rendered;
        }
    }

    @RegisterForReflection
    static class SpellArea {
        public List<String> types;
        public String entry;
    }

}
