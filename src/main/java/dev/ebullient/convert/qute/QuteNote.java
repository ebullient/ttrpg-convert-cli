package dev.ebullient.convert.qute;

import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.tools.CompendiumSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class QuteNote extends QuteBase {

    public QuteNote(String name, String sourceText, List<String> text, Collection<String> tags) {
        super(null, name, sourceText, String.join("\n", text), tags);
    }

    public QuteNote(String name, String sourceText, String text, Collection<String> tags) {
        super(null, name, sourceText, text, tags);
    }

    public QuteNote(CompendiumSources sources, String name, String sourceText, String text, Collection<String> tags) {
        super(sources, name, sourceText, text, tags);
    }

    public String getName() {
        return name;
    }

    public String getSource() {
        return source;
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
    public String template() {
        return "note2md.txt";
    }
}
