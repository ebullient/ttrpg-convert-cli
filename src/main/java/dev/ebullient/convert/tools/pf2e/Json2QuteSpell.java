package dev.ebullient.convert.tools.pf2e;

import static dev.ebullient.convert.StringUtil.toOrdinal;
import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataDuration;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataTimedDuration;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellAmp;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellCasting;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellDuration;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellSave;
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
        Tags tags = new Tags(sources);
        List<String> text = new ArrayList<>();

        appendToText(text, SourceField.entries.getFrom(rootNode), "##");

        Collection<String> traits = collectTraitsFrom(rootNode, tags);

        boolean focus = Pf2eSpell.focus.booleanOrDefault(rootNode, false);
        String level = Pf2eSpell.level.getTextOrDefault(rootNode, "1");
        String type = "spell";
        if (join("", traits).contains("cantrip")) {
            type = "cantrip";
            tags.add(SPELL_TAG, type);
        } else if (focus) {
            type = "focus";
            tags.add(SPELL_TAG, type, level);
        } else {
            tags.add(SPELL_TAG, "level", level);
        }

        Pf2eIndexType.domain.getListOfStrings(rootNode, tui()).forEach(d -> tags.add("domain", d, "spell"));

        // subclass --> link to subclass definition
        NamedText.SortedBuilder namedText = new NamedText.SortedBuilder();
        JsonNode scNode = Pf2eSpell.subclass.getFrom(rootNode);
        if (scNode != null) {
            for (Entry<String, JsonNode> e : iterableFields(scNode)) {
                String[] parts = e.getKey().split("\\|");
                String name = parts[0];

                String value = e.getValue().asText();
                String[] vParts = value.split("\\|");

                // Construct a proper subclass link. ugh.
                String desc = linkify(Pf2eIndexType.classtype,
                        String.format("%s|%s|%s|%s",
                                parts[1],
                                parts.length > 2 ? parts[2] : Pf2eIndexType.classtype.defaultSource(),
                                vParts[0].toLowerCase(),
                                value));

                namedText.add(name, desc);
            }
            ;
        }

        return new QuteSpell(sources, text, tags,
                level, toTitleCase(type),
                traits,
                Field.alias.replaceTextFromList(rootNode, this),
                getQuteSpellCasting(),
                getQuteSpellTarget(tags),
                Pf2eSpell.savingThrow.getSpellSaveFrom(rootNode, this),
                Pf2eSpell.duration.getSpellDurationFrom(rootNode, this),
                Pf2eSpell.domains.linkifyListFrom(rootNode, Pf2eIndexType.domain, this),
                Pf2eSpell.traditions.linkifyListFrom(rootNode, Pf2eIndexType.trait, this),
                Pf2eSpell.spellLists.getListOfStrings(rootNode, tui()),
                namedText.build(),
                getHeightenedCast(),
                getAmpEffects());
    }

    QuteSpellCasting getQuteSpellCasting() {
        QuteSpellCasting quteCast = new QuteSpellCasting();

        quteCast.components = Pf2eSpell.components.getNestedListOfStrings(rootNode, tui())
                .stream()
                .map(c -> Pf2eSpellComponent.valueFromEncoding(c).getRulesPath(cfg().rulesVaultRoot()))
                .collect(Collectors.toList());

        quteCast.duration = Pf2eSpell.cast.getDurationFrom(rootNode, this);
        quteCast.cost = Pf2eSpell.cost.transformTextFrom(rootNode, ", ", this);
        quteCast.trigger = Pf2eSpell.trigger.transformTextFrom(rootNode, ", ", this);
        quteCast.requirements = Field.requirements.transformTextFrom(rootNode, ", ", this);

        return quteCast;
    }

    QuteSpellTarget getQuteSpellTarget(Tags tags) {
        String targets = replaceText(Pf2eSpell.targets.getTextOrEmpty(rootNode));
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
            area.types.forEach(t -> tags.add(SPELL_TAG, "area", t));
        }
        return spellTarget;
    }

    Collection<NamedText> getHeightenedCast() {
        JsonNode heightened = Pf2eSpell.heightened.getFrom(rootNode);
        if (heightened == null) {
            return null;
        }
        NamedText.SortedBuilder namedText = new NamedText.SortedBuilder();
        JsonNode plusX = Pf2eSpell.plusX.getFrom(heightened);
        JsonNode X = Pf2eSpell.X.getFrom(heightened);
        if (plusX != null) {
            plusX.fields().forEachRemaining(x -> {
                namedText.add(
                        String.format("Heightened (+ %s)", x.getKey()),
                        getHeightenedValue(x.getValue()));
            });
        }
        if (X != null) {
            X.fields().forEachRemaining(x -> {
                namedText.add(
                        String.format("Heightened (%s)", toOrdinal(x.getKey())),
                        getHeightenedValue(x.getValue()));
            });
        }
        return namedText.build();
    }

    String getHeightenedValue(JsonNode value) {
        List<String> inner = new ArrayList<>();
        appendToText(inner, value, null);
        return String.join("\n", inner);
    }

    QuteSpellAmp getAmpEffects() {
        JsonNode ampNode = Pf2eSpell.amp.getFrom(rootNode);
        if (ampNode == null) {
            return null;
        }

        QuteSpellAmp amp = new QuteSpellAmp();

        List<String> inner = new ArrayList<>();
        appendToText(inner, SourceField.entries.getFrom(ampNode), null);
        appendToText(inner, SourceField.entry.getFrom(ampNode), null);
        if (!inner.isEmpty()) {
            amp.text = String.join("\n", inner);
        }

        JsonNode heightened = Pf2eSpell.heightened.getFrom(ampNode);
        if (heightened != null) {
            NamedText.SortedBuilder namedText = new NamedText.SortedBuilder();
            JsonNode plusX = Pf2eSpell.plusX.getFrom(heightened);
            JsonNode X = Pf2eSpell.X.getFrom(heightened);
            if (plusX != null) {
                plusX.fields().forEachRemaining(x -> {
                    namedText.add(
                            String.format("Amp Heightened (+ %s)", x.getKey()),
                            getHeightenedValue(x.getValue()));
                });
            }
            if (X != null) {
                X.fields().forEachRemaining(x -> {
                    namedText.add(
                            String.format("Amp Heightened (%s)", toOrdinal(x.getKey())),
                            getHeightenedValue(x.getValue()));
                });
            }
            amp.ampEffects = namedText.build();
        }

        return amp;
    }

    public enum Pf2eSpell implements Pf2eJsonNodeReader {
        amp,
        area,
        basic,
        cast,
        components, // nested array
        cost,
        dismiss,
        domains,
        duration,
        focus,
        heightened,
        hidden,
        level,
        plusX, // heightened
        primaryCheck, // ritual
        range,
        savingThrow,
        secondaryCasters, //ritual
        secondaryCheck, // ritual
        spellLists,
        subclass,
        sustained,
        targets,
        traditions,
        trigger,
        type,
        X; // heightened

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

        private QuteSpellSave getSpellSaveFrom(JsonNode source, JsonSource convert) {
            JsonNode saveNode = getFromOrEmptyObjectNode(source);
            List<String> saves = type.getListOfStrings(saveNode, convert.tui())
                    .stream()
                    .map(s -> switch (s.charAt(0)) {
                        case 'F' -> "Fortitude";
                        case 'R' -> "Reflex";
                        case 'W' -> "Will";
                        default -> null;
                    })
                    .filter(Objects::nonNull).toList();
            return saves.isEmpty() ? null
                    : new QuteSpellSave(
                            saves, basic.booleanOrDefault(saveNode, false),
                            hidden.booleanOrDefault(saveNode, false));
        }

        private QuteSpellDuration getSpellDurationFrom(JsonNode source, JsonSource convert) {
            if (!isObjectIn(source)) {
                return null;
            }
            JsonNode node = getFrom(source);
            QuteDataDuration quteDuration = duration.getDurationFrom(source, convert);
            if (quteDuration != null && quteDuration.isActivity()) {
                convert.tui().errorf("Got activity as a spell duration from %s", source.toPrettyString());
            }
            return new QuteSpellDuration(
                    (QuteDataTimedDuration) quteDuration,
                    sustained.booleanOrDefault(node, false),
                    dismiss.booleanOrDefault(node, false));
        }
    }

    @RegisterForReflection
    static class SpellArea {
        public List<String> types;
        public String entry;
    }

}
