package dev.ebullient.convert.qute;

import java.util.List;

import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.Tags;
import io.quarkus.qute.TemplateData;

/**
 * Common attributes for simple notes. THese attributes are more
 * often used by books, adventures, rules, etc.
 *
 * Notes created from {@code QuteNote} (or a derivative) will look for a template
 * named {@code note2md.txt} by default.
 */
@TemplateData
public class QuteNote extends QuteBase {

    public QuteNote(String name, String sourceText, List<String> text, Tags tags) {
        super(null, name, sourceText, String.join("\n", text), tags);
    }

    public QuteNote(String name, String sourceText, String text, Tags tags) {
        super(null, name, sourceText, text, tags);
    }

    public QuteNote(CompendiumSources sources, String name, String sourceText, String text, Tags tags) {
        super(sources, name, sourceText, text, tags);
    }

    public String title() {
        return name;
    }

    public String targetFile() {
        return name;
    }

    public String targetPath() {
        return ".";
    }

    public String template() {
        return "note2md.txt";
    }
}
