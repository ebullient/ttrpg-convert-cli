package dev.ebullient.json5e.qute;

import java.util.List;

public class QuteSubclass implements QuteSource {
    public final String name;
    public final String source;
    public final String parentClass;
    public final String parentClassLink;
    public final String subclassTitle;
    public final String classProgression;
    public final String text;
    public final List<String> tags;

    public QuteSubclass(String name, String source,
            String parentClass,
            String parentClassLink,
            String subclassTitle, String classProgression,
            String text, List<String> tags) {
        this.name = name;
        this.source = source;
        this.parentClass = parentClass;
        this.parentClassLink = parentClassLink;
        this.subclassTitle = subclassTitle;
        this.classProgression = classProgression;
        this.text = text;
        this.tags = tags;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSource() {
        return source;
    }

}
