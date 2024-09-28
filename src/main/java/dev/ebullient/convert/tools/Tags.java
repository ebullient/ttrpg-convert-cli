package dev.ebullient.convert.tools;

import static dev.ebullient.convert.StringUtil.join;

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
            String sourceTag = config.tagOfRaw(sources.primarySourceTag());
            tags.add(sourceTag);
        }
    }

    /** Prepend configured prefix and slugify parts */
    public void addRaw(String... rawValues) {
        String rawTag = config.tagOfRaw(join("/", List.of(rawValues)));
        tags.add(rawTag);
    }

    /** Prepend configured prefix and slugify parts */
    public void add(String... segments) {
        String tag = config.tagOf(segments);
        tags.add(tag);
    }

    public Set<String> build() {
        return tags;
    }

    @Override
    public String toString() {
        return "Tags [tags=" + tags + "]";
    }
}
