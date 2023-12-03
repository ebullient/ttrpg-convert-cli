package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteNote;

public class Json2QuteLegendaryGroup extends Json2QuteCommon {
    static final Pattern UPPERCASE_LETTER = Pattern.compile("([A-Z]|\\d+)");
    static final List<String> LEGENDARY_IGNORE_LIST = List.of("name", "source", "page",
            TtrpgValue.indexInputType.name(), TtrpgValue.indexKey.name(), "_copy", "_meta",
            "additionalSources", "_rawName", "_isCopy", "_copiedFrom");

    Json2QuteLegendaryGroup(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    protected Tools5eQuteNote buildQuteNote() {
        Tags tags = new Tags(getSources());
        tags.add("monster", "legendary-group");

        List<String> text = new ArrayList<>();
        for (Entry<String, JsonNode> field : iterableFields(rootNode)) {
            String fieldName = field.getKey();
            if (LEGENDARY_IGNORE_LIST.contains(fieldName)) {
                continue;
            }
            boolean pushed = parseState().push(field.getValue());
            try {
                fieldName = fieldName.substring(0, 1).toUpperCase()
                        + UPPERCASE_LETTER.matcher(fieldName.substring(1))
                                .replaceAll(matchResult -> " " + (matchResult.group(1).toLowerCase()));

                maybeAddBlankLine(text);
                text.add("## " + fieldName);
                text.add(getSourceText(parseState()));
                text.add("");
                appendToText(text, field.getValue(), "###");
            } finally {
                parseState().pop(pushed);
            }
        }

        return new Tools5eQuteNote(sources,
                sources.getName(),
                null,
                String.join("\n", text),
                tags)
                .withTargetFile(Tools5eQuteBase.fixFileName(getName(), sources))
                .withTargetPath(type.getRelativePath());
    }
}
