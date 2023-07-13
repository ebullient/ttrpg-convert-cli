package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.List;

import dev.ebullient.convert.qute.QuteNote;
import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.Tags;

public class Tools5eQuteNote extends QuteNote {

    String targetPath;
    String filename;
    String template;

    public Tools5eQuteNote(CompendiumSources sources, String name, String sourceText, String text, Tags tags) {
        super(sources, name, sourceText, text, tags.build());
    }

    public Tools5eQuteNote(String name, String sourceText, String text, Tags tags) {
        super(name, sourceText, text, tags.build());
    }

    public Tools5eQuteNote(String name, String sourceText, List<String> text, Tags tags) {
        super(name, sourceText, text, tags.build());
    }

    public Tools5eQuteNote withTargeFile(String filename) {
        this.filename = filename;
        return this;
    }

    public String targetFile() {
        return filename == null ? super.targetFile() : filename;
    }

    public Tools5eQuteNote withTargetPath(String path) {
        this.targetPath = path;
        return this;
    }

    public String targetPath() {
        return targetPath == null ? super.targetPath() : targetPath;
    }

    public Tools5eQuteNote withTemplate(String template) {
        this.template = template;
        return this;
    }

    public String template() {
        return template == null ? super.template() : template;
    }
}
