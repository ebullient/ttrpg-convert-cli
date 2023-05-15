package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.qute.QuteNote;
import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

@TemplateData
public class Pf2eQuteNote extends QuteNote {
    public final Pf2eIndexType type;

    public Pf2eQuteNote(Pf2eIndexType type, String name, String sourceText, List<String> text, Collection<String> tags) {
        this(type, name, sourceText, String.join("\n", text), tags);
    }

    public Pf2eQuteNote(Pf2eIndexType type, Pf2eSources sources, String name, List<String> text, Collection<String> tags) {
        super(sources, name, sources.getSourceText(), String.join("\n", text), tags);
        this.type = type;
    }

    public Pf2eQuteNote(Pf2eIndexType type, String name, String sourceText, String text, Collection<String> tags) {
        super(name, sourceText, text, tags);
        this.type = type;
    }

    public Pf2eQuteNote(Pf2eIndexType type, Pf2eSources sources, String text, Collection<String> tags) {
        super(sources, sources.getName(), sources.getSourceText(), text, tags);
        this.type = type;
    }

    public Pf2eQuteNote(Pf2eIndexType type, Pf2eSources sources, String name) { // custom indexes
        super(sources, name, sources.getSourceText(), null, List.of());
        this.type = type;
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
