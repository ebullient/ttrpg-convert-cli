package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteNote;

public class Json2QuteTable extends Json2QuteBase {

    public Json2QuteTable(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode) {
        super(index, type, rootNode);
    }

    @Override
    public Pf2eQuteNote build() {
        List<String> tags = new ArrayList<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();

        ((ObjectNode) rootNode).put(Field.type.name(), "table");
        appendEntryToText(text, rootNode, null);

        return new Pf2eQuteNote(type,
                sources.getName(),
                sources.getSourceText(),
                join("\n", text), tags);
    }
}
