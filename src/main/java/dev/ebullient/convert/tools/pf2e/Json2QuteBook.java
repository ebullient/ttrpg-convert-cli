package dev.ebullient.convert.tools.pf2e;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.tools.NodeReader;
import dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteNote;
import dev.ebullient.convert.tools.pf2e.qute.QuteBook;

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
        boolean pushed = parseState.push(rootNode); // set source
        try {
            List<Pf2eQuteNote> pages = new ArrayList<>();

            JsonNode cover = null;
            ArrayNode data = Pf2eBook.data.withArrayFrom(dataNode);
            for (int i = 0; i < data.size(); i++) {
                JsonNode node = data.get(i);
                String name = Field.name.getTextOrNull(node);
                if (name == null) {
                    if (node.toString().contains("\"style\": \"cover\"")) {
                        cover = node;
                    }
                    continue;
                }
                pages.add(chapterPage(name, node));
            }

            pages.add(coverPage(cover));

            return pages;
        } finally {
            parseState.pop(pushed);
        }
    }

    Pf2eQuteNote coverPage(JsonNode cover) {
        Set<String> tags = new HashSet<>(sources.getSourceTags());
        List<String> text = new ArrayList<>();

        tags.add(cfg().tagOf("book",
                Field.group.getTextOrDefault(rootNode, "no-group"),
                bookRelativePath));

        QuteBook.BookInfo bookInfo = new QuteBook.BookInfo();
        bookInfo.author = Pf2eBook.author.getTextOrNull(rootNode);
        bookInfo.group = Pf2eBook.group.getTextOrNull(rootNode);
        bookInfo.published = Pf2eBook.published.getTextOrNull(rootNode);

        String coverUrl = Pf2eBook.coverUrl.getTextOrNull(rootNode);
        if (coverUrl != null) {
            Path coverPath = Path.of(coverUrl);
            bookInfo.cover = Pf2eSources.buildImageRef(Pf2eIndexType.book, index, coverPath, sources.getName());
        }

        Pf2eBook.contents.withArrayFrom(rootNode).forEach(n -> {
            String name = Field.name.getTextOrEmpty(n);
            if ("Cover".equals(name)) {
                return;
            }
            List<String> headers = Pf2eBook.headers.getListOfStrings(n, tui());
            Ordinal ordinal = Pf2eBook.ordinal.fieldFromTo(n, Ordinal.class, tui());

            String prefix = "";
            if (ordinal != null) {
                prefix = String.format("%s %s: ", toTitleCase(ordinal.type), ordinal.identifier);
            }

            String heading = String.format("%s%s", prefix, name);
            String filename = slugify(heading) + ".md";

            maybeAddBlankLine(text);
            text.add(String.format("**[%s](%s%s/%s)**", heading,
                    index.rulesVaultRoot(), bookRelativePath, filename));
            text.add("");

            headers.forEach(h -> text.add(String.format("- [%s](%s%s/%s#%s)", h,
                    index.rulesVaultRoot(), bookRelativePath, filename,
                    h.replace(" ", "%20")
                            .replace(".", ""))));
        });

        return new QuteBook(sources.getName(), text, tags, bookRelativePath, bookInfo, List.of());
    }

    Pf2eQuteNote chapterPage(String name, JsonNode pageNode) {
        boolean pushed = parseState.push(pageNode);
        try {
            Set<String> tags = new HashSet<>(sources.getSourceTags());
            List<String> text = new ArrayList<>();

            appendEntryToText(text, pageNode, "#");

            return new QuteBook(name, text, tags, bookRelativePath, null, List.of());
        } finally {
            parseState.pop(pushed);
        }
    }

    static class Ordinal {
        public String type;
        public Object identifier;
    }

    enum Pf2eBook implements NodeReader {
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
