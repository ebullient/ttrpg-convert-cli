package dev.ebullient.convert.tools.pf2e.qute;

import static dev.ebullient.convert.StringUtil.join;

import dev.ebullient.convert.qute.QuteUtil;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools activity attributes. This attribute will render itself as a formatted link:
 *
 * <pre>
 *     [textGlyph](rulesPath "action name")&lt;optional text&gt;
 * </pre>
 *
 * @param activity The type of activity, as a {@link QuteDataActivity.Activity}
 * @param actionRef A {@link QuteDataRef} to the rules for this particular action type
 * @param note Text associated with this activity
 */
@TemplateData
public record QuteDataActivity(Activity activity, QuteDataRef actionRef, String note) implements QuteUtil, QuteDataDuration {

    public QuteDataActivity(Activity activity, String rulesPath, String note) {
        this(activity, new QuteDataRef(activity.textGlyph, rulesPath, activity.longName), note);
    }

    /** Return the text associated with the action. */
    public String text() {
        return isPresent(note) ? note : activity.longName;
    }

    /**
     * Return the single-character Pathfinder 2e font unicode glyph used to represent this action, or if there is no single
     * character (eg for varies and duration activities), return {@link QuteDataActivity#text()}.
     */
    public String getUnicodeGlyphOrText() {
        return activity.unicodeChar != null ? activity.unicodeChar.toString() : text();
    }

    @Override
    public String toString() {
        return join(" ", actionRef.toString(), note);
    }

    public enum Activity {
        single("Single Action", ">", '⬻'),
        two("Two-Action", ">>", '⬺'),
        three("Three-Action", ">>>", '⬽'),
        free("Free Action", "F", '⭓'),
        reaction("Reaction", "R", '⬲'),
        varies("Varies", "V", null),
        timed("Duration or Frequency", "⏲", null);

        public final String longName;
        public final String textGlyph;
        public final Character unicodeChar;

        Activity(String longName, String textGlyph, Character unicodeChar) {
            this.longName = longName;
            this.textGlyph = textGlyph;
            this.unicodeChar = unicodeChar;
        }
    }
}
