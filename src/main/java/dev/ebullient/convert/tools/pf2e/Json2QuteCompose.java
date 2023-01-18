package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.qute.QuteNote;

public class Json2QuteCompose extends Json2QuteBase {
    List<JsonNode> nodes = new ArrayList<>();
    Pf2eSources currentSources;
    String title;

    public Json2QuteCompose(Pf2eIndex index, String title) {
        super(index, Pf2eIndexType.skill, null,
                Pf2eSources.constructSyntheticSource(title));
        currentSources = super.getSources();
        this.title = title;
    }

    public void add(JsonNode node) {
        nodes.add(node);
    }

    @Override
    public Pf2eSources getSources() {
        return currentSources;
    }

    @Override
    public QuteBase build() {
        Set<String> tags = new HashSet<>();
        List<String> text = new ArrayList<>();

        nodes.sort(Comparator.comparing(Field.name::getTextOrEmpty));
        nodes.forEach(x -> appendElement(x, text, tags));

        return new QuteNote(sources,
                title,
                null,
                String.join("\n", text),
                tags);
    }

    private void appendElement(JsonNode entry, List<String> text, Set<String> tags) {
        String key = TtrpgValue.indexKey.getFromNode(entry);
        currentSources = Pf2eSources.findSources(key);
        String name = Field.name.getTextOrNull(entry);

        if (index.keyIsIncluded(key, entry)) {
            tags.addAll(currentSources.getSourceTags());
            maybeAddBlankLine(text);
            text.add("## " + replaceText(name));
            maybeAddBlankLine(text);
            appendEntryToText(text, Field.entries.getFrom(entry), "###");
            maybeAddBlankLine(text);
            text.add(String.format("_Source: %s_", currentSources.getSourceText()));
        }
    }
}
