package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

        List<String> leadsTo = FeatField.leadsTo.getListOfStrings(rootNode, tui())
                .stream()
                .map(x -> linkify(Pf2eIndexType.feat, x))
                .collect(Collectors.toList());

        return new QuteFeat(sources, text, tags,
                collectTraitsFrom(rootNode),
                transformListFrom(rootNode, Field.alias),
                FeatField.level.getTextOrDefault(rootNode, "1"),
                transformTextFrom(rootNode, FeatField.access, ", "),
                getFrequency(rootNode),
                transformTextFrom(rootNode, FeatField.trigger, ", "),
                transformTextFrom(rootNode, FeatField.cost, ", "),
                transformTextFrom(rootNode, Field.requirements, ", "),
                transformTextFrom(rootNode, FeatField.prerequisites, ", "),
                leadsTo);
    }

    enum FeatField implements NodeReader {
        access,
        cost,
        leadsTo,
        level,
        prerequisites,
        trigger,
        ;
    }
}
