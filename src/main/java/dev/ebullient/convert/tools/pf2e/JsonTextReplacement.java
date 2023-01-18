package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.io.Tui;

public interface JsonTextReplacement {
    Pattern asPattern = Pattern.compile("\\{@as ([^}]+)}");
    Pattern dicePattern = Pattern.compile("\\{@(dice|damage) ([^|}]+)[^}]*}");
    Pattern chancePattern = Pattern.compile("\\{@chance ([^}]+)}");
    Pattern notePattern = Pattern.compile("\\{@note (\\*|Note:)?\\s?([^}]+)}");
    Pattern quickRefPattern = Pattern.compile("\\{@quickref ([^}]+)}");

    enum Activity {
        single("Single Action", "\\[>\\]", "single_action.svg"),
        two("Two-Action activity", "\\[>>\\]", "two_actions.svg"),
        three("Three-Action activity", "\\[>>>\\]", "three_actions.svg"),
        free("Free Action", "\\[F\\]", "delay.svg"),
        reaction("Reaction", "\\[R\\]", "reaction.svg"),
        varies("Varies", "\\[?\\]", "hour-glass.svg");

        String activity;
        String textGlyph;
        String glyph;

        Activity(String desc, String textGlyph, String glyph) {
            this.activity = desc;
            this.textGlyph = textGlyph;
            this.glyph = glyph;
        }

        public static Activity toActivity(String unit, int number) {
            switch (unit) {
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
            }
            throw new IllegalArgumentException("Unable to find Activity for " + number + " " + unit);
        }

        public String getText() {
            return this.activity;
        }

        public String getTextGlyph() {
            return this.textGlyph;
        }

        public String getGlyph() {
            return this.glyph;
        }
    }

    Pf2eIndex index();

    Pf2eSources getSources();

    default String slugify(String s) {
        return tui().slugify(s);
    }

    default Tui tui() {
        return cfg().tui();
    }

    default CompendiumConfig cfg() {
        return index().cfg();
    }

    default Stream<JsonNode> streamOf(ArrayNode array) {
        return StreamSupport.stream(array.spliterator(), false);
    }

    default boolean textContains(List<String> haystack, String needle) {
        return haystack.stream().anyMatch(x -> x.contains(needle));
    }

    default JsonNode copyNode(JsonNode sourceNode) {
        try {
            return Tui.MAPPER.readTree(sourceNode.toString());
        } catch (JsonProcessingException ex) {
            tui().errorf(ex, "Unable to copy %s", sourceNode.toString());
            throw new IllegalStateException("JsonProcessingException processing " + sourceNode);
        }
    }

    default JsonNode copyReplaceNode(JsonNode sourceNode, Pattern replace, String with) {
        try {
            String modified = replace.matcher(sourceNode.toString()).replaceAll(with);
            return Tui.MAPPER.readTree(modified);
        } catch (JsonProcessingException ex) {
            tui().errorf(ex, "Unable to copy %s", sourceNode.toString());
            throw new IllegalStateException("JsonProcessingException processing " + sourceNode);
        }
    }

    default List<String> findAndReplace(JsonNode jsonSource, String field) {
        return findAndReplace(jsonSource, field, s -> s);
    }

    default List<String> findAndReplace(JsonNode jsonSource, String field, Function<String, String> replacement) {
        JsonNode node = jsonSource.get(field);
        if (node == null || node.isNull()) {
            return List.of();
        } else if (node.isTextual()) {
            return List.of(replaceText(node.asText()));
        } else if (node.isObject()) {
            throw new IllegalArgumentException(
                    String.format("Unexpected object node (expected array): %s (referenced from %s)", node,
                            getSources()));
        }
        return streamOf(jsonSource.withArray(field))
                .map(x -> replaceText(x.asText()).trim())
                .map(replacement)
                .filter(x -> !x.isBlank())
                .collect(Collectors.toList());
    }

    default String joinAndReplace(JsonNode jsonSource, String field) {
        JsonNode node = jsonSource.get(field);
        if (node == null || node.isNull()) {
            return "";
        } else if (node.isTextual()) {
            return node.asText();
        } else if (node.isObject()) {
            throw new IllegalArgumentException(
                    String.format("Unexpected object node (expected array): %s (referenced from %s)", node,
                            getSources()));
        }
        return joinAndReplace((ArrayNode) node);
    }

    default String joinAndReplace(ArrayNode array) {
        List<String> list = new ArrayList<>();
        array.forEach(v -> list.add(replaceText(v.asText())));
        return String.join(", ", list);
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
                .replaceAll((match) -> match.group(2));
        result = chancePattern.matcher(result)
                .replaceAll((match) -> match.group(1) + "% chance");

        result = asPattern.matcher(result)
                .replaceAll(this::replaceAs);

        result = notePattern.matcher(result)
                .replaceAll((match) -> {
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

        try {
            result = result
                    .replace("{@hitYourSpellAttack}", "the summoner's spell attack modifier")
                    .replaceAll("\\{@link ([^}|]+)\\|([^}]+)}", "$1 ($2)") // this must come first
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
                    .replaceAll("\\{@table ([^|}]+)\\|?[^}]*}", "$1")
                    .replaceAll("\\{@variantrule ([^|}]+)\\|?[^}]*}", "$1")
                    .replaceAll("\\{@book ([^}|]+)\\|?[^}]*}", "\"$1\"")
                    .replaceAll("\\{@hit ([^}<]+)}", "+$1")
                    .replaceAll("\\{@h}", "Hit: ")
                    .replaceAll("\\{@b ([^}]+?)}", "**$1**")
                    .replaceAll("\\{@bold ([^}]+?)}", "**$1**")
                    .replaceAll("\\{@i ([^}]+?)}", "_$1_")
                    .replaceAll("\\{@italic ([^}]+)}", "_$1_");
        } catch (Exception e) {
            tui().errorf(e, "Unable to parse string from %s: %s", getSources().getKey(), input);
        }
        // TODO
        return result;
    }

    default String replaceAs(MatchResult match) {
        switch (match.group(1).toLowerCase()) {
            case "1":
            case "a":
                return "<s data-symbol=\"\\[>\\]\"></s>";
            case "2":
            case "d":
                return "<s data-symbol=\"\\[>>\\]\"></s>";
            case "3":
            case "t":
                return "<s data-symbol=\"\\[>>>\\]\"></s>";
            case "f":
                return "<s data-symbol=\"\\[F\\]\"></s>";
            case "r":
                return "<s data-symbol=\"\\[R\\]\"></s>";
            default:
                return "<s data-symbol=\"\\[?\\]\"></s>";
        }
    }

    default String linkifyRules(String text, String rules) {
        return String.format("[%s](%s%s.md#%s)",
                text, index().rulesRoot(), rules,
                text.replace(" ", "%20")
                        .replace(".", ""));
    }

    default String linkify(MatchResult match) {
        Pf2eIndexType targetType = Pf2eIndexType.fromTemplateName(match.group(1));
        return linkify(targetType, match.group(2));
    }

    default String linkify(Pf2eIndexType targetType, String match) {
        switch (targetType) {
            case skill:
                //	"Skill tags; {@skill Athletics}, {@skill Lore}, {@skill Perception} (case sensitive) provide tooltips on hover.",
                return linkifyRules(match, "skills");
            case classtype:
                return linkifyClass(match);
            case classFeature:
                return linkifyClassFeature(match);
            case subclassFeature:
                return linkifySubClassFeature(match);
        }

        // TODO not an _item_: "{@b Items with Runes:} {@runeItem longsword||+1 weapon potency||flaming|}, {@runeItem buugeng|LOAG|+3 weapon potency||optional display text}. In general, the syntax is this: (open curly brace)@runeItem base item|base item source|rune 1|rune 1 source|rune 2|rune 2 source|...|rune n|rune n source|display text(close curly brace). For each source, we assume CRB by default.",

        // "{@b Actions:} {@action strike} assumes CRB by default, {@action act together|som} can have sources added with a pipe, {@action devise a stratagem|apg|and optional link text added with another pipe}.",
        // "{@b Conditions:} {@condition stunned} assumes CRB by default, {@condition stunned|crb} can have sources added with a pipe (not that it's ever useful), {@condition stunned|crb|and optional link text added with another pipe}.",
        String[] parts = match.split("\\|");
        String linkText = parts[0];
        String source = targetType.defaultSource().name();
        if (parts.length > 2) {
            linkText = parts[2];
        }
        if (targetType.compendiumPath() == null) {
            return linkText;
        }
        if (parts.length > 1) {
            source = parts[1].isBlank() ? source : parts[1];
        }
        // TODO: aliases?
        String key = targetType.createKey(parts[0], source);
        // TODO: nested file structure for some types
        return index().isIncluded(key)
                ? String.format("[%s](%s%s/%s.md)", linkText,
                        index().compendiumRoot(), targetType.compendiumPath(), slugify(parts[0]))
                : linkText;
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
