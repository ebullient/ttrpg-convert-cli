package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteNote;

public class Json2QuteTable extends Json2QuteCommon {

    Json2QuteTable(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    public Tools5eQuteNote buildQuteNote() {
        String key = getSources().getKey();
        if (index.isExcluded(key)) {
            return null;
        }

        Tags tags = new Tags(getSources());
        List<String> text = new ArrayList<>();
        String targetDir = Tools5eQuteBase.getRelativePath(type);
        String targetFile = null;

        if (getName().equals("Damage Types")) {
            for (JsonNode row : iterableElements(rootNode.get("rows"))) {
                ArrayNode cols = (ArrayNode) row;
                maybeAddBlankLine(text);
                text.add("## " + replaceText(cols.get(0).asText()));
                maybeAddBlankLine(text);
                appendToText(text, cols.get(1), null);
            }
            targetDir = null;
        } else if (type == Tools5eIndexType.tableGroup) {
            appendToText(text, Tools5eFields.tables.getFrom(rootNode), "##");
        } else {
            appendTable(text, rootNode);

            String blockid = "^table";
            String lastLine = text.get(text.size() - 1);
            if (lastLine.startsWith("^")) {
                blockid = lastLine;
            } else {
                text.add("^table");
            }

            // prepend a dice roller
            targetFile = slugify(getName()) + Tools5eQuteBase.sourceIfNotDefault(getSources());
            text.add(0, String.format("`dice: [](%s.md#%s)`", targetFile, blockid));
            text.add(1, "");
        }

        return new Tools5eQuteNote(getName(), getSourceText(sources), text, tags)
                .withTargetPath(targetDir)
                .withTargetFile(targetFile);
    }
}
