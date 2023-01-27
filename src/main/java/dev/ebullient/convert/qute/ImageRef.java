package dev.ebullient.convert.qute;

import java.nio.file.Path;

import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class ImageRef {
    public final Path sourcePath;
    public final Path targetPath;
    public final String caption;
    public final String path;

    public static class Builder {
        public Path sourcePath;
        public Path targetPath;
        public Path relativeTarget;
        public String title;
        public String image;

        public Builder setSourcePath(Path sourcePath) {
            this.sourcePath = sourcePath;
            return this;
        }

        public Builder setStreamSource(String glyph) {
            this.sourcePath = Path.of("stream", glyph);
            return this;
        }

        public Builder setTargetPath(Path rootPath, Path relativeTarget) {
            this.targetPath = rootPath.resolve(relativeTarget);
            this.relativeTarget = relativeTarget;
            return this;
        }

        public Builder setMarkdownPath(String title, String relativeRoot) {
            if (relativeTarget == null) {
                throw new IllegalStateException("Call setTargetPath first");
            }
            this.title = title.replaceAll("\\[(.+?)]\\(.+?\\)", "$1");
            this.image = String.format("%s%s", relativeRoot,
                    relativeTarget.toString().replace('\\', '/'));
            return this;
        }

        public ImageRef build() {
            if (sourcePath == null || targetPath == null) {
                throw new IllegalStateException("Call setSourcePath and setTargetPath first");
            }
            return new ImageRef(sourcePath, targetPath, title, image);
        }
    }

    private ImageRef(Path sourcePath, Path targetPath, String caption, String image) {
        this.sourcePath = sourcePath;
        this.targetPath = targetPath;
        this.caption = caption == null ? "" : caption;
        this.path = image;
    }
}
