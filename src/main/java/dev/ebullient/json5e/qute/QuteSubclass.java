package dev.ebullient.json5e.qute;

import java.util.List;

import dev.ebullient.json5e.tools5e.CompendiumSources;

public class QuteSubclass extends QuteBase {

    public static String getFileName(String name, String parentClass) {
        return parentClass + "-" + name;
    }

    public final String parentClass;
    public final String parentClassLink;
    public final String subclassTitle;
    public final String classProgression;

    public QuteSubclass(CompendiumSources sources, String name, String source,
            String parentClass,
            String parentClassLink,
            String subclassTitle, String classProgression,
            String text, List<String> tags) {
        super(sources, name, source, text, tags);

        this.parentClass = parentClass;
        this.parentClassLink = parentClassLink;
        this.subclassTitle = subclassTitle;
        this.classProgression = classProgression;
    }

    @Override
    public String targetPath() {
        return "classes";
    }

    @Override
    public String targetFile() {
        return getFileName(name, parentClass);
    }

    @Override
    public String title() {
        return parentClass + ": " + getName();
    }
}
