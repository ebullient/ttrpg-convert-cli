package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.tools.dnd5e.qute.QuteSource;
import dev.ebullient.convert.tools.dnd5e.qute.QuteSpell;

public class Json2QuteSpell extends Json2QuteCommon {

    final String decoratedName;

    Json2QuteSpell(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
        decoratedName = decoratedTypeName(getName(), getSources());
    }

    @Override
    protected QuteBase buildQuteResource() {
        boolean ritual = spellIsRitual();
        SchoolEnum school = getSchool();
        String level = node.get("level").asText();

        Set<String> tags = new TreeSet<>(sources.getSourceTags());

        tags.add("spell/school/" + slugify(school.name()));
        tags.add("spell/level/" + (level.equals("0") ? "cantrip" : level));
        if (ritual) {
            tags.add("spell/ritual");
        }

        Set<String> classes = indexedSpellClasses(tags);
        classes.addAll(spellClasses(school, tags)); // legacy

        List<String> text = new ArrayList<>();
        appendEntryToText(text, node, "##");
        if (node.has("entriesHigherLevel")) {
            maybeAddBlankLine(text);
            appendEntryToText(text, node.get("entriesHigherLevel"),
                    textContains(text, "## ") ? "##" : null);
        }
        appendFootnotes(text, 0);

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
                getFluffImages(Tools5eIndexType.spellfluff),
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

    Set<String> indexedSpellClasses(Collection<String> tags) {
        Collection<String> list = index().classesForSpell(this.sources.getKey());
        if (list == null) {
            tui().warnf("No classes found for %s", this.sources.getKey());
            return new TreeSet<>();
        }

        return list.stream()
                .filter(k -> index().isIncluded(k))
                .map(k -> {
                    Tools5eSources sources = Tools5eSources.findSources(k);
                    Tools5eIndexType type = Tools5eIndexType.getTypeFromKey(k);
                    if (type == Tools5eIndexType.subclass) {
                        JsonNode subclassNode = index().getOrigin(k);
                        String subclassName = sources.getName();
                        String className = subclassNode.get("className").asText().trim();
                        String classSource = subclassNode.get("classSource").asText().trim();
                        return getSubclass(tags, className, classSource, subclassName, sources.primarySource(), k);
                    }
                    String className = sources.getName();
                    return getClass(tags, className, sources.primarySource(), k);
                })
                .collect(Collectors.toCollection(TreeSet::new));
    }

    Set<String> spellClasses(SchoolEnum school, Collection<String> tags) {
        JsonNode classesNode = node.get("classes");
        if (classesNode == null || classesNode.isNull()) {
            return Set.of();
        }
        Set<String> classes = new TreeSet<>();
        classesNode.withArray("fromClassList").forEach(c -> {
            String className = c.get("name").asText();
            String classSource = c.get("source").asText();
            String finalKey = Tools5eIndexType.classtype.createKey(className, classSource);
            if (index().isIncluded(finalKey)) {
                classes.add(getClass(tags, className, classSource, finalKey));
            }
        });
        classesNode.withArray("fromClassListVariant").forEach(c -> {
            String definedInSource = c.get("definedInSource").asText();
            String className = c.get("name").asText();
            String classSource = c.get("source").asText();
            String finalKey = Tools5eIndexType.classtype.createKey(className, classSource);
            if (index.sourceIncluded(definedInSource) && index().isIncluded(finalKey)) {
                classes.add(getClass(tags, className, classSource, finalKey));
            }
        });
        classesNode.withArray("fromSubclass").forEach(s -> {
            String className = s.get("class").get("name").asText().trim();
            String classSource = s.get("class").get("source").asText();
            String subclassName = s.get("subclass").get("name").asText();
            String subclassSource = s.get("subclass").get("source").asText();
            String finalKey = Tools5eIndexType.getSubclassKey(className.trim(), classSource.trim(), subclassName.trim(),
                    subclassSource.trim());
            if (index().isIncluded(finalKey)) {
                classes.add(getSubclass(tags, className, classSource, subclassName, subclassSource, finalKey));
            }
        });
        if (classes.contains("Wizard")) {
            if (school == SchoolEnum.Abjuration || school == SchoolEnum.Evocation) {
                String finalKey = Tools5eIndexType.getSubclassKey("Fighter", "PHB", "Eldritch Knight", "PHB");
                if (index().isIncluded(finalKey)) {
                    classes.add(getSubclass(tags, "Fighter", "PHB", "Eldritch Knight", "PHB", finalKey));
                }
            }
            if (school == SchoolEnum.Enchantment || school == SchoolEnum.Illusion) {
                String finalKey = Tools5eIndexType.getSubclassKey("Rogue", "PHB", "Arcane Trickster", "PHB");
                if (index().isIncluded(finalKey)) {
                    classes.add(getSubclass(tags, "Rogue", "PHB", "Arcane Trickster", "PHB", finalKey));
                }
            }
        }
        return classes;
    }

    private String getClass(Collection<String> tags, String className, String classSource, String classKey) {
        tags.add("spell/class/" + slugify(className));
        return linkOrText(
                className,
                classKey,
                QuteSource.CLASSES_PATH,
                className + QuteSource.sourceIfNotCore(classSource));
    }

    private String getSubclass(Collection<String> tags, String className, String classSource, String subclassName,
            String subclassSource, String subclassKey) {
        tags.add("spell/class/" + slugify(className) + "/" + slugify(subclassName));
        return linkOrText(
                String.format("%s (%s)", className, subclassName),
                subclassKey,
                QuteSource.CLASSES_PATH,
                QuteSource.getSubclassResource(subclassName, className, subclassSource));
    }
}
