package dev.ebullient.json5e.qute;

import java.nio.file.Path;

public class ImageRef {
    public final Path sourcePath;
    public final Path targetPath;
    public final String link;

    public ImageRef(Path sourcePath, Path targetPath, String link) {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.link = link;
    }
}
