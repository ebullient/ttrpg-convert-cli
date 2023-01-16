package dev.ebullient.convert.qute;

import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.IndexType;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class QuteBase {
    protected final String name;
    protected final CompendiumSources sources;
    public final String source;
    public final String text;
    public final Collection<String> tags;

    public QuteBase(CompendiumSources sources, String name, String source, String text, Collection<String> tags) {
        this.sources = sources;
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

    public boolean hasSections() {
        return text.contains("\n## ");
    }

    public List<ImageRef> images() {
        return List.of();
    };

    public CompendiumSources sources() {
        return sources;
    };

    public String title() {
        return name;
    }

    public String targetFile() {
        return name;
    }

    public String targetPath() {
        return ".";
    }

    public IndexType type() {
        return sources.getType();
    }

    public String key() {
        return sources.getKey();
    }
}
