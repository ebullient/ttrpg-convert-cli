package dev.ebullient.json5e.qute;

import java.nio.file.Path;

public class ImageRef {
    public final Path sourcePath;
    public final Path targetPath;
    public final String caption;
    public final String path;

    public static class Builder {
        public Path sourcePath;
        public Path targetPath;
        public Path relativeTarget;
        public String caption;
        public String image;

        public Builder setSourcePath(Path sourcePath) {
            this.sourcePath = sourcePath;
            return this;
        }

        public Builder setTargetPath(Path rootPath, Path relativeTarget) {
            this.targetPath = rootPath.resolve(relativeTarget);
            this.relativeTarget = relativeTarget;
            return this;
        }

        public Builder setMarkdownPath(String caption, String relativeRoot) {
            if (relativeTarget == null) {
                throw new IllegalStateException("Call setTargetPath first");
            }
            this.caption = caption;
            this.image = String.format("%s%s", relativeRoot,
                    relativeTarget.toString().replace('\\', '/'));
            return this;
        }

        public ImageRef build() {
            if (sourcePath == null || targetPath == null) {
                throw new IllegalStateException("Call setTargetPath first");
            }
            return new ImageRef(sourcePath, targetPath, caption, image);
        }
    }

    private ImageRef(Path sourcePath, Path targetPath, String caption, String image) {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.caption = caption == null ? "" : caption;
        this.path = image;
    }
}
