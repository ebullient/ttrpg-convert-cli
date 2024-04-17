package dev.ebullient.convert.qute;

import java.nio.file.Path;

import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.config.TtrpgConfig.ImageRoot;
import dev.ebullient.convert.io.Tui;
import io.quarkus.qute.TemplateData;

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
public class ImageRef {
    final Path sourcePath;
    final Path targetFilePath;
    final String url;
    final Integer width;
    final String vaultPath;

    /** Descriptive title (or caption) for the image. This can be long. */
    public final String title;
    final String titleAttr;

    private ImageRef(String url, Path sourcePath, Path targetFilePath, String title, String vaultPath, Integer width) {
        // some things are already escaped in the source (so escaping again would be wrong)
        this.url = url;
        this.sourcePath = sourcePath;
        this.targetFilePath = targetFilePath;
        title = title == null
                ? ""
                : title.replaceAll("\\[(.+?)]\\(.+?\\)", "$1");

        if (title.length() > 50) {
            this.title = escape(title.substring(0, 26) + "...");
            this.titleAttr = " \"" + escape(title) + "\"";
        } else {
            this.title = escape(title);
            this.titleAttr = "";
        }
        this.vaultPath = vaultPath;
        this.width = width;

        if (url == null && vaultPath == null) {
            Tui.instance().errorf("ImageRef (target=%s) has no url or vaultPath", targetFilePath);
        }
    }

    String escape(String s) {
        return s.replace("\"", "&quot;");
    }

    /**
     * A shortened image title (max 50 characters) for use in markdown links.
     */
    public String getShortTitle() {
        return title;
    }

    private String titleAttribute() {
        return titleAttr;
    }

    /** Path of the image in the vault or url for external images. */
    public String getVaultPath() {
        return vaultPath == null ? url : vaultPath;
    }

    public String getEmbeddedLink(String anchor) {
        return String.format("![%s](%s%s%s)",
                getShortTitle(),
                getVaultPath(),
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
                getVaultPath(),
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

    /** Not available in templates */
    public String url() {
        return url;
    }

    public static class Builder {
        private String sourcePath;
        private Path relativeTarget;
        private String title = "";
        private Integer width;

        private String vaultRoot;
        private Path rootFilePath;

        private String url;

        public Builder setSourcePath(Path sourcePath) {
            this.sourcePath = sourcePath.toString();
            return this;
        }

        public Builder setInternalPath(String sourcePath) {
            this.sourcePath = sourcePath;
            return this;
        }

        public Builder setStreamSource(String glyph) {
            this.sourcePath = "stream/" + glyph;
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
            final ImageRoot imageRoot = TtrpgConfig.internalImageRoot();

            if (url != null && !imageRoot.copyExternalToVault()) {
                // leave external images alone (referenced as url)
                return new ImageRef(url, null, null, title, null, width);
            }

            if (url == null && sourcePath == null) {
                Tui.instance().errorf("ImageRef build for internal image called without url or sourcePath set");
                return null;
            }
            if (relativeTarget == null || vaultRoot == null || rootFilePath == null) {
                Tui.instance().errorf("ImageRef build called without target paths set");
                return null;
            }

            Path targetFilePath = rootFilePath.resolve(relativeTarget);
            String vaultPath = String.format("%s%s", vaultRoot,
                    relativeTarget.toString().replace('\\', '/'));

            // Escaping spaces is a mess. Remove here (local file paths won't want it)
            // It is changed back (from space to %20) if in URL form (file or http)
            String remoteUrl = url == null
                    ? sourcePath.toString().replace("%20", " ")
                    : url;
            if (remoteUrl.startsWith("http")) {
                remoteUrl = remoteUrl.replaceAll("^(https?):/+", "$1://");
            } else if (!remoteUrl.startsWith("file:/")) {
                remoteUrl = imageRoot.getRootPath() + remoteUrl;
            }

            if (imageRoot.copyInternalToVault() || imageRoot.copyExternalToVault()) {
                // remote images to be copied into the vault
                if (remoteUrl.startsWith("http") || remoteUrl.startsWith("file")) {
                    String filename = remoteUrl.substring(remoteUrl.lastIndexOf('/') + 1);
                    if (!filename.contains("%")) {
                        try {
                            String encoded = java.net.URLEncoder.encode(filename, "UTF-8")
                                    .replace("+", "%20");
                            remoteUrl = remoteUrl.replace(filename, encoded);
                        } catch (java.io.UnsupportedEncodingException e) {
                            Tui.instance().errorf("Failed to encode filename: %s", filename);
                        }
                    }
                    return new ImageRef(remoteUrl, null, targetFilePath, title, vaultPath, width);
                }
                return new ImageRef(null, Path.of(remoteUrl), targetFilePath, title, vaultPath, width);
            }

            // remote images are not copied to the vault --> url image ref
            return new ImageRef(remoteUrl,
                    null, null, title, null, width);
        }

        public ImageRef build(ImageRef previous) {
            if (previous != null) {
                return new ImageRef(previous.url,
                        previous.sourcePath,
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
