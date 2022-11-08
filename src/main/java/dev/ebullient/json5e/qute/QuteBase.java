package dev.ebullient.json5e.qute;

import java.util.Collection;
import java.util.List;

import dev.ebullient.json5e.tools5e.CompendiumSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class QuteBase implements QuteSource {
    final String name;
    final CompendiumSources sources;
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

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSource() {
        return source;
    }

    public boolean hasSections() {
        return text.contains("\n## ");
    }

    @Override
    public List<ImageRef> images() {
        return List.of();
    }

    @Override
    public String title() {
        return name;
    }

    @Override
    public String targetFile() {
        return name + QuteSource.sourceIfNotCore(sources.primarySource());
    }

    @Override
    public String targetPath() {
        return ".";
    }

    @Override
    public String key() {
        return sources.getKey();
    }
}
