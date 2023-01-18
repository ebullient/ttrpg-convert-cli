package dev.ebullient.convert.tools.pf2e;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.tools.pf2e.qute.QuteAction;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class Json2QuteAction extends Json2QuteBase {

    public Json2QuteAction(Pf2eIndex index, Pf2eIndexType type, JsonNode node) {
        super(index, type, node, Pf2eSources.findSources(node));
    }

    public QuteBase build() {
        List<String> tags = new ArrayList<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();

        appendEntryToText(text, Field.entries.getFrom(rootNode), "##");
        appendEntryToText(text, Field.info.getFrom(rootNode), null);
        if (text.isEmpty()) {
            return null;
        }

        JsonActivity jsonActivity = Field.activity.fieldFromTo(rootNode, JsonActivity.class, tui());
        ActionType actionType = Field.actionType.fieldFromTo(rootNode, ActionType.class, tui());

        String trigger = replaceText(Field.trigger.getTextOrNull(rootNode));
        List<String> alias = transform(Field.alias);
        List<String> traits = linkify(Field.traits);
        List<String> requirements = transform(Field.requirements);

        return new QuteAction(
                getSources(),
                getSources().getName(),
                getSources().getSourceText(),
                trigger, alias, traits,
                String.join(", ", requirements),
                jsonActivity == null ? null : jsonActivity.build(this),
                actionType == null ? null : actionType.build(this),
                String.join("\n", text),
                tags);
    }

    List<String> linkify(Field field) {
        List<String> list = field.getListOfStrings(rootNode, tui());
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream()
                .sorted()
                .map(s -> linkify(Pf2eIndexType.trait, s)).collect(Collectors.toList());
    }

    List<String> transform(Field field) {
        List<String> list = field.getListOfStrings(rootNode, tui());
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return list.stream().map(s -> replaceText(s)).collect(Collectors.toList());
    }

    @RegisterForReflection
    public static class ActionType {
        public Boolean item;
        public Boolean basic;
        public Skill skill;
        public List<String> ancestry;
        public List<String> archetype;
        public List<String> heritage;
        public List<String> versatileHeritage;
        @JsonProperty("class")
        public List<String> classType;
        public List<String> subclass;
        public List<String> variantrule;

        public QuteAction.ActionType build(JsonSource convert) {

            return new QuteAction.ActionType(
                    basic == null ? false : basic,
                    item == null ? false : item,
                    skill == null ? null : skill.asList(convert),
                    ancestry, archetype, heritage, versatileHeritage,
                    classType, subclass, variantrule);
        }
    }

    @RegisterForReflection
    public static class Skill {
        public List<String> trained;
        public List<String> untrained;

        List<String> asList(JsonSource convert) {
            List<String> allSkills = new ArrayList<>();
            if (trained != null) {
                trained.forEach(s -> allSkills.add(String.format("%s (trained)",
                        convert.linkify(Pf2eIndexType.skill, s))));
            }
            if (untrained != null) {
                untrained.forEach(s -> allSkills.add(String.format("%s (untrained)",
                        convert.linkify(Pf2eIndexType.skill, s))));
            }
            return allSkills;
        }
    }

    @RegisterForReflection
    public static class JsonActivity {
        public int number;
        public String unit;
        public String entry;

        public QuteAction.ActivityType build(JsonSource convert) {
            String extra = entry == null || entry.toLowerCase().contains("varies")
                    ? ""
                    : " (" + convert.replaceText(entry) + ")";

            switch (unit) {
                case "action":
                case "free":
                case "reaction":
                    Activity activity = Activity.toActivity(unit, number);

                    String fileName = activity.getGlyph();
                    int x = fileName.lastIndexOf('.');
                    Path target = Path.of("img",
                            convert.slugify(fileName.substring(0, x)) + fileName.substring(x));

                    return new QuteAction.ActivityType(
                            activity.getText(),
                            new ImageRef.Builder()
                                    .setStreamSource(activity.getGlyph())
                                    .setTargetPath(convert.index().rulesPath(), target)
                                    .setMarkdownPath(activity.getText(), convert.index().rulesRoot())
                                    .build(),
                            activity.getTextGlyph());
                case "varies":
                    return new QuteAction.ActivityType(
                            String.format("%s%s", Activity.varies.getText(), extra),
                            null,
                            Activity.varies.getTextGlyph());
                case "day":
                case "minute":
                case "hour":
                case "round":
                    return new QuteAction.ActivityType(
                            String.format("%s %s%s", number, unit, extra),
                            null, // TODO
                            "\\[⏲️\\]");
                default:
                    throw new IllegalArgumentException("What is this? " + String.format("%s, %s, %s", number, unit, entry));
            }
        }

    }

}
