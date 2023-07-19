package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.JsonTextConverter;

public interface JsonTextReplacement extends JsonTextConverter<Pf2eIndexType> {

    enum Field implements JsonNodeReader {
        alias,
        auto,
        by,
        categories, // trait categories for indexing
        customUnit,
        data, // embedded data
        footnotes,
        frequency,
        group,
        head,
        interval,
        number,
        overcharge,
        range, // level effect
        recurs,
        reference,
        requirements,
        signature,
        special,
        style,
        tag, // embedded data
        title,
        traits,
        unit,
        add_hash
    }

    Pattern asPattern = Pattern.compile("\\{@as ([^}]+)}");
    Pattern runeItemPattern = Pattern.compile("\\{@runeItem ([^}]+)}");
    Pattern dicePattern = Pattern.compile("\\{@(dice|damage) ([^}]+)}");
    Pattern chancePattern = Pattern.compile("\\{@chance ([^}]+)}");
    Pattern notePattern = Pattern.compile("\\{@note (\\*|Note:)?\\s?([^}]+)}");
    Pattern quickRefPattern = Pattern.compile("\\{@quickref ([^}]+)}");
    Pattern footnoteReference = Pattern.compile("\\{@sup ([^}]+)}");

    Pf2eIndex index();

    Pf2eSources getSources();

    default Tui tui() {
        return cfg().tui();
    }

    default CompendiumConfig cfg() {
        return index().cfg();
    }

    default String replaceText(JsonNode input) {
        if (input == null) {
            return null;
        }
        if (input.isObject() || input.isArray()) {
            throw new IllegalArgumentException("Can only replace text for textual nodes: " + input);
        }
        return replaceText(input.asText());
    }

    default String replaceText(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        try {
            String result = input
                    .replace("#$prompt_number:title=Enter Alert Level$#", "Alert Level")
                    .replace("#$prompt_number:title=Enter Charisma Modifier$#", "Charisma modifier")
                    .replace("#$prompt_number:title=Enter Lifestyle Modifier$#", "Charisma modifier")
                    .replace("#$prompt_number:title=Enter a Modifier$#", "Modifier")
                    .replace("#$prompt_number:title=Enter a Modifier,default=10$#", "Modifier (default 10)")
                    .replaceAll("#\\$prompt_number.*default=(.*)\\$#", "$1");

            // TODO: review against Pf2e formatting patterns
            if (cfg().alwaysUseDiceRoller()) {
                result = result
                        .replaceAll("\\{@h}([ \\d]+) \\(\\{@damage (" + DICE_FORMULA + ")}\\)",
                                "Hit: `dice: $2|avg` (`$2`)")
                        .replaceAll("plus ([\\d]+) \\(\\{@damage (" + DICE_FORMULA + ")}\\)",
                                "plus `dice: $2|avg` (`$2`)")
                        .replaceAll("(takes?) [\\d]+ \\(\\{@damage (" + DICE_FORMULA + ")}\\)",
                                "$1 `dice: $2|avg` (`$2`)")
                        .replaceAll("(takes?) [\\d]+ \\(\\{@dice (" + DICE_FORMULA + ")}\\)",
                                "$1 `dice: $2|avg` (`$2`)")
                        .replaceAll("\\{@hit (\\d+)} to hit", "`dice: d20+$1` (+$1 to hit)")
                        .replaceAll("\\{@hit (-\\d+)} to hit", "`dice: d20-$1` (-$1 to hit)")
                        .replaceAll("\\{@hit (\\d+)}", "`dice: d20+$1` (+$1)")
                        .replaceAll("\\{@hit (-\\d+)}", "`dice: d20-$1` (-$1)")
                        .replaceAll("\\{@d20 (\\d+?)}", "`dice: d20+$1` (+$1)")
                        .replaceAll("\\{@d20 (-\\d+?)}", "`dice: d20-$1` (-$1)");
            }

            result = dicePattern.matcher(result)
                    .replaceAll((match) -> {
                        String[] parts = match.group(2).split("\\|");
                        if (parts.length > 1) {
                            return parts[1];
                        }
                        return formatDice(parts[0]);
                    });

            result = chancePattern.matcher(result)
                    .replaceAll((match) -> match.group(1) + "% chance");

            result = asPattern.matcher(result)
                    .replaceAll(this::replaceActionAs);

            result = footnoteReference.matcher(result)
                    .replaceAll(this::replaceFootnoteReference);

            result = quickRefPattern.matcher(result)
                    .replaceAll((match) -> {
                        String[] parts = match.group(1).split("\\|");
                        if (parts.length > 4) {
                            return parts[4];
                        }
                        return parts[0];
                    });

            result = runeItemPattern.matcher(result)
                    .replaceAll(this::linkifyRuneItem);

            result = Pf2eIndexType.matchPattern.matcher(result)
                    .replaceAll(this::linkify);

            // "Style tags; {@bold some text to be bolded} (alternative {@b shorthand}),
            // {@italic some text to be italicised} (alternative {@i shorthand}),
            // {@underline some text to be underlined} (alternative {@u shorthand}),
            // {@strike some text to strike-through}, (alternative {@s shorthand}),
            // {@color color|e40707} tags, {@handwriting handwritten text},
            // {@sup some superscript,} {@sub some subscript,}
            // {@center some centered text} {@c with alternative shorthand,}
            // {@nostyle to escape font formatting} {@n (see below).}}
            // {@indentFirst You can use @indentFirst to indent the first line of text}
            // {@indentSubsequent is the counterpart to @indentFirst. }",

            try {
                result = result
                        .replace("{@hitYourSpellAttack}", "the summoner's spell attack modifier")
                        .replaceAll("\\{@link ([^}|]+)\\|([^}]+)}", "$1 ($2)") // this must come first
                        .replaceAll("\\{@pf2etools ([^}|]+)\\|?[^}]*}", "$1")
                        .replaceAll("\\{@Pf2eTools ([^}|]+)\\|?[^}]*}", "$1")
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
                        .replaceAll("\\{@c ([^}]+?)}", "$1")
                        .replaceAll("\\{@center ([^}]+?)}", "$1")
                        .replaceAll("\\{@s ([^}]+?)}", "$1")
                        .replaceAll("\\{@strike ([^}]+?)}", "$1")
                        .replaceAll("\\{@n ([^}]+?)}", "$1")
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

            // note pattern often wraps others. Do this one last.
            result = notePattern.matcher(result)
                    .replaceAll((match) -> {
                        if (parseState.inFootnotes()) {
                            return match.group(2);
                        }
                        List<String> text = new ArrayList<>();
                        text.add("> [!pf2-note]");
                        for (String line : match.group(2).split("\n")) {
                            text.add("> " + line);
                        }
                        return String.join("\n", text);
                    });
            return result;
        } catch (IllegalArgumentException e) {
            tui().errorf(e, "Failure replacing text: %s", e.getMessage());
        }
        return input;
    }

    default String replaceFootnoteReference(MatchResult match) {
        return String.format("[^%s]%s", match.group(1),
                parseState.inFootnotes() ? ": " : "");
    }

    default String replaceActionAs(MatchResult match) {
        final Pf2eActivity type;
        switch (match.group(1).toLowerCase()) {
            case "1":
            case "a":
                type = Pf2eActivity.single;
                break;
            case "2":
            case "d":
                type = Pf2eActivity.two;
                break;
            case "3":
            case "t":
                type = Pf2eActivity.three;
                break;
            case "f":
                type = Pf2eActivity.free;
                break;
            case "r":
                type = Pf2eActivity.reaction;
                break;
            default:
                type = Pf2eActivity.varies;
                break;
        }
        return type.linkify(index().rulesVaultRoot());
    }

    default String linkifyRuneItem(MatchResult match) {
        String[] parts = match.group(1).split("\\|");
        String linkText = parts[0];
        // TODO {@runeItem longsword||+1 weapon potency||flaming|},
        // {@runeItem buugeng|LOAG|+3 weapon potency||optional display text}.
        // In general, the syntax is this:
        // (open curly brace)@runeItem base item|base item source|rune 1|rune 1
        // source|rune 2|rune 2 source|...|rune n|rune n source|display text(close curly
        // brace).
        // For each source, we assume CRB by default.",

        tui().debugf("TODO RuneItem found: %s", match);
        return linkText;
    }

    default String linkify(MatchResult match) {
        Pf2eIndexType targetType = Pf2eIndexType.fromText(match.group(1));
        if (targetType == null) {
            throw new IllegalStateException("Unknown type to linkify (how?)" + match.group(0));
        }
        return linkify(targetType, match.group(2));
    }

    default String linkify(Pf2eIndexType targetType, String match) {
        if (match == null || match.isEmpty()) {
            return match;
        }
        switch (targetType) {
            case skill:
                // "Skill tags; {@skill Athletics}, {@skill Lore}, {@skill Perception}",
                // {@skill Lore||Farming Lore}
                String[] parts = match.split("\\|");
                String linkText = parts.length > 1 ? parts[2] : parts[0];
                return linkifyRules(Pf2eIndexType.skill, linkText, "skills", toTitleCase(parts[0]));
            case classtype:
                return linkifyClass(match);
            case classFeature:
                return linkifyClassFeature(match);
            case subclassFeature:
                return linkifySubClassFeature(match);
            case trait:
                return linkifyTrait(match);
            default:
                break;
        }

        // {@action strike}
        // {@action act together|som} can have sources added with a pipe,
        // {@action devise a stratagem|apg|and optional link text added with another pipe}.",
        // {@condition stunned} assumes CRB by default,
        // {@condition stunned|crb} can have sources added with a pipe (not that it's ever useful),
        // {@condition stunned|crb|and optional link text added with another pipe}
        // {@table ability modifiers} assumes CRB by default,
        // {@table automatic bonus progression|gmg} can have sources added with a pipe,
        // {@table domains|logm|and optional link text added with another pipe}.",
        String[] parts = match.split("\\|");
        String linkText = parts.length > 2 ? parts[2] : parts[0];
        String source = targetType.defaultSourceString();

        if (linkText.matches("\\[.+]\\(.+\\)")) {
            // skip if already a link
            return linkText;
        }
        if (targetType == Pf2eIndexType.domain) {
            parts[0] = parts[0].replaceAll("\\s+\\([Aa]pocryphal\\)", "");
            return linkifyRules(Pf2eIndexType.domain, linkText, "domains", toTitleCase(parts[0]));
        } else if (targetType == Pf2eIndexType.condition) {
            return linkifyRules(Pf2eIndexType.condition, linkText.replaceAll("\\s\\d+$", ""),
                    "conditions", toTitleCase(parts[0].replaceAll("\\s\\d+$", "")));
        }

        if (parts.length > 1) {
            source = parts[1].isBlank() ? source : parts[1];
        }

        if (targetType == Pf2eIndexType.spell) {
            parts[0] = parts[0].replaceAll("\\s+\\((.*)\\)$", "-$1");
        }

        // TODO: aliases?
        String key = targetType.createKey(parts[0], source);

        // TODO: nested file structure for some types
        String link = String.format("[%s](%s/%s%s.md)",
                linkText,
                targetType.relativeRepositoryRoot(index()),
                slugify(parts[0]),
                targetType.isDefaultSource(source) ? "" : "-" + slugify(source));

        // if (targetType != Pf2eIndexType.action
        // && targetType != Pf2eIndexType.spell
        // && targetType != Pf2eIndexType.feat
        // && targetType != Pf2eIndexType.trait) {
        // tui().debugf("LINK for %s (%s): %s", match, index().isIncluded(key), link);
        // }
        return index().isIncluded(key) ? link : linkText;
    }

    default String linkifyTrait(String match) {
        // {@trait fire} does not require sources for official sources,
        // {@trait brutal|b2} can have sources added with a pipe in case of homebrew or duplicate trait names,
        // {@trait agile||and optional link text added with another pipe}.",

        String[] parts = match.split("\\|");
        String linkText = parts.length > 2 ? parts[2] : parts[0];

        if (parts.length < 2 && linkText.contains("<")) {
            String[] pieces = parts[0].split(" ");
            parts[0] = pieces[0];
        } else if (parts[0].startsWith("[")) {
            // Do the same replacement we did when doing the initial import
            // [...] becomes "Any ..."
            parts[0] = parts[0].replaceAll("\\[(.*)]", "Any $1");
        } else if (parts[0].length() <= 2) {
            Pf2eTypeReader.Pf2eAlignmentValue alignment = Pf2eTypeReader.Pf2eAlignmentValue.fromString(parts[0]);
            parts[0] = alignment == null ? parts[0] : alignment.longName;
        }

        String source = parts.length > 1 ? parts[1] : index().traitToSource(parts[0]);
        String key = Pf2eIndexType.trait.createKey(parts[0], source);
        JsonNode traitNode = index().getIncludedNode(key);
        return linkifyTrait(traitNode, linkText);
    }

    default String linkifyTrait(JsonNode traitNode, String linkText) {
        if (traitNode != null) {
            String source = SourceField.source.getTextOrEmpty(traitNode);
            List<String> categories = Field.categories.getListOfStrings(traitNode, tui())
                    .stream()
                    .filter(x -> !"_alignAbv".equals(x))
                    .toList();

            String title;
            if (categories.contains("Alignment")) {
                title = "Alignment";
            } else if (categories.contains("Rarity")) {
                title = "Rarity";
            } else if (categories.contains("Size")) {
                title = "Size";
            } else {
                title = categories.stream().sorted().findFirst().orElse("");
            }
            title = (SourceField.name.getTextOrEmpty(traitNode) + " " + title + " Trait").trim();

            return String.format("[%s](%s/%s%s.md \"%s\")",
                    linkText,
                    Pf2eIndexType.trait.relativeRepositoryRoot(index()),
                    slugify(linkText),
                    Pf2eIndexType.trait.isDefaultSource(source) ? "" : "-" + slugify(source),
                    title.trim());
        }
        return linkText;
    }

    default String linkifyRules(Pf2eIndexType type, String text, String rules, String anchor) {
        if (text.matches("\\[.+]\\(.+\\)")) {
            // skip if already a link
            return text;
        }
        return String.format("[%s](%s/%s.md#%s)",
                text,
                type.relativeRepositoryRoot(index()),
                rules,
                toAnchorTag(anchor));
    }

    default String linkifyClass(String match) {
        // "{@b Classes:}
        // {@class alchemist} assumes CRB by default,
        // {@class investigator|apg} can have sources added with a pipe,
        // {@class summoner|som|optional link text added with another pipe},
        // {@class barbarian|crb|subclasses added|giant} with another pipe,
        // {@class barbarian|crb|and class feature added|giant|crb|2-2} with another
        // pipe
        // (first number is level index (0-19), second number is feature index (0-n)),
        // although this is prone to changes in the index, it's best to use the above
        // method instead.",
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
        // Class source is assumed to be CRB. Class feature source is assumed to be the
        // same as class source.",
        tui().debugf("TODO CLASS FEATURE found: %s", match);
        return match;
    }

    default String linkifySubClassFeature(String match) {
        // "{@b Subclass Features:}
        // {@subclassFeature research field|alchemist||bomber||1},
        // {@subclassFeature methodology|investigator|apg|empiricism|apg|1},
        // {@subclassFeature methodology|investigator|apg|empiricism|apg|1||and optional
        // display text} Class and Class feature source is assumed to be CRB.",
        tui().debugf("TODO CLASS FEATURE found: %s", match);
        return match;
    }
}
