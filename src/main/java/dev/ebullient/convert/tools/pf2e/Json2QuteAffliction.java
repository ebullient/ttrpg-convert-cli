package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.QuteAffliction;
import dev.ebullient.convert.tools.pf2e.qute.QuteInlineAffliction;

import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.toTitleCase;
import static dev.ebullient.convert.StringUtil.isPresent;

public class Json2QuteAffliction extends Json2QuteBase {

    public Json2QuteAffliction(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode) {
        super(index, type, rootNode);
    }

    @Override
    protected QuteAffliction buildQuteResource() {
        Tags tags = new Tags(sources);

        List<String> text = new ArrayList<>();
        appendToText(text, Pf2eAffliction.entries.getFrom(rootNode), "##");

        String temptedCurse = Pf2eAffliction.temptedCurse.transformTextFrom(rootNode, "\n", this);
        String type = Pf2eAffliction.type.getTextOrEmpty(rootNode);
        String level = Pf2eAffliction.level.getTextOrDefault(rootNode, "1");

        if (isPresent(temptedCurse)) {
            tags.add("affliction", type, "tempted");
        } else {
            tags.add("affliction", type);
        }
        tags.add("affliction", "level", level);

        return new QuteAffliction(getSources(), text, tags,
                collectTraitsFrom(rootNode, tags),
                Field.alias.replaceTextFromList(rootNode, this),
                level, toTitleCase(type),
                temptedCurse);
    }

    public enum Pf2eAffliction implements JsonNodeReader {
        name,
        DC,
        duration,
        level,
        maxDuration,
        note,
        onset,
        savingThrow,
        stage,
        stages,
        temptedCurse,
        type,
        entries,
        entry;

        public static boolean isAfflictionBlock(JsonNode node) {
            return type.getTextFrom(node).map(s -> s.equals("affliction")).orElse(false);
        }

        static QuteInlineAffliction createInlineAffliction(JsonNode node, JsonSource convert) {
            Tags tags = new Tags();
            Collection<String> traits = convert.collectTraitsFrom(node, tags);

            String levelString = level.getIntFrom(node).map("Level %d"::formatted).orElse(null);
            if (levelString != null) {
                tags.add("affliction", "level", levelString);
            }

            // DC can be either an int or a custom string
            String dcString = DC.getIntFrom(node).map(Objects::toString)
                    .orElseGet(() -> DC.replaceTextFrom(node, convert));

            List<String> notes = new ArrayList<>();
            convert.appendToText(notes, note.getFrom(node), null);

            return new QuteInlineAffliction(
                    name.getTextOrEmpty(node), notes, tags, traits, levelString,
                    maxDuration.replaceTextFrom(node, convert),
                    onset.replaceTextFrom(node, convert),
                    join(" ",
                            toTitleCase(savingThrow.getTextOrEmpty(node)),
                            isPresent(dcString) ? "DC" : "",
                            dcString),
                    entries.transformTextFrom(node, "\n", convert),
                    stages.streamFrom(node)
                            .map(n -> Map.entry("Stage %s".formatted(stage.getTextOrDefault(n, "1")), n))
                            .collect(Collectors.toMap(
                                    Map.Entry::getKey,
                                    e -> new QuteInlineAffliction.QuteAfflictionStage(
                                            duration.replaceTextFrom(e.getValue(), convert),
                                            entry.transformTextFrom(e.getValue(), "\n", convert, e.getKey())),
                                    (x, y) -> y,
                                    LinkedHashMap::new)));
        }
    }
}
