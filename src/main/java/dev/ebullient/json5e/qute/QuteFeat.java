package dev.ebullient.json5e.qute;

import java.util.List;

import dev.ebullient.json5e.tools5e.CompendiumSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class QuteFeat extends QuteBase {

    public final String level;
    public final String prerequisite;

    public QuteFeat(CompendiumSources sources, String name, String source, String prerequisite, String level, String text,
            List<String> tags) {
        super(sources, name, source, text, tags);
        this.level = level;
        this.prerequisite = prerequisite; // optional
    }

    @Override
    public String targetPath() {
        return QuteSource.FEATS_PATH;
    }
}
