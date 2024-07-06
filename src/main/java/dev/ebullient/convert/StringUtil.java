package dev.ebullient.convert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Holds common, generic string utiltity methods.
 *
 * <p>
 * This should only contain string utility methods which don't involve any domain-specific manipulation or knowledge.
 * Those should instead go in a {@link dev.ebullient.convert.tools.JsonTextConverter} or a
 * {@link dev.ebullient.convert.tools.JsonNodeReader}.
 * </p>
 */
public class StringUtil {

    /**
     * Return {@code formatString} formatted with {@code o} as the first parameter.
     * If {@code o} is null, then return an empty string.
     */
    public static String formatIfPresent(String formatString, Object val) {
        return val == null || val.toString().isBlank() ? "" : formatString.formatted(val);
    }

    public static String valueOrDefault(String value, String fallback) {
        return value == null || value.isEmpty() ? fallback : value;
    }

    public static String valueOrDefault(String[] parts, int index, String fallback) {
        return index < 0 || index >= parts.length
                ? fallback
                : valueOrDefault(parts[index], fallback);
    }

    public static String uppercaseFirst(String value) {
        return value == null || value.isEmpty() ? value : Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    public static boolean equal(Object o1, Object o2) {
        return o1 == null ? o2 == null : o1.equals(o2);
    }

    /**
     * {@link #join(String, Collection)} but with the ability to accept varargs.
     *
     * @see #join(String, Collection)
     */
    public static String join(String joiner, Object o1, Object... rest) {
        List<Object> args = new ArrayList<>();
        args.add(o1);
        args.addAll(Arrays.asList(rest));
        return join(joiner, args);
    }

    /**
     * Join the list into a single trimmed string, delimited using the given delimiter. Returns an empty string if the
     * input list is null or empty, and ignores empty and null input elements.
     *
     * @param joiner The delimiter to use to join the strings
     * @param list The input strings to join together
     */
    public static String join(String joiner, Collection<?> list) {
        return list == null ? "" : list.stream().collect(joiningNonEmpty(joiner)).trim();
    }

    /**
     * Like {@link #join(String, Collection)} but acts on a number of collections by first flattening them into a single
     * collection.
     */
    public static String flatJoin(String joiner, Collection<?>... lists) {
        return join(joiner, Arrays.stream(lists).flatMap(Collection::stream).toList());
    }

    /**
     * {@link #joinConjunct(String, String, List)} with a {@code ", "} joiner.
     *
     * @see #joinConjunct(List, String, String, boolean)
     * @see #joinConjunct(String, String, List)
     */
    public static String joinConjunct(String lastJoiner, List<String> list) {
        return joinConjunct(", ", lastJoiner, list);
    }

    /**
     * {@link #joinConjunct(List, String, String, boolean)} with a false {@code nonOxford}.
     *
     * @see #joinConjunct(List, String, String, boolean)
     * @see #joinConjunct(String, List)
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
     * @param lastJoiner The delimiter to add before the last element (or instead of the last delimiter, if {@code nonOxford} is
     *        true).
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

    /**
     * Return the given string pluralized or singularized based on the input number. Use {@code assumeSingular} to
     * correctly format non-plural inputs which end in 's' (e.g. "walrus").
     *
     * <p>
     * <b>Known Limitations: </b>This does not handle possessives because English is difficult.
     * </p>
     *
     * <p>
     * Examples:
     * </p>
     *
     * <pre>
     *     pluralize("foot", 2)    // -> "feet"
     *     pluralize("feet", 1)    // -> "foot"
     *     pluralize("mile", 2)    // -> "miles"
     *     pluralize("miles", 1)   // -> "mile"
     *     // -> "walrus" (INCORRECT)
     *     pluralize("walrus", 2, false)
     *     // -> "walruses" (CORRECT)
     *     pluralize("walrus", 2, true)
     * </pre>
     */
    public static String pluralize(String s, Integer howMany, boolean assumeSingular) {
        if (s == null || howMany == null) {
            return null;
        }
        if (s.isEmpty()) {
            return "";
        }
        if (s.endsWith("s")) {
            if (assumeSingular) {
                return s + "es";
            }
            s = s.substring(0, s.length() - 1);
        }
        return switch (s) {
            case "foot", "feet" -> howMany == 1 ? "foot" : "feet";
            default -> howMany == 1 ? s : s + "s";
        };
    }

    /**
     * {@link #pluralize(String, Integer, boolean)} with {@code assumeSingular} set to {@code false}
     *
     * @see #pluralize(String, Integer, boolean)
     */
    public static String pluralize(String s, Integer howMany) {
        return pluralize(s, howMany, false);
    }

    /**
     * Return the given string surrounded in parentheses. Return null if the input is null, or an empty string if
     * the input is empty.
     */
    public static String parenthesize(String s) {
        if (s == null) {
            return null;
        }
        if (s.isBlank()) {
            return "";
        }
        return "(%s)".formatted(s);
    }

    /** Return the given map as a formatted list of strings, formatted by the given formatter function. */
    public static <T, U> List<String> formatMap(Map<T, U> map, BiFunction<T, U, String> formatter) {
        return map.entrySet().stream().map(e -> formatter.apply(e.getKey(), e.getValue())).toList();
    }

    /**
     * Returns a collector which performs like {@link #join(String, Collection)}, but usable as a collector for a
     * stream.
     */
    public static <T> JoiningNonEmptyCollector<T> joiningNonEmpty(String delimiter) {
        return joiningNonEmpty(delimiter, null);
    }

    /**
     * Returns a collector which performs like {@link #join(String, Collection)}, but usable as a collector for a
     * stream, with an optional prefix. Examples:
     *
     * <pre>
     *     // "Label: one, two"
     *     Stream.of("one", "two").collect(joiningNonEmpty(", ", "Label: "))
     *     // ""
     *     Stream.of().collect(joiningNonEmpty(", ", "Label: "))
     * </pre>
     */
    public static <T> JoiningNonEmptyCollector<T> joiningNonEmpty(String delimiter, String prefix) {
        return new JoiningNonEmptyCollector<>(delimiter, null, true, prefix);
    }

    /**
     * {@link #joiningConjunct(String, String)} with a {@code ", "} delimiter.
     *
     * @see #joiningConjunct(String, String)
     */
    public static <T> JoiningNonEmptyCollector<T> joiningConjunct(String finalDelimiter) {
        return joiningConjunct(finalDelimiter, ", ");
    }

    /**
     * Returns a collector which performs like {@link #joinConjunct(String, String, List)}, but usable as a collector
     * for a stream.
     *
     * <p>
     * Example:
     * </p>
     *
     * <pre>
     * // "one, two, and three"
     * Stream.of("one", "two", "three").collect(joiningConjunct(" and ", ", "))
     * </pre>
     */
    public static <T> JoiningNonEmptyCollector<T> joiningConjunct(String finalDelimiter, String delimiter) {
        return new JoiningNonEmptyCollector<>(delimiter, finalDelimiter, true, null);
    }

    /**
     * Returns the given number as its textual English representation. Examples:
     *
     * <pre>
     *     numberAsWords(0) // -> "zero"
     *     numberAsWords(3) // -> "three"
     *     numberAsWords(77) // -> "seventy-seven"
     *     numberAsWords(101) // -> "101"
     * </pre>
     *
     * @param number The number to convert. For numbers greater than 100, just give the integer as a string.
     */
    public static String numberAsWords(int number) {
        int abs = Math.abs(number);
        return (number < 0 ? "negative " : "") + switch (abs) {
            case 0 -> "zero";
            case 1 -> "one";
            case 2 -> "two";
            case 3 -> "three";
            case 4 -> "four";
            case 5 -> "five";
            case 6 -> "six";
            case 7 -> "seven";
            case 8 -> "eight";
            case 9 -> "nine";
            case 10 -> "ten";
            case 11 -> "eleven";
            case 12 -> "twelve";
            case 13 -> "thirteen";
            case 14 -> "fourteen";
            case 15 -> "fifteen";
            case 16 -> "sixteen";
            case 17 -> "seventeen";
            case 18 -> "eighteen";
            case 19 -> "nineteen";
            case 20 -> "twenty";
            case 30 -> "thirty";
            case 40 -> "forty";
            case 50 -> "fifty";
            case 60 -> "sixty";
            case 70 -> "seventy";
            case 80 -> "eighty";
            case 90 -> "ninety";
            default -> {
                if (abs >= 100) {
                    yield abs + "";
                }
                int r = abs % 10;
                yield numberAsWords(abs - r) + "-" + numberAsWords(r);
            }
        };
    }

    /** Return the given {@code n} as an ordinal, e.g. 1st, 2nd, 3rd. */
    public static String toOrdinal(Integer n) {
        return n == null ? null : switch (n) {
            case 1 -> "1st";
            case 2 -> "2nd";
            case 3 -> "3rd";
            default -> n + "th";
        };
    }

    /** @see #toOrdinal(Integer) */
    public static String toOrdinal(String level) {
        try {
            return toOrdinal(Integer.parseInt(level));
        } catch (NumberFormatException e) {
            return level + "th";
        }
    }

    public static String toAnchorTag(String x) {
        return x.replace(" ", "%20")
                .replace(":", "")
                .replace(".", "")
                .replace('‑', '-');
    }

    // markdown link to href
    public static String markdownLinkToHtml(String x) {
        return x.replaceAll("\\[([^\\]]+)\\]\\(([^\\s)]+)(?:\\s\"[^\"]*\")?\\)",
                "<a href=\"$2\">$1</a>");

    }

    /**
     * A {@link java.util.stream.Collector} which converts the elements to strings, and joins the non-empty, non-null
     * strings into a single string. Allows providing an optional final delimiter that will be inserted before the
     * last element.
     *
     * @param delimiter The delimiter used to join the strings.
     * @param finalDelimiter The delimiter to use before the last element. Helpful for building strings like e.g.
     *        "one, two, and three".
     * @param oxford If false, then don't add a delimiter for the 'oxford comma' - e.g. replace the final delimiter
     *        with {@code finalDelimiter} rather than adding it.
     * @param prefix A prefix to add to the final result, if it's non-empty.
     */
    public record JoiningNonEmptyCollector<T>(
            String delimiter, String finalDelimiter, Boolean oxford,
            String prefix) implements Collector<T, List<String>, String> {
        @Override
        public Supplier<List<String>> supplier() {
            return ArrayList::new;
        }

        @Override
        public BiConsumer<List<String>, T> accumulator() {
            return (acc, cur) -> {
                if (cur != null && !cur.toString().isBlank()) {
                    acc.add(cur.toString());
                }
            };
        }

        @Override
        public BinaryOperator<List<String>> combiner() {
            return (left, right) -> {
                left.addAll(right);
                return left;
            };
        }

        @Override
        public Function<List<String>, String> finisher() {
            return acc -> {
                String joined = isPresent(finalDelimiter)
                        ? joinConjunct(acc, delimiter, finalDelimiter, !oxford)
                        : String.join(delimiter, acc);
                return isPresent(joined) && isPresent(prefix) ? prefix + joined : joined;
            };
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of();
        }
    }
}
