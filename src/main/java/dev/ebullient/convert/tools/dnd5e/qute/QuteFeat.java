package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.Collection;

import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class QuteFeat extends Tools5eQuteBase {

    public final String level;
    public final String prerequisite;

    public QuteFeat(Tools5eSources sources, String name, String source, String prerequisite, String level, String text,
            Collection<String> tags) {
        super(sources, name, source, text, tags);
        this.level = level;
        this.prerequisite = prerequisite; // optional
    }

    @Override
    public String targetPath() {
        return Tools5eQuteBase.FEATS_PATH;
    }
}
