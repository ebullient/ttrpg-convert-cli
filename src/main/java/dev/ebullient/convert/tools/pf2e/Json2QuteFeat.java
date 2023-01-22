package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.NodeReader;
import dev.ebullient.convert.tools.pf2e.qute.QuteFeat;

public class Json2QuteFeat extends Json2QuteBase {

    public Json2QuteFeat(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode) {
        super(index, type, rootNode);
    }

    @Override
    public QuteFeat build() {
        List<String> tags = new ArrayList<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();

        appendEntryToText(text, Field.entries.getFrom(rootNode), "##");
        appendFootnotes(text, 0);

        return new QuteFeat(sources, String.join("\n", text), tags,
                collectTraits(),
                transformListFrom(rootNode, Field.alias),
                FeatFields.level.getTextOrDefault(rootNode, "1"),
                transformTextFrom(rootNode, FeatFields.access, ", "),
                getFrequency(rootNode),
                transformTextFrom(rootNode, Field.trigger, ", "),
                transformTextFrom(rootNode, Field.cost, ", "),
                transformTextFrom(rootNode, Field.requirements, ", "),
                transformTextFrom(rootNode, Field.prerequisites, ", "),
                transformListFrom(rootNode, FeatFields.leadsTo));
    }

    enum FeatFields implements NodeReader {
        access,
        leadsTo,
        level;
    }
}
