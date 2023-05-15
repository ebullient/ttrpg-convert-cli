package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteNote;

public class Json2QuteTable extends Json2QuteBase {

    public Json2QuteTable(Pf2eIndex index, JsonNode rootNode) {
        super(index, Pf2eIndexType.table, rootNode);
    }

    @Override
    protected Pf2eQuteNote buildQuteNote() {
        Set<String> tags = new TreeSet<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();

        ((ObjectNode) rootNode).put(Field.type.name(), "table");
        appendEntryToText(text, rootNode, null);

        return new Pf2eQuteNote(type, sources,
                join("\n", text), tags);
    }
}
