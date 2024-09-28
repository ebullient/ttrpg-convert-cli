package dev.ebullient.convert.tools.pf2e.qute;

/**
 * A duration of time. This may be either a {@link QuteDataTimedDuration}, which represents a period of time longer
 * than an activity, or a {@link QuteDataActivity}. Use {@link QuteDataDuration#isActivity()} to check whether this
 * duration is an activity.
 *
 * Using this directly will give the default representation for either object.
 */
public sealed interface QuteDataDuration permits QuteDataActivity, QuteDataTimedDuration {

    /** Returns true if this duration is a {@link QuteDataActivity}. */
    default boolean isActivity() {
        return this instanceof QuteDataActivity;
    }
}
