package dev.ebullient.convert.tools.pf2e.qute;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.QuteUtil;
import io.quarkus.qute.TemplateData;

import static dev.ebullient.convert.StringUtil.join;

/**
 * Pf2eTools activity attributes. This attribute will render itself as a formatted link:
 *
 * <pre>
 *     [textGlyph](rulesPath "glyph.title")&lt;optional text&gt;
 * </pre>
 *
 * @param text The text associated with the action - may be null.
 * @param glyph icon/image representing this activity as a {@link dev.ebullient.convert.qute.ImageRef ImageRef}
 * @param textGlyph A textual representation of the glyph, used as the link text
 * @param rulesPath The path which leads to an explanation of this particular activity
 */
@TemplateData
public record QuteDataActivity(String text, ImageRef glyph, String textGlyph, String rulesPath) implements QuteUtil {

    /** Return the text associated with the action. */
    @Override
    public String text() {
        return isPresent(text) ? text : glyph.title;
    }

    public String toString() {
        return join(" ", "[%s](%s \"%s\")".formatted(textGlyph, rulesPath, glyph.title), text);
    }
}
