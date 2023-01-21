package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.NodeReader;

public interface JsonSource extends JsonTextReplacement {
    Pattern footnotePattern = Pattern.compile("\\{@footnote ([^}]+)}");

    default int appendFootnotes(List<String> text, int count) {
        readingFootnotes.set(true);
        List<String> footnotes = new ArrayList<>();
        for (int i = 0; i < text.size(); i++) {
            // "Footnote tags; allows a footnote to be embedded
            // {@footnote directly in text|This is primarily for homebrew purposes, as the official texts (so far) avoid using footnotes},
            // {@footnote optional reference information|This is the footnote. References are free text.|Footnote 1, page 20}.",
            text.set(i, footnotePattern.matcher(text.get(i))
                    .replaceAll((match) -> {
                        int index = count + footnotes.size() + 1;
                        String footnote = replaceText(match.group(1));
                        String[] parts = footnote.split("\\|");
                        footnotes.add(String.format("[^%s]: %s%s", index, parts[1],
                                parts.length > 2 ? " (" + parts[2] + ")" : ""));

                        return String.format("%s[^%s]", parts[0], index);
                    }));
        }

        if (footnotes.size() > 0) {
            maybeAddBlankLine(text);
            footnotes.forEach(f -> text.add(replaceText(f)));

        }
        readingFootnotes.set(false);
        return count + footnotes.size();
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
        if (!source.isEmpty() && !cfg().sourceIncluded(source)) {
            if (!cfg().sourceIncluded(getSources().alternateSource())) {
                return;
            }
        }

        AppendTypeValue type = AppendTypeValue.valueFrom(node, Field.type);
        if (type != null) {
            switch (type) {
                case section:
                case pf2h1:
                case pf2h2:
                case pf2h3:
                case pf2h4:
                case pf2h5:
                    appendTextHeaderBlock(text, node, heading);
                    break;
                case pf2h1flavor:
                    appendTextHeaderFlavorBlock(text, node);
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

                // lists & items

                case pf2options:
                case list:
                    appendList(text, Field.items.withArrayFrom(node));
                    break;
                case item:
                    appendListItem(text, node);
                    break;
                case entries:
                    appendEntryToText(text, Field.entries.getFrom(node), heading);
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
                case successDegree:
                    appendSuccessDegree(text, node);
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

    default void appendTextHeaderBlock(List<String> text, JsonNode node, String heading) {
        if (heading == null) {
            List<String> inner = new ArrayList<>();
            appendEntryToText(inner, Field.entry.getFrom(node), null);
            appendEntryToText(inner, Field.entries.getFrom(node), null);
            if (prependField(node, Field.name, inner)) {
                maybeAddBlankLine(text);
            }
            text.addAll(inner);
        } else if (Field.name.existsIn(node)) {
            maybeAddBlankLine(text);
            text.add(heading + " " + Field.name.getTextOrEmpty(node));
            text.add("");
            appendEntryToText(text, Field.entry.getFrom(node), "#" + heading);
            appendEntryToText(text, Field.entries.getFrom(node), "#" + heading);
        } else {
            appendEntryToText(text, node.get("entries"), heading);
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
        text.add("\nCHECK ME: KEY ABILITY ABOVE");
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
    }

    default void appendSuccessDegree(List<String> text, JsonNode node) {
        JsonNode entries = Field.entries.getFrom(node);

        List<String> inner = new ArrayList<>();
        inner.add("[!success-degree] ");

        JsonNode field = Field.criticalSuccess.getFrom(entries);
        if (field != null) {
            prependTextMakeListItem(inner, field, "**Critical Success** ");
        }
        field = Field.success.getFrom(entries);
        if (field != null) {
            prependTextMakeListItem(inner, field, "**Success** ");
        }
        field = Field.failure.getFrom(entries);
        if (field != null) {
            prependTextMakeListItem(inner, field, "**Failure** ");
        }
        field = Field.criticalFailure.getFrom(entries);
        if (field != null) {
            prependTextMakeListItem(inner, field, "**Critical Failure** ");
        }

        maybeAddBlankLine(text);
        inner.forEach(x -> text.add("> " + x));
    }

    default void prependTextMakeListItem(List<String> text, JsonNode e, String prepend) {
        List<String> inner = new ArrayList<>();
        appendEntryToText(inner, e, null);
        if (inner.size() > 0) {
            prependText("- " + prepend, inner);
            text.add(String.join("  \n    ", inner).trim()); // preserve line items
        }
    }

    default void appendTable(List<String> text, JsonNode tableNode) {
        List<String> table = new ArrayList<>();

        String blockid = "";

        String name = Field.name.getTextOrEmpty(tableNode);
        String id = Field.id.getTextOrEmpty(tableNode);

        ArrayNode rows = Field.rows.withArrayFrom(tableNode);
        List<Integer> labelIdx = Field.labelRowIdx.fieldFromTo(tableNode, Tui.LIST_INT, tui());

        if (!name.isEmpty()) {
            blockid = slugify(name + " " + id);
        }

        if (labelIdx == null) {
            int length = rows.get(0).size();
            String[] array = new String[length];
            Arrays.fill(array, " ");
            String header = "|" + String.join(" | ", array) + " |";
            table.add(header);
            table.add(header.replaceAll("[^|]", "-"));
        }

        for (int r = 0; r < rows.size(); r++) {
            JsonNode rowNode = rows.get(r);

            if (labelIdx != null && labelIdx.contains(r)) {
                final int ri = r;
                String header = StreamSupport.stream(rowNode.spliterator(), false)
                        .map(x -> replaceText(x.asText()))
                        .map(x -> ri != 0 ? "**" + x + "**" : x)
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
            } else if (FieldValue.multiRow.isValueOfField(rows, Field.type)) {
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
            readingFootnotes.set(true);
            appendEntryToText(text, footnotes, null);
            readingFootnotes.set(false);
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

    // Special one-offs for accounting/tracking
    enum TtrpgValue implements NodeReader {
        categoryTag,
        traitTag,
        indexKey;

        public void addToNode(JsonNode node, String value) {
            ((ObjectNode) node).put(this.name(), value);
        }

        public void addToNode(JsonNode node, List<String> categories) {
            ((ObjectNode) node).set(this.name(), Tui.MAPPER.valueToTree(categories));
        }

        public String getFromNode(JsonNode node) {
            return this.getTextOrNull(node);
        }
    }

    // Other context-constrained type values (not the big append loop)
    enum FieldValue implements NodeReader.FieldValue {
        multiRow;

        @Override
        public String value() {
            return this.name();
        }
    }

    enum Field implements NodeReader {
        activity,
        actionType,
        alias,
        by,
        categories,
        cost,
        customUnit,
        entry,
        entries,
        footnotes,
        freq, // inside frequency
        frequency,
        group,
        head,
        id,
        implies,
        info,
        intro,
        interval,
        items,
        labelRowIdx,
        name,
        number,
        overcharge,
        outro,
        page,
        prerequisites,
        recurs,
        requirements,
        rows,
        signature,
        source,
        special,
        style,
        title,
        traits,
        trigger,
        type,
        unit,
        criticalSuccess("Critical Success"),
        success("Success"),
        failure("Failure"),
        criticalFailure("Critical Failure"),
        ;

        final String nodeName;

        Field() {
            this.nodeName = this.name();
        }

        Field(String nodeName) {
            this.nodeName = nodeName;
        }

        public String nodeName() {
            return nodeName;
        }
    }

    enum AppendTypeValue implements NodeReader.FieldValue {
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

        final String nodeValue;

        AppendTypeValue() {
            nodeValue = this.name();
        }

        AppendTypeValue(String nodeValue) {
            this.nodeValue = nodeValue;
        }

        public String value() {
            return this.nodeValue;
        }

        static AppendTypeValue valueFrom(JsonNode source, Field field) {
            String textOrNull = field.getTextOrNull(source);
            if (textOrNull == null) {
                return null;
            }
            return Stream.of(AppendTypeValue.values())
                    .filter((t) -> t.nodeValue.equals(textOrNull) || t.name().equalsIgnoreCase(textOrNull))
                    .findFirst().orElse(null);
        }
    }
    // enum Type

}
