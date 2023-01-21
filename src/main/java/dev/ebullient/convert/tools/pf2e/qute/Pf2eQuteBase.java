package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;

import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class Pf2eQuteBase extends QuteBase {

    protected Pf2eIndexType type;

    public Pf2eQuteBase(Pf2eSources sources, String name, String source, String text, Collection<String> tags) {
        super(sources, name, source, text, tags);
        this.type = sources.getType();
    }

    @Override
    public String targetPath() {
        return type.relativePath();
    }

    @Override
    public String targetFile() {
        if (sources != null && !type.defaultSource().sameSource(sources.primarySource())) {
            return getName() + "-" + sources.primarySource();
        }
        return getName();
    }

}
