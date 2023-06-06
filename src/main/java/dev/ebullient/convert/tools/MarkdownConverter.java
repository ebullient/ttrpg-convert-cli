package dev.ebullient.convert.tools;

import java.util.List;

public interface MarkdownConverter {

    MarkdownConverter writeAll();

    MarkdownConverter writeFiles(List<? extends IndexType> types);

    MarkdownConverter writeNotesAndTables();

    MarkdownConverter writeFiles(IndexType feat);

    MarkdownConverter writeImages();
}
