package dev.ebullient.convert.tools.pf2e.qute;

import java.util.List;

import dev.ebullient.convert.qute.QuteNote;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

/**
 * Attributes for notes that are generated from the Pf2eTools data.
 * This is a trivial extension of {@link dev.ebullient.convert.qute.QuteNote QuteNote}.
 * <p>
 * Notes created from {@code Pf2eQuteNote} will use the {@code note2md.txt} template
 * unless otherwise noted. Folder index notes use {@code index2md.txt}.
 * </p>
 */
@TemplateData
public class Pf2eQuteNote extends QuteNote {
    final Pf2eIndexType type;

    public Pf2eQuteNote(Pf2eIndexType type, String name, String sourceText, List<String> text, Tags tags) {
        this(type, name, sourceText, String.join("\n", text), tags);
    }

    public Pf2eQuteNote(Pf2eIndexType type, Pf2eSources sources, String name, List<String> text, Tags tags) {
        super(sources, name, sources != null ? sources.getSourceText() : null, String.join("\n", text), tags);
        this.type = type;
    }

    public Pf2eQuteNote(Pf2eIndexType type, Pf2eSources sources, String name, String text, Tags tags) {
        super(sources, name, sources != null ? sources.getSourceText() : null, text, tags);
        this.type = type;
    }

    public Pf2eQuteNote(Pf2eIndexType type, String name, String sourceText, String text, Tags tags) {
        super(name, sourceText, text, tags);
        this.type = type;
    }

    public Pf2eQuteNote(Pf2eIndexType type, Pf2eSources sources, String text, Tags tags) {
        super(sources, sources.getName(), sources.getSourceText(), text, tags);
        this.type = type;
    }

    public Pf2eQuteNote(Pf2eIndexType type, Pf2eSources sources, String name) { // custom indexes
        super(sources, name, sources.getSourceText(), null, null);
        this.type = type;
    }

    public Pf2eIndexType indexType() {
        return type;
    }

    @Override
    public String targetPath() {
        return type.relativePath();
    }

    @Override
    public String targetFile() {
        if (sources != null && !type.defaultSource().sameSource(sources.primarySource())) {
            return getName() + "-" + sources.primarySource();
        }
        return getName();
    }
}
