package dev.ebullient.json5e.qute;

import java.util.List;

import dev.ebullient.json5e.tools5e.CompendiumSources;

public class QuteBackground extends QuteBase {

    public QuteBackground(CompendiumSources sources, String name, String source, String text, List<String> tags) {
        super(sources, name, source, text, tags);
    }

    @Override
    public String targetPath() {
        return QuteSource.BACKGROUND_PATH;
    }
}
