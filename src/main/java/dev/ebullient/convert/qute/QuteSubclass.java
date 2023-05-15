package dev.ebullient.convert.qute;

import java.util.List;

import dev.ebullient.convert.tools5e.CompendiumSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class QuteSubclass extends QuteBase {

    public final String parentClass;
    public final String parentClassLink;

    public final String parentClassSource;

    public final String subclassTitle;
    public final String classProgression;

    public QuteSubclass(CompendiumSources sources, String name, String source,
            String parentClass,
            String parentClassLink,
            String parentClassSource,
            String subclassTitle, String classProgression,
            String text, List<String> tags) {
        super(sources, name, source, text, tags);

        this.parentClass = parentClass;
        this.parentClassLink = parentClassLink;
        this.parentClassSource = parentClassSource;
        this.subclassTitle = subclassTitle;
        this.classProgression = classProgression;
    }

    @Override
    public String targetPath() {
        return QuteSource.CLASSES_PATH;
    }

    @Override
    public String targetFile() {
        return QuteSource.getSubclassResourceName(name, parentClass)
                + QuteSource.sourceIfNotCore(parentClassSource);
    }

    @Override
    public String title() {
        return parentClass + ": " + getName();
    }
}
