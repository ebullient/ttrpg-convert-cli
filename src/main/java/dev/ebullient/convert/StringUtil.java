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
     * {@link #joinWithPrefix(String, Collection, String, String)} with an empty suffix.
     *
     * @see #joinWithPrefix(String, Collection, String)
     */
    public static String joinWithPrefix(String joiner, Collection<?> list, String prefix) {
        return joinWithPrefix(joiner, list, prefix, null);
    }

    /**
     * Like {@link #join(String, Collection)} but add a prefix (and optionally a suffix) to the resulting string if
     * it's non-empty.
     */
    public static String joinWithPrefix(String joiner, Collection<?> list, String prefix, String suffix) {
        String s = join(joiner, list);
        if (s.isEmpty()) {
            return "";
        }
        if (isPresent(prefix)) {
            s = prefix + s;
        }
        if (isPresent(suffix)) {
            s += suffix;
        }
        return s;
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
    public static String pluralize(String s, int howMany, boolean assumeSingular) {
        if (s == null) {
            return null;
        }
        if (s.isEmpty()) {
            return "";
        }
        if (s.endsWith("s")) {
            if (assumeSingular) {
                return s + "es";
            }
            s = s.substring(0, s.length() - 2);
        }
        return switch (s) {
            case "foot", "feet" -> howMany == 1 ? "foot" : "feet";
            default -> howMany == 1 ? s : s + "s";
        };
    }

    /**
     * {@link #pluralize(String, int, boolean)} with {@code assumeSingular} set to {@code false}
     *
     * @see #pluralize(String, int, boolean)
     */
    public static String pluralize(String s, int howMany) {
        return pluralize(s, howMany, false);
    }

    /** @see #pluralize(String, int) */
    public static String pluralize(String s, String howMany) {
        return pluralize(s, Integer.parseInt(howMany));
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
        return new JoiningNonEmptyCollector<>(delimiter, null, true);
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
     */
    public static <T> JoiningNonEmptyCollector<T> joiningConjunct(String finalDelimiter, String delimiter) {
        return new JoiningNonEmptyCollector<>(delimiter, finalDelimiter, true);
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
     */
    public record JoiningNonEmptyCollector<T>(
            String delimiter, String finalDelimiter, Boolean oxford) implements Collector<T, List<String>, String> {
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
                if (isPresent(finalDelimiter)) {
                    return joinConjunct(acc, delimiter, finalDelimiter, !oxford);
                }
                return String.join(delimiter, acc);
            };
        }

        @Override
        public Set<Characteristics> characteristics() {
            return Set.of();
        }
    }
}
