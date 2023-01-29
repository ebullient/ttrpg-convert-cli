package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.pf2e.qute.QuteAffliction;

public class Json2QuteAffliction extends Json2QuteBase {

    public Json2QuteAffliction(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode) {
        super(index, type, rootNode);
    }

    @Override
    protected QuteAffliction buildQuteResource() {
        List<String> tags = new ArrayList<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();
        List<String> temptedCurse = new ArrayList<>();

        appendEntryToText(text, Field.entries.getFrom(rootNode), "##");
        int count = appendFootnotes(text, 0);

        appendEntryToText(temptedCurse, AfflictionField.temptedCurse.getFrom(rootNode), null);
        appendFootnotes(temptedCurse, count);

        String type = AfflictionField.type.getTextOrEmpty(rootNode);
        String level = AfflictionField.level.getTextOrDefault(rootNode, "1");

        if (temptedCurse.isEmpty()) {
            tags.add(cfg().tagOf("affliction", type));
        } else {
            tags.add(cfg().tagOf("affliction", type, "tempted"));
        }
        tags.add(cfg().tagOf("affliction", "level", level));

        return new QuteAffliction(getSources(), text, tags,
                collectTraitsFrom(rootNode, tags),
                Field.alias.transformListFrom(rootNode, this),
                level, toTitleCase(type),
                String.join("\n", temptedCurse));
    }
}
