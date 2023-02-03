package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.pf2e.qute.QuteFeat;

public class Json2QuteFeat extends Json2QuteBase {

    public Json2QuteFeat(Pf2eIndex index, JsonNode rootNode) {
        super(index, Pf2eIndexType.feat, rootNode);
    }

    @Override
    protected QuteFeat buildQuteResource() {
        Set<String> tags = new TreeSet<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();

        appendEntryToText(text, Field.entries.getFrom(rootNode), "##");
        appendFootnotes(text, 0);

        NumberUnitEntry jsonActivity = Pf2eFeat.activity.fieldFromTo(rootNode, NumberUnitEntry.class, tui());

        List<String> leadsTo = Pf2eFeat.leadsTo.getListOfStrings(rootNode, tui())
                .stream()
                .map(x -> linkify(Pf2eIndexType.feat, x))
                .collect(Collectors.toList());

        return new QuteFeat(sources, text, tags,
                collectTraitsFrom(rootNode, tags),
                Field.alias.replaceTextFromList(rootNode, this),
                Pf2eFeat.level.getTextOrDefault(rootNode, "1"),
                Pf2eFeat.access.transformTextFrom(rootNode, ", ", this),
                getFrequency(rootNode),
                jsonActivity == null ? null : jsonActivity.toQuteActivity(this),
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
        List<String> tags = new ArrayList<>();

        String note = null;
        if (dedicationLevel != featLevel) {
            note = String.format(
                    "> [!note] This version of %s is intended for use with the %s Archetype. Its level has been changed accordingly.",
                    index.linkify(this.type, String.join("|", List.of(sources.getName(), sources.primarySource()))),
                    archetypeName);
        }

        appendEntryToText(text, Field.entries.getFrom(rootNode), "##");
        appendFootnotes(text, 0);

        NumberUnitEntry jsonActivity = Pf2eFeat.activity.fieldFromTo(rootNode, NumberUnitEntry.class, tui());

        return new QuteFeat(sources, text, tags,
                collectTraitsFrom(rootNode, tags),
                List.of(),
                dedicationLevel,
                Pf2eFeat.access.transformTextFrom(rootNode, ", ", this),
                getFrequency(rootNode),
                jsonActivity == null ? null : jsonActivity.toQuteActivity(this),
                Pf2eFeat.trigger.transformTextFrom(rootNode, ", ", this),
                Pf2eFeat.cost.transformTextFrom(rootNode, ", ", this),
                Field.requirements.transformTextFrom(rootNode, ", ", this),
                Pf2eFeat.prerequisites.transformTextFrom(rootNode, ", ", this),
                Pf2eFeat.special.transformTextFrom(rootNode, ", ", this),
                note, List.of(), true);
    }

}
