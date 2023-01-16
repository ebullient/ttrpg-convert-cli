package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.io.Tui;

public interface JsonSource {

    ToolsPf2eIndex index();

    ToolsPf2eSources getSources();

    default String slugify(String s) {
        return tui().slugify(s);
    }

    default Tui tui() {
        return cfg().tui();
    }

    default CompendiumConfig cfg() {
        return index().cfg();
    }

    default Stream<JsonNode> streamOf(ArrayNode array) {
        return StreamSupport.stream(array.spliterator(), false);
    }

    default boolean textContains(List<String> haystack, String needle) {
        return haystack.stream().anyMatch(x -> x.contains(needle));
    }

    default void appendEntryToText(List<String> text, JsonNode node, String heading) {
        if (node == null || node.isNull()) {
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
        String source = Field.source.getTextOrEmpty(node);
        if (!source.isEmpty() && !index().sourceIncluded(source)) {
            if (!index().sourceIncluded(getSources().alternateSource())) {
                return;
            }
        }

        AppendTypeValue type = AppendTypeValue.from(Field.type.getTextOrNull(node));
        if (type != null) {
            switch (type) {
                case section:
                case pf2h1:
                    appendTextHeaderBlock(text, node, "#");
                    break;
                case pf2h1flavor:
                    appendTextHeaderFlavorBlock(text, node);
                    break;
                case pf2h2:
                    appendTextHeaderBlock(text, node, "##");
                    break;
                case pf2h3:
                    appendTextHeaderBlock(text, node, "###");
                    break;
                case pf2h4:
                    appendTextHeaderBlock(text, node, "####");
                    break;
                case pf2h5:
                    appendTextHeaderBlock(text, node, "#####");
                    break;

                // callout boxes
                case pf2sidebar:
                    appendCallout(text, node, "pf2-sidebar");
                    break;
                case pf2inset:
                    appendCallout(text, node, "pf2-inset");
                    break;
                case pf2tipsBox:
                    appendCallout(text, node, "tip");
                    break;
                case pf2sampleBox:
                    appendCallout(text, node, "example");
                    break;
                case pf2beigeBox:
                    appendCallout(text, node, "pf2-beige");
                    break;
                case pf2redBox:
                    appendCallout(text, node, "pf2-red");
                    break;
                case pf2brownBox:
                    appendCallout(text, node, "pf2-brown");
                    break;

                case pf2keyAbility:
                    appendKeyAbility(text, node, "pf2-key-box");
                    break;
                case pf2keyBox:
                    appendCallout(text, node, "pf2-key-box");
                    break;

                case pf2title:
                    appendTextHeaderBlock(text, node,
                            heading == null ? null : heading + "#");
                    break;
                case pf2options:
                    appendList(text, Field.items.withArrayFrom(node));
                    break;

                case entries:
                    appendEntryToText(text, AppendTypeValue.entries.getFrom(node), heading);
                    break;
                case entriesOtherSource:
                    tui().debugf("TODO: %s %s", type, node.toString());
                    break;

                case list:
                    appendList(text, Field.items.withArrayFrom(node));
                    break;
                case table:
                    appendTable(text, node);
                    break;
                case paper:
                    appendPaper(text, node, "pf2-paper");
                    break;
                case quote:
                    appendQuote(text, node);
                    break;

                // pf2-abilities
                case ability:
                    appendAbility(text, node);
                    break;
                case affliction:
                    tui().debugf("TODO: %s %s", type, node.toString());
                    break;
                case attack:
                    tui().debugf("TODO: %s %s", type, node.toString());
                    break;
                case lvlEffect:
                    tui().debugf("TODO: %s %s", type, node.toString());
                    break;

                // list items
                case item:
                    appendListItem(text, node);
                    break;

                default:
                    tui().debugf("TODO / How did I get here?: %s %s", type, node.toString());
                    break;
            }
        }
        appendEntryToText(text, Field.entry.getFrom(node), heading);
        appendEntryToText(text, Field.entries.getFrom(node), heading);
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

    default boolean prependField(JsonNode entry, Field field, List<String> inner) {
        String n = field.getTextOrNull(entry);
        if (n != null) {
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

    default void appendAbility(List<String> text, JsonNode node) {
        List<String> inner = new ArrayList<>();
        appendEntryToText(inner, Field.entry.getFrom(node), null);
        appendEntryToText(inner, Field.entries.getFrom(node), null);

        String name = Field.name.getTextOrNull(node);
        if (name != null) {
            if (inner.isEmpty()) {
                inner.add(name);
            } else {
                name = replaceText(name.trim().replace(":", ""));
                name = "***" + name + ".*** ";
                inner.set(0, name + inner.get(0));
                maybeAddBlankLine(text);
            }
        }
        text.addAll(inner);
        text.add("CHECK ME: ABILITY ABOVE");
    }

    default void appendTextHeaderBlock(List<String> text, JsonNode node, String heading) {
        if (heading == null) {
            List<String> inner = new ArrayList<>();
            appendEntryToText(inner, Field.entry.getFrom(node), heading);
            appendEntryToText(inner, Field.entries.getFrom(node), heading);
            if (prependField(node, Field.name, inner)) {
                maybeAddBlankLine(text);
            }
            text.addAll(inner);
        } else {
            maybeAddBlankLine(text);
            text.add(heading + " " + node.get("name").asText());
            text.add("");
            appendEntryToText(text, Field.entry.getFrom(node), heading);
            appendEntryToText(text, Field.entries.getFrom(node), heading);
        }
    }

    default void appendTextHeaderFlavorBlock(List<String> text, JsonNode node) {
        List<String> inner = new ArrayList<>();
        inner.add("[!tip] " + Field.name.getTextOrEmpty(node));
        appendEntryToText(inner, Field.entries.getFrom(node), null);
        inner.forEach(x -> text.add("> " + x));
        maybeAddBlankLine(text);
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

    default void appendListItem(List<String> text, JsonNode itemNode) {
        List<String> inner = new ArrayList<>();
        appendEntryToText(inner, Field.entry.getFrom(itemNode), null);
        appendEntryToText(inner, Field.entries.getFrom(itemNode), null);
        if (prependField(itemNode, Field.name, inner)) {
            maybeAddBlankLine(text);
        }
        text.addAll(inner);
    }

    default void appendPaper(List<String> text, JsonNode paper, String callout) {
        List<String> paperText = new ArrayList<>();
        String title = Field.title.getTextOrDefault(paper, "A letter");

        paperText.add("[!" + callout + "] " + replaceText(title));

        appendEntryToText(paperText, Field.head.getFrom(paper), null);
        maybeAddBlankLine(paperText);
        appendEntryToText(paperText, Field.entry.getFrom(paper), null);
        maybeAddBlankLine(paperText);
        appendEntryToText(paperText, Field.signature.getFrom(paper), null);

        maybeAddBlankLine(text);
        paperText.forEach(x -> text.add("> " + x));
    }

    default void appendQuote(List<String> text, JsonNode entry) {
        List<String> quoteText = new ArrayList<>();
        String by = Field.by.getTextOrEmpty(entry);
        if (by.isEmpty()) {
            quoteText.add("[!quote]-  ");
        } else {
            quoteText.add("[!quote]- A quote from " + replaceText(by) + "  ");
        }
        appendEntryToText(quoteText, Field.entry.getFrom(entry), null);
        appendEntryToText(quoteText, Field.entries.getFrom(entry), null);

        maybeAddBlankLine(text);
        quoteText.forEach(x -> text.add("> " + x));
        maybeAddBlankLine(text);
    }

    default void appendCallout(List<String> text, JsonNode entry, String callout) {
        List<String> insetText = new ArrayList<>();
        String name = Field.name.getTextOrEmpty(entry);

        insetText.add("[!" + callout + "] " + replaceText(name));
        appendEntryToText(insetText, Field.entry.getFrom(entry), null);
        appendEntryToText(insetText, Field.entries.getFrom(entry), null);

        maybeAddBlankLine(text);
        insetText.forEach(x -> text.add("> " + x));
    }

    default void appendKeyAbility(List<String> text, JsonNode entry, String callout) {
        List<String> insetText = new ArrayList<>();
        String name = Field.name.getTextOrEmpty(entry);

        insetText.add("[!" + callout + "] " + replaceText(name));
        // TODO

        appendEntryToText(insetText, Field.entry.getFrom(entry), null);
        appendEntryToText(insetText, Field.entries.getFrom(entry), null);

        maybeAddBlankLine(text);
        insetText.forEach(x -> text.add("> " + x));
    }

    default void appendTable(List<String> text, JsonNode tableNode) {
        List<String> table = new ArrayList<>();

        String blockid = "";

        String name = Field.name.getTextOrEmpty(tableNode);
        String id = Field.id.getTextOrEmpty(tableNode);

        ArrayNode rows = Field.rows.withArrayFrom(tableNode);
        List<Integer> labelIdx = Field.labelRowIdx.fromTo(tableNode, Tui.LIST_INT, tui());

        if (!name.isEmpty()) {
            blockid = slugify(name + " " + id);
        }

        // TODO: No label index
        // {
        //     int length = entry.withArray("rows").size();
        //     String[] array = new String[length];
        //     Arrays.fill(array, " ");
        //     header = "|" + String.join(" | ", array) + " |";
        // }

        for (int r = 0; r < rows.size(); r++) {
            JsonNode rowNode = rows.get(r);

            if (labelIdx.contains(r)) {
                String header = StreamSupport.stream(rowNode.spliterator(), false)
                        .map(x -> replaceText(x.asText()))
                        .collect(Collectors.joining(" | "));

                // make rollable dice headers
                header = "| " + header.replaceAll("^(d\\d+.*)", "dice: $1") + " |";

                if (r == 0 && blockid.isBlank()) {
                    blockid = slugify(header.replaceAll("d\\d+", "")
                            .replace("|", "")
                            .replaceAll("\\s+", " ")
                            .trim());
                } else {
                    if (!blockid.isEmpty()) {
                        table.add("^" + blockid + "-" + r);
                    }
                    table.add("");
                }
                table.add(header);
                table.add(header.replaceAll("[^|]", "-"));

            } else if (FieldValue.multiRow.isFieldValue(rows, Field.type)) {
                ArrayNode rows2 = Field.rows.withArrayFrom(rowNode);
                for (int j = 0; j < rows2.size(); j++) {
                    final int rindex = j;
                    String row = "| " +
                            StreamSupport.stream(rowNode.spliterator(), false)
                                    .map(x -> replaceText(x.asText()))
                                    .map(x -> rindex == 0 ? "**" + x + "**" : x)
                                    .collect(Collectors.joining(" | "))
                            +
                            " |";
                    table.add(row);
                }

            } else {
                String row = "| " +
                        StreamSupport.stream(rowNode.spliterator(), false)
                                .map(x -> replaceText(x.asText()))
                                .collect(Collectors.joining(" | "))
                        +
                        " |";
                table.add(row);
            }
        }

        JsonNode intro = Field.intro.getFrom(tableNode);
        if (intro != null) {
            maybeAddBlankLine(text);
            appendEntryToText(text, intro, null);
        }
        maybeAddBlankLine(text);

        text.addAll(table);
        if (!blockid.isEmpty()) {
            table.add("^" + blockid);
        }
        maybeAddBlankLine(text);
        JsonNode footnotes = Field.footnotes.getFrom(tableNode);
        if (footnotes != null) {
            maybeAddBlankLine(text);
            appendCallout(text, footnotes, "pf-table-footnotes");
        }
        JsonNode outro = Field.outro.getFrom(tableNode);
        if (outro != null) {
            maybeAddBlankLine(text);
            appendEntryToText(text, outro, null);
        }
    }

    default void maybeAddBlankLine(List<String> text) {
        if (text.size() > 0 && !text.get(text.size() - 1).isBlank()) {
            text.add("");
        }
    }

    default JsonNode copyNode(JsonNode sourceNode) {
        try {
            return Tui.MAPPER.readTree(sourceNode.toString());
        } catch (JsonProcessingException ex) {
            tui().errorf(ex, "Unable to copy %s", sourceNode.toString());
            throw new IllegalStateException("JsonProcessingException processing " + sourceNode);
        }
    }

    default JsonNode copyReplaceNode(JsonNode sourceNode, Pattern replace, String with) {
        try {
            String modified = replace.matcher(sourceNode.toString()).replaceAll(with);
            return Tui.MAPPER.readTree(modified);
        } catch (JsonProcessingException ex) {
            tui().errorf(ex, "Unable to copy %s", sourceNode.toString());
            throw new IllegalStateException("JsonProcessingException processing " + sourceNode);
        }
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

    /**
     * Remove/replace syntax within text
     *
     * @param input
     * @return
     */
    default String replaceText(String input) {
        String result = input;

        // TODO
        return result;
    }

    // Other context-constrained type values (not the big append loop)
    enum FieldValue implements NodeReader {
        multiRow;
    }

    enum Field implements NodeReader {
        by,
        entry,
        entries,
        footnotes,
        head,
        id,
        intro,
        items,
        labelRowIdx,
        name,
        outro,
        page,
        rows,
        signature,
        source,
        style,
        title,
        type;
    }

    enum AppendTypeValue implements NodeReader {
        ability,
        affliction,
        attack,
        data,
        entries,
        entriesOtherSource,
        item,
        list,
        lvlEffect,
        paper,
        pf2beigeBox("pf2-beige-box"),
        pf2brownBox("pf2-brown-box"),
        pf2h1("pf2-h1"),
        pf2h1flavor("pf2-h1-flavor"),
        pf2h2("pf2-h2"),
        pf2h3("pf2-h3"),
        pf2h4("pf2-h4"),
        pf2h5("pf2-h5"),
        pf2inset("pf2-inset"),
        pf2keyBox("pf2-key-box"),
        pf2keyAbility("pf2-key-ability"),
        pf2options("pf2-options"),
        pf2redBox("pf2-red-box"),
        pf2sampleBox("pf2-sample-box"),
        pf2sidebar("pf2-sidebar"),
        pf2tipsBox("pf2-tips-box"),
        pf2title("pf2-title"),
        quote,
        section,
        successDegree,
        table;

        final String nodeName;

        AppendTypeValue() {
            nodeName = this.name();
        }

        static AppendTypeValue from(String textOrNull) {
            if (textOrNull == null) {
                return null;
            }
            return Stream.of(AppendTypeValue.values())
                    .filter((t) -> t.nodeName.equals(textOrNull) || t.name().equalsIgnoreCase(textOrNull))
                    .findFirst().orElse(null);
        }

        AppendTypeValue(String nodeName) {
            this.nodeName = nodeName;
        }

        @Override
        public String nodeName() {
            return nodeName;
        }
    }

    // enum Type
}
