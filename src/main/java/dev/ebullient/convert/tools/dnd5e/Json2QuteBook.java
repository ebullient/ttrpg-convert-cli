package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteNote;

public class Json2QuteBook extends Json2QuteCommon {

    final String bookRelativePath;
    final JsonNode dataNode;
    final String title;
    String fileName;

    public Json2QuteBook(Tools5eIndex index, Tools5eIndexType type, JsonNode rootNode, JsonNode dataNode) {
        super(index, type, rootNode);
        this.dataNode = dataNode;
        this.bookRelativePath = slugify(sources.getName());
        this.title = replaceText(sources.getName());

        String key = getSources().getKey();
        final String basePath;
        if (key.contains("adventure-") || key.contains("book-")) {
            basePath = type.getRelativePath();
        } else {
            basePath = ".";
        }
        imagePath = basePath + "/" + bookRelativePath;
    }

    @Override
    public String getName() {
        if (fileName != null) {
            return fileName;
        }
        return title;
    }

    /**
     * From index entry and supporting data, construct a set of pages for the book.
     * Page state has to be maintained.
     */
    public List<Tools5eQuteNote> buildBook() {
        List<Tools5eQuteNote> pages = new ArrayList<>();
        Tags tags = new Tags(getSources());
        JsonNode data = dataNode.get("data");

        AtomicInteger prefix = new AtomicInteger(1);
        final String pFormat;
        if (data.size() + 1 > 10) {
            pFormat = "%02d";
        } else {
            pFormat = "%01d";
        }

        boolean p1 = parseState().push(getSources()); // set source
        try {
            for (JsonNode x : iterableElements(data)) {
                boolean p2 = parseState().push(x); // inner node
                try {
                    String name = replaceText(SourceField.name.getTextOrEmpty(x));
                    fileName = String.format("%s-%s",
                            String.format(pFormat, prefix.get()),
                            slugify(name));

                    List<String> text = new ArrayList<>();
                    appendToText(text, SourceField.entries.getFrom(x), "##");

                    String content = String.join("\n", text);

                    if (!content.isBlank()) {
                        String titlePage = title;
                        if (x.has("page")) {
                            String page = x.get("page").asText();
                            titlePage = title + ", p. " + page;
                        }
                        Tools5eQuteNote note = new Tools5eQuteNote(name, titlePage, content, tags)
                                .withTargetPath(imagePath)
                                .withTargetFile(fileName);
                        pages.add(note);
                        prefix.incrementAndGet();
                    }
                } finally {
                    parseState().pop(p2);
                }
            }
        } finally {
            parseState().pop(p1);
        }
        return pages;
    }
}
