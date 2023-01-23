package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.NodeReader;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellAmp;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellCasting;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellSaveDuration;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellSubclass;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellTarget;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class Json2QuteSpell extends Json2QuteBase {
    static final String SPELLS = "spells";

    public Json2QuteSpell(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode) {
        super(index, type, rootNode);
    }

    @Override
    public Pf2eQuteBase build() {
        List<String> tags = new ArrayList<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();

        appendEntryToText(text, Field.entries.getFrom(rootNode), "##");
        appendFootnotes(text, 0);

        List<String> traits = collectTraitsFrom(rootNode);

        boolean focus = SpellFields.focus.booleanOrDefault(rootNode, false);
        String level = SpellFields.level.getTextOrDefault(rootNode, "1");
        String type = "spell";
        if (traits.contains(cfg().traitTagOf("cantrip"))) {
            type = "cantrip";
            tags.add(cfg().tagOf(SPELLS, type));
        } else if (focus) {
            type = "focus";
            tags.add(cfg().tagOf(SPELLS, type, level));
        } else {
            tags.add(cfg().tagOf(SPELLS, "level", level));
        }

        // traditions --> map tradition to trait
        List<String> traditions = SpellFields.traditions.getListOfStrings(rootNode, tui()).stream()
                .sorted()
                .map(s -> linkify(Pf2eIndexType.trait, s))
                .collect(Collectors.toList());

        // domain --> link to deity|domain, replace (Apocryphal)
        List<String> domains = SpellFields.domain.getListOfStrings(rootNode, tui()).stream()
                .sorted()
                .map(s -> linkify(Pf2eIndexType.domain, s))
                .collect(Collectors.toList());

        // subclass --> link to subclass definition
        List<QuteSpellSubclass> subclass = null;
        JsonNode scNode = SpellFields.subclass.getFrom(rootNode);
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
            });
        }

        return new QuteSpell(sources, text, tags,
                level, toTitleCase(type),
                traits,
                transformListFrom(rootNode, Field.alias),
                getQuteSpellCasting(),
                getQuteSpellTarget(tags),
                getQuteSaveDuration(),
                domains,
                traditions,
                SpellFields.spellLists.getListOfStrings(rootNode, tui()),
                subclass,
                getHeightenedCast(),
                getAmpEffects());
    }

    QuteSpellCasting getQuteSpellCasting() {
        QuteSpellCasting quteCast = new QuteSpellCasting();

        NumberUnitEntry cast = SpellFields.cast.fieldFromTo(rootNode, NumberUnitEntry.class, tui());
        quteCast.cast = cast.convertToDurationString(this);

        quteCast.components = SpellFields.components.getNestedListOfStrings(rootNode, tui())
                .stream()
                .map(c -> Pf2eSpellComponent.valueFromEncoding(c).getRulesPath(cfg().rulesRoot()))
                .collect(Collectors.toList());

        quteCast.cost = transformTextFrom(rootNode, SpellFields.cost, ", ");
        quteCast.trigger = transformTextFrom(rootNode, SpellFields.trigger, ", ");
        quteCast.requirements = transformTextFrom(rootNode, Field.requirements, ", ");

        return quteCast;
    }

    QuteSpellSaveDuration getQuteSaveDuration() {
        JsonNode savingThrow = SpellFields.savingThrow.getFrom(rootNode);
        SpellDuration duration = SpellFields.duration.fieldFromTo(rootNode, SpellDuration.class, tui());
        if (savingThrow == null && duration == null) {
            return null;
        }
        QuteSpellSaveDuration saveDuration = new QuteSpellSaveDuration();

        boolean hidden = SpellFields.hidden.booleanOrDefault(savingThrow, false);
        String throwString = SpellFields.type.getListOfStrings(savingThrow, tui())
                .stream()
                .map(t -> Pf2eSavingThrowType.valueFromEncoding(t))
                .map(t -> toTitleCase(t.name()))
                .collect(Collectors.joining(" or "));
        if (!hidden && !throwString.isEmpty()) {
            saveDuration.basic = SpellFields.basic.booleanOrDefault(savingThrow, false);
            saveDuration.savingThrow = throwString;
        }
        if (duration != null) {
            saveDuration.duration = duration.convertToSpellDurationString(this);
        }

        return saveDuration;
    }

    QuteSpellTarget getQuteSpellTarget(List<String> tags) {
        String targets = replaceText(SpellFields.targets.getTextOrNull(rootNode));
        NumberUnitEntry range = SpellFields.range.fieldFromTo(rootNode, NumberUnitEntry.class, tui());
        SpellArea area = SpellFields.area.fieldFromTo(rootNode, SpellArea.class, tui());
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
            area.types.forEach(t -> tags.add(cfg().tagOf(SPELLS, "area", t)));
        }
        return spellTarget;
    }

    Map<String, String> getHeightenedCast() {
        JsonNode heightened = SpellFields.heightened.getFrom(rootNode);
        if (heightened == null) {
            return null;
        }
        Map<String, String> heightenedCast = new LinkedHashMap<>();
        heightened.fields().forEachRemaining(e -> {
            JsonNode plusX = SpellFields.plusX.getFrom(heightened);
            JsonNode X = SpellFields.X.getFrom(heightened);
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
        JsonNode ampNode = SpellFields.amp.getFrom(rootNode);
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

        JsonNode heightened = SpellFields.heightened.getFrom(ampNode);
        if (heightened != null) {
            amp.ampEffects = new LinkedHashMap<>();
            heightened.fields().forEachRemaining(e -> {
                JsonNode plusX = SpellFields.plusX.getFrom(heightened);
                JsonNode X = SpellFields.X.getFrom(heightened);
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

    enum SpellFields implements NodeReader {
        amp,
        area,
        basic,
        cast,
        components, // nested array
        cost,
        domain,
        duration,
        focus,
        heightened,
        hidden,
        level,
        plusX,
        primaryCheck, // ritual
        range,
        savingThrow,
        secondaryCasters, //ritual
        secondaryCheck, // ritual
        spellLists,
        subclass,
        targets,
        traditions,
        trigger,
        type,
        X;

        List<String> getNestedListOfStrings(JsonNode source, Tui tui) {
            JsonNode result = source.get(this.nodeName());
            if (result == null) {
                return List.of();
            } else if (result.isTextual()) {
                return List.of(result.asText());
            } else {
                JsonNode first = result.get(0);
                return getListOfStrings(first, tui);
            }
        }
    }
}
