package dev.ebullient.convert.tools.dnd5e.qute;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class QuteSubclass extends Tools5eQuteBase {

    public final String parentClass;
    public final String parentClassLink;

    public final String parentClassSource;

    public final String subclassTitle;
    public final String classProgression;

    public QuteSubclass(Tools5eSources sources, String name, String source,
            String parentClass,
            String parentClassLink,
            String parentClassSource,
            String subclassTitle,
            String classProgression,
            String text, Tags tags) {
        super(sources, name, source, text, tags);

        this.parentClass = parentClass;
        this.parentClassLink = parentClassLink;
        this.parentClassSource = parentClassSource;
        this.subclassTitle = subclassTitle;
        this.classProgression = classProgression;
    }

    @Override
    public String targetFile() {
        return Tools5eQuteBase.getSubclassResource(name, parentClass, sources.primarySource());
    }

    @Override
    public String title() {
        return parentClass + ": " + getName();
    }
}
