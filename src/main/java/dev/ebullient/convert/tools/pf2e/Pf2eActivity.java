package dev.ebullient.convert.tools.pf2e;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.join;

import java.nio.file.Path;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataActivity;

public enum Pf2eActivity {
    single("Single Action", ">", "single_action.svg"),
    two("Two-Action", ">>", "two_actions.svg"),
    three("Three-Action", ">>>", "three_actions.svg"),
    free("Free Action", "F", "delay.svg"),
    reaction("Reaction", "R", "reaction.svg"),
    varies("Varies", "V", "load.svg"),
    timed("Duration or Frequency", "⏲", "hour-glass.svg");

    final static String DOC_PATH = "core-rulebook/chapter-9-playing-the-game.md#Actions";

    final String longName;
    final String markdownName;
    final String textGlyph;
    final String glyph;
    final String targetFileName;

    Pf2eActivity(String longName, String textGlyph, String glyph) {
        this.longName = longName;
        this.markdownName = longName.replace(" ", "%20");
        this.textGlyph = textGlyph;
        this.glyph = glyph;

        int x = glyph.lastIndexOf('.');
        this.targetFileName = Tui.slugify(glyph.substring(0, x)) + glyph.substring(x);
    }

    public static Pf2eActivity toActivity(String unit, int number) {
        switch (unit) {
            case "single":
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
        return String.format("%s%s", rulesRoot, DOC_PATH);
    }

    public QuteDataActivity toQuteActivity(JsonSource convert, String text) {
        Path relativeTarget = Path.of("img", targetFileName);
        return new QuteDataActivity(
                this != timed && isPresent(text) ? join(" ", getLongName(), text) : text,
                Pf2eSources.buildStreamImageRef(convert.index(), glyph, relativeTarget, longName),
                textGlyph,
                this.getRulesPath(convert.index().rulesVaultRoot()));
    }
}
