package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.NodeReader;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteBase;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell;

public class Json2QuteSpell extends Json2QuteBase {

    public Json2QuteSpell(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode) {
        super(index, type, rootNode, Pf2eSources.findSources(rootNode));
    }

    @Override
    public Pf2eQuteBase build() {
        List<String> tags = new ArrayList<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();

        appendEntryToText(text, Field.entries.getFrom(rootNode), "##");

        List<String> traits = Field.traits.getListOfStrings(rootNode, tui()).stream()
                .sorted()
                .map(s -> linkify(Pf2eIndexType.trait, s))
                .collect(Collectors.toList());

        boolean focus = SpellFields.focus.booleanOrDefault(rootNode, false);
        String level = SpellFields.level.getTextOrDefault(rootNode, "1");
        String type = "spell";
        if (traits.contains(cfg().traitTagOf("cantrip"))) {
            type = "cantrip";
            tags.add(cfg().tagOf("spells", type));
        } else if (focus) {
            type = "focus";
            tags.add(cfg().tagOf("spells", type, level));
        } else {
            tags.add(cfg().tagOf("spells", level));
        }

        // traditions --> map tradition to trait
        List<String> traditions = SpellFields.traditions.getListOfStrings(rootNode, tui()).stream()
                .sorted()
                .map(s -> linkify(Pf2eIndexType.trait, s))
                .collect(Collectors.toList());

        // domain --> link to deity|domain, replace (Apocryphal)
        // subclass --> link to subclass definition
        // map abbreviated saving throw to full name

        return new QuteSpell(sources,
                String.join("\n", text), tags,
                level, toTitleCase(type),
                traits,
                transform(rootNode, Field.alias),
                null,
                null,
                null,
                null,
                traditions,
                null,
                null,
                null);
    }

    static class CastDuration {
        public int number;
        public String unit;
        public String entry;
    }

    enum SpellFields implements NodeReader {
        focus,
        level,
        traditions;
    }
}
