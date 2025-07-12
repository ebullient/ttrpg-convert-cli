package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteNote;

public class Json2QuteLegendaryGroup extends Json2QuteCommon {

    Json2QuteLegendaryGroup(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    protected Tools5eQuteNote buildQuteNote() {
        Tags tags = new Tags(getSources());
        tags.add("monster", "legendary-group");

        List<String> text = new ArrayList<>();
        appendSectionText(text, LeGroupFields.lairActions, "Lair Actions");
        appendSectionText(text, LeGroupFields.regionalEffects, "Regional Effects");
        appendSectionText(text, LeGroupFields.mythicEncounter, "As a Mythic Encounter");

        return new Tools5eQuteNote(sources,
                sources.getName(),
                null,
                String.join("\n", text),
                tags)
                .withTargetFile(linkifier().getTargetFileName(getName(), sources))
                .withTargetPath(linkifier().getRelativePath(type));
    }

    void appendSectionText(List<String> text, LeGroupFields field, String header) {
        JsonNode node = field.getFrom(rootNode);
        if (node == null || node.isMissingNode() || node.isNull()) {
            return;
        }
        boolean pushed = parseState().push(node);
        try {
            maybeAddBlankLine(text);
            text.add("## " + header);
            text.add(getSourceText(parseState()));
            text.add("");
            appendToText(text, node, "###");
        } finally {
            parseState().pop(pushed);
        }
    }

    enum LeGroupFields implements JsonNodeReader {
        lairActions,
        mythicEncounter,
        regionalEffects,
    }
}
