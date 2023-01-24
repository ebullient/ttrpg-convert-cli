package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.NodeReader;
import dev.ebullient.convert.tools.pf2e.qute.QuteArchetype;
import dev.ebullient.convert.tools.pf2e.qute.QuteFeat;

public class Json2QuteArchetype extends Json2QuteBase {

    public Json2QuteArchetype(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode) {
        super(index, type, rootNode);
    }

    @Override
    public QuteArchetype build() {
        List<String> tags = new ArrayList<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();

        appendEntryToText(text, Field.entries.getFrom(rootNode), "##");
        appendFootnotes(text, 0);

        List<String> benefits = ArchetypeField.benefits.getListOfStrings(rootNode, tui());
        benefits.forEach(b -> tags.add(cfg().tagOf("archetype", "benefit", b)));

        List<String> extraFeats = new ArrayList<>();
        List<String> featNames = ArchetypeField.extraFeats.getListOfStrings(rootNode, tui());

        int dedicationLevel = ArchetypeField.dedicationLevel.intOrDefault(rootNode, 2);

        featNames.forEach(featKey -> {
            extraFeats.add(createExtraFeat(featKey));
        });

        return new QuteArchetype(sources, text, tags,
                collectTraitsFrom(rootNode),
                dedicationLevel,
                benefits,
                extraFeats);
    }

    String createExtraFeat(String featKey) {
        String[] parts = featKey.split("\\|");
        String key = Pf2eIndexType.feat.createKey(parts[1], parts[2]);

        JsonNode origFeat = index.getIncludedNode(key);
        if (origFeat == null && key.contains(" (")) {
            int pos = parts[1].indexOf(" (");
            key = Pf2eIndexType.feat.createKey(parts[1].substring(0, pos), parts[2]);
            origFeat = index.getIncludedNode(key);
        }
        if (origFeat == null) {
            tui().errorf("Could not find original feat matching %s", featKey);
            return null;
        }

        Json2QuteFeat json2Qute = new Json2QuteFeat(index, Pf2eIndexType.feat, origFeat);
        QuteFeat quteFeat = json2Qute.buildArchetype(sources.getName(), parts[0]);

        String rendered = tui().applyTemplate(quteFeat);
        int begin = rendered.indexOf("# ");

        List<String> inner = Stream.of(rendered.substring(begin).split("\n")).collect(Collectors.toList());
        inner.remove(0); // remove H1
        if (inner.get(0).startsWith("*")) {
            inner.remove(0);
        }

        inner.add(0, "collapse: closed");
        inner.add(0, String.format("title: %s, %sFeat %s*",
                quteFeat.getName(),
                "2".equals(parts[0]) ? "Dedication " : "",
                parts[0]));
        inner.add(0, "```ad-embed-feat");
        inner.add("```");

        return String.join("\n", inner);
    }

    enum ArchetypeField implements NodeReader {
        benefits,
        dedicationLevel,
        extraFeats,
        miscTags,
    }
}
