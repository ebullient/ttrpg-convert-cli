package dev.ebullient.json5e.tools5e;

import static java.util.Map.entry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.json5e.io.Json5eTui;

public interface JsonSource {
    Pattern backgroundPattern = Pattern.compile("\\{@(background) ([^}]+)}");
    Pattern classPattern1 = Pattern.compile("\\{@(class) ([^}]+)}");
    Pattern featPattern = Pattern.compile("\\{@(feat) ([^}]+)}");
    Pattern itemPattern = Pattern.compile("\\{@(item) ([^}]+)}");
    Pattern racePattern = Pattern.compile("\\{@(race) ([^}]+)}");
    Pattern spellPattern = Pattern.compile("\\{@(spell) ([^}]+)}");
    Pattern creaturePattern = Pattern.compile("\\{@(creature) ([^}]+)}");
    Pattern dicePattern = Pattern.compile("\\{@(dice|damage) ([^|}]+)[^}]*}");
    Pattern chancePattern = Pattern.compile("\\{@chance ([^}]+)}");
    Pattern notePattern = Pattern.compile("\\{@note (\\*|Note:)?\\s?([^}]+)}");
    Pattern condPattern = Pattern.compile("\\{@condition ([^|}]+)\\|?[^}]*}");
    Pattern diseasePattern = Pattern.compile("\\{@disease ([^|}]+)\\|?[^}]*}");
    Pattern skillPattern = Pattern.compile("\\{@skill ([^}]+)}");
    Pattern sensePattern = Pattern.compile("\\{@sense ([^}]+)}");
    int CR_UNKNOWN = 100001;
    int CR_CUSTOM = 100000;

    JsonIndex index();

    CompendiumSources getSources();

    default Json5eTui tui() {
        return index().tui;
    }

    default String slugify(String s) {
        return index().tui.slugify(s);
    }

    default Stream<JsonNode> streamOf(ArrayNode array) {
        return StreamSupport.stream(array.spliterator(), false);
    }

    default List<String> findAndReplace(JsonNode jsonSource, String field) {
        return findAndReplace(jsonSource, field, s -> s);
    }

    default List<String> findAndReplace(JsonNode jsonSource, String field, Function<String, String> replacement) {
        JsonNode node = jsonSource.get(field);
        if (node == null || node.isNull()) {
            return List.of();
        } else if (node.isTextual()) {
            return List.of(replaceText(node.asText()));
        } else if (node.isObject()) {
            throw new IllegalArgumentException(
                    String.format("Unexpected object node (expected array): %s (referenced from %s)", node,
                            getSources()));
        }
        return streamOf(jsonSource.withArray(field))
                .map(x -> replaceText(x.asText()).trim())
                .map(replacement)
                .filter(x -> !x.isBlank())
                .collect(Collectors.toList());
    }

    default String joinAndReplace(JsonNode jsonSource, String field) {
        JsonNode node = jsonSource.get(field);
        if (node == null || node.isNull()) {
            return "";
        } else if (node.isTextual()) {
            return node.asText();
        } else if (node.isObject()) {
            throw new IllegalArgumentException(
                    String.format("Unexpected object node (expected array): %s (referenced from %s)", node,
                            getSources()));
        }
        return joinAndReplace((ArrayNode) node);
    }

    default String joinAndReplace(ArrayNode array) {
        List<String> list = new ArrayList<>();
        array.forEach(v -> list.add(replaceText(v.asText())));
        return String.join(", ", list);
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

    default String getOrEmptyIfEqual(JsonNode x, String field, String expected) {
        if (x.has(field)) {
            String value = x.get(field).asText().trim();
            return value.equalsIgnoreCase(expected) ? "" : value;
        }
        return "";
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
            return Json5eTui.MAPPER.readTree(sourceNode.toString());
        } catch (JsonProcessingException ex) {
            tui().errorf(ex, "Unable to copy %s", sourceNode.toString());
            throw new IllegalStateException("JsonProcessingException processing " + sourceNode);
        }
    }

    default JsonNode copyReplaceNode(JsonNode sourceNode, Pattern replace, String with) {
        try {
            String modified = replace.matcher(sourceNode.toString()).replaceAll(with);
            return Json5eTui.MAPPER.readTree(modified);
        } catch (JsonProcessingException ex) {
            tui().errorf(ex, "Unable to copy %s", sourceNode.toString());
            throw new IllegalStateException("JsonProcessingException processing " + sourceNode);
        }
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

    default String getMonsterType(JsonNode node) {
        if (node == null || !node.has("type")) {
            tui().warn("Empty type for " + getSources());
            return null;
        }
        JsonNode typeNode = node.get("type");
        if (typeNode.isTextual()) {
            return typeNode.asText();
        } else if (typeNode.has("type")) {
            // We have an object: type + tags
            return typeNode.get("type").asText();
        }
        return null;
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

    default void maybeAddBlankLine(List<String> text) {
        if (text.size() > 0 && !text.get(text.size() - 1).isBlank()) {
            text.add("");
        }
    }

    default void appendEntryToText(List<String> text, JsonNode node, String heading) {
        if (node == null) {
            // do nothing
        } else if (node.isTextual()) {
            text.add(replaceText(node.asText()));
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
    }

    default void appendEntryObjectToText(List<String> text, JsonNode node, String heading) {
        if (node.has("source") && !index().sourceIncluded(node.get("source").asText())) {
            if (!index().sourceIncluded(getSources().alternateSource())) {
                return;
            }
        }

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
    }

    default boolean prependField(JsonNode entry, String fieldName, List<String> inner) {
        if (entry.has(fieldName)) {
            String n = entry.get(fieldName).asText();
            if (inner.isEmpty()) {
                inner.add(n);
            } else {
                n = replaceText(n.trim().replace(":", ""));
                n = "**" + n + ".** ";
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
        maybeAddBlankLine(text);
        itemArray.forEach(e -> {
            List<String> item = new ArrayList<>();
            appendEntryToText(item, e, null);
            if (item.size() > 0) {
                prependText("- ", item);
                text.add(String.join("  \n    ", item)); // preserve line items
            }
        });
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
            StringBuilder row = new StringBuilder()
                    .append("| ")
                    .append(StreamSupport.stream(r.spliterator(), false)
                            .map(x -> replaceText(x.asText()))
                            .collect(Collectors.joining(" | ")))
                    .append(" |");
            table.add(row.toString());
        });

        if (blockid.equals("personality-trait")) {
            Json2QuteBackground.traits.addAll(table);
        } else if (blockid.equals("ideal")) {
            Json2QuteBackground.ideals.addAll(table);
        } else if (blockid.equals("bond")) {
            Json2QuteBackground.bonds.addAll(table);
        } else if (blockid.equals("flaw")) {
            Json2QuteBackground.flaws.addAll(table);
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
        } else if (getSources().type == IndexType.race) {
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

    default String decoratedRaceName(JsonNode jsonSource, CompendiumSources sources) {
        String raceName = sources.getName();
        JsonNode raceNameNode = jsonSource.get("raceName");
        if (raceNameNode != null) {
            raceName = String.format("%s (%s)", raceNameNode.asText(), raceName);
        }
        return decoratedTypeName(raceName.replace("Variant; ", ""), getSources());
    }

    default String decorateMonsterName(JsonNode jsonSource, CompendiumSources sources) {
        return sources.getName().replace("\"", "");
    }

    default String decoratedTypeName(CompendiumSources sources) {
        return decoratedTypeName(sources.name, sources);
    }

    default String decoratedTypeName(String name, CompendiumSources sources) {
        if (sources.isPrimarySource("DMG") && !name.contains("(DMG)")) {
            return name + " (DMG)";
        }
        if (sources.isFromUA() && !name.contains("(UA)")) {
            return name + " (UA)";
        }
        return name;
    }

    default String decoratedFeatureTypeName(CompendiumSources valueSources, JsonNode value) {
        String name = decoratedTypeName(value.get("name").asText(), valueSources);
        String type = getTextOrEmpty(value, "featureType");

        if (!type.isEmpty()) {
            switch (type) {
                case "ED":
                    return "Elemental Discipline: " + name;
                case "EI":
                    return "Eldritch Invocation: " + name;
                case "MM":
                    return "Metamagic: " + name;
                case "MV":
                case "MV:B":
                case "MV:C2-UA":
                    return "Maneuver: " + name;
                case "FS:F":
                case "FS:B":
                case "FS:R":
                case "FS:P":
                    return "Fighting Style: " + name;
                case "AS":
                case "AS:V1-UA":
                case "AS:V2-UA":
                    return "Arcane Shot: " + name;
                case "PB":
                    return "Pact Boon: " + name;
                case "AI":
                    return "Artificer Infusion: " + name;
                case "SHP:H":
                case "SHP:M":
                case "SHP:W":
                case "SHP:F":
                case "SHP:O":
                    return "Ship Upgrade: " + name;
                case "IWM:W":
                    return "Infernal War Machine Variant: " + name;
                case "IWM:A":
                case "IWM:G":
                    return "Infernal War Machine Upgrade: " + name;
                case "OR":
                    return "Onomancy Resonant: " + name;
                case "RN":
                    return "Rune Knight Rune: " + name;
                case "AF":
                    return "Alchemical Formula: " + name;
                default:
                    tui().errorf("Unknown feature type %s for class feature %s", type, name);
            }
        }

        return name;
    }

    default String asAbilityEnum(JsonNode textNode) {
        return SkillOrAbility.format(textNode.asText());
    }

    default String replaceText(String input) {
        String result = input;

        // "{@atk mw} {@hit 1} to hit, reach 5 ft., one target. {@h}1 ({@damage 1d4 ‒1})
        // piercing damage."
        // "{@atk mw} {@hit 4} to hit, reach 5 ft., one target. {@h}1 ({@damage 1d4+2})
        // slashing damage."
        // "{@atk mw} {@hit 14} to hit, one target. {@h}22 ({@damage 3d8}) piercing
        // damage. Target must make a {@dc 19} Dexterity save, or be swallowed by the
        // worm!"
        try {
            result = result
                    .replace("#$prompt_number:title=Enter Alert Level$#", "Alert Level")
                    .replace("#$prompt_number:title=Enter Charisma Modifier$#", "Charisma modifier")
                    .replace("#$prompt_number:title=Enter Lifestyle Modifier$#", "Charisma modifier")
                    .replace("#$prompt_number:title=Enter a Modifier$#", "Modifier")
                    .replace("#$prompt_number:title=Enter a Modifier,default=10$#", "Modifier (default 10)");
            result = dicePattern.matcher(result)
                    .replaceAll((match) -> match.group(2));
            result = chancePattern.matcher(result)
                    .replaceAll((match) -> match.group(1) + "% chance");

            result = backgroundPattern.matcher(result).replaceAll(this::linkify);
            result = classPattern1.matcher(result).replaceAll(this::linkify);
            result = creaturePattern.matcher(result).replaceAll(this::linkify);
            result = featPattern.matcher(result).replaceAll(this::linkify);
            result = itemPattern.matcher(result).replaceAll(this::linkify);
            result = racePattern.matcher(result).replaceAll(this::linkify);
            result = spellPattern.matcher(result).replaceAll(this::linkify);

            result = condPattern.matcher(result)
                    .replaceAll((match) -> linkifyRules(match.group(1), "conditions"));

            result = diseasePattern.matcher(result)
                    .replaceAll((match) -> linkifyRules(match.group(1), "diseases"));

            result = sensePattern.matcher(result)
                    .replaceAll((match) -> linkifyRules(match.group(1), "senses"));

            result = skillPattern.matcher(result)
                    .replaceAll((match) -> linkifyRules(match.group(1), "skills"));

            result = notePattern.matcher(result)
                    .replaceAll((match) -> {
                        List<String> text = new ArrayList<>();
                        text.add("> [!note]");
                        for (String line : match.group(2).split("\n")) {
                            text.add("> " + line);
                        }
                        return String.join("\n", text);
                    });

            result = result
                    .replace("{@hitYourSpellAttack}", "the summoner's spell attack modifier")
                    .replaceAll("\\{@link ([^}|]+)\\|([^}]+)}", "$1 ($2)") // this must come first
                    .replaceAll("\\{@5etools ([^}|]+)\\|?[^}]*}", "$1")
                    .replaceAll("\\{@area ([^|}]+)\\|?[^}]*}", "$1")
                    .replaceAll("\\{@action ([^}]+)\\|?[^}]*}", "$1")
                    .replaceAll("\\{@hazard ([^|}]+)\\|?[^}]*}", "$1")
                    .replaceAll("\\{@reward ([^|}]+)\\|?[^}]*}", "$1")
                    .replaceAll("\\{@dc ([^}]+)}", "DC $1")
                    .replaceAll("\\{@d20 ([^}]+?)}", "$1")
                    .replaceAll("\\{@recharge ([^}]+?)}", "(Recharge $1-6)")
                    .replaceAll("\\{@recharge}", "(Recharge 6)")
                    .replaceAll("\\{@filter ([^|}]+)\\|?[^}]*}", "$1")
                    .replaceAll("\\{@classFeature ([^|}]+)\\|?[^}]*}", "$1")
                    .replaceAll("\\{@optfeature ([^|}]+)\\|?[^}]*}", "$1")
                    .replaceAll("\\{@cult ([^|}]+)\\|([^|}]+)\\|[^|}]*}", "$2")
                    .replaceAll("\\{@cult ([^|}]+)\\|[^}]*}", "$1")
                    .replaceAll("\\{@deity ([^|}]+)\\|?[^}]*}", "$1")
                    .replaceAll("\\{@language ([^|}]+)\\|?[^}]*}", "$1")
                    .replaceAll("\\{@quickref ([^|}]+)\\|?[^}]*}", "$1")
                    .replaceAll("\\{@table ([^|}]+)\\|?[^}]*}", "$1")
                    .replaceAll("\\{@variantrule ([^|}]+)\\|?[^}]*}", "$1")
                    .replaceAll("\\{@book ([^}|]+)\\|?[^}]*}", "\"$1\"")
                    .replaceAll("\\{@hit ([^}]+)}", "+$1")
                    .replaceAll("\\{@h}", "Hit: ")
                    .replaceAll("\\{@atk m}", "*Melee Attack:*")
                    .replaceAll("\\{@atk mw}", "*Melee Weapon Attack:*")
                    .replaceAll("\\{@atk rw}", "*Ranged Weapon Attack:*")
                    .replaceAll("\\{@atk mw,rw}", "*Melee or Ranged Weapon Attack:*")
                    .replaceAll("\\{@atk ms}", "*Melee Spell Attack:*")
                    .replaceAll("\\{@atk rs}", "*Ranged Spell Attack:*")
                    .replaceAll("\\{@atk ms,rs}", "*Melee or Ranged Spell Attack:*")
                    .replaceAll("\\{@b ([^}]+?)}", "**$1**")
                    .replaceAll("\\{@bold ([^}]+?)}", "**$1**")
                    .replaceAll("\\{@i ([^}]+?)}", "_$1_")
                    .replaceAll("\\{@italic ([^}]+)}", "_$1_");

        } catch (Exception e) {
            tui().errorf(e, "Unable to parse string from %s: %s", getSources().getKey(), input);
        }
        // after other replacements
        return result.replaceAll("\\{@adventure ([^|}]+)\\|[^}]*}", "$1");
    }

    default String linkifyRules(String text, String rules) {
        return String.format("[%s](%s%s.md#%s)",
                text, index().rulesRoot(), rules,
                text.replace(" ", "%20")
                        .replace(".", ""));
    }

    default String linkify(MatchResult match) {
        switch (match.group(1)) {
            case "background":
                // "Backgrounds:
                // {@background Charlatan} assumes PHB by default,
                // {@background Anthropologist|toa} can have sources added with a pipe,
                // {@background Anthropologist|ToA|and optional link text added with another
                // pipe}.",
                return linkifyType(IndexType.background, match.group(2), "backgrounds");
            case "creature":
                // "Creatures:
                // {@creature goblin} assumes MM by default,
                // {@creature cow|vgm} can have sources added with a pipe,
                // {@creature cow|vgm|and optional link text added with another pipe}.",
                return linkifyCreature(match.group(2));
            case "class":
                return linkifyClass(match.group(2));
            case "feat":
                // "Feats:
                // {@feat Alert} assumes PHB by default,
                // {@feat Elven Accuracy|xge} can have sources added with a pipe,
                // {@feat Elven Accuracy|xge|and optional link text added with another pipe}.",
                return linkifyType(IndexType.feat, match.group(2), "feats");
            case "item":
                // "Items:
                // {@item alchemy jug} assumes DMG by default,
                // {@item longsword|phb} can have sources added with a pipe,
                // {@item longsword|phb|and optional link text added with another pipe}.",
                return linkifyType(IndexType.item, match.group(2), "items", "dmg");
            case "race":
                // "Races:
                // {@race Human} assumes PHB by default,
                // {@race Aasimar (Fallen)|VGM}
                // {@race Aarakocra|eepc} can have sources added with a pipe,
                // {@race Aarakocra|eepc|and optional link text added with another pipe}.",
                // {@race dwarf (hill)||Dwarf, hill}
                return linkifyType(IndexType.race, match.group(2), "races");
            case "spell":
                // "Spells:
                // {@spell acid splash} assumes PHB by default,
                // {@spell tiny servant|xge} can have sources added with a pipe,
                // {@spell tiny servant|xge|and optional link text added with another pipe}.",
                return linkifyType(IndexType.spell, match.group(2), "spells");
        }
        throw new IllegalArgumentException("Unknown group to linkify: " + match.group(1));
    }

    default String linkOrText(String linkText, String key, String dirName, String resourceName) {
        return index().isIncluded(key)
                ? String.format("[%s](%s%s/%s.md)",
                        linkText, index().compendiumRoot(), dirName, slugify(resourceName))
                : linkText;
    }

    default String linkifyType(IndexType type, String match, String dirName) {
        return linkifyType(type, match, dirName, "phb");
    }

    default String linkifyType(IndexType type, String match, String dirName, String defaultSource) {
        String[] parts = match.split("\\|");
        String linkText = parts[0];
        String source = defaultSource;
        if (parts.length > 2) {
            linkText = parts[2];
        }
        if (parts.length > 1) {
            source = parts[1].isBlank() ? source : parts[1];
        }
        String key = index().createSimpleKey(type, parts[0], source);
        if (index().isExcluded(key)) {
            return linkText;
        }
        JsonNode jsonSource = getJsonNodeForKey(key);
        CompendiumSources sources = index().constructSources(type, jsonSource);
        if (type == IndexType.item) {
            return linkOrText(linkText, key, dirName, parts[0]);
        } else if (type == IndexType.race) {
            return linkOrText(linkText, key, dirName, decoratedRaceName(jsonSource, sources));
        }
        return linkOrText(linkText, key, dirName, decoratedTypeName(sources));
    }

    default String linkifyClass(String match) {
        // "Classes:
        // {@class fighter} assumes PHB by default,
        // {@class artificer|uaartificer} can have sources added with a pipe,
        // {@class fighter|phb|optional link text added with another pipe},
        // {@class fighter|phb|subclasses added|Eldritch Knight} with another pipe,
        // {@class fighter|phb|and class feature added|Eldritch Knight|phb|2-0} with
        // another pipe
        // (first number is level index (0-19), second number is feature index (0-n)).",
        String[] parts = match.split("\\|");
        String className = parts[0];
        String classSource = "phb";
        String linkText = className;
        String subclass = null;

        if (parts.length > 3) {
            subclass = parts[3];
        }
        if (parts.length > 2) {
            linkText = parts[2];
        }
        if (parts.length > 1) {
            classSource = parts[1];
        }

        if (subclass != null) {
            String key = index().getSubclassKey(subclass, className, classSource);
            return linkOrText(linkText, key, "classes", className + "-" + subclass);
        } else {
            String key = index().getClassKey(className, classSource);
            return linkOrText(linkText, key, "classes", className);
        }
    }

    default String linkifyCreature(String match) {
        // "Creatures:
        // {@creature goblin} assumes MM by default,
        // {@creature cow|vgm} can have sources added with a pipe,
        // {@creature cow|vgm|and optional link text added with another pipe}.",
        String[] parts = match.trim().split("\\|");
        String linkText = parts[0];
        String source = "mm";
        if (parts.length > 2) {
            linkText = parts[2];
        }
        if (parts.length > 1) {
            source = parts[1].isBlank() ? source : parts[1];
        }
        String key = index().createSimpleKey(IndexType.monster, parts[0], source);
        if (index().isExcluded(key)) {
            return linkText;
        }
        JsonNode jsonSource = getJsonNodeForKey(key);
        CompendiumSources sources = index().constructSources(IndexType.monster, jsonSource);
        String resourceName = decorateMonsterName(jsonSource, sources);
        final String subdir;
        if (Json2QuteMonster.isNpc(jsonSource)) {
            subdir = "npc";
        } else {
            String type = getMonsterType(jsonSource); // may be missing for partial index
            if (type == null) {
                return linkText;
            }
            subdir = slugify(type);
        }
        return linkOrText(linkText, key, "bestiary/" + subdir, resourceName);
    }

    default JsonNode getJsonNodeForKey(String key) {
        String alias = index().getAlias(key);
        if (alias != null) {
            return index().getNode(alias);
        }
        return index().getNode(key);
    }

    static final Map<String, Integer> XP_CHART_ALT = Map.ofEntries(
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
