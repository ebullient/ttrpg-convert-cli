package dev.ebullient.json5e.qute;

import java.nio.file.Path;

public class ImageRef {
    public final Path sourcePath;
    public final Path targetPath;
    public final String link;

    public static class Builder {
        public Path sourcePath;
        public Path targetPath;
        public Path relativeTarget;
        public String link;

        public Builder setSourcePath(Path sourcePath) {
            this.sourcePath = sourcePath;
            return this;
        }

        public Builder setTargetPath(Path rootPath, Path relativeTarget) {
            this.targetPath = rootPath.resolve(relativeTarget);
            this.relativeTarget = relativeTarget;
            return this;
        }

        public Builder createMarkdownLink(String title, String relativeRoot, String suffix) {
            if (relativeTarget == null) {
                throw new IllegalStateException("Call setTargetPath first");
            }
            this.link = String.format("![%s](%s%s%s)", title, relativeRoot, relativeTarget.toString().replace('\\', '/'),
                    suffix);
            return this;
        }

        public ImageRef build() {
            if (sourcePath == null || targetPath == null) {
                throw new IllegalStateException("Call setTargetPath first");
            }
            return new ImageRef(sourcePath, targetPath, link);
        }
    }

    private ImageRef(Path sourcePath, Path targetPath, String link) {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.link = link;
    }

    public String getMarkdownLink() {
        return link;
    }
}
