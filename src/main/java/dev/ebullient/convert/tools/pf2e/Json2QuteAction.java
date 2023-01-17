package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.tools.pf2e.qute.QuteAction;

public class Json2QuteAction extends Json2QuteBase {

    public Json2QuteAction(Pf2eIndex index, Pf2eIndexType type, JsonNode node) {
        super(index, type, node, Pf2eSources.findSources(node));
    }

    public QuteBase build() {
        List<String> tags = new ArrayList<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();

        appendEntryToText(text, Field.entries.getFrom(rootNode), "##");

        if (text.isEmpty()) {
            return null;
        }

        return new QuteAction(
                getSources(),
                getSources().getName(),
                getSources().getSourceText(),
                String.join("\n", text),
                tags);
    }
}
