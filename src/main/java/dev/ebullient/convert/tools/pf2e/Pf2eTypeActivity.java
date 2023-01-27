package dev.ebullient.convert.tools.pf2e;

import java.nio.file.Path;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.pf2e.qute.QuteActivityType;

public enum Pf2eTypeActivity {
    single("Single Action", ">", "single_action.svg"),
    two("Two-Action", ">>", "two_actions.svg"),
    three("Three-Action", ">>>", "three_actions.svg"),
    free("Free Action", "F", "delay.svg"),
    reaction("Reaction", "R", "reaction.svg"),
    varies("Varies", "V", "load.svg"),
    timed("Duration or Frequency", "⏲", "hour-glass.svg");

    final String longName;
    final String markdownName;
    final String textGlyph;
    final String glyph;
    final String targetFileName;

    Pf2eTypeActivity(String longName, String textGlyph, String glyph) {
        this.longName = longName;
        this.markdownName = longName.replace(" ", "%20");
        this.textGlyph = textGlyph;
        this.glyph = glyph;

        int x = glyph.lastIndexOf('.');
        this.targetFileName = Tui.slugify(glyph.substring(0, x)) + glyph.substring(x);
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

    public String getLongName() {
        return this.longName;
    }

    public String getTextGlyph() {
        return this.textGlyph;
    }

    public String getGlyph() {
        return this.glyph;
    }

    public String linkify(String rulesRoot) {
        return String.format("[%s](%s \"%s\")",
                this.textGlyph, getRulesPath(rulesRoot), longName);
    }

    public String getRulesPath(String rulesRoot) {
        return String.format("%sTODO.md#%s", rulesRoot, markdownName);
    }

    public QuteActivityType toQuteActivityType(JsonSource convert, String text) {
        Path target = Path.of("img", targetFileName);
        return new QuteActivityType(
                text == null ? longName : text,
                new ImageRef.Builder()
                        .setStreamSource(glyph)
                        .setTargetPath(convert.index().rulesPath(), target)
                        .setMarkdownPath(longName, convert.index().rulesRoot())
                        .build(),
                textGlyph,
                this.getRulesPath(convert.index().rulesRoot()));
    }
}
