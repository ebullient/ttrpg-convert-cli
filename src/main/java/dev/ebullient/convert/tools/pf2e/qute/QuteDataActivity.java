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

    @Override
    public String toString() {
        return join(" ", actionRef.toString(), note);
    }

    public enum Activity {
        single("Single Action", ">"),
        two("Two-Action", ">>"),
        three("Three-Action", ">>>"),
        free("Free Action", "F"),
        reaction("Reaction", "R"),
        varies("Varies", "V"),
        timed("Duration or Frequency", "⏲");

        public final String longName;
        public final String textGlyph;

        Activity(String longName, String textGlyph) {
            this.longName = longName;
            this.textGlyph = textGlyph;
        }
    }
}
