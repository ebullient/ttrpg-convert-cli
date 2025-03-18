package dev.ebullient.convert.tools.pf2e;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.pf2e.qute.QuteBackground;

public class Json2QuteBackground extends Json2QuteBase {

    public Json2QuteBackground(Pf2eIndex index, JsonNode rootNode) {
        super(index, Pf2eIndexType.background, rootNode);
    }

    @Override
    protected QuteBackground buildQuteResource() {
        Pf2eBackground.boosts.getListOfStrings(rootNode, tui())
                .stream()
                .filter(b -> !b.equalsIgnoreCase("Free"))
                .forEach(s -> tags.add("background", "boost", s));

        Pf2eBackground.skills.getListOfStrings(rootNode, tui())
                .forEach(s -> tags.add("background", "skill", s));

        Pf2eBackground.feat.getListOfStrings(rootNode, tui())
                .forEach(s -> tags.add("background", "feat", s));

        return new QuteBackground(sources, entries, tags);
    }

    enum Pf2eBackground implements Pf2eJsonNodeReader {
        boosts,
        skills,
        feat,
    }
}
