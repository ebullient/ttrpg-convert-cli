package dev.ebullient.json5e.qute;

import java.util.List;

public class QuteSubclass extends QuteNote {
    public final String parentClass;
    public final String parentClassLink;
    public final String subclassTitle;
    public final String classProgression;

    public QuteSubclass(String name, String source,
            String parentClass,
            String parentClassLink,
            String subclassTitle, String classProgression,
            String text, List<String> tags) {
        super(name, source, text, tags);

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
    public String title() {
        return parentClass + ": " + getName();
    }
}
