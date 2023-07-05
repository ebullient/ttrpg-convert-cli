package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteNote;

public class Json2QuteTable extends Json2QuteCommon {

    Json2QuteTable(Tools5eIndex index, JsonNode jsonNode) {
        super(index, Tools5eIndexType.table, jsonNode);
    }

    @Override
    public Tools5eQuteNote buildQuteNote() {
        String key = getSources().getKey();
        if (index.isExcluded(key)) {
            return null;
        }

        Set<String> tags = new HashSet<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();
        String targetDir = Tools5eQuteBase.TABLES_PATH;
        String targetFile = null;

        if (getName().equals("Damage Types")) {
            for (JsonNode row : iterableElements(node.get("rows"))) {
                ArrayNode cols = (ArrayNode) row;
                maybeAddBlankLine(text);
                text.add("## " + replaceText(cols.get(0).asText()));
                maybeAddBlankLine(text);
                appendEntryToText(text, cols.get(1), null);
            }

            targetDir = null;
        } else {
            appendTable(text, node);

            String blockid = "^table";
            String lastLine = text.get(text.size() - 1);
            if (lastLine.startsWith("^")) {
                blockid = lastLine;
            } else {
                text.add("^table");
            }

            // prepend a dice roller
            targetFile = slugify(getName()) + Tools5eQuteBase.sourceIfNotCore(getSources().primarySource());
            text.add(0, String.format("`dice: [](%s.md#%s)`", targetFile, blockid));
            text.add(1, "");
        }

        return new Tools5eQuteNote(getName(), sources.getSourceText(index.srdOnly()), text, tags)
                .withTargetPath(targetDir)
                .withTargeFile(targetFile);
    }
}
