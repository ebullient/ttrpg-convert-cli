package dev.ebullient.convert.tools.pf2e;

import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteNote;
import dev.ebullient.convert.tools.pf2e.qute.QuteBook;
import io.quarkus.runtime.annotations.RegisterForReflection;

public class Json2QuteBook extends Json2QuteBase {

    final String bookRelativePath;
    final JsonNode dataNode;

    public Json2QuteBook(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode, JsonNode dataNode) {
        super(index, type, rootNode);
        this.dataNode = dataNode;
        this.bookRelativePath = slugify(sources.getName());
    }

    /**
     * From index entry and supporting data, construct a set of pages for the book.
     * Page state has to be maintained.
     */
    public List<Pf2eQuteNote> buildBook() {
        boolean pushed = parseState().push(getSources()); // set source
        try {
            QuteBook.BookInfo bookInfo = new QuteBook.BookInfo();
            bookInfo.author = Pf2eBook.author.getTextOrEmpty(rootNode);
            bookInfo.group = Pf2eBook.group.getTextOrEmpty(rootNode);
            bookInfo.published = Pf2eBook.published.getTextOrEmpty(rootNode);

            Tags coverTags = new Tags(sources);
            coverTags.add(cfg().tagOf("book",
                    Field.group.getTextOrDefault(rootNode, "ungrouped"),
                    bookRelativePath));

            String coverUrl = Pf2eBook.coverUrl.getTextOrEmpty(rootNode);
            if (coverUrl != null) {
                Path coverPath = Path.of(coverUrl);
                bookInfo.cover = Pf2eSources.buildImageRef(Pf2eIndexType.book, index, coverPath, sources.getName());
            }

            // Find coverNode and book sections
            Map<String, JsonNode> bookSections = new HashMap<>();
            for (JsonNode node : Pf2eBook.data.iterateArrayFrom(dataNode)) {
                String name = SourceField.name.getTextOrEmpty(node);
                if (name.isEmpty()) {
                    continue;
                }
                bookSections.put(name, node);
            }

            List<Pf2eQuteNote> pages = new ArrayList<>();
            List<String> coverText = new ArrayList<>();

            for (JsonNode n : Pf2eBook.contents.iterateArrayFrom(rootNode)) {
                String name = SourceField.name.getTextOrEmpty(n);
                if ("Cover".equals(name)) {
                    continue;
                }
                Ordinal ordinal = Pf2eBook.ordinal.fieldFromTo(n, Ordinal.class, tui());

                String prefix = "";
                if (ordinal != null) {
                    prefix = String.format("%s %s: ", toTitleCase(ordinal.type), ordinal.identifier);
                }

                final String sectionTitle = String.format("%s%s", prefix, name);
                final String sectionFilename = slugify(sectionTitle);
                maybeAddBlankLine(coverText);
                coverText.add(String.format("**[%s](%s%s/%s.md)**", sectionTitle,
                        index.rulesVaultRoot(), bookRelativePath, sectionFilename));
                coverText.add("");

                List<String> sectionHeaders = Pf2eBook.headers.getListOfStrings(n, tui());
                for (String header : sectionHeaders) {
                    coverText.add(String.format("- [%s](%s%s/%s.md#%s)", header,
                            index.rulesVaultRoot(), bookRelativePath, sectionFilename,
                            toAnchorTag(header)));
                }

                JsonNode sectionNode = bookSections.getOrDefault(sectionTitle, bookSections.get(name));
                if (sectionNode == null) {
                    tui().errorf("Unable to find section for %s", sectionTitle);
                } else {
                    pages.add(chapterPage(sectionFilename, sectionNode));
                }
            }

            // folder note / cover
            pages.add(new QuteBook(sources.getName(), coverText, coverTags, bookRelativePath, bookInfo, List.of()));

            return pages;
        } finally {
            parseState().pop(pushed);
        }
    }

    Pf2eQuteNote chapterPage(String name, JsonNode pageNode) {
        boolean pushed = parseState().push(pageNode);
        try {
            Tags tags = new Tags(sources);
            List<String> text = new ArrayList<>();

            appendToText(text, pageNode, "#");

            return new QuteBook(name, text, tags, bookRelativePath, null, List.of());
        } finally {
            parseState().pop(pushed);
        }
    }

    @RegisterForReflection
    static class Ordinal {
        public String type;
        public Object identifier;
    }

    enum Pf2eBook implements Pf2eJsonNodeReader {
        author,
        contents,
        coverUrl,
        data,
        group,
        headers,
        ordinal,
        published,
    }
}
