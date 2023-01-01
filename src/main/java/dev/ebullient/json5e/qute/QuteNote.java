package dev.ebullient.json5e.qute;

import java.util.Collection;

import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class QuteNote {
    final String name;
    public final String source;
    public final String text;
    public final Collection<String> tags;

    public QuteNote(String name, String source, String text, Collection<String> tags) {
        this.name = name;
        this.source = source;
        this.text = text;
        this.tags = tags;
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
}
