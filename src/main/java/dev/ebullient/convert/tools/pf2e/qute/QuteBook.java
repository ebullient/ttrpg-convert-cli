package dev.ebullient.convert.tools.pf2e.qute;

import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools Book attributes ({@code book2md.txt})
 *
 * Extension of {@link dev.ebullient.convert.tools.pf2e.qute.Pf2eQuteNote Pf2eQuteNote}
 */
@TemplateData
public class QuteBook extends Pf2eQuteNote {

    public final List<String> altNames;
    /** Information about the book as {@code dev.ebullient.convert.tools.pf2e.qute.QuteBook.BookInfo} */
    public final BookInfo bookInfo;

    final String bookDir;

    public QuteBook(String name, List<String> text, Tags tags, String bookDir, BookInfo bookInfo,
            List<String> aliases) {
        super(Pf2eIndexType.book, name, null, text, tags);
        this.bookDir = bookDir;
        this.bookInfo = bookInfo;
        this.altNames = aliases;
    }

    @Override
    public List<String> getAltNames() {
        // Used by getAliases in QuteBase/Pf2eQuteNote
        return altNames;
    }

    @Override
    public String targetPath() {
        return bookDir;
    }

    @Override
    public String targetFile() {
        return this.getName();
    }

    @Override
    public String template() {
        return "book2md.txt";
    }

    /**
     * Pf2eTools book information
     *
     *
     * This data object provides a default mechanism for creating
     * a marked up string based on the attributes that are present.
     * To use it, reference it directly: `{resource.actionType}`.
     *
     */
    @TemplateData
    public static class BookInfo {
        /** Name of the book */
        public String name;
        /** Book id */
        public String id;
        /** Date published */
        public String published;
        /** Group this book belongs to (core, lost-omens, supplement, etc.) */
        public String group;
        /** Author */
        public String author;
        /** Cover image as {@code dev.ebullient.convert.qute.ImageRef} */
        public ImageRef cover;
    }
}
