package dev.ebullient.convert.tools.pf2e;

import java.nio.file.Path;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.pf2e.qute.QuteActivityType;

public enum Pf2eTypeActivity {
    single("Single Action", ">", "single_action.svg"),
    two("Two-Action activity", ">>", "two_actions.svg"),
    three("Three-Action activity", ">>>", "three_actions.svg"),
    free("Free Action", "F", "delay.svg"),
    reaction("Reaction", "R", "reaction.svg"),
    varies("Varies", "V", "load.svg"),
    timed("Duration or Frequency", "⏲", "hour-glass.svg");

    String caption;
    String textGlyph;
    String glyph;

    Pf2eTypeActivity(String caption, String textGlyph, String glyph) {
        this.caption = caption;
        this.textGlyph = textGlyph;
        this.glyph = glyph;
    }

    public static Pf2eTypeActivity toActivity(String unit, int number) {
        switch (unit) {
            case "action":
                switch (number) {
                    case 1:
                        return single;
                    case 2:
                        return two;
                    case 3:
                        return three;
                }
                break;
            case "free":
                return free;
            case "reaction":
                return reaction;
            case "varies":
                return varies;
            case "timed":
                return timed;
        }
        return null;
    }

    public String getCaption() {
        return this.caption;
    }

    public String getTextGlyph() {
        return this.textGlyph;
    }

    public String getGlyph() {
        return this.glyph;
    }

    public String linkify(String rulesRoot) {
        return String.format("[%s](%s \"%s\")",
                this.textGlyph, getRulesPath(rulesRoot), caption);
    }

    public String getRulesPath(String rulesRoot) {
        return String.format("%sTODO.md#%s",
                rulesRoot, this.caption.replace(" ", "%20")
                        .replace(".", ""));
    }

    public QuteActivityType toQuteActivityType(JsonSource convert) {
        String fileName = this.getGlyph();
        int x = fileName.lastIndexOf('.');
        Path target = Path.of("img",
                Tui.slugify(fileName.substring(0, x)) + fileName.substring(x));

        return new QuteActivityType(
                getCaption(),
                new ImageRef.Builder()
                        .setStreamSource(this.getGlyph())
                        .setTargetPath(convert.index().rulesPath(), target)
                        .setMarkdownPath(this.getCaption(), convert.index().rulesRoot())
                        .build(),
                this.getTextGlyph(),
                this.getRulesPath(convert.index().rulesRoot()));
    }
}
