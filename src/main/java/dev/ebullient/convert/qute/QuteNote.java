package dev.ebullient.convert.qute;

import java.util.Collection;

import dev.ebullient.convert.tools.CompendiumSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class QuteNote extends QuteBase {

    public QuteNote(String name, String source, String text, Collection<String> tags) {
        super(null, name, source, text, tags);
    }

    public QuteNote(CompendiumSources sources, String name, String source, String text, Collection<String> tags) {
        super(sources, name, source, text, tags);
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
