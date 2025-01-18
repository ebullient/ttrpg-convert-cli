package dev.ebullient.convert.qute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.IndexType;
import dev.ebullient.convert.tools.Tags;
import io.quarkus.qute.TemplateData;

/**
 * Defines attributes inherited by other Qute templates.
 *
 * Notes created from {@code QuteBase} (or a derivative) will use a specific template
 * for the type. For example, {@code QuteBackground} will use {@code background2md.txt}.
 */
@TemplateData
public class QuteBase implements QuteUtil {
    protected final String name;
    protected final CompendiumSources sources;
    protected final String sourceText;

    /** Formatted text. For most templates, this is the bulk of the content. */
    public final String text;

    /** Collected tags for inclusion in frontmatter */
    public final Collection<String> tags;

    private String vaultPath;

    public QuteBase(CompendiumSources sources, String name, String source, String text, Tags tags) {
        this.sources = sources;
        this.name = name;
        this.sourceText = source;
        this.text = text;
        this.tags = tags == null ? List.of() : tags.build();
    }

    /** Note name */
    public String getName() {
        return name;
    }

    /** String describing the content's source(s) */
    public String getSource() {
        return sourceText;
    }

    /** Formatted string describing the content's source(s): `_Source: &lt;sources&gt;_` */
    public String getLabeledSource() {
        return "_Source: " + sourceText + "_";
    }

    /** Book sources as list of {@link dev.ebullient.convert.qute.SourceAndPage} */
    public Collection<SourceAndPage> getSourceAndPage() {
        if (sources == null) {
            return List.of();
        }
        return sources.getSourceAndPage();
    }

    /**
     * List of source books using abbreviated name. Fantasy statblocks uses this list format, as an example.
     */
    public final List<String> getBooks() {
        return getSourceAndPage().stream()
                .map(x -> x.source)
                .toList();
    }

    /** List of content superceded by this note (as {@link dev.ebullient.convert.qute.Reprinted}) */
    public Collection<Reprinted> getReprintOf() {
        if (sources == null) {
            return List.of();
        }
        return sources.getReprints();
    }

    /**
     * Get Sources as a footnote.
     *
     * Calling this method will return an italicised string with the primary source
     * followed by a footnote listing all other sources. Useful for types
     * that tend to have many sources.
     */
    public String getSourcesWithFootnote() {
        if (sources == null) {
            return "";
        }
        if (sources.getSources().size() == 1) {
            SourceAndPage sp = sources.getSourceAndPage().iterator().next();
            String txt = sp.toString();
            if (!txt.isEmpty()) {
                return "_Source: " + txt + "_";
            }
        }
        String primary = null;
        List<String> srcTxt = new ArrayList<>();
        for(var sp : sources.getSourceAndPage()) {
            String txt = sp.toString();
            if (!txt.isEmpty()) {
                if (primary == null) {
                    primary = txt;
                } else {
                    srcTxt.add(txt);
                }
            }
        }
        return "_Source: %s_ ^[%s]".formatted(primary, String.join(", ", srcTxt));
    }

    /** True if the content (text) contains sections */
    public boolean getHasSections() {
        return text != null && !text.isEmpty() && text.contains("\n## ");
    }

    public void vaultPath(String vaultPath) {
        this.vaultPath = vaultPath;
    }

    /** Path to this note in the vault */
    public String getVaultPath() {
        if (vaultPath != null) {
            return vaultPath;
        }
        return targetPath() + '/' + targetFile();
    }

    public CompendiumSources sources() {
        return sources;
    }

    public String title() {
        return name;
    }

    public String targetFile() {
        return name;
    }

    public String targetPath() {
        return ".";
    }

    @Override
    public IndexType indexType() {
        return sources.getType();
    }

    public String key() {
        return sources.getKey();
    }

    public boolean createIndex() {
        return true;
    }

    public String template() {
        IndexType type = indexType();
        return String.format("%s2md.txt", type.templateName());
    }

    public Collection<QuteBase> inlineNotes() {
        return List.of();
    }
}
