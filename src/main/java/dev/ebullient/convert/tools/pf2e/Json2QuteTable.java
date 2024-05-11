package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteNote;

import static dev.ebullient.convert.StringUtil.join;

public class Json2QuteTable extends Json2QuteBase {

    public Json2QuteTable(Pf2eIndex index, JsonNode rootNode) {
        super(index, Pf2eIndexType.table, rootNode);
    }

    @Override
    protected Pf2eQuteNote buildQuteNote() {
        Tags tags = new Tags(sources);
        List<String> text = new ArrayList<>();

        ((ObjectNode) rootNode).put(SourceField.type.name(), "table");
        appendToText(text, rootNode, null);

        return new Pf2eQuteNote(type, sources,
                join("\n", text), tags);
    }
}
