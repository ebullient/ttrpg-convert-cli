package dev.ebullient.convert.tools.pf2e;

import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.QuteAffliction;

public class Json2QuteAffliction extends Json2QuteBase {

    public Json2QuteAffliction(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode) {
        super(index, type, rootNode);
    }

    @Override
    protected QuteAffliction buildQuteResource() {
        Tags tags = new Tags(sources);
        List<String> text = new ArrayList<>();
        List<String> temptedCurse = new ArrayList<>();

        appendToText(text, SourceField.entries.getFrom(rootNode), "##");
        appendToText(temptedCurse, AfflictionField.temptedCurse.getFrom(rootNode), null);

        String type = AfflictionField.type.getTextOrEmpty(rootNode);
        String level = AfflictionField.level.getTextOrDefault(rootNode, "1");

        if (temptedCurse.isEmpty()) {
            tags.add("affliction", type);
        } else {
            tags.add("affliction", type, "tempted");
        }
        tags.add("affliction", "level", level);

        return new QuteAffliction(getSources(), text, tags,
                collectTraitsFrom(rootNode, tags),
                Field.alias.replaceTextFromList(rootNode, this),
                level, toTitleCase(type),
                String.join("\n", temptedCurse));
    }
}
