package dev.ebullient.json5e.qute;

import java.util.List;

public interface QuteSource {
    String getName();

    String getSource();

    List<ImageRef> images();

    String targetFile();

    String targetPath();

    String title();

    String key();
}
