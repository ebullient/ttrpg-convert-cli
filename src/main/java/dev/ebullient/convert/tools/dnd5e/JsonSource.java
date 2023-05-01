package dev.ebullient.convert.tools.dnd5e;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import io.quarkus.runtime.annotations.RegisterForReflection;

public interface JsonSource extends JsonTextReplacement {
    int CR_UNKNOWN = 100001;
    int CR_CUSTOM = 100000;

    default boolean textContains(List<String> haystack, String needle) {
        return haystack.stream().anyMatch(x -> x.contains(needle));
    }

    default String getTextOrEmpty(JsonNode x, String field) {
        if (x.has(field)) {
            return x.get(field).asText();
        }
        return "";
    }

    default String getTextOrDefault(JsonNode x, String field, String value) {
        if (x.has(field)) {
            return x.get(field).asText();
        }
        return value;
    }

    default boolean booleanOrDefault(JsonNode source, String key, boolean value) {
        JsonNode result = source.get(key);
        return result == null ? value : result.asBoolean(value);
    }

    default int intOrDefault(JsonNode source, String key, int value) {
        JsonNode result = source.get(key);
        return result == null ? value : result.asInt();
    }

    default JsonNode copyNode(JsonNode sourceNode) {
        try {
            return mapper().readTree(sourceNode.toString());
        } catch (JsonProcessingException ex) {
            tui().errorf(ex, "Unable to copy %s", sourceNode.toString());
            throw new IllegalStateException("JsonProcessingException processing " + sourceNode);
        }
    }

    default JsonNode copyReplaceNode(JsonNode sourceNode, Pattern replace, String with) {
        try {
            String modified = replace.matcher(sourceNode.toString()).replaceAll(with);
            return mapper().readTree(modified);
        } catch (JsonProcessingException ex) {
            tui().errorf(ex, "Unable to copy %s", sourceNode.toString());
            throw new IllegalStateException("JsonProcessingException processing " + sourceNode);
        }
    }

    default void appendEntryToText(List<String> text, JsonNode node, String heading) {
        boolean pushed = parseState.push(node); // store state
        try {
            if (node == null) {
                // do nothing
            } else if (node.isTextual()) {
                text.add(replaceText(node.asText()));
            } else if (node.isNumber()) {
                text.add(node.asText());
            } else if (node.isArray()) {
                node.elements().forEachRemaining(f -> {
                    maybeAddBlankLine(text);
                    appendEntryToText(text, f, heading);
                });
            } else if (node.isObject()) {
                appendEntryObjectToText(text, node, heading);
            } else {
                tui().errorf("Unknown entry type in %s: %s", getSources(), node.toPrettyString());
            }
        } finally {
            parseState.pop(pushed); // restore state
        }
    }

    default void appendEntryObjectToText(List<String> text, JsonNode node, String heading) {
        if (node.has("source") && !index().sourceIncluded(node.get("source").asText())) {
            if (!index().sourceIncluded(getSources().alternateSource())) {
                return;
            }
        }

        boolean pushed = parseState.push(node);
        try {
            if (node.has("type")) {
                String objectType = node.get("type").asText();
                switch (objectType) {
                    case "section":
                    case "entries": {
                        if (heading == null) {
                            List<String> inner = new ArrayList<>();
                            appendEntryToText(inner, node.get("entries"), null);
                            if (prependField(node, "name", inner)) {
                                maybeAddBlankLine(text);
                            }
                            text.addAll(inner);
                        } else if (node.has("name")) {
                            maybeAddBlankLine(text);
                            text.add(heading + " " + node.get("name").asText());
                            text.add("");
                            appendEntryToText(text, node.get("entries"), "#" + heading);
                        } else {
                            appendEntryToText(text, node.get("entries"), heading);
                        }
                        break;
                    }
                    case "entry":
                    case "itemSpell":
                    case "item": {
                        List<String> inner = new ArrayList<>();
                        appendEntryToText(inner, node.get("entry"), null);
                        appendEntryToText(inner, node.get("entries"), null);
                        if (prependField(node, "name", inner)) {
                            maybeAddBlankLine(text);
                        }
                        text.addAll(inner);
                        break;
                    }
                    case "link": {
                        text.add(node.get("text").asText());
                        break;
                    }
                    case "list": {
                        appendList(text, node.withArray("items"));
                        break;
                    }
                    case "abilityGeneric": {
                        List<String> abilities = new ArrayList<>();
                        node.withArray("attributes").forEach(x -> abilities.add(asAbilityEnum(x)));

                        List<String> inner = new ArrayList<>();
                        appendUnlessEmpty(inner, node, "name");
                        appendUnlessEmpty(inner, node, "text");
                        inner.add(String.join(", ", abilities));
                        inner.add("modifier");

                        maybeAddBlankLine(text);
                        text.add(String.join(" ", inner));
                        maybeAddBlankLine(text);
                        break;
                    }
                    case "table": {
                        appendTable(text, node);
                        break;
                    }
                    case "tableGroup": {
                        List<String> inner = new ArrayList<>();
                        maybeAddBlankLine(text);
                        inner.add("[!example] " + replaceText(node.get("name").asText()));
                        appendEntryToText(inner, node.get("tables"), "###");
                        inner.forEach(x -> text.add("> " + x));
                        maybeAddBlankLine(text);
                        break;
                    }
                    case "options":
                        appendOptions(text, node);
                        break;
                    case "inset":
                    case "insetReadaloud": {
                        appendInset(text, node);
                        break;
                    }
                    case "quote": {
                        appendQuote(text, node);
                        break;
                    }
                    case "abilityDc":
                        text.add(String.format("**Spell save DC**: 8 + your proficiency bonus + your %s modifier",
                                asAbilityEnum(node.withArray("attributes").get(0))));
                        break;
                    case "abilityAttackMod":
                        text.add(String.format("**Spell attack modifier**: your proficiency bonus + your %s modifier",
                                asAbilityEnum(node.withArray("attributes").get(0))));
                        break;
                    case "inline":
                    case "inlineBlock": {
                        List<String> inner = new ArrayList<>();
                        appendEntryToText(inner, node.get("entries"), null);
                        text.add(String.join("", inner));
                        break;
                    }
                    case "optfeature":
                        maybeAddBlankLine(text);
                        text.add(heading + " " + node.get("name").asText());
                        String prereq = getTextOrDefault(node, "prerequisite", null);
                        if (prereq != null) {
                            text.add("*Prerequisites* " + prereq);
                        }
                        text.add("");
                        appendEntryToText(text, node.get("entries"), "#" + heading);
                        break;
                    case "flowchart":
                        appendFlowchart(text, node, heading);
                        break;
                    case "refClassFeature":
                        text.add(node.get("classFeature").asText());
                        break;
                    case "refOptionalfeature":
                        text.add(node.get("optionalfeature").asText());
                        break;
                    case "refSubclassFeature":
                        text.add(node.get("subclassFeature").asText());
                        break;
                    case "gallery":
                    case "image":
                        // TODO: maybe someday?
                        break;
                    default:
                        tui().errorf("Unknown entry object type %s from %s: %s", objectType, getSources(),
                                node.toPrettyString());
                }
                // any entry/entries handled by type..
                return;
            }

            if (node.has("entry")) {
                appendEntryToText(text, node.get("entry"), heading);
            }
            if (node.has("entries")) {
                appendEntryToText(text, node.get("entries"), heading);
            }

            if (node.has("additionalEntries")) {
                String altSource = getSources().alternateSource();
                node.withArray("additionalEntries").forEach(entry -> {
                    if (entry.has("source") && !index().sourceIncluded(entry.get("source").asText())) {
                        return;
                    } else if (!index().sourceIncluded(altSource)) {
                        return;
                    }
                    appendEntryToText(text, entry, heading);
                });
            }
        } catch (RuntimeException ex) {
            tui().errorf(ex, "Error [%s] occurred while parsing %s", ex.getMessage(), node.toString());
            throw ex;
        } finally {
            parseState.pop(pushed);
        }
    }

    default boolean prependField(JsonNode entry, String fieldName, List<String> inner) {
        if (entry.has(fieldName)) {
            String n = replaceText(entry.get(fieldName).asText().trim());
            if (inner.isEmpty()) {
                inner.add(n);
            } else if (inner.get(0).startsWith("|")) {
                // we have a table..
                n = "**" + n + "** ";
                inner.add(0, "");
                inner.add(0, n);
                return true;
            } else {
                n = "**" + n.replace(":", "") + ".** ";
                inner.set(0, n + inner.get(0));
                return true;
            }
        }
        return false;
    }

    default void prependText(String prefix, List<String> inner) {
        if (inner.isEmpty()) {
            inner.add(prefix);
        } else {
            if (inner.get(0).isEmpty() && inner.size() > 1) {
                inner.set(1, prependText(prefix, inner.get(1)));
            } else {
                inner.set(0, prependText(prefix, inner.get(0)));
            }
        }
    }

    default String prependText(String prefix, String text) {
        return text.startsWith(prefix) ? text : prefix + text;
    }

    default void appendUnlessEmpty(List<String> text, JsonNode node, String field) {
        String value = getTextOrEmpty(node, field);
        if (!value.isEmpty()) {
            text.add(value);
        }
    }

    default void appendList(List<String> text, ArrayNode itemArray) {
        String indent = parseState.getListIndent();
        boolean pushed = parseState.indentList();
        try {
            maybeAddBlankLine(text);
            itemArray.forEach(e -> {
                List<String> item = new ArrayList<>();
                appendEntryToText(item, e, null);
                if (item.size() > 0) {
                    prependText(indent + "- ", item);
                    text.add(String.join("  \n" + indent, item));
                }
            });
        } finally {
            parseState.pop(pushed);
        }
    }

    default void appendTable(List<String> text, JsonNode entry) {
        List<String> table = new ArrayList<>();

        String header;
        String blockid = "";
        String caption = getTextOrEmpty(entry, "caption");

        if (entry.has("colLabels")) {
            header = StreamSupport.stream(entry.withArray("colLabels").spliterator(), false)
                    .map(x -> replaceText(x.asText()))
                    .collect(Collectors.joining(" | "));

            if (blockid.isEmpty()) {
                blockid = slugify(header.replaceAll("d\\d+", "")
                        .replace("|", "")
                        .replaceAll("\\s+", " ")
                        .trim());
            }
        } else if (entry.has("colStyles")) {
            header = StreamSupport.stream(entry.withArray("colStyles").spliterator(), false)
                    .map(x -> "  ")
                    .collect(Collectors.joining(" | "));
        } else {
            int length = entry.withArray("rows").size();
            String[] array = new String[length];
            Arrays.fill(array, " ");
            header = "|" + String.join(" | ", array) + " |";
        }

        entry.withArray("rows").forEach(r -> {
            JsonNode cells;
            if ("row".equals(getTextOrDefault(r, "type", null))) {
                cells = r.get("row");
            } else {
                cells = r;
            }

            String row = "| " +
                    StreamSupport.stream(cells.spliterator(), false)
                            .map(x -> replaceText(x.asText()))
                            .collect(Collectors.joining(" | "))
                    +
                    " |";
            table.add(row);
        });

        switch (blockid) {
            case "personality-trait":
                Json2QuteBackground.traits.addAll(table);
                break;
            case "ideal":
                Json2QuteBackground.ideals.addAll(table);
                break;
            case "bond":
                Json2QuteBackground.bonds.addAll(table);
                break;
            case "flaw":
                Json2QuteBackground.flaws.addAll(table);
                break;
        }

        header = "| " + header.replaceAll("^(d\\d+.*)", "dice: $1") + " |";
        table.add(0, header.replaceAll("[^|]", "-"));
        table.add(0, header);

        if (!caption.isBlank()) {
            table.add(0, "");
            table.add(0, "**" + caption + "**");
            blockid = slugify(caption);
        }
        if (!blockid.isBlank()) {
            table.add("^" + blockid);
        }

        maybeAddBlankLine(text);
        text.addAll(table);
    }

    default void appendOptions(List<String> text, JsonNode entry) {
        List<String> list = new ArrayList<>();
        entry.withArray("entries").forEach(e -> {
            List<String> item = new ArrayList<>();
            appendEntryToText(item, e, null);
            if (item.size() > 0) {
                prependText("- ", item);
                list.add(String.join("  \n    ", item)); // preserve line items
            }
        });
        if (list.size() > 0) {
            maybeAddBlankLine(text);
            int count = intOrDefault(entry, "count", 0);
            text.add(String.format("Options%s:",
                    count > 0 ? " (choose " + count + ")" : ""));
            maybeAddBlankLine(text);
            text.addAll(list);
        }
    }

    default void appendInset(List<String> text, JsonNode entry) {
        List<String> insetText = new ArrayList<>();
        String id = null;
        if (entry.has("name")) {
            id = entry.get("name").asText();
            insetText.add("[!quote] " + id);
            appendEntryToText(insetText, entry.get("entries"), null);
        } else if (getSources().getType() == Tools5eIndexType.race) {
            appendEntryToText(insetText, entry.get("entries"), null);
            id = insetText.remove(0);
            insetText.add(0, "[!quote] " + id);
        } else {
            if (entry.has("id")) {
                id = entry.get("id").asText();
            }
            insetText.add("[!quote] ...");
            appendEntryToText(insetText, entry.get("entries"), null);
        }

        maybeAddBlankLine(text);
        insetText.forEach(x -> text.add("> " + x));
        if (id != null) {
            text.add("^" + slugify(id));
        }
    }

    default void appendQuote(List<String> text, JsonNode entry) {
        List<String> quoteText = new ArrayList<>();
        if (entry.has("by")) {
            String by = replaceText(entry.get("by").asText());
            quoteText.add("[!quote]- A quote from " + by + "  ");
        } else {
            quoteText.add("[!quote]-  ");
        }
        appendEntryToText(quoteText, entry.get("entries"), null);

        maybeAddBlankLine(text);
        quoteText.forEach(x -> text.add("> " + x));
        maybeAddBlankLine(text);
    }

    default void appendFlowchart(List<String> text, JsonNode entry, String heading) {
        if (entry.has("name")) {
            maybeAddBlankLine(text);
            text.add(heading + " " + entry.get("name").asText());
        }

        for (JsonNode n : entry.withArray("blocks")) {
            maybeAddBlankLine(text);
            text.add("> [!flowchart] " + getTextOrEmpty(n, "name"));
            for (JsonNode e : n.withArray("entries")) {
                text.add("> " + replaceText(e.asText()));
            }
            text.add("%% %%");
        }
    }

    default String asAbilityEnum(JsonNode textNode) {
        return SkillOrAbility.format(textNode.asText());
    }

    default String mapAlignmentToString(String a) {
        switch (a) {
            case "A":
                return "Any alignment";
            case "C":
                return "Chaotic";
            case "CE":
                return "Chaotic Evil";
            case "CELENE":
            case "LNXCE":
                return "Any Evil Alignment";
            case "CG":
                return "Chaotic Good";
            case "CGNE":
                return "Chaotic Good or Neutral Evil";
            case "CGNYE":
                return "Any Chaotic alignment";
            case "CN":
                return "Chaotic Neutral";
            case "N":
            case "NX":
            case "NY":
                return "Neutral";
            case "NE":
                return "Neutral Evil";
            case "NG":
                return "Neutral Good";
            case "NGNE":
            case "NENG":
                return "Neutral Good or Neutral Evil";
            case "NNXNYN":
            case "NXCGNYE":
                return "Any Non-Lawful alignment";
            case "L":
                return "Lawful";
            case "LE":
                return "Lawful Evil";
            case "LG":
                return "Lawful Good";
            case "LN":
                return "Lawful Neutral";
            case "LNXCNYE":
                return "Any Non-Good alignment";
            case "E":
                return "Any Evil alignment";
            case "G":
                return "Any Good alignment";
            case "U":
                return "Unaligned";
        }
        tui().errorf("What alignment is this? %s (from %s)", a, getSources());
        return "Unknown";
    }

    default int levelToPb(int level) {
        // 2 + (¼ * (Level – 1))
        return 2 + ((int) (.25 * (level - 1)));
    }

    default String monsterCr(JsonNode monster) {
        if (monster.has("cr")) {
            JsonNode crNode = monster.get("cr");
            if (crNode.isTextual()) {
                return crNode.asText();
            } else if (crNode.has("cr")) {
                return crNode.get("cr").asText();
            } else {
                tui().errorf("Unable to parse cr value from %s", crNode.toPrettyString());
            }
        }
        return null;
    }

    default int crToXp(JsonNode cr) {
        if (cr != null && cr.has("xp")) {
            return cr.get("xp").asInt();
        }
        if (cr.has("cr")) {
            cr = cr.get("cr");
        }
        String crKey = cr.asText();
        return XP_CHART_ALT.get(crKey);
    }

    default int crToPb(JsonNode cr) {
        if (cr.isTextual()) {
            return crToPb(cr.asText());
        }
        return crToPb(cr.get("cr").asText());
    }

    default int crToPb(String crValue) {
        double crDouble = crToNumber(crValue);
        if (crDouble < 5)
            return 2;
        return (int) Math.ceil(crDouble / 4) + 1;
    }

    default double crToNumber(String crValue) {
        if (crValue.equals("Unknown") || crValue.equals("\u2014")) {
            return CR_UNKNOWN;
        }
        String[] parts = crValue.trim().split("/");
        try {
            if (parts.length == 1) {
                return Double.parseDouble(parts[0]);
            } else if (parts.length == 2) {
                return Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]);
            }
        } catch (NumberFormatException nfe) {
            return CR_CUSTOM;
        }
        return 0;
    }

    default String getSize(JsonNode value) {
        JsonNode size = value.get("size");
        if (size == null) {
            throw new IllegalArgumentException("Missing size attribute from " + getSources());
        }
        try {
            if (size.isTextual()) {
                return sizeToString(size.asText());
            } else if (size.isArray()) {
                String merged = streamOf((ArrayNode) size).map(JsonNode::asText).collect(Collectors.joining());
                return sizeToString(merged);
            }
        } catch (IllegalArgumentException ignored) {
        }
        tui().errorf("Unable to parse size for %s from %s", getSources(), size.toPrettyString());
        return "Unknown";
    }

    default String sizeToString(String size) {
        switch (size) {
            case "F":
                return "Fine";
            case "D":
                return "Diminutive";
            case "T":
                return "Tiny";
            case "S":
                return "Small";
            case "M":
                return "Medium";
            case "L":
                return "Large";
            case "H":
                return "Huge";
            case "G":
                return "Gargantuan";
            case "C":
                return "Colossal";
            case "V":
                return "Varies";
            case "SM":
                return "Small or Medium";
        }
        return "Unknown";
    }

    default String getSpeed(JsonNode value) {
        JsonNode speed = value.get("speed");
        try {
            if (speed == null) {
                return "30 ft.";
            } else if (speed.isTextual()) {
                return speed.asText();
            } else if (speed.isIntegralNumber()) {
                return speed.asText() + " ft.";
            } else if (speed.isObject()) {
                List<String> list = new ArrayList<>();
                speed.fields().forEachRemaining(f -> {
                    if (f.getValue().isIntegralNumber()) {
                        list.add(String.format("%s: %s ft.",
                                f.getKey(), f.getValue().asText()));
                    } else if (f.getValue().isBoolean()) {
                        list.add(f.getKey() + " equal to your walking speed");
                    }
                });
                return String.join("; ", list);
            }
        } catch (IllegalArgumentException ignored) {
        }
        tui().errorf("Unable to parse speed for %s from %s", getSources(), speed);
        return "30 ft.";
    }

    default String raceToText(JsonNode race) {
        StringBuilder str = new StringBuilder();
        str.append(race.get("name").asText());
        if (race.has("subrace")) {
            str.append(" (").append(race.get("subrace").asText()).append(")");
        }
        return str.toString();
    }

    default String levelToText(JsonNode levelNode) {
        if (levelNode.isObject()) {
            List<String> levelText = new ArrayList<>();
            levelText.add(levelToText(levelNode.get("level").asText()));
            if (levelNode.has("class") || levelNode.has("subclass")) {
                JsonNode classNode = levelNode.get("class");
                if (classNode == null) {
                    classNode = levelNode.get("subclass");
                }
                boolean visible = !classNode.has("visible") || classNode.get("visible").asBoolean();
                JsonNode source = classNode.get("source");
                boolean included = source == null || index().sourceIncluded(source.asText());
                if (visible && included) {
                    levelText.add(classNode.get("name").asText());
                }
            }
            return String.join(" ", levelText);
        } else {
            return levelToText(levelNode.asText());
        }
    }

    default String levelToText(String level) {
        switch (level) {
            case "0":
                return "cantrip";
            case "1":
                return "1st-level";
            case "2":
                return "2nd-level";
            case "3":
                return "3rd-level";
            default:
                return level + "th-level";
        }
    }

    static String levelToString(int level) {
        switch (level) {
            case 1:
                return "1st";
            case 2:
                return "2nd";
            case 3:
                return "3rd";
            default:
                return level + "th";
        }
    }

    @RegisterForReflection
    class JsonMediaHref {
        public String type;
        public JsonHref href;
        public String title;
    }

    @RegisterForReflection
    static class JsonHref {
        public String type;
        public String path;
        public String url;
    }

    Map<String, Integer> XP_CHART_ALT = Map.ofEntries(
            entry("0", 10),
            entry("1/8", 25),
            entry("1/4", 50),
            entry("1/2", 100),
            entry("1", 200),
            entry("2", 450),
            entry("3", 700),
            entry("4", 1100),
            entry("5", 1800),
            entry("6", 2300),
            entry("7", 2900),
            entry("8", 3900),
            entry("9", 5000),
            entry("10", 5900),
            entry("11", 7200),
            entry("12", 8400),
            entry("13", 10000),
            entry("14", 11500),
            entry("15", 13000),
            entry("16", 15000),
            entry("17", 18000),
            entry("18", 20000),
            entry("19", 22000),
            entry("20", 25000),
            entry("21", 33000),
            entry("22", 41000),
            entry("23", 50000),
            entry("24", 62000),
            entry("25", 75000),
            entry("26", 90000),
            entry("27", 105000),
            entry("28", 120000),
            entry("29", 135000),
            entry("30", 155000));
}
