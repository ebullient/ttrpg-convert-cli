package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.QuteSpell;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QuteSpell extends Json2QuteCommon {

    final String decoratedName;

    Json2QuteSpell(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
        decoratedName = type.decoratedName(jsonNode);
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {
        boolean ritual = spellIsRitual();
        SpellSchool school = getSchool();
        String level = SpellFields.level.getTextOrEmpty(rootNode);

        Tags tags = new Tags(getSources());

        tags.add("spell", "school", school.name());
        tags.add("spell", "level", (level.equals("0") ? "cantrip" : level));
        if (ritual) {
            tags.add("spell", "ritual");
        }

        Set<String> classes = indexedSpellClasses(tags);
        classes.addAll(spellClasses(school, tags)); // legacy

        List<String> text = new ArrayList<>();
        appendToText(text, rootNode, "##");
        if (SpellFields.entriesHigherLevel.existsIn(rootNode)) {
            maybeAddBlankLine(text);
            appendToText(text, SpellFields.entriesHigherLevel.getFrom(rootNode),
                    textContains(text, "## ") ? "##" : null);
        }

        return new QuteSpell(sources,
                decoratedName,
                getSourceText(sources),
                levelToText(level),
                school.name(),
                ritual,
                spellCastingTime(),
                spellRange(),
                spellComponents(),
                spellDuration(),
                String.join(", ", classes),
                String.join("\n", text),
                getFluffImages(Tools5eIndexType.spellFluff),
                tags);
    }

    SpellSchool getSchool() {
        String code = SpellFields.school.getTextOrEmpty(rootNode);
        return index().findSpellSchool(code, getSources());
    }

    boolean spellIsRitual() {
        boolean ritual = false;
        JsonNode meta = SpellFields.meta.getFrom(rootNode);
        if (meta != null) {
            ritual = SpellFields.ritual.booleanOrDefault(meta, false);
        }
        return ritual;
    }

    String spellComponents() {
        JsonNode components = SpellFields.components.getFrom(rootNode);
        if (components == null) {
            return "";
        }

        List<String> list = new ArrayList<>();
        for (Entry<String, JsonNode> f : iterableFields(components)) {
            switch (f.getKey().toLowerCase()) {
                case "v" -> list.add("V");
                case "s" -> list.add("S");
                case "m" -> {
                    if (f.getValue().isObject()) {
                        list.add(replaceText(SpellFields.text.getTextOrEmpty(f.getValue())));
                    } else {
                        list.add(replaceText(f.getValue().asText()));
                    }
                }
            }
        }
        return String.join(", ", list);
    }

    String spellDuration() {
        StringBuilder result = new StringBuilder();
        JsonNode durations = SpellFields.duration.ensureArrayIn(rootNode);
        if (durations.size() > 0) {
            addDuration(durations.get(0), result);
        }
        if (durations.size() > 1) {
            JsonNode ends = durations.get(1);
            result.append(", ");
            String type = getTextOrEmpty(ends, "type");
            if ("timed".equals(type)) {
                result.append(" up to ");
            }
            addDuration(ends, result);
        }
        return result.toString();
    }

    void addDuration(JsonNode element, StringBuilder result) {
        String type = getTextOrEmpty(element, "type");
        switch (type) {
            case "instant" -> result.append("Instantaneous");
            case "permanent" -> {
                result.append("Until dispelled");
                if (element.withArray("ends").size() > 1) {
                    result.append(" or triggered");
                }
            }
            case "special" -> result.append("Special");
            case "timed" -> {
                if (booleanOrDefault(element, "concentration", false)) {
                    result.append("Concentration, up to ");
                }
                JsonNode duration = element.get("duration");
                result.append(SpellFields.amount.getTextOrEmpty(duration))
                        .append(" ")
                        .append(SpellFields.type.getTextOrEmpty(duration));
            }
            default -> tui().errorf("What is this? %s", element.toPrettyString());
        }
    }

    String spellRange() {
        StringBuilder result = new StringBuilder();
        JsonNode range = SpellFields.range.getFrom(rootNode);
        if (range != null) {
            String type = SpellFields.type.getTextOrEmpty(range);
            JsonNode distance = SpellFields.distance.getFrom(range);
            String distanceType = SpellFields.type.getTextOrEmpty(distance);
            String amount = SpellFields.amount.getTextOrEmpty(distance);

            switch (type) {
                case "cube", "cone", "hemisphere", "line", "radius", "sphere" -> // Self (xx-foot yy)
                    result.append("Self (")
                            .append(amount)
                            .append("-")
                            .append(distanceType)
                            .append(" ")
                            .append(type)
                            .append(")");
                case "point" -> {
                    switch (distanceType) {
                        case "self", "sight", "touch", "unlimited" ->
                            result.append(distanceType.substring(0, 1).toUpperCase())
                                    .append(distanceType.substring(1));
                        default -> result.append(amount)
                                .append(" ")
                                .append(distanceType);
                    }
                }
                case "special" -> result.append("Special");
            }
        }
        return result.toString();
    }

    String spellCastingTime() {
        JsonNode time = rootNode.withArray("time").get(0);
        return String.format("%s %s",
                SpellFields.number.getTextOrEmpty(time),
                SpellFields.unit.getTextOrEmpty(time));
    }

    Set<String> indexedSpellClasses(Tags tags) {
        Collection<String> list = index().classesForSpell(this.sources.getKey());
        if (list == null) {
            tui().debugf("No classes found for %s", this.sources.getKey());
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
                        String className = SpellFields.className.getTextOrEmpty(subclassNode).trim();
                        String classSource = SpellFields.classSource.getTextOrEmpty(subclassNode).trim();
                        return getSubclass(tags, className, classSource, subclassName, sources.primarySource(), k);
                    }
                    String className = sources.getName();
                    return getClass(tags, className, sources.primarySource(), k);
                })
                .collect(Collectors.toCollection(TreeSet::new));
    }

    Set<String> spellClasses(SpellSchool school, Tags tags) {
        JsonNode classesNode = SpellFields.classes.getFrom(rootNode);
        if (classesNode == null || classesNode.isNull()) {
            return Set.of();
        }
        Set<String> classes = new TreeSet<>();
        classesNode.withArray("fromClassList").forEach(c -> {
            String className = SourceField.name.getTextOrEmpty(c);
            String classSource = SourceField.source.getTextOrEmpty(c);
            String finalKey = Tools5eIndexType.classtype.createKey(className, classSource);
            if (index().isIncluded(finalKey)) {
                classes.add(getClass(tags, className, classSource, finalKey));
            }
        });
        classesNode.withArray("fromClassListVariant").forEach(c -> {
            String definedInSource = SpellFields.definedInSource.getTextOrEmpty(c);
            String className = SourceField.name.getTextOrEmpty(c);
            String classSource = SourceField.source.getTextOrEmpty(c);
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
            if (school == SpellSchool.SchoolEnum.Abjuration || school == SpellSchool.SchoolEnum.Evocation) {
                String finalKey = Tools5eIndexType.getSubclassKey("Fighter", "XPHB", "Eldritch Knight", "XPHB");
                if (index().isIncluded(finalKey)) {
                    classes.add(getSubclass(tags, "Fighter", "XPHB", "Eldritch Knight", "XPHB", finalKey));
                }
            }
            if (school == SpellSchool.SchoolEnum.Enchantment || school == SpellSchool.SchoolEnum.Illusion) {
                String finalKey = Tools5eIndexType.getSubclassKey("Rogue", "XPHB", "Arcane Trickster", "XPHB");
                if (index().isIncluded(finalKey)) {
                    classes.add(getSubclass(tags, "Rogue", "XPHB", "Arcane Trickster", "XPHB", finalKey));
                }
            }
        }
        return classes;
    }

    private String getClass(Tags tags, String className, String classSource, String classKey) {
        tags.add("spell", "class", className);
        return linkOrText(
                className,
                classKey,
                Tools5eIndexType.classtype.getRelativePath(),
                Tools5eQuteBase.getClassResource(className, classSource));
    }

    private String getSubclass(Tags tags, String className, String classSource, String subclassName,
            String subclassSource, String subclassKey) {
        tags.add("spell", "class", className, subclassName);
        return linkOrText(
                String.format("%s (%s)", className, subclassName),
                subclassKey,
                Tools5eIndexType.classtype.getRelativePath(),
                Tools5eQuteBase.getSubclassResource(subclassName, className, subclassSource));
    }

    enum SpellFields implements JsonNodeReader {
        amount,
        className,
        classSource,
        classes,
        components,
        distance,
        duration,
        entriesHigherLevel,
        level,
        meta,
        number,
        range,
        ritual,
        school,
        self,
        sight,
        special,
        text,
        touch,
        type,
        unit,
        unlimited,
        definedInSource,
    }
}
