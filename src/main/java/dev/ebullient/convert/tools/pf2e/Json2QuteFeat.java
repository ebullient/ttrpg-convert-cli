package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.pf2e.qute.QuteFeat;

public class Json2QuteFeat extends Json2QuteBase {

    public Json2QuteFeat(Pf2eIndex index, JsonNode rootNode) {
        super(index, Pf2eIndexType.feat, rootNode);
    }

    @Override
    protected QuteFeat buildQuteResource() {
        Tags tags = new Tags(sources);
        List<String> text = new ArrayList<>();

        appendToText(text, SourceField.entries.getFrom(rootNode), "##");

        List<String> leadsTo = Pf2eFeat.leadsTo.getListOfStrings(rootNode, tui())
                .stream()
                .map(x -> linkify(Pf2eIndexType.feat, x))
                .collect(Collectors.toList());

        return new QuteFeat(sources, text, tags,
                collectTraitsFrom(rootNode, tags),
                Field.alias.replaceTextFromList(rootNode, this),
                Pf2eFeat.level.getTextOrDefault(rootNode, "1"),
                Pf2eFeat.access.transformTextFrom(rootNode, ", ", this),
                Pf2eFeat.frequency.getFrequencyFrom(rootNode, this),
                Pf2eFeat.activity.getActivityFrom(rootNode, this),
                Pf2eFeat.trigger.transformTextFrom(rootNode, ", ", this),
                Pf2eFeat.cost.transformTextFrom(rootNode, ", ", this),
                Field.requirements.transformTextFrom(rootNode, ", ", this),
                Pf2eFeat.prerequisites.transformTextFrom(rootNode, ", ", this),
                Pf2eFeat.special.transformTextFrom(rootNode, "\n", this),
                null, leadsTo, true);
    }

    public QuteFeat buildArchetype(String archetypeName, String dedicationLevel) {
        String featLevel = Pf2eFeat.level.getTextOrDefault(rootNode, "1");
        List<String> text = new ArrayList<>();
        Tags tags = new Tags();

        String note = null;
        if (!Objects.equals(dedicationLevel, featLevel)) {
            note = String.format(
                    "> [!pf2-note] This version of %s is intended for use with the %s Archetype. Its level has been changed accordingly.",
                    index.linkify(this.type, String.join("|", List.of(sources.getName(), sources.primarySource()))),
                    archetypeName);
        }

        appendToText(text, SourceField.entries.getFrom(rootNode), "##");

        return new QuteFeat(sources, text, tags,
                collectTraitsFrom(rootNode, tags),
                List.of(),
                dedicationLevel,
                Pf2eFeat.access.transformTextFrom(rootNode, ", ", this),
                Pf2eFeat.frequency.getFrequencyFrom(rootNode, this),
                Pf2eFeat.activity.getActivityFrom(rootNode, this),
                Pf2eFeat.trigger.transformTextFrom(rootNode, ", ", this),
                Pf2eFeat.cost.transformTextFrom(rootNode, ", ", this),
                Field.requirements.transformTextFrom(rootNode, ", ", this),
                Pf2eFeat.prerequisites.transformTextFrom(rootNode, ", ", this),
                Pf2eFeat.special.transformTextFrom(rootNode, ", ", this),
                note, List.of(), true);
    }

    public enum Pf2eFeat implements Pf2eJsonNodeReader {
        access,
        activity,
        archetype, // child of featType
        cost,
        featType,
        frequency,
        leadsTo,
        level,
        prerequisites,
        special,
        trigger
    }
}
