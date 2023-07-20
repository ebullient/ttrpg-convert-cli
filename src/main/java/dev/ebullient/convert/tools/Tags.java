package dev.ebullient.convert.tools;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.TtrpgConfig;

public class Tags {
    private final Set<String> tags = new TreeSet<>();
    private final CompendiumConfig config;

    public Tags() {
        this(null);
    }

    public Tags(CompendiumSources sources) {
        this.config = TtrpgConfig.getConfig();
        addSourceTags(sources);
    }

    public void addSourceTags(CompendiumSources sources) {
        if (sources != null) {
            tags.addAll(sources.getSourceTags());
        }
    }

    public void add(String... tag) {
        tags.add(config.tagOf(tag));
    }

    public Tags addAll(List<String> newTags) {
        newTags.forEach(x -> add(x));
        return this;
    }

    public Set<String> build() {
        return tags;
    }
}
