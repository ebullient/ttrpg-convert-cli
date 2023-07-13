package dev.ebullient.convert.qute;

import java.nio.file.Path;

import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class ImageRef {
    final Path sourcePath;
    final Path targetFilePath;
    final String url;
    final Integer width;

    public final String title;
    public final String vaultPath;

    private ImageRef(String title, String url, Integer width) {
        this.sourcePath = null;
        this.targetFilePath = null;
        this.title = title == null ? "" : title;
        this.vaultPath = null;
        this.width = width;
        this.url = url;
    }

    private ImageRef(Path sourcePath, Path targetFilePath, String title, String vaultPath, Integer width) {
        this.sourcePath = sourcePath;
        this.targetFilePath = targetFilePath;
        this.title = title == null ? "" : title;
        this.vaultPath = vaultPath;
        this.width = width;
        this.url = null;
    }

    private String shortTitle() {
        return title.length() > 50 ? title.substring(0, 26) + "..." : title;
    }

    private String titleAttribute() {
        return title.length() > 50 ? " \"" + title + "\"" : "";
    }

    public String getEmbeddedLink(String anchor) {
        return String.format("![%s](%s%s%s)",
                shortTitle(),
                url == null ? vaultPath : url,
                anchor.length() > 0 ? "#" + anchor : "",
                titleAttribute());
    }

    public String getEmbeddedLink() {
        String anchor = "center";
        // if (width != null && width < 500) {
        //     anchor = "right";
        // }
        return String.format("![%s](%s#%s%s)",
                shortTitle(),
                url == null ? vaultPath : url,
                anchor,
                titleAttribute());
    }

    @Deprecated
    public String getEmbeddedLinkWithTitle(String anchor) {
        return getEmbeddedLink(anchor);
    }

    @Deprecated
    public String getCaption() {
        return title;
    }

    @Deprecated
    public String getPath() {
        return vaultPath;
    }

    /** Not available in templates */
    public Path sourcePath() {
        return sourcePath;
    }

    /** Not available in templates */
    public Path targetFilePath() {
        return targetFilePath;
    }

    public static class Builder {
        private Path sourcePath;
        private Path relativeTarget;
        private String title = "";
        private Integer width;

        private String vaultRoot;
        private Path rootFilePath;

        private String url;

        public Builder setSourcePath(Path sourcePath) {
            this.sourcePath = sourcePath;
            return this;
        }

        public Builder setStreamSource(String glyph) {
            this.sourcePath = Path.of("stream", glyph);
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title.replaceAll("\\[(.+?)]\\(.+?\\)", "$1");
            return this;
        }

        public Builder setVaultRoot(String vaultRoot) {
            this.vaultRoot = vaultRoot;
            return this;
        }

        public Builder setRootFilepath(Path rootFilePath) {
            this.rootFilePath = rootFilePath;
            return this;
        }

        public Builder setRelativePath(Path relativeTarget) {
            this.relativeTarget = relativeTarget;
            return this;
        }

        public Builder setWidth(Integer width) {
            this.width = width;
            return this;
        }

        public Builder setUrl(String url) {
            this.url = url;
            return this;
        }

        public ImageRef build() {
            if (url != null) {
                return new ImageRef(title, url, width);
            }

            if (sourcePath == null || relativeTarget == null || vaultRoot == null || rootFilePath == null) {
                throw new IllegalStateException("Set paths first (source, relative, vaultRoot, fileRoot) first");
            }
            Path targetFilePath = rootFilePath.resolve(relativeTarget);
            String vaultPath = String.format("%s%s", vaultRoot,
                    relativeTarget.toString().replace('\\', '/'));

            return new ImageRef(sourcePath, targetFilePath, title, vaultPath, width);
        }

        public ImageRef build(ImageRef previous) {
            if (previous != null) {
                return new ImageRef(previous.sourcePath,
                        previous.targetFilePath,
                        title,
                        previous.vaultPath,
                        width);
            } else {
                return build();
            }
        }
    }
}
