package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.SpellEntry.SpellReference;
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
        SpellEntry spellEntry = index().getSpellIndex().getSpellEntry(getSources().getKey());

        Tags tags = new Tags(getSources());

        tags.add("spell", "school", spellEntry.school.name());
        tags.add("spell", "level", JsonSource.spellLevelToText(spellEntry.level));
        if (spellEntry.ritual) {
            tags.add("spell", "ritual");
        }

        // 🔧 Spell: spell|fireball|phb,
        //    references: {subclass|destruction domain|cleric|phb|vss=subclass|destruction domain|cleric|phb|vss;c:5;s:null;null, ...}
        //    expanded: {subclass|the fiend|warlock|phb|phb=subclass|the fiend|warlock|phb|phb;c:null;s:3;null, ...}
        List<String> referenceLinks = new ArrayList<>();
        Set<SpellReference> allRefs = new TreeSet<>(Comparator.comparing(x -> x.refererKey));
        allRefs.addAll(spellEntry.references.values());
        allRefs.addAll(spellEntry.expandedList.values());

        for (var r : allRefs) {
            tags.addRaw(r.tagifyReference());
            referenceLinks.add(r.linkifyReference());
        }

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
                JsonSource.spellLevelToText(spellEntry.level),
                spellEntry.school.name(),
                spellEntry.ritual,
                spellCastingTime(),
                spellRange(),
                spellComponents(),
                spellDuration(),
                referenceLinks,
                getFluffImages(Tools5eIndexType.spellFluff),
                String.join("\n", text),
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
                case "r" -> list.add("R"); // Royalty. Acquisitions Incorporated
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
            String type = SpellFields.type.getTextOrEmpty(ends);
            if ("timed".equals(type)) {
                result.append(" up to ");
            }
            addDuration(ends, result);
        }
        return result.toString();
    }

    void addDuration(JsonNode element, StringBuilder result) {
        String type = SpellFields.type.getTextOrEmpty(element);
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
        spellAttack,
    }
}
