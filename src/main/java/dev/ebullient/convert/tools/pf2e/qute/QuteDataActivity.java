package dev.ebullient.convert.tools.pf2e.qute;

import dev.ebullient.convert.qute.ImageRef;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools activity attributes
 * <p>
 * This attribute will render itself as a formatted link:
 * `[textGlyph](rulesPath "glyph.title")<optional text>`
 * </p>
 */
@TemplateData
public class QuteDataActivity {
    /** icon/image representing this activity as a {@link dev.ebullient.convert.qute.ImageRef ImageRef} */
    public final ImageRef glyph;
    public final String textGlyph;
    public final String rulesPath;
    final String text;

    public QuteDataActivity(String text, ImageRef glyph, String textGlyph, String rulesPath) {
        this.text = text;
        this.glyph = glyph;
        this.textGlyph = textGlyph;
        this.rulesPath = rulesPath;
    }

    /** True if this is a descriptive activity (duration or condition) */
    public boolean isVerbose() {
        return text != null;
    }

    /** Return the text associated with the action */
    public String getText() {
        return text == null ? glyph.title : text;
    }

    public String toString() {
        return String.format("[%s](%s \"%s\")%s",
                textGlyph, rulesPath, glyph.title,
                isVerbose() ? " " + text : "");
    }
}
