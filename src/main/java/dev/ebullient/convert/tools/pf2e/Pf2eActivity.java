package dev.ebullient.convert.tools.pf2e;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.join;

import java.nio.file.Path;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataActivity;
import dev.ebullient.convert.tools.pf2e.qute.QuteDataActivity.Activity;

public class Pf2eActivity {
    final static String DOC_PATH = "core-rulebook/chapter-9-playing-the-game.md#Actions";

    public static void addImageRef(Activity activity, JsonSource convert) {
        String glyph = switch (activity) {
            case single -> "single_action";
            case two -> "two_actions";
            case three -> "three_actions";
            case free -> "delay";
            case reaction -> "reaction";
            case varies -> "load";
            case timed -> "hour-glass";
        };
        String targetFileName = Tui.slugify(glyph) + ".svg";
        Pf2eSources.buildStreamImageRef(
            convert.index(), glyph + ".svg", Path.of("img", targetFileName), activity.longName);
    }

    public static String linkifyActivity(Activity activity, String rulesRoot) {
        return "[%s](%s \"%s\")".formatted(activity.textGlyph, rulesRoot + DOC_PATH, activity.longName);
    }

    public static QuteDataActivity toQuteActivity(JsonSource convert, String unit, int number, String text) {
        Activity activity = switch (unit) {
            case "single", "action" -> switch (number) {
                    case 1 -> Activity.single;
                    case 2 -> Activity.two;
                    case 3 -> Activity.three;
                    default -> null;
            };
            case "free" -> Activity.free;
            case "reaction" -> Activity.reaction;
            case "varies" -> Activity.varies;
            case "timed" -> Activity.timed;
            default -> null;
        };
        if (activity == null) {
            return null;
        }
        return toQuteActivity(convert, activity, text);
    }

    public static QuteDataActivity toQuteActivity(JsonSource convert, Activity activity, String text) {
        addImageRef(activity, convert);
        return new QuteDataActivity(
            activity,
            convert.index().rulesVaultRoot() + DOC_PATH,
            activity != Activity.timed && isPresent(text) ? join(" ", activity.longName, text) : text);
    }
}
