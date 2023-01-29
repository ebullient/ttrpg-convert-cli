package dev.ebullient.convert.tools.pf2e.qute;

import dev.ebullient.convert.qute.ImageRef;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class QuteActivityType {
    public final ImageRef glyph;
    public final String text;
    public final String textGlyph;
    public final String rulesPath;

    public QuteActivityType(String text, ImageRef glyph, String textGlyph, String rulesPath) {
        this.text = text;
        this.glyph = glyph;
        this.textGlyph = textGlyph;
        this.rulesPath = rulesPath;
    }

    public String getText() {
        return text == null ? glyph.title : text;
    }

    public String toString() {
        return String.format("[%s](%s \"%s\")", textGlyph, rulesPath, glyph.title);
    }
}
