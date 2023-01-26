package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.io.Tui;

public interface JsonTextReplacement {
    static AtomicBoolean readingFootnotes = new AtomicBoolean(); // only works because we're single threaded

    Pattern asPattern = Pattern.compile("\\{@as ([^}]+)}");
    Pattern dicePattern = Pattern.compile("\\{@(dice|damage) ([^}]+)}");
    Pattern chancePattern = Pattern.compile("\\{@chance ([^}]+)}");
    Pattern notePattern = Pattern.compile("\\{@note (\\*|Note:)?\\s?([^}]+)}");
    Pattern quickRefPattern = Pattern.compile("\\{@quickref ([^}]+)}");
    Pattern footnoteReference = Pattern.compile("\\{@sup ([^}]+)}");

    Pf2eIndex index();

    Pf2eSources getSources();

    default String slugify(String s) {
        return Tui.slugify(s);
    }

    default Tui tui() {
        return cfg().tui();
    }

    default CompendiumConfig cfg() {
        return index().cfg();
    }

    default String join(List<String> list, String joiner) {
        if (list == null || list.isEmpty()) {
            return "";
        }
        return String.join(joiner, list).trim();
    }

    default String joinConjunct(List<String> list, String lastJoiner) {
        return joinConjunct(list, ", ", " or ", false);
    }

    default String joinConjunct(List<String> list, String joiner, String lastJoiner, boolean nonOxford) {
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
    };

    default String nestedEmbed(List<String> content) {
        int embedDepth = content.stream()
                .filter(s -> s.matches("^`+$"))
                .map(s -> s.length())
                .max(Integer::compare).orElse(2);
        char[] ticks = new char[embedDepth + 1];
        Arrays.fill(ticks, '`');
        return new String(ticks);
    }

    default List<String> removePreamble(List<String> content) {
        int start = -1;
        for (int i = 0; i < content.size(); i++) {
            String line = content.get(i);
            if (line.startsWith("%%--")) {
                start = i;
                break;
            }
        }
        if (start >= 0) {
            for (int i = 0; i <= start; i++) {
                content.remove(0); // remove until start
            }
        }
        return content;
    }

    default String toTitleCase(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return Arrays
                .stream(text.split(" "))
                .map(word -> word.isEmpty()
                        ? word
                        : Character.toTitleCase(word.charAt(0)) + word
                                .substring(1)
                                .toLowerCase())
                .collect(Collectors.joining(" "));
    }

    /**
     * Remove/replace syntax within text
     *
     * @param input
     * @return
     */
    default String replaceText(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        String result = input;

        result = dicePattern.matcher(result)
                .replaceAll((match) -> {
                    String[] parts = match.group(2).split("\\|");
                    if (parts.length > 1) {
                        return parts[1];
                    }
                    return parts[0];
                });
        result = chancePattern.matcher(result)
                .replaceAll((match) -> match.group(1) + "% chance");

        result = asPattern.matcher(result)
                .replaceAll(this::replaceAs);

        result = footnoteReference.matcher(result)
                .replaceAll(this::replaceFootnoteReference);

        result = notePattern.matcher(result)
                .replaceAll((match) -> {
                    if (readingFootnotes.get()) {
                        return match.group(2);
                    }
                    List<String> text = new ArrayList<>();
                    text.add("> [!note]");
                    for (String line : match.group(2).split("\n")) {
                        text.add("> " + line);
                    }
                    return String.join("\n", text);
                });

        result = quickRefPattern.matcher(result)
                .replaceAll((match) -> {
                    String[] parts = match.group(1).split("\\|");
                    if (parts.length > 4) {
                        return parts[4];
                    }
                    return parts[0];
                });

        result = Pf2eIndexType.matchPattern.matcher(result)
                .replaceAll(this::linkify);

        // "Style tags; {@bold some text to be bolded} (alternative {@b shorthand}),
        // {@italic some text to be italicised} (alternative {@i shorthand}),
        // {@underline some text to be underlined} (alternative {@u shorthand}),
        // {@strike some text to strike-through}, (alternative {@s shorthand}),
        // {@color color|e40707} tags, {@handwriting handwritten text},
        // {@sup some superscript,} {@sub some subscript,}
        // {@center some centered text} {@c with alternative shorthand,}
        // {@i nostyle {@nostyle to escape font formatting, which can be used with other entry types} {@n (see below).}}
        // {@indentFirst You can use @indentFirst to indent the first line of text, all subsequent lines will not be indented. This is most often useful in tables, but it can be used anywhere.}
        // {@indentSubsequent @indentSubsequent is the counterpart to @indentFirst. You can use it to indent all lines after the first. This is most often useful in sidebars, but it can be used anywhere.}",

        try {
            result = result
                    .replace("{@hitYourSpellAttack}", "the summoner's spell attack modifier")
                    .replaceAll("\\{@link ([^}|]+)\\|([^}]+)}", "$1 ($2)") // this must come first
                    .replaceAll("\\{@pf2etools ([^}|]+)\\|?[^}]*}", "$1")
                    .replaceAll("\\{@reward ([^|}]+)\\|?[^}]*}", "$1")
                    .replaceAll("\\{@dc ([^}]+)}", "DC $1")
                    .replaceAll("\\{@flatDC ([^}]+)}", "$1")
                    .replaceAll("\\{@d20 ([^}]+?)}", "$1")
                    .replaceAll("\\{@recharge ([^}]+?)}", "(Recharge $1-6)")
                    .replaceAll("\\{@recharge}", "(Recharge 6)")
                    .replaceAll("\\{@(scaledice|scaledamage) [^|]+\\|[^|]+\\|([^|}]+)[^}]*}", "$2")
                    .replaceAll("\\{@filter ([^|}]+)\\|?[^}]*}", "$1")
                    .replaceAll("\\{@cult ([^|}]+)\\|([^|}]+)\\|[^|}]*}", "$2")
                    .replaceAll("\\{@cult ([^|}]+)\\|[^}]*}", "$1")
                    .replaceAll("\\{@language ([^|}]+)\\|?[^}]*}", "$1")
                    .replaceAll("\\{@variantrule ([^|}]+)\\|?[^}]*}", "$1")
                    .replaceAll("\\{@book ([^}|]+)\\|?[^}]*}", "\"$1\"")
                    .replaceAll("\\{@hit ([^}<]+)}", "+$1")
                    .replaceAll("\\{@h}", "Hit: ")
                    .replaceAll("\\{@s ([^}]+?)}", "$1")
                    .replaceAll("\\{@strike ([^}]+?)}", "$1")
                    .replaceAll("\\{@b ([^}]+?)}", "**$1**")
                    .replaceAll("\\{@bold ([^}]+?)}", "**$1**")
                    .replaceAll("\\{@i ([^}]+?)}", "_$1_")
                    .replaceAll("\\{@italic ([^}]+)}", "_$1_")
                    .replaceAll("\\{@indentFirst ([^}]+?)}", "$1")
                    .replaceAll("\\{@indentSubsequent ([^}]+?)}", "$1");
        } catch (Exception e) {
            tui().errorf(e, "Unable to parse string from %s: %s", getSources().getKey(), input);
        }

        // second pass (nested references)
        result = Pf2eIndexType.matchPattern.matcher(result)
                .replaceAll(this::linkify);

        // TODO
        return result;
    }

    default String replaceFootnoteReference(MatchResult match) {
        return String.format("[^%s]%s", match.group(1),
                readingFootnotes.get() ? ": " : "");
    }

    default String replaceAs(MatchResult match) {
        final Pf2eTypeActivity type;
        switch (match.group(1).toLowerCase()) {
            case "1":
            case "a":
                type = Pf2eTypeActivity.single;
                break;
            case "2":
            case "d":
                type = Pf2eTypeActivity.two;
                break;
            case "3":
            case "t":
                type = Pf2eTypeActivity.three;
                break;
            case "f":
                type = Pf2eTypeActivity.free;
                break;
            case "r":
                type = Pf2eTypeActivity.reaction;
                break;
            default:
                type = Pf2eTypeActivity.varies;
                break;
        }
        String link = type.linkify(index().rulesRoot());
        tui().debugf("AS LINK for %s (%s): %s", match.group(1), type, link);
        return link;
    }

    default String linkifyRules(String text, String rules, String anchor) {
        if (text.matches("\\[.+\\]\\(.+\\)")) {
            // skip if already a link
            return text;
        }
        String link = String.format("[%s](%s%s.md#%s)",
                text, index().rulesRoot(), rules,
                anchor.replace(" ", "%20")
                        .replace(".", ""));
        return link;
    }

    default String linkify(MatchResult match) {
        Pf2eIndexType targetType = Pf2eIndexType.fromText(match.group(1));
        if (targetType == null) {
            throw new IllegalStateException("Unknown type to linkify (how?)" + match.group(0));
        }
        return linkify(targetType, match.group(2));
    }

    default String linkify(Pf2eIndexType targetType, String match) {
        switch (targetType) {
            case skill:
                //	"Skill tags; {@skill Athletics}, {@skill Lore}, {@skill Perception}",
                // {@skill Lore||Farming Lore}
                String[] parts = match.split("\\|");
                String linkText = parts.length > 1 ? parts[2] : parts[0];
                return linkifyRules(linkText, "skills", toTitleCase(parts[0]));
            case classtype:
                return linkifyClass(match);
            case classFeature:
                return linkifyClassFeature(match);
            case subclassFeature:
                return linkifySubClassFeature(match);
            default:
                break;
        }

        // TODO {@runeItem longsword||+1 weapon potency||flaming|}, {@runeItem buugeng|LOAG|+3 weapon potency||optional display text}. In general, the syntax is this: (open curly brace)@runeItem base item|base item source|rune 1|rune 1 source|rune 2|rune 2 source|...|rune n|rune n source|display text(close curly brace). For each source, we assume CRB by default.",

        // "{@b Actions:} {@action strike} assumes CRB by default, {@action act together|som} can have sources added with a pipe, {@action devise a stratagem|apg|and optional link text added with another pipe}.",
        // "{@b Conditions:} {@condition stunned} assumes CRB by default, {@condition stunned|crb} can have sources added with a pipe (not that it's ever useful), {@condition stunned|crb|and optional link text added with another pipe}.",
        // "{@b Tables:} {@table ability modifiers} assumes CRB by default, {@table automatic bonus progression|gmg} can have sources added with a pipe, {@table domains|logm|and optional link text added with another pipe}.",
        String[] parts = match.split("\\|");
        String linkText = parts[0];
        String source = targetType.defaultSource().name();

        if (parts.length > 2) {
            linkText = parts[2];
        }
        if (linkText.matches("\\[.+\\]\\(.+\\)")) {
            // skip if already a link
            return linkText;
        }
        if (targetType == Pf2eIndexType.trait) {
            if (parts.length < 2 && linkText.contains("<")) {
                String[] pieces = parts[0].split(" ");
                parts[0] = pieces[0];
            } else if (parts[0].startsWith("[")) {
                // Do the same replacement we did when doing the initial import
                // [...] becomes "Any ..."
                parts[0].replaceAll("\\[(.*)\\]", "Any $1");
            }
        } else if (targetType == Pf2eIndexType.domain) {
            parts[0] = parts[0].replaceAll("\\s+\\(Apocryphal\\)", "");
        }

        if (parts.length > 1) {
            source = parts[1].isBlank() ? source : parts[1];
        }
        if (targetType == Pf2eIndexType.condition) {
            return linkifyRules(linkText.replaceAll("\\s\\d+$", ""),
                    "conditions", toTitleCase(parts[0].replaceAll("\\s\\d+$", "")));
        }

        if (targetType.relativePath() == null) {
            return linkText;
        }

        // TODO: aliases?
        String key = targetType.createKey(parts[0], source);
        // TODO: nested file structure for some types
        String link = String.format("[%s](%s%s/%s.md)", linkText,
                targetType.getRepoRoot(index()),
                targetType.relativePath(), slugify(parts[0]));

        if (targetType != Pf2eIndexType.action && targetType != Pf2eIndexType.spell && targetType != Pf2eIndexType.feat
                && targetType != Pf2eIndexType.trait) {
            tui().debugf("LINK for %s (%s): %s", match, index().isIncluded(key), link);
        }
        return index().isIncluded(key) ? link : linkText;
    }

    default String linkifyClass(String match) {
        // "{@b Classes:}
        // {@class alchemist} assumes CRB by default,
        // {@class investigator|apg} can have sources added with a pipe,
        // {@class summoner|som|optional link text added with another pipe},
        // {@class barbarian|crb|subclasses added|giant} with another pipe,
        // {@class barbarian|crb|and class feature added|giant|crb|2-2} with another pipe
        // (first number is level index (0-19), second number is feature index (0-n)), although this is prone to changes in the index, it's best to use the above method instead.",
        String[] parts = match.split("\\|");
        String className = parts[0];
        String classSource = String.valueOf(Pf2eIndexType.classtype.defaultSource());
        String linkText = className;
        String subclass = null;
        if (parts.length > 3) {
            subclass = parts[3];
        }
        if (parts.length > 2) {
            linkText = parts[2];
        }
        if (parts.length > 1) {
            classSource = parts[1];
        }

        tui().debugf("TODO CLASS found: %s", match);
        return linkText;
    }

    default String linkifyClassFeature(String match) {
        // "{@b Class Features:}
        // {@classFeature rage|barbarian||1},
        // {@classFeature precise strike|swashbuckler|apg|1},
        // {@classFeature arcane spellcasting|magus|som|1|som},
        // {@classFeature rage|barbarian||1||optional display text}.
        // Class source is assumed to be CRB. Class feature source is assumed to be the same as class source.",
        tui().debugf("TODO CLASS FEATURE found: %s", match);
        return match;
    }

    default String linkifySubClassFeature(String match) {
        // "{@b Subclass Features:}
        // {@subclassFeature research field|alchemist||bomber||1},
        // {@subclassFeature methodology|investigator|apg|empiricism|apg|1},
        // {@subclassFeature methodology|investigator|apg|empiricism|apg|1||and optional display text} Class and Class feature source is assumed to be CRB.",
        tui().debugf("TODO CLASS FEATURE found: %s", match);
        return match;
    }
}
