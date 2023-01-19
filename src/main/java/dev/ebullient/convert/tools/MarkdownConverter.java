package dev.ebullient.convert.tools;

import java.util.List;

public interface MarkdownConverter {

    MarkdownConverter writeAll();

    MarkdownConverter writeFiles(List<IndexType> types);

    MarkdownConverter writeNotesAndTables();

    MarkdownConverter writeFiles(IndexType feat);
}
