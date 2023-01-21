package dev.ebullient.convert.tools.pf2e.qute;

import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import io.quarkus.runtime.annotations.RegisterForReflection;

@RegisterForReflection
public class QuteActivityType {
    public final ImageRef glyph;
    public final String text;
    public final String textGlyph;
    public final String rulesPath;

    public QuteActivityType(String text, ImageRef glyph, String textGlyph, String rulesPath) {
        this.glyph = glyph;
        this.text = text;
        this.textGlyph = textGlyph;
        this.rulesPath = rulesPath;
    }

    public String getCaption() {
        return glyph != null ? glyph.caption : text;
    }

    public String toString() {
        return String.format("[%s](%s)", text, rulesPath);
    }

    public List<ImageRef> image() {
        return glyph == null ? List.of() : List.of(glyph);
    }
}
