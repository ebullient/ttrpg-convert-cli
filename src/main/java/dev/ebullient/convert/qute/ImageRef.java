package dev.ebullient.convert.qute;

import java.nio.file.Path;

import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Create links to referenced images.
 *
 * <p>
 * The general form of a markdown image link is: `![alt text](vaultPath "title")`.
 * You can also use anchors to position the image within the page,
 * which creates links that look like this: `![alt text](vaultPath#anchor "title")`.
 * </p>
 *
 * <h2>Anchor Tags</h2>
 *
 * <p>
 * Anchor tags are used to position images within a page and are styled with CSS. Examples:
 * </p>
 *
 * <ul>
 * <li>`center` centers the image and constrains its height.</li>
 * <li>`gallery` constrains images within a gallery callout.</li>
 * <li>`portrait` floats an image to the right.</li>
 * <li>`symbol` floats Deity symbols to the right.</li>
 * <li>`token` is a smaller image, also floated to the right. Used in statblocks.</li>
 * </ul>
 */
@TemplateData
@RegisterForReflection
public class ImageRef {
    final Path sourcePath;
    final Path targetFilePath;
    final String url;
    final Integer width;
    final String vaultPath;

    /** Descriptive title (or caption) for the image. This can be long. */
    public final String title;

    private ImageRef(String title, String url, Integer width) {
        this.url = url.replace(" ", "%20");
        this.sourcePath = null;
        this.targetFilePath = null;
        this.title = title == null ? ""
                : title.replaceAll("\\[(.+?)]\\(.+?\\)", "$1");
        this.vaultPath = null;
        this.width = width;
    }

    private ImageRef(Path sourcePath, Path targetFilePath, String title, String vaultPath, Integer width) {
        this.sourcePath = sourcePath;
        this.targetFilePath = targetFilePath;
        this.title = title == null ? ""
                : title.replaceAll("\\[(.+?)]\\(.+?\\)", "$1");
        this.vaultPath = vaultPath;
        this.width = width;
        this.url = null;
    }

    /**
     * A shortened image title (max 50 characters) for use in markdown links.
     */
    public String getShortTitle() {
        return title.length() > 50 ? title.substring(0, 26) + "..." : title;
    }

    private String titleAttribute() {
        return title.length() > 50 ? " \"" + title + "\"" : "";
    }

    /** Path of the image in the vault or url for external images. */
    public String getVaultPath() {
        return url == null ? vaultPath : url;
    }

    public String getEmbeddedLink(String anchor) {
        return String.format("![%s](%s%s%s)",
                getShortTitle(),
                url == null ? vaultPath : url,
                anchor.length() > 0 ? "#" + anchor : "",
                titleAttribute());
    }

    /**
     * Return an embedded markdown link to the image, using an optional
     * anchor tag to position the image in the page.
     * For example: `{resource.image.getEmbeddedLink("symbol")}`
     * <p>
     * If the title is longer than 50 characters:
     * `![{resource.shortTitle}]({resource.vaultPath}#anchor "{resource.title}")`,
     * </p>
     * <p>
     * If the title is 50 characters or less:
     * `![{resource.title}]({resource.vaultPath}#anchor)`,
     * </p>
     * <p>
     * Links will be generated using "center" as the anchor by default.
     * </p>
     */
    public String getEmbeddedLink() {
        String anchor = "center";
        // if (width != null && width < 500) {
        //     anchor = "right";
        // }
        return String.format("![%s](%s#%s%s)",
                getShortTitle(),
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
            this.title = title;
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
