package dev.ebullient.json5e.qute;

import java.util.List;

import dev.ebullient.json5e.tools5e.CompendiumSources;

public class QuteSubclass extends QuteBase {

    public final String parentClass;
    public final String parentClassLink;

    public final String parentClassSource;

    final String subclassName;
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

        int first = name.indexOf('|');
        int second = name.indexOf('|', first + 1);
        subclassName = name.substring(first + 1, second);
    }

    @Override
    public String targetPath() {
        return QuteSource.CLASSES_PATH;
    }

    @Override
    public String targetFile() {
        return QuteSource.getSubclassResourceName(subclassName, parentClass)
                + QuteSource.sourceIfNotCore(parentClassSource);
    }

    @Override
    public String title() {
        return parentClass + ": " + getName();
    }
}
