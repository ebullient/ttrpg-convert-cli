package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.qute.QuteNote;
import dev.ebullient.convert.qute.QuteSource;
import dev.ebullient.convert.tools.IndexType;

public class Table2QuteNote extends Json2QuteCommon {
    Table2QuteNote(JsonIndex index, JsonNode jsonNode) {
        super(index, IndexType.table, jsonNode);
    }

    @Override
    public QuteSource build() {
        throw new IllegalStateException("Not implemented");
    }

    public QuteNote buildNote() {
        if (index.rulesSourceExcluded(node, getName())) {
            return null;
        }

        Set<String> tags = new HashSet<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();

        appendTable(text, node);

        String blockid = "^table";
        String lastLine = text.get(text.size() - 1);
        if (lastLine.startsWith("^")) {
            blockid = lastLine;
        } else {
            text.add("^table");
        }

        text.add(0, String.format("`dice: [](%s.md#%s)`", slugify(getName()), blockid));
        text.add(1, "");

        return new QuteNote(getName(),
                sources.getSourceText(index.srdOnly()),
                String.join("\n", text),
                tags);
    }

    public QuteNote buildRules() {
        if (index.rulesSourceExcluded(node, getName())) {
            return null;
        }
        Set<String> tags = new HashSet<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();

        node.withArray("rows").forEach(row -> {
            ArrayNode cols = (ArrayNode) row;
            maybeAddBlankLine(text);
            text.add("## " + replaceText(cols.get(0).asText()));
            maybeAddBlankLine(text);
            appendEntryToText(text, cols.get(1), null);
        });

        return new QuteNote(getName(),
                sources.getSourceText(index.srdOnly()),
                String.join("\n", text),
                tags);
    }
}
