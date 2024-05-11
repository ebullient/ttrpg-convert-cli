package dev.ebullient.convert;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Holds common, generic string utiltity methods.
 *
 * <p>This should only contain string utility methods which don't involve any domain-specific manipulation or knowledge.
 * Those should instead go in a {@link dev.ebullient.convert.tools.JsonTextConverter} or a
 * {@link dev.ebullient.convert.tools.JsonNodeReader}.</p>
 */
public class StringUtil {
    /**
     * Returns the given strings joined into a single trimmed string using the given delimiter, or an empty string if
     * the input list is null or empty.
     *
     * @param joiner The delimiter to use to join the strings
     * @param list The input strings to join together
     */
    public static String join(String joiner, Collection<String> list) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(joiner, list).trim();
    }

    /**
     * Return the given list joined into a comma-delimited string, with an additional delimiter for the last element.
     *
     * <pre>
     *     joinConjunct(" and ", List.of("one", "two", "three"))  // -> "one, two, and three"
     * </pre>
     *
     * @param list The list of strings to join
     * @param lastJoiner The delimiter to use for the last element
     */
    public static String joinConjunct(String lastJoiner, List<String> list) {
        return joinConjunct(list, ", ", lastJoiner, false);
    }

    /**
     * Return the given list joined into a single string, with an additional delimiter for the last element.
     *
     * <pre>
     *     joinConjunct(", ", " and ", List.of("one", "two", "three"))  // -> "one, two, and three"
     * </pre>
     *
     * @param joiner The delimiter to use for all elements
     * @param list The list of strings to join
     * @param lastJoiner The additional delimiter to add before the last element
     */
    public static String joinConjunct(String joiner, String lastJoiner, List<String> list) {
        return joinConjunct(list, joiner, lastJoiner, false);
    }

    /**
     * Return the given list joined into a single string, using a different delimiter for the last element.
     *
     * <pre>
     *     joinConjunct(List.of("one", "two", "three"), ", ", " and ", false)  // "one, two, and three"
     *     joinConjunct(List.of("one", "two", "three"), ", ", " and ", true)   // "one, two and three"
     * </pre>
     *
     * @param list The list of strings to join
     * @param joiner The delimiter to use for all elements except the last
     * @param lastJoiner The delimiter to add before the last element (or instead of the last delimiter, if {@code nonOxford} is true).
     * @param nonOxford If this is true, then don't add a normal delimiter before the final delimiter.
     */
    public static String joinConjunct(List<String> list, String joiner, String lastJoiner, boolean nonOxford) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        if (list.size() == 2) {
            return String.join(lastJoiner, list);
        }

        int pause = list.size() - 2;
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < list.size(); ++i) {
            out.append(list.get(i));

            if (i < pause) {
                out.append(joiner);
            } else if (i == pause) {
                if (!nonOxford) {
                    out.append(joiner.trim());
                }
                out.append(lastJoiner);
            }
        }
        return out.toString();
    }

    /** Return the given text converted to title case, with the first letter of each word capitalized. */
    public static String toTitleCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Arrays.stream(text.split(" "))
            .map(word -> word.isEmpty()
                ? word
                : Character.toTitleCase(word.charAt(0)) + word.substring(1).toLowerCase())
            .collect(Collectors.joining(" "));
    }

    /** Returns true if the given string is non-null and non-blank. */
    public static boolean isPresent(String s) {
        return s != null && !s.isBlank();
    }

    /** Return the given string, possibly pluralized based on the number given. */
    public static String pluralize(String s, int howMany) {
        return s + (howMany == 1 ? "" : "s");
    }

    /** Return the given string, possibly pluralized based on the number given. */
    public static String pluralize(String s, String howMany) {
        return s + (howMany.equals("1") ? "" : "s");
    }
}
