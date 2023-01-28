package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.NodeReader;
import dev.ebullient.convert.tools.pf2e.qute.QuteBackground;

public class Json2QuteBackground extends Json2QuteBase {

    public Json2QuteBackground(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode) {
        super(index, type, rootNode);
    }

    @Override
    protected QuteBackground buildQuteResource() {
        List<String> tags = new ArrayList<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();

        Pf2eBackground.boosts.getListOfStrings(rootNode, tui())
            .stream()
            .filter(b -> !b.equalsIgnoreCase("Free"))
            .forEach(s -> tags.add(cfg().tagOf("background", "boost", s)));

        Pf2eBackground.skills.getListOfStrings(rootNode, tui())
            .forEach(s -> tags.add(cfg().tagOf("background", "skill", s)));

            Pf2eBackground.feat.getListOfStrings(rootNode, tui())
            .forEach(s -> tags.add(cfg().tagOf("background", "feat", s)));

        appendEntryToText(text, rootNode, "##");

        return new QuteBackground(sources, text, tags);
    }

    enum Pf2eBackground implements NodeReader  {
        boosts,
        skills,
        feat,
    }
}
