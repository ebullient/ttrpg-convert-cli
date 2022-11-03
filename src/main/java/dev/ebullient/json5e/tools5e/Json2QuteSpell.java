package dev.ebullient.json5e.tools5e;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.json5e.qute.QuteSource;
import dev.ebullient.json5e.qute.QuteSpell;

public class Json2QuteSpell extends Json2QuteCommon {

    final String decoratedName;

    Json2QuteSpell(JsonIndex index, IndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
        decoratedName = decoratedTypeName(getName(), getSources());
    }

    @Override
    public QuteSource build() {
        boolean ritual = spellIsRitual();
        SchoolEnum school = getSchool();
        String level = node.get("level").asText();

        Collection<String> classes = spellClasses(school);

        List<String> tags = new ArrayList<>(sources.getSourceTags());

        tags.add("spell/school/" + slugify(school.name()));
        tags.add("spell/level/" + (level.equals("0") ? "cantrip" : level));
        if (ritual) {
            tags.add("spell/ritual");
        }
        for (String c : classes) {
            String[] split = c.split("\\(");
            for (int i = 0; i < split.length; i++) {
                split[i] = slugify(split[i].trim());
            }
            tags.add("spell/class/" + String.join("/", split));
        }

        List<String> text = new ArrayList<>();
        appendEntryToText(text, node, "##");
        if (node.has("entriesHigherLevel")) {
            maybeAddBlankLine(text);
            appendEntryToText(text, node.get("entriesHigherLevel"),
                    textContains(text, "## ") ? "##" : null);
        }

        return new QuteSpell(sources,
                decoratedName,
                sources.getSourceText(index.srdOnly()),
                levelToText(level),
                school.name(),
                ritual,
                spellCastingTime(),
                spellRange(),
                spellComponents(),
                spellDuration(),
                String.join(", ", classes),
                String.join("\n", text),
                tags);
    }

    SchoolEnum getSchool() {
        String code = node.get("school").asText();
        return SchoolEnum.fromShortcode(code);
    }

    boolean spellIsRitual() {
        boolean ritual = false;
        JsonNode meta = node.get("meta");
        if (meta != null) {
            ritual = booleanOrDefault(meta, "ritual", false);
        }
        return ritual;
    }

    String spellComponents() {
        JsonNode components = node.get("components");

        List<String> list = new ArrayList<>();
        components.fields().forEachRemaining(f -> {
            switch (f.getKey().toLowerCase()) {
                case "v":
                    list.add("V");
                    break;
                case "s":
                    list.add("S");
                    break;
                case "m":
                    if (f.getValue().isObject()) {
                        list.add(f.getValue().get("text").asText());
                    } else {
                        list.add(f.getValue().asText());
                    }
                    break;
            }
        });
        return String.join(", ", list);
    }

    String spellDuration() {
        StringBuilder result = new StringBuilder();
        JsonNode durations = node.withArray("duration");
        if (durations.size() > 0) {
            addDuration(durations.get(0), result);
        }
        if (durations.size() > 1) {
            result.append(", ");
            String type = getTextOrEmpty(durations.get(1), "type");
            if ("timed".equals(type)) {
                result.append(" up to ");
            }
            addDuration(durations.get(1), result);
        }
        return result.toString();
    }

    void addDuration(JsonNode element, StringBuilder result) {
        String type = getTextOrEmpty(element, "type");
        switch (type) {
            case "instant":
                result.append("Instantaneous");
                break;
            case "permanent":
                result.append("Until dispelled");
                if (element.withArray("ends").size() > 1) {
                    result.append(" or triggered");
                }
                break;
            case "special":
                result.append("Special");
                break;
            case "timed": {
                if (booleanOrDefault(element, "concentration", false)) {
                    result.append("Concentration, up to ");
                }
                JsonNode duration = element.get("duration");
                result.append(duration.get("amount").asText())
                        .append(" ")
                        .append(duration.get("type").asText());
                break;
            }
            default:
                tui().errorf("What is this? %s", element.toPrettyString());
        }
    }

    String spellRange() {
        StringBuilder result = new StringBuilder();
        JsonNode range = node.get("range");
        if (range != null) {
            String type = getTextOrEmpty(range, "type");
            JsonNode distance = range.get("distance");
            switch (type) {
                case "cube":
                case "cone":
                case "hemisphere":
                case "line":
                case "radius":
                case "sphere": {
                    // Self (xx-foot yy)
                    result.append("Self (")
                            .append(distance.get("amount").asText())
                            .append("-")
                            .append(distance.get("type").asText())
                            .append(" ")
                            .append(type)
                            .append(")");
                    break;
                }
                case "point": {
                    String distanceType = distance.get("type").asText();
                    switch (distanceType) {
                        case "self":
                        case "sight":
                        case "touch":
                        case "unlimited":
                            result.append(distanceType.substring(0, 1).toUpperCase())
                                    .append(distanceType.substring(1));
                            break;
                        default:
                            result.append(distance.get("amount").asText())
                                    .append(" ")
                                    .append(distanceType);
                            break;
                    }
                    break;
                }
                case "special": {
                    result.append("Special");
                    break;
                }
            }
        }
        return result.toString();
    }

    String spellCastingTime() {
        JsonNode time = node.withArray("time").get(0);
        return String.format("%s %s",
                time.get("number").asText(),
                time.get("unit").asText());
    }

    Collection<String> spellClasses(SchoolEnum school) {
        JsonNode classesNode = node.get("classes");
        if (classesNode == null || classesNode.isNull()) {
            return List.of();
        }
        Set<String> classes = new TreeSet<>();
        classesNode.withArray("fromClassList").forEach(c -> {
            String className = c.get("name").asText();
            String classSource = c.get("source").asText();
            if (includeClass(className, classSource)) {
                classes.add(className);
            }
        });
        classesNode.withArray("fromClassListVariant").forEach(c -> {
            String definedInSource = c.get("definedInSource").asText();
            String className = c.get("name").asText();
            String classSource = c.get("source").asText();
            if (index.sourceIncluded(definedInSource) && includeClass(className, classSource)) {
                classes.add(className);
            }
        });
        classesNode.withArray("fromSubclass").forEach(s -> {
            String className = s.get("class").get("name").asText().trim();
            if (classes.contains(className)) {
                return;
            }
            String classSource = s.get("class").get("source").asText();
            String subclassName = s.get("subclass").get("name").asText();
            if (includeSubclass(className, classSource, subclassName)) {
                classes.add(String.format("%s (%s)", className, subclassName));
            }
        });
        if (classes.contains("Wizard")) {
            if (school == SchoolEnum.Abjuration || school == SchoolEnum.Evocation) {
                classes.add("Fighter (Eldritch Knight)");
            }
            if (school == SchoolEnum.Enchantment || school == SchoolEnum.Illusion) {
                classes.add("Rogue (Arcane Trickster)");
            }
        }
        return classes;
    }

    private boolean includeClass(String className, String classSource) {
        String finalKey = index().getClassKey(className, classSource);
        return index().isIncluded(finalKey);
    }

    private boolean includeSubclass(String className, String classSource, String subclassName) {
        String finalKey = index().getSubclassKey(subclassName.trim(), className.trim(), classSource.trim());
        return index().isIncluded(finalKey);
    }
}
