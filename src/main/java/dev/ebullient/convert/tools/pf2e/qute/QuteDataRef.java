package dev.ebullient.convert.tools.pf2e.qute;

import static dev.ebullient.convert.StringUtil.formatIfPresent;
import static dev.ebullient.convert.StringUtil.isPresent;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A reference to another note. This will render itself as a formatted link.
 *
 * @param displayText The display text for the link
 * @param notePath The path to the note that this link references. Null if we couldn't find a note to reference.
 * @param title The hover title to use for the reference (optional)
 */
public record QuteDataRef(String displayText, String notePath, String title) implements Comparable<QuteDataRef> {

    private static final Pattern MARKDOWN_LINK_PAT = Pattern.compile("^\\[(?<display>[^]]+)]\\((?<path>.*?)(?: \"(?<title>.*)\")?\\)$");

    public QuteDataRef(String displayText) {
        this(displayText, null, null);
    }

    public static QuteDataRef fromMarkdownLink(String link) {
        if (!isPresent(link)) {
            return null;
        }
        Matcher matcher = MARKDOWN_LINK_PAT.matcher(link);
        return matcher.matches()
            ? new QuteDataRef(matcher.group("display"), matcher.group("path"), matcher.group("title"))
            : new QuteDataRef(link);
    }

    /** Return this reference as a Markdown link, without the title attribute. */
    public String withoutTitle() {
        return notePath != null ? "[%s](%s)".formatted(displayText, notePath) : displayText;
    }

    @Override
    public String toString() {
        return notePath != null
            ? "[%s](%s%s)".formatted(displayText, notePath, formatIfPresent(" \"%s\"", title))
            : displayText;
    }

    @Override
    public int compareTo(QuteDataRef o) {
        if (!displayText.equals(o.displayText)) {
            return displayText.compareTo(o.displayText);
        } else if (!Objects.equals(notePath, o.notePath)) {
            if (notePath == null) {
                return -1;
            } else if (o.notePath == null) {
                return 1;
            }
            return notePath.compareTo(o.notePath);
        } else if (!Objects.equals(title, o.title)) {
            if (title == null) {
                return -1;
            } else if (o.title == null) {
                return 1;
            }
            return title.compareTo(o.title);
        }
        return 0;
    }
}
