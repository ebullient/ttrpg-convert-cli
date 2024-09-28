package dev.ebullient.convert.tools.dnd5e.qute;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools subclass attributes ({@code subclass2md.txt})
 *
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase}.
 */
@TemplateData
public class QuteSubclass extends Tools5eQuteBase {

    /** Name of the parent class */
    public final String parentClass;
    /** Markdown link to the parent class */
    public final String parentClassLink;
    /** Source of the parent class (abbreviation) */
    public final String parentClassSource;
    /** Title of subclass: "Bard College", or "Primal Path" */
    public final String subclassTitle;
    /** A pre-foramatted markdown callout describing subclass spell or feature progression */
    public final String classProgression;

    public QuteSubclass(Tools5eSources sources,
            String name, String source,
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
        return Tools5eQuteBase.getSubclassResource(name,
                parentClass, parentClassSource,
                sources.primarySource());
    }

    @Override
    public String title() {
        return parentClass + ": " + getName();
    }
}
