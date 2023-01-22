package dev.ebullient.convert.tools.pf2e;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.pf2e.qute.QuteAction;
import dev.ebullient.convert.tools.pf2e.qute.QuteActivityType;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class Json2QuteAction extends Json2QuteBase {

    public Json2QuteAction(Pf2eIndex index, Pf2eIndexType type, JsonNode node) {
        super(index, type, node, Pf2eSources.findSources(node));
    }

    public QuteAction build() {
        List<String> tags = new ArrayList<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();

        appendEntryToText(text, Field.entries.getFrom(rootNode), "##");
        appendEntryToText(text, Field.info.getFrom(rootNode), null);
        appendFootnotes(text, 0);

        JsonActivity jsonActivity = Field.activity.fieldFromTo(rootNode, JsonActivity.class, tui());
        ActionType actionType = Field.actionType.fieldFromTo(rootNode, ActionType.class, tui());

        if (actionType == null) {
            tags.add(cfg().tagOf("action"));
        } else {
            actionType.addTags(this, tags);
        }

        return new QuteAction(
                getSources(),
                transformTextFrom(rootNode, Field.cost, ", "),
                transformTextFrom(rootNode, Field.trigger, ", "),
                transformListFrom(rootNode, Field.alias),
                collectTraits(),
                transformTextFrom(rootNode, Field.prerequisites, ", "),
                transformTextFrom(rootNode, Field.requirements, ", "),
                getFrequency(rootNode),
                jsonActivity == null ? null : jsonActivity.toQuteActivity(this),
                actionType == null ? null : actionType.build(this),
                String.join("\n", text),
                tags);
    }

    @RegisterForReflection
    static class ActionType {
        public Boolean basic;
        public Boolean item;
        public Skill skill;
        public List<String> ancestry;
        public List<String> archetype;
        public List<String> heritage;
        public List<String> versatileHeritage;
        @JsonProperty("class")
        public List<String> classType;
        public List<String> subclass;
        public List<String> variantrule;

        public void addTags(JsonSource convert, List<String> tags) {
            if (isBasic()) {
                tags.add(convert.cfg().tagOf("action", "basic"));
            }
            if (isItem()) {
                tags.add(convert.cfg().tagOf("action", "item"));
            }
            if (ancestry != null) {
                ancestry.forEach(c -> tags.add(convert.cfg().tagOf("action", "ancestry", c)));
            }
            if (archetype != null) {
                archetype.forEach(c -> tags.add(convert.cfg().tagOf("action", "archetype", c)));
            }
            if (classType != null) {
                classType.forEach(c -> tags.add(convert.cfg().tagOf("action", "class", c)));
            }
        }

        public boolean isBasic() {
            return basic != null && basic == true;
        }

        public boolean isItem() {
            return item != null && item == true;
        }

        public QuteAction.ActionType build(JsonSource convert) {
            return new QuteAction.ActionType(isBasic(), isItem(),
                    skill == null ? null
                            : skill.buildString(convert),
                    classType == null ? null
                            : classType.stream()
                                    .map(s -> convert.linkify(Pf2eIndexType.classtype, s))
                                    .collect(Collectors.toList()),
                    subclass == null ? null
                            : subclass.stream()
                                    .map(s -> createSubclassLink(s))
                                    .map(s -> convert.linkify(Pf2eIndexType.classtype, s))
                                    .collect(Collectors.toList()),
                    archetype == null ? null
                            : archetype.stream()
                                    .map(s -> convert.linkify(Pf2eIndexType.archetype, s))
                                    .collect(Collectors.toList()),
                    ancestry == null ? null
                            : ancestry.stream()
                                    .map(s -> createAncestryLink(s))
                                    .map(s -> convert.linkify(Pf2eIndexType.ancestry, s))
                                    .collect(Collectors.toList()),
                    heritage == null ? null
                            : heritage.stream()
                                    .map(s -> createHeritageLink(s))
                                    .collect(Collectors.toList()),
                    versatileHeritage == null ? null
                            : versatileHeritage.stream()
                                    .map(s -> createVersatileHeritageLink(s))
                                    .collect(Collectors.toList()),
                    variantrule == null ? null
                            : variantrule.stream()
                                    .map(s -> convert.linkify(Pf2eIndexType.variantrule, s))
                                    .collect(Collectors.toList()));
        }

        private String createSubclassLink(String subclassName) {
            String[] cSrc = this.classType.get(0).split("\\|");
            String[] scSrc = subclassName.split("\\|");
            return String.format("%s|%s|%s|%s|%s",
                    cSrc[0],
                    cSrc.length > 1 ? cSrc[1] : "",
                    scSrc[0],
                    scSrc[0],
                    scSrc.length > 1 ? scSrc[1] : "");
        }

        private String createAncestryLink(String ancestry) {
            String[] aSrc = ancestry.split("\\|");
            return String.format("%s|%s",
                    aSrc[0],
                    aSrc.length > 1 ? aSrc[1] : "");
        }

        private String createHeritageLink(String heritage) {
            String[] aSrc = this.ancestry.get(0).split("\\|");
            String[] hSrc = heritage.split("\\|");
            return String.format("%s|%s|%s|%s|%s|",
                    aSrc[0],
                    aSrc.length > 1 ? aSrc[1] : "",
                    hSrc[0],
                    hSrc[0],
                    hSrc.length > 1 ? hSrc[1] : "");
        }

        private String createVersatileHeritageLink(String versatile) {
            String[] aSrc = (this.ancestry == null ? "Human|CRB" : this.ancestry.get(0))
                    .split("\\|");
            String[] vSrc = versatile.split("\\|");
            return String.format("%s|%s|%s|%s|%s|",
                    aSrc[0],
                    aSrc.length > 1 ? aSrc[1] : "",
                    vSrc[0],
                    vSrc[0],
                    vSrc.length > 1 ? vSrc[1] : "");
        }
    }

    @RegisterForReflection
    static class Skill {
        public List<String> trained;
        public List<String> untrained;
        public List<String> expert;
        public List<String> legendary;

        public String buildString(JsonSource convert) {
            List<String> allSkills = new ArrayList<>();
            if (untrained != null) {
                List<String> inner = new ArrayList<>();
                untrained.forEach(s -> inner.add(convert.linkify(Pf2eIndexType.skill, s)));
                allSkills.add(String.format("%s (untrained)", String.join(", ", inner)));
            }
            if (trained != null) {
                List<String> inner = new ArrayList<>();
                trained.forEach(s -> inner.add(convert.linkify(Pf2eIndexType.skill, s)));
                allSkills.add(String.format("%s (trained)", String.join(", ", inner)));
            }
            if (expert != null) {
                List<String> inner = new ArrayList<>();
                expert.forEach(s -> inner.add(convert.linkify(Pf2eIndexType.skill, s)));
                allSkills.add(String.format("%s (expert)", String.join(", ", inner)));
            }
            if (legendary != null) {
                List<String> inner = new ArrayList<>();
                legendary.forEach(s -> inner.add(convert.linkify(Pf2eIndexType.skill, s)));
                allSkills.add(String.format("%s (legendary)", String.join(", ", inner)));
            }
            return String.join("; ", allSkills);
        }
    }

    @RegisterForReflection
    static class JsonActivity {
        public int number;
        public String unit;
        public String entry;

        public QuteActivityType toQuteActivity(JsonSource convert) {
            String extra = entry == null || entry.toLowerCase().contains("varies")
                    ? ""
                    : " (" + convert.replaceText(entry) + ")";

            switch (unit) {
                case "action":
                case "free":
                case "reaction":
                    Pf2eTypeActivity activity = Pf2eTypeActivity.toActivity(unit, number);
                    return createActivity(convert,
                            String.format("%s%s", activity.getCaption(), extra),
                            activity);
                case "varies":
                    return createActivity(convert,
                            String.format("%s%s", Pf2eTypeActivity.varies.getCaption(), extra),
                            Pf2eTypeActivity.varies);
                case "day":
                case "minute":
                case "hour":
                case "round":
                    return createActivity(convert,
                            String.format("%s %s%s", number, unit, extra),
                            Pf2eTypeActivity.timed);
                default:
                    throw new IllegalArgumentException("What is this? " + String.format("%s, %s, %s", number, unit, entry));
            }
        }

        QuteActivityType createActivity(JsonSource convert, String text, Pf2eTypeActivity activity) {
            String fileName = activity.getGlyph();
            int x = fileName.lastIndexOf('.');
            Path target = Path.of("img",
                    Tui.slugify(fileName.substring(0, x)) + fileName.substring(x));

            return new QuteActivityType(
                    text,
                    new ImageRef.Builder()
                            .setStreamSource(activity.getGlyph())
                            .setTargetPath(convert.index().rulesPath(), target)
                            .setMarkdownPath(activity.getCaption(), convert.index().rulesRoot())
                            .build(),
                    activity.getTextGlyph(),
                    activity.getRulesPath(convert.index().rulesRoot()));
        }
    }

}
