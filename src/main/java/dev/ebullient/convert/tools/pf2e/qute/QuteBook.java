package dev.ebullient.convert.tools.pf2e.qute;

import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;
import io.quarkus.qute.TemplateData;

@TemplateData
public class QuteBook extends Pf2eQuteNote {

    public final List<String> aliases;
    final String bookDir;
    final BookInfo bookInfo;

    public QuteBook(String name, List<String> text, Tags tags, String bookDir, BookInfo bookInfo,
            List<String> aliases) {
        super(Pf2eIndexType.book, name, null, text, tags);
        this.bookDir = bookDir;
        this.bookInfo = bookInfo;
        this.aliases = aliases;
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

    @TemplateData
    public static class BookInfo {
        public String name;
        public String id;
        public String published;
        public String group;
        public String author;
        public ImageRef cover;
    }
}
