package dev.ebullient.convert.tools;

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
            tags.addAll(sources.primarySourceTag());
        }
    }

    /** Prepend configured prefix and slugify parts */
    public void addRaw(String first, String rawValue) {
        tags.add(config.tagOfRaw(first + "/" + rawValue));
    }

    /** Prepend configured prefix and slugify parts */
    public void add(String... tag) {
        tags.add(config.tagOf(tag));
    }

    public Set<String> build() {
        return tags;
    }

    @Override
    public String toString() {
        return "Tags [tags=" + tags + "]";
    }
}
