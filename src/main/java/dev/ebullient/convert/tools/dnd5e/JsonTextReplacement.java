package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.isPresent;
import static dev.ebullient.convert.StringUtil.toAnchorTag;
import static dev.ebullient.convert.StringUtil.valueOrDefault;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.CompendiumConfig.DiceRoller;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.Msg;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.JsonTextConverter;
import dev.ebullient.convert.tools.dnd5e.JsonSource.Tools5eFields;
import dev.ebullient.convert.tools.dnd5e.OptionalFeatureIndex.OptionalFeatureType;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndexType.IndexFields;
import dev.ebullient.convert.tools.dnd5e.qute.AbilityScores;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public interface JsonTextReplacement extends JsonTextConverter<Tools5eIndexType> {
    static final Pattern FRACTIONAL = Pattern.compile("^(\\d+)?([⅛¼⅜½⅝¾⅞⅓⅔⅙⅚])?$");
    static final Pattern linkifyPattern = Pattern.compile("\\{@("
            + "|action|background|card|class|condition|creature|deck|deity|disease|facility"
            + "|feat|hazard|item|itemMastery|itemProperty|itemType|legroup|object|psionic|race|reward"
            + "|sense|skill|spell|status|subclass|table|variantrule|vehicle"
            + "|optfeature|classFeature|subclassFeature|trap) ([^}]+)}");
    static final Pattern chancePattern = Pattern.compile("\\{@chance ([^}]+)}");
    static final Pattern fontPattern = Pattern.compile("\\{@font ([^}]+)}");
    static final Pattern homebrewPattern = Pattern.compile("\\{@homebrew ([^}]+)}");
    static final Pattern linkTo5eImgRepo = Pattern.compile("\\{@5etoolsImg ([^}]+)}");
    static final Pattern quickRefPattern = Pattern.compile("\\{@quickref ([^}]+)}");
    static final Pattern notePattern = Pattern.compile("\\{@(note|tip) ([^}]+)}");
    static final Pattern footnotePattern = Pattern.compile("\\{@footnote ([^}]+)}");
    static final Pattern abilitySavePattern = Pattern.compile("\\{@(ability|savingThrow) ([^}]+)}"); // {@ability str
                                                                                                     // 20}
    static final Pattern savingThrowPattern = Pattern.compile("\\{@actSave ([^}]+)}");
    static final Pattern attackPattern = Pattern.compile("\\{@atkr? ([^}]+)}");
    static final Pattern skillCheckPattern = Pattern.compile("\\{@skillCheck ([^}]+)}"); // {@skillCheck animal_handling
                                                                                         // 5}
    static final Pattern optionalFeaturesFilter = Pattern.compile("\\{@filter ([^|}]+)\\|optionalfeatures\\|([^}]*)}");
    static final Pattern featureTypePattern = Pattern.compile("(?:[Ff]eature )?[Tt]ype=([^|}]+)");
    static final Pattern featureSourcePattern = Pattern.compile("source=([^|}]+)");
    static final Pattern superscriptCitationPattern = Pattern.compile("\\{@(sup|cite) ([^}]+)}");
    static final Pattern promptPattern = Pattern.compile("#\\$prompt_number(?::(.*?))?\\$#");
    static final String subclassFeatureMask = "subclassfeature\\|(.*)\\|.*?\\|.*?\\|.*?\\|.*?\\|(\\d+)\\|.*";

    static final Set<String> missingKeys = new HashSet<>();

    Tools5eIndex index();

    Tools5eSources getSources();

    default Tui tui() {
        return cfg().tui();
    }

    default CompendiumConfig cfg() {
        return index().cfg();
    }

    default boolean useCompendium() {
        return getSources().getType().useCompendiumBase();
    }

    default String getImagePath() {
        Tools5eIndexType type = getSources().getType();
        return type.getRelativePath();
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
                    "Unexpected object node (expected array): %s (referenced from %s)".formatted(
                            node,
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
                    "Unexpected object node (expected array): %s (referenced from %s)".formatted(
                            node,
                            getSources()));
        }
        return joinAndReplace((ArrayNode) node);
    }

    default String joinAndReplace(ArrayNode array) {
        List<String> list = new ArrayList<>();
        array.forEach(v -> list.add(replaceText(v.asText())));
        return String.join(", ", list);
    }

    default String tableHeader(String x) {
        if (x.contains("dice")) {
            // don't do the usual dice formatting in a column header
            x = replacePromptStrings(x);
            if (x.endsWith("Card}")) {
                x = x.replaceAll("\\{@dice ([^}|]+)\\|?([^}]*)}", "$1 | $2");
            } else {
                x = x.replaceAll("\\{@dice ([^}|]+)\\|?[^}]*}", "$1");
            }
            x = replaceText(x);
        } else {
            x = replaceText(x);
        }
        if (x.matches("^\\d*d\\d+( \\|.*)?$")) {
            return "dice: " + x;
        }
        return x;
    }

    default String replacePromptStrings(String s) {
        return promptPattern.matcher(s).replaceAll((match) -> {
            List<String> prompts = new ArrayList<>();
            String title = null;
            String[] parts = match.group(1).split(",");
            for (String t : parts) {
                if (t.startsWith("title=")) {
                    title = t.substring(6)
                            .replaceAll("^Enter ?(a|your|the)? ", "")
                            .replace("!", "")
                            .trim();
                } else {
                    prompts.add(t);
                }
            }
            if (title == null) {
                for (String x : prompts) {
                    if (x.startsWith("default=")) {
                        title = x.substring(8);
                        prompts.remove(x);
                        break;
                    }
                }
            }
            prompts.sort(String::compareToIgnoreCase);
            return "<span%s>[%s]</span>".formatted(
                    prompts.isEmpty() ? "" : " title='" + String.join(", ", prompts) + "'",
                    title);
        });
    }

    @Override
    default String replaceTokenText(String input, boolean nested) {
        String result = input;

        // render.js this._renderString_renderTag
        try {
            result = replacePromptStrings(result);

            // {@dice .. }, {@damage ..}{@hit ..}, {@d20 ..}, {@initiative ...},
            // {@scaledice..}, {@scaledamage..}
            result = replaceWithDiceRoller(result);

            result = chancePattern.matcher(result).replaceAll((match) -> {
                // "Chance tags; similar to dice roller tags, but output success/failure.
                // {@chance 50}; {@chance 50|display text}; {@chance 50|display text|rolled by
                // name};
                // {@chance 50|display text|rolled by name|on success text};
                // {@chance 50|display text|rolled by name|on success text|on failure text}.",
                String[] parts = match.group(1).split("\\|");
                return parts.length > 1
                        ? parts[1]
                        : parts[0] + " percent";
            });

            result = abilitySavePattern.matcher(result).replaceAll(this::replaceSkillOrAbility);
            result = skillCheckPattern.matcher(result).replaceAll(this::replaceSkillCheck);
            result = savingThrowPattern.matcher(result).replaceAll(this::replaceSavingThrow);

            result = superscriptCitationPattern.matcher(result).replaceAll((match) -> {
                // {@sup {@cite Casting Times|FleeMortals|A}}
                // {@sup whatever}
                // {@cite Casting Times|FleeMortals|A}
                // {@cite Casting Times|FleeMortals|{@sup A}}
                if (match.group(1).equals("sup")) {
                    String text = replaceText(match.group(2));
                    if (text.startsWith("[^") || text.startsWith("^[")) {
                        // do not put citations in superscript (obsidian/markdown will do it)
                        return text;
                    }
                    return "<sup>" + text + "</sup>";
                }
                return handleCitation(match.group(2));
            });

            result = homebrewPattern.matcher(result).replaceAll((match) -> {
                // {@homebrew changes|modifications}, {@homebrew additions} or {@homebrew
                // |removals}
                String s = match.group(1);
                int pos = s.indexOf('|');
                if (pos == 0) { // removal
                    return "[...] ^[The following text has been removed with this homebrew: " + s.substring(1) + "]";
                } else if (pos < 0) { // addition
                    return s + " ^[This is a homebrew addition]";
                }
                String oldText = s.substring(0, pos);
                String newText = s.substring(pos + 1);

                return newText + " ^[This is a homebrew addition, replacing the following: " + oldText + "]";
            });

            result = linkTo5eImgRepo.matcher(result).replaceAll((match) -> {
                // External links to materials in the 5eTools image repo (usually pdf):
                // {@5etoolsImg Players Handbook Cover|covers/PHB.webp}
                // const fauxEntry = {
                //     type: "link",
                //     href: {
                //         type: "external",
                //         url: UrlUtil.link(this.getMediaUrl("img", page)),
                //     },
                //     text: displayText,
                // };
                String orig = match.group(0);
                if (!orig.contains("|")) {
                    return orig;
                }

                String[] parts = match.group(1).split("\\|");
                String imgRepo = TtrpgConfig.getConstant(TtrpgConfig.DEFAULT_IMG_ROOT);
                String url = ImageRef.Builder.fixUrl(imgRepo + (imgRepo.endsWith("/") ? "" : "/") + parts[1]);

                return "[%s](%s)".formatted(parts[0], url);
            });

            result = linkifyPattern.matcher(result)
                    .replaceAll(this::linkify);

            result = optionalFeaturesFilter.matcher(result)
                    .replaceAll(this::linkifyOptionalFeatureType);

            result = quickRefPattern.matcher(result).replaceAll((match) -> {
                String[] parts = match.group(1).split("\\|");
                if (parts.length > 4) {
                    return parts[4];
                }
                return parts[0];
            });

            result = fontPattern.matcher(result).replaceAll((match) -> {
                String[] parts = match.group(1).split("\\|");
                String fontFamily = Tools5eSources.getFontReference(parts[1]);
                if (fontFamily != null) {
                    return "<span style=\"font-family: %s\">%s</span>".formatted(
                            fontFamily, parts[0]);
                }
                return parts[0];
            });

            result = attackPattern.matcher(result).replaceAll((match) -> {
                List<String> type = new ArrayList<>();
                String method = "";
                // render.js Renderer.attackTagToFull
                // const ptType = tags.includes("m") ? "Melee " : tags.includes("r") ? "Ranged "
                // : tags.includes("g") ? "Magical " : tags.includes("a") ? "Area " : "";
                // const ptMethod = tags.includes("w") ? "Weapon " : tags.includes("s") ? "Spell
                // " : tags.includes("p") ? "Power " : "";
                if (match.group(1).contains("m")) {
                    type.add("Melee ");
                }
                if (match.group(1).contains("r")) {
                    type.add("Ranged ");
                }
                if (match.group(1).contains("g")) {
                    type.add("Magical ");
                }
                if (match.group(1).contains("a")) {
                    type.add("Area ");
                }

                if (match.group(1).contains("w")) {
                    method = "Weapon ";
                } else if (match.group(1).contains("s")) {
                    method = "Spell ";
                } else if (match.group(1).contains("p")) {
                    method = "Power ";
                }
                return "*%s%sAttack:*".formatted(String.join("or ", type), method);
            });

            try {
                result = result
                        .replaceAll("\\{@hitYourSpellAttack ([^}]+)}", "$1")
                        .replaceAll("\\{@hitYourSpellAttack}", "the summoner's spell attack modifier")
                        // "Internal links: {@5etools This Is Your Life|lifegen.html}",
                        // "External links: {@link https://discord.gg/5etools} or {@link
                        // Discord|https://discord.gg/5etools}"
                        .replaceAll("\\{@link ([^}|]+)\\|([^}]+)}", "$1 ($2)") // this must come first
                        .replaceAll("\\{@link ([^}|]+)}", "$1") // this must come first
                        .replaceAll("\\{@5etools ([^}|]+)\\|?[^}]*}", "$1")
                        .replaceAll("\\{@loader ([^}|]+)\\|([^}]+)}", "$1 ^[$2]")
                        .replaceAll("\\{@area ([^|}]+)\\|?[^}]*}", "$1")
                        .replaceAll("\\{@vehupgrade ([^|}]+)\\|?[^}]*}", "$1") // TODO: vehicle upgrade type
                        .replaceAll("\\{@dc ([^}]+)}", "DC $1")
                        .replaceAll("\\{@recharge ([^}]+?)}", "(Recharge $1-6)")
                        .replaceAll("\\{@recharge}", "(Recharge 6)")
                        .replaceAll("\\{@coinflip ([^|}]+)\\|?[^}]*}", "$1")
                        .replaceAll("\\{@coinflip}", "flip a coin")
                        .replaceAll("\\{@filter ([^|}]+)\\|?[^}]*}", "$1")
                        .replaceAll("\\{@boon ([^|}]+)\\|([^|}]+)\\|[^|}]*}", "$2")
                        .replaceAll("\\{@boon ([^|}]+)\\|[^}]*}", "$1")
                        .replaceAll("\\{@boon ([^|}]+)}", "$1")
                        .replaceAll("\\{@charoption ([^|}]+)\\|([^|}]+)\\|[^|}]*}", "$2")
                        .replaceAll("\\{@charoption ([^|}]+)\\|[^}]*}", "$1")
                        .replaceAll("\\{@charoption ([^|}]+)}", "$1")
                        .replaceAll("\\{@recipe ([^|}]+)\\|([^|}]+)\\|[^|}]*}", "$2")
                        .replaceAll("\\{@recipe ([^|}]+)\\|[^}]*}", "$1")
                        .replaceAll("\\{@recipe ([^|}]+)}", "$1")
                        .replaceAll("\\{@cult ([^|}]+)\\|([^|}]+)\\|[^|}]*}", "$2")
                        .replaceAll("\\{@cult ([^|}]+)\\|[^}]*}", "$1")
                        .replaceAll("\\{@cult ([^|}]+)}", "$1")
                        .replaceAll("\\{@language ([^|}]+)\\|?[^}]*}", "$1")
                        .replaceAll("\\{@book ([^}|]+)\\|?[^}]*}", "\"$1\"")
                        .replaceAll("\\{@h}", "*Hit:* ") // render.js Renderer.tag
                        .replaceAll("\\{@m}", "*Miss:* ")
                        .replaceAll("\\{@hom}", "*Hit or Miss:* ")// render.js Renderer.tag
                        .replaceAll("\\{@actSaveFail}", "*Failure:*") // render.js Renderer.tag
                        .replaceAll("\\{@actSaveSuccess}", "*Success:*") // render.js Renderer.tag
                        .replaceAll("\\{@actSaveSuccessOrFail}", "*Failure or Success:*") // render.js Renderer.tag
                        .replaceAll("\\{@actResponse}", "Response:") // render.js Renderer.tag
                        .replaceAll("\\{@actTrigger}", "Trigger:") // render.js Renderer.tag
                        .replaceAll("\\{@spell\\s*}", "") // error in homebrew
                        .replaceAll("\\{@color ([^|}]+)\\|?[^}]*}", "$1")
                        .replaceAll("\\{@style ([^|}]+)\\|?[^}]*}", "$1")
                        .replaceAll("\\{@b ([^}]+?)}", "**$1**")
                        .replaceAll("\\{@bold ([^}]+?)}", "**$1**")
                        .replaceAll("\\{@c ([^}]+?)}", "$1")
                        .replaceAll("\\{@center ([^}]+?)}", "$1")
                        .replaceAll("\\{@i ([^}]+?)}", "*$1*")
                        .replaceAll("\\{@italic ([^}]+)}", "*$1*")
                        .replaceAll("\\{@s ([^}]+?)}", "~~$1~~")
                        .replaceAll("\\{@strike ([^}]+)}", "~~$1~~")
                        .replaceAll("\\{@u ([^}]+?)}", "_$1_")
                        .replaceAll("\\{@underline ([^}]+?)}", "_$1_")
                        .replaceAll("\\{@comic ([^}]+?)}", "$1")
                        .replaceAll("\\{@comicH1 ([^}]+?)}", "$1")
                        .replaceAll("\\{@comicH2 ([^}]+?)}", "$1")
                        .replaceAll("\\{@comicH3 ([^}]+?)}", "$1")
                        .replaceAll("\\{@comicH4 ([^}]+?)}", "$1")
                        .replaceAll("\\{@comicNote ([^}]+?)}", "$1")
                        .replaceAll("\\{@highlight ([^}]+?)}", "==$1==")
                        .replaceAll("\\{@code ([^}]+?)}", "`$1`")
                        .replaceAll("\\{@kbd ([^}]+?)}", "`$1`")
                        .replaceAll("\\{@b}", " ")
                        .replaceAll("\\{@i}", " ");
            } catch (Exception e) {
                tui().errorf(e, "Unable to parse string from %s: %s", getSources().getKey(), input);
            }

            result = footnotePattern.matcher(result).replaceAll((match) -> {
                // {@footnote directly in text|This is primarily for homebrew purposes, as the
                // official texts (so far) avoid using footnotes},
                // {@footnote optional reference information|This is the footnote. References
                // are free text.|Footnote 1, page 20}.",
                // We're converting these to _inline_ markdown footnotes, as numbering is
                // difficult to track
                String[] parts = match.group(1).split("\\|");
                if (parts[0].contains("<sup>")) {
                    // This already assumes what the footnote name will be
                    // TODO: Note content is lost on this path at the moment
                    return parts[0];
                }
                if (parts.length > 2) {
                    return "%s ^[%s, _%s_]".formatted(parts[0], parts[1], parts[2]);
                }
                return "%s ^[%s]".formatted(parts[0], parts[1]);
            });

            result = notePattern.matcher(result).replaceAll((match) -> {
                return switch (match.group(1)) {
                    case "note" -> {
                        // {@note This is a note}
                        if (nested) {
                            yield "<span class='note'>**Note:** " + match.group(2).trim() + "</span>";
                        } else {
                            List<String> text = new ArrayList<>();
                            text.add("> [!note]");
                            for (String line : match.group(2).split("\n")) {
                                text.add("> " + line);
                            }
                            yield String.join("\n", text);
                        }
                    }
                    case "tip" -> {
                        // {@tip tooltip tags|a note}
                        String[] parts = match.group(2).split("\\|");
                        yield "<span class='tip' title='%s'>%s</span>".formatted(parts[1], parts[0]);
                    }
                    default -> {
                        yield match.group(0);
                    }
                };
            });

            // after other replacements
            return result.replaceAll("\\{@adventure ([^|}]+)\\|[^}]*}", "$1");

        } catch (IllegalArgumentException e) {
            tui().errorf(e, "Failure replacing text: %s", e.getMessage());
        }

        return input;
    }

    default String replaceSavingThrow(MatchResult match) {
        // format: {@actSave dex}
        String key = match.group(1);
        SkillOrAbility ability = index().findSkillOrAbility(key, getSources());

        return String.format("*%s Saving Throw:*", ability.value());
    }

    default String replaceSkillOrAbility(MatchResult match) {
        // format: {@ability str 20} or {@ability str 20|Display Text}
        // or {@ability str 20|Display Text|Roll Name Text}
        // format: {@savingThrow str 5} or {@savingThrow str 5|Display Text}
        // or {@savingThrow str 5|Display Text|Roll Name Text}
        String[] parts = match.group(2).split("\\|");
        String[] score = parts[0].split(" ");

        boolean abilityCheck = match.group(1).equals("ability");
        String text = valueOrDefault(parts, 1, null);

        SkillOrAbility ability = index().findSkillOrAbility(score[0], getSources());

        if (!abilityCheck && !score[1].matches("\\+?\\d+")) { // saving throw with text, like +PB
            return text == null
                    ? ability.value() + " " + score[1]
                    : text;
        } else {
            int value = Integer.parseInt(score[1]);
            String mod = abilityCheck
                    ? "" + AbilityScores.getModifier(value)
                    : (value >= 0 ? "+" : "") + value;

            DiceRoller roller = cfg().useDiceRoller();
            boolean notSuppressed = roller == DiceRoller.enabledUsingFS && !parseState().inTrait();
            if (!abilityCheck && (roller == DiceRoller.enabled || notSuppressed)) {
                mod = "`dice: d20" + mod + "|text(" + mod + ")`";
            }

            return "%s (%s)".formatted(text == null ? ability.value() : text, mod);
        }
    }

    default String replaceSkillCheck(MatchResult match) {
        // format: {@skillCheck animal_handling 5} or {@skillCheck animal_handling
        // 5|Display Text}
        // or {@skillCheck animal_handling 5|Display Text|Roll Name Text}
        String[] parts = match.group(1).split("\\|");
        String[] score = parts[0].split(" ");
        SkillOrAbility skill = index().findSkillOrAbility(score[0], getSources());
        String text = valueOrDefault(parts, 1,
                linkifyRules(Tools5eIndexType.skill, skill.value(), "skills"));

        String dice = score[1];
        if (score[1].matches("\\d+")) {
            int value = Integer.parseInt(score[1]);
            dice = (value >= 0 ? "+" : "") + value;
        }

        DiceRoller roller = cfg().useDiceRoller();
        boolean notSuppressed = roller.useFantasyStatblocks() && !parseState().inTrait();
        if (roller == DiceRoller.enabled || notSuppressed) {
            dice = "`dice: d20" + dice + "|text(" + dice + ")`";
        }

        return "%s (%s)".formatted(text, dice);
    }

    default String linkifySkill(SkillOrAbility skill) {
        String source = skill.source();
        return linkify(Tools5eIndexType.skill,
                "%s|%s|%s".formatted(skill.value(), source, skill.value()));
    }

    default String linkifyRules(Tools5eIndexType type, String text, String rules) {
        // {@condition stunned} assumes PHB by default,
        // {@condition stunned|PHB} can have sources added with a pipe (not that it's
        // ever useful),
        // {@condition stunned|PHB|and optional link text added with another pipe}.",

        String[] parts = text.split("\\|");
        String name = parts[0];
        String source = valueOrDefault(parts, 1, type.defaultSourceString());
        String linkText = valueOrDefault(parts, 2, name);

        if (name.isBlank()) {
            return "[%s](%s%s.md)".formatted(linkText, index().rulesVaultRoot(), rules);
        }

        String aliasKey = index().getAliasOrDefault(type.createKey(name, source));
        if (index().isExcluded(aliasKey)) {
            return linkText;
        }
        Tools5eIndexType aliasType = Tools5eIndexType.getTypeFromKey(aliasKey);
        Tools5eSources rulesSources = Tools5eSources.findSources(aliasKey);
        if (rulesSources != null) {
            name = rulesSources.getName();
        }
        if (aliasType != type) {
            // we've changed types.. so we need to linkify based on the new type
            // typically phb -> xphb changes
            return linkify(aliasType, "%s|%s|%s".formatted(name, source, linkText));
        }
        return "[%s](%s%s.md#%s)".formatted(
                linkText, index().rulesVaultRoot(), rules, toAnchorTag(name));
    }

    default String linkify(MatchResult match) {
        Tools5eIndexType type = Tools5eIndexType.fromText(match.group(1));
        if (type == null) {
            throw new IllegalArgumentException("Unable to linkify " + match.group(0));
        }
        return linkify(type, match.group(2));
    }

    default String linkify(Tools5eIndexType type, String s) {
        if (!isPresent(s)) {
            return "";
        }
        return switch (type) {
            // {@background Charlatan} assumes PHB by default,
            // {@background Anthropologist|toa} can have sources added with a pipe,
            // {@background Anthropologist|ToA|and optional link text added with another
            // pipe}.",
            // {@feat Alert} assumes PHB by default,
            // {@feat Elven Accuracy|xge} can have sources added with a pipe,
            // {@feat Elven Accuracy|xge|and optional link text added with another pipe}.",
            // {@deck Tarokka Deck|CoS|tarokka deck} // like items
            // {@hazard brown mold} assumes DMG by default,
            // {@hazard russet mold|vgm} can have sources added with a pipe,
            // {@hazard russet mold|vgm|and optional link text added with another pipe}.",
            // {@item alchemy jug} assumes DMG by default,
            // {@item longsword|phb} can have sources added with a pipe,
            // {@item longsword|phb|and optional link text added with another pipe}.",
            // {@legroup unicorn} assumes MM by default,
            // {@legroup balhannoth|MPMM} can have sources added with a pipe,
            // {@legroup balhannoth|MPMM|and optional link text added with another pipe}.",
            // {@object Ballista} assumes DMG by default,
            // {@object Ballista|DMG|and optional link text added with another pipe}.",
            // {@optfeature Agonizing Blast} assumes PHB by default,
            // {@optfeature Aspect of the Moon|xge} can have sources added with a pipe,
            // {@optfeature Aspect of the Moon|xge|and optional link text added with another
            // pipe}.",
            // {@psionic Mastery of Force} assumes UATheMysticClass by default
            // {@psionic Mastery of Force|UATheMysticClass} can have sources added with a
            // pipe
            // {@psionic Mastery of Force|UATheMysticClass|and optional link text added with
            // another pipe}.",
            // {@race Human} assumes PHB by default,
            // {@race Aasimar (Fallen)|VGM}
            // {@race Aasimar|DMG|racial traits for the aasimar}
            // {@race Aarakocra|eepc} can have sources added with a pipe,
            // {@race Aarakocra|eepc|and optional link text added with another pipe}.",
            // {@race dwarf (hill)||Dwarf, hill}
            // {@reward Blessing of Health} assumes DMG by default,
            // {@reward Blessing of Health} can have sources added with a pipe,
            // {@reward Blessing of Health|DMG|and optional link text added with another
            // pipe}.",
            // {@spell acid splash} assumes PHB by default,
            // {@spell tiny servant|xge} can have sources added with a pipe,
            // {@spell tiny servant|xge|and optional link text added with another pipe}.",
            // {@table 25 gp Art Objects} assumes DMG by default,
            // {@table Adventuring Gear|phb} can have sources added with a pipe,
            // {@table Adventuring Gear|phb|and optional link text added with another
            // pipe}.",
            // {@trap falling net} assumes DMG by default,
            // {@trap falling portcullis|xge} can have sources added with a pipe,
            // {@trap falling portcullis|xge|and optional link text added with another
            // pipe}.",
            // {@vehicle Galley} assumes GoS by default,
            // {@vehicle Galley|UAOfShipsAndSea} can have sources added with a pipe,
            // {@vehicle Galley|GoS|and optional link text added with another pipe}.",
            case background,
                    feat,
                    deck,
                    facility,
                    hazard,
                    item,
                    legendaryGroup,
                    object,
                    optfeature,
                    psionic,
                    race,
                    reward,
                    spell,
                    table,
                    tableGroup,
                    trap,
                    vehicle ->
                linkifyType(type, s);
            case action -> linkifyRules(type, s, "actions");
            case condition, status -> linkifyRules(type, s, "conditions");
            case disease -> linkifyRules(type, s, "diseases");
            case sense -> linkifyRules(type, s, "senses");
            case skill -> linkifyRules(type, s, "skills");
            case itemMastery, itemProperty, itemType -> linkifyItemAttribute(type, s);
            case monster -> linkifyCreature(s);
            case subclass -> linkifySubclass(s); // RARE!!
            case classtype -> linkifyClass(s);
            case deity -> linkifyDeity(s);
            case card -> linkifyCardType(s);
            case classfeature -> linkifyClassFeature(s);
            case subclassFeature -> linkifySubclassFeature(s);
            case variantrule -> linkifyVariant(s);
            default -> {
                tui().debugf(Msg.UNKNOWN, "unknown tag/type {@%s %s} from %s",
                        type, s, parseState().getSource());
                yield s;
            }
        };
    }

    default String linkOrText(String linkText, String key, String dirName, String resourceName) {
        return index().isIncluded(key)
                ? "[%s](%s%s/%s.md)".formatted(
                        linkText, index().compendiumVaultRoot(), dirName, slugify(resourceName)
                                .replace("-dmg-dmg", "-dmg")) // bad combo for some race names
                : linkText;
    }

    default String linkifyType(Tools5eIndexType type, String match) {
        String[] parts = match.split("\\|");
        String source = valueOrDefault(parts, 1, type.defaultSourceString());
        String linkText = valueOrDefault(parts, 2, parts[0]);

        String key = index().getAliasOrDefault(type.createKey(parts[0].trim(), source));
        return linkifyType(type, key, linkText, match);
    }

    default String linkifyType(Tools5eIndexType type, String aliasKey, String linkText, String match) {
        String dirName = type.getRelativePath();
        JsonNode jsonSource = index().getNode(aliasKey); // filtered
        if (jsonSource == null) {
            if (index().getOrigin(aliasKey) == null) {
                // sources can be excluded, that's fine.. but if this is something that doesn't
                // exist at all..
                tui().debugf(Msg.UNRESOLVED, "unresolvable {@%s %s}   as [%s]   from %s",
                        type, match, aliasKey, parseState().getSource());
                // log a stack trace of how we got here
            }
            return linkText;
        }
        Tools5eSources linkSource = Tools5eSources.findSources(jsonSource);
        return linkOrText(linkText, aliasKey, dirName,
                Tools5eQuteBase.fixFileName(type.decoratedName(jsonSource), linkSource));
    }

    default String linkifyCardType(String match) {
        String dirName = Tools5eIndexType.card.getRelativePath();
        // {@card The Fates|Deck of Many Things}
        // {@card Donjon|Deck of Several Things|LLK}
        String[] parts = match.split("\\|");
        String cardName = parts[0];
        String deckName = parts[1];
        String source = valueOrDefault(parts, 2, Tools5eIndexType.card.defaultSourceString());

        String key = index().getAliasOrDefault(Tools5eIndexType.deck.createKey(deckName, source));
        if (index().isExcluded(key)) {
            return cardName;
        }
        String resource = Tools5eQuteBase.fixFileName(deckName, source, Tools5eIndexType.card);
        return "[%s](%s%s/%s.md#%s)".formatted(
                cardName,
                index().compendiumVaultRoot(), dirName,
                resource, cardName.replace(" ", "%20"));
    }

    default String linkifyDeity(String match) {
        // "Deities: {@deity Gond} assumes PHB Forgotten Realms pantheon by default,
        // {@deity Gruumsh|nonhuman} can have pantheons added with a pipe,
        // {@deity Ioun|dawn war|dmg} can have sources added with another pipe,
        // {@deity Ioun|dawn war|dmg|and optional link text added with another pipe}.",
        String[] parts = match.split("\\|");
        String deity = parts[0];
        String pantheon = valueOrDefault(parts, 1, "Forotten Realms");
        String source = valueOrDefault(parts, 2, Tools5eIndexType.deity.defaultSourceString());
        String linkText = valueOrDefault(parts, 3, deity);
        String key = index().getAliasOrDefault(Tools5eIndexType.deity.createKey(parts[0], source));
        return linkOrText(linkText, key,
                Tools5eIndexType.deity.getRelativePath(),
                Tools5eQuteBase.getDeityResourceName(parts[0], source, pantheon));
    }

    default String linkifyClass(String match) {
        // {@class fighter} assumes PHB by default,
        // {@class artificer|uaartificer} can have sources added with a pipe,
        // {@class fighter|phb|optional link text added with another pipe},
        // {@class fighter|phb|subclasses added|Eldritch Knight} with another pipe,
        // {@class fighter|phb|and class feature added|Eldritch Knight|phb|2-0} with
        // another pipe
        // {@class Barbarian|phb|Path of the Ancestral Guardian|Ancestral Guardian|xge}
        // {@class Fighter|phb|Samurai|Samurai|xge}
        String[] parts = match.split("\\|");
        String className = parts[0];
        String classSource = valueOrDefault(parts, 1, Tools5eIndexType.classtype.defaultSourceString());
        String linkText = valueOrDefault(parts, 2, className);
        String subclass = valueOrDefault(parts, 3, null);
        String subclassSource = valueOrDefault(parts, 4, Tools5eIndexType.classtype.defaultSourceString());

        String relativePath = Tools5eIndexType.classtype.getRelativePath();
        if (subclass != null) {
            String key = index()
                    .getAliasOrDefault(
                            Tools5eIndexType.getSubclassKey(className, classSource, subclass, subclassSource));
            // "subclass|path of wild magic|barbarian|phb|"
            Tools5eSources scSources = Tools5eSources.findSources(key);
            return linkOrText(linkText, key, relativePath,
                    Tools5eQuteBase.getSubclassResource(
                            scSources == null ? subclass : scSources.getName(),
                            className, classSource, subclassSource));
        } else {
            String key = index().getAliasOrDefault(Tools5eIndexType.classtype.createKey(className, classSource));
            return linkOrText(linkText, key, relativePath,
                    Tools5eQuteBase.getClassResource(className, classSource));
        }
    }

    default String linkifySubclass(String match) {
        // Only used in homebrew (so far)
        // "Subclasses:{@subclass Berserker|Barbarian},
        // {@subclass Berserker|Barbarian},
        // {@subclass Ancestral Guardian|Barbarian||XGE},
        // {@subclass Artillerist|Artificer|TCE|TCE}.
        // Class and subclass source is assumed to be PHB."
        String[] parts = match.split("\\|");
        String scShortName = parts[0];
        String className = parts[1];
        String classSource = valueOrDefault(parts, 2, Tools5eIndexType.classtype.defaultSourceString());
        String scSource = valueOrDefault(parts, 3, Tools5eIndexType.subclass.defaultSourceString());
        String linkText = valueOrDefault(parts, 4, scShortName);

        // "subclass|path of wild magic|barbarian|phb|phb"
        String key = index()
                .getAliasOrDefault(
                        Tools5eIndexType.getSubclassKey(className, classSource, scShortName, scSource));
        Tools5eSources scSources = Tools5eSources.findSources(key);
        return linkOrText(linkText, key,
                Tools5eIndexType.classtype.getRelativePath(),
                Tools5eQuteBase.getSubclassResource(scSources.getName(), className, classSource, scSource));
    }

    default String linkifyClassFeature(String match) {
        // "Class Features: Class source is assumed to be PHB, class feature source is
        // assumed to be the same as class source"
        // {@classFeature Rage|Barbarian||1},
        // {@classFeature Infuse Item|Artificer|TCE|2},
        // {@classFeature Survival Instincts|Barbarian||2|UAClassFeatureVariants},
        // {@classFeature Rage|Barbarian||1||optional display text}.
        String[] parts = match.split("\\|");
        String linkText = parts[0];
        if (parts.length < 4) {
            tui().errorf("Bad Class Feature Link {@classFeature %s} in %s", match, getSources());
            return linkText;
        }
        String className = parts[1];
        String classSource = parts[2].isBlank() ? "phb" : parts[2];
        String level = parts[3];
        if (parts.length > 5) {
            linkText = parts[5];
            int pos = match.lastIndexOf("|");
            match = match.substring(0, pos);
        }
        String classFeatureKey = index().getAliasOrDefault(Tools5eIndexType.classfeature.fromTagReference(match));
        if (classFeatureKey == null || index().isExcluded(classFeatureKey)) {
            return linkText;
        }

        JsonNode featureJson = index().getNode(classFeatureKey);
        Tools5eSources featureSources = Tools5eSources.findSources(classFeatureKey);

        String headerName = decoratedFeatureTypeName(featureSources, featureJson) + " (Level " + level + ")";
        String resource = Tools5eQuteBase.getClassResource(className, classSource);

        String relativePath = Tools5eIndexType.classtype.getRelativePath();
        return "[%s](%s%s/%s.md#%s)".formatted(
                linkText,
                index().compendiumVaultRoot(), relativePath,
                resource, toAnchorTag(headerName));
    }

    default String linkifyOptionalFeatureType(MatchResult match) {
        String linkText = match.group(1);
        String conditions = match.group(2);

        Matcher m = featureTypePattern.matcher(conditions);
        String featureType = m.find() ? m.group(1) : null;

        m = featureSourcePattern.matcher(conditions);
        String featureSource = m.find() ? m.group(1) : null;
        if (featureSource == null) {
            featureSource = parseState().getSource();
        }

        OptionalFeatureType oft = index().getOptionalFeatureType(featureType);
        if (oft == null) {
            return linkText;
        }
        return linkOrText(linkText, oft.getKey(),
                Tools5eIndexType.optionalFeatureTypes.getRelativePath(),
                oft.getFilename());
    }

    default String linkifySubclassFeature(String match) {
        // "Subclass Features:
        // {@subclassFeature Path of the Berserker|Barbarian||Berserker||3},
        // {@subclassFeature Alchemist|Artificer|TCE|Alchemist|TCE|3},
        // {@subclassFeature Path of the Battlerager|Barbarian||Battlerager|SCAG|3}, -->
        // "barbarian-path-of-the-... "
        // {@subclassFeature Blessed Strikes|Cleric||Life||8|UAClassFeatureVariants},
        // --> "-domain"
        // {@subclassFeature Blessed Strikes|Cleric|PHB|Twilight|TCE|8|TCE}
        // {@subclassFeature Path of the Berserker|Barbarian||Berserker||3||optional
        // display text}.
        // Class source is assumed to be PHB.
        // Subclass source is assumed to be PHB.
        // Subclass feature source is assumed to be the same as subclass source.",
        String[] parts = match.split("\\|");
        String linkText = parts[0];
        if (parts.length < 6) {
            tui().warnf("Bad Subclass Feature Link {@subclassFeature %s} in %s", match, getSources());
            return linkText;
        }

        if (parts.length > 7) {
            linkText = parts[7];
            // trim optional display text
            int pos = match.lastIndexOf("|");
            match = match.substring(0, pos);
        }

        // Get the right subclass feature key
        String featureKey = Tools5eIndexType.subclassFeature.fromTagReference(match);
        featureKey = index().getAliasOrDefault(featureKey);
        if (featureKey == null || index().isExcluded(featureKey)) {
            return linkText;
        }

        // Find the subclass that will be emitted...
        String subclassKey = Tools5eIndexType.subclass.fromChildKey(featureKey);

        // look up alias for subclass so link is correct, but don't follow reprints
        // "subclass|redemption|paladin|phb|"
        //    : "subclass|oath of redemption|paladin|phb|",
        // "subclass|twilight|cleric|phb|tce"
        //    : "subclass|twilight domain|cleric|phb|tce"
        subclassKey = index().getAliasOrDefault(subclassKey, false);

        JsonNode subclassNode = index().getNode(subclassKey);
        if (subclassNode == null) {
            // if the subclass was reprinted, the target file name will change (minimally)
            subclassKey = index().getAliasOrDefault(subclassKey);
            subclassNode = index().getNode(subclassKey);
            if (subclassNode == null) {
                tui().warnf(Msg.UNRESOLVED, "Subclass %s not found for {@subclassfeature %s} in %s",
                        subclassKey, match, getSources().getKey());
                return linkText;
            }
            // Examine new subclass node's features, to see if there is a match
            // e.g. for
            //   "subclassfeature|primal companion|ranger|phb|beast master|phb|3|tce",
            // consider
            //   "subclassfeature|primal companion|ranger|xphb|beast master|xphb|3|xphb"
            String test = featureKey.replaceAll(subclassFeatureMask, "$1-$2");
            boolean found = false;
            for (String fkey : index().findClassFeatures(subclassKey)) {
                String compare = fkey.replaceAll(subclassFeatureMask, "$1-$2");
                if (test.equals(compare)) {
                    featureKey = fkey;
                    found = true;
                    break;
                }
            }
            if (!found) {
                tui().warnf(Msg.UNRESOLVED,
                        "No equivalent subclass feature found for {@subclassfeature %s} in %s (from %s)",
                        match, subclassKey, getSources().getKey());
                return linkText;
            }
        }

        JsonNode featureJson = index().getNode(featureKey);
        Tools5eSources featureSources = Tools5eSources.findSources(featureKey);

        String level = Tools5eFields.level.getTextOrEmpty(featureJson);
        String headerName = decoratedFeatureTypeName(featureSources, featureJson) + " (Level " + level + ")";
        String resource = slugify(Tools5eQuteBase.getSubclassResource(
                SourceField.name.getTextOrEmpty(subclassNode),
                IndexFields.className.getTextOrEmpty(subclassNode),
                IndexFields.classSource.getTextOrEmpty(subclassNode),
                SourceField.source.getTextOrEmpty(subclassNode)));
        String relativePath = Tools5eIndexType.classtype.getRelativePath();
        return "[%s](%s%s/%s.md#%s)".formatted(
                linkText,
                index().compendiumVaultRoot(), relativePath,
                resource,
                toAnchorTag(headerName));
    }

    default String linkifyCreature(String match) {
        Tools5eIndexType indexType = Tools5eIndexType.monster;
        // "Creatures:
        // {@creature goblin} assumes MM by default,
        // {@creature cow|vgm} can have sources added with a pipe,
        // {@creature cow|vgm|and optional link text added with another pipe}.",
        String[] parts = match.trim().split("\\|");
        String source = valueOrDefault(parts, 1, indexType.defaultSourceString());
        String linkText = valueOrDefault(parts, 2, parts[0]);

        String key = index().getAliasOrDefault(indexType.createKey(parts[0], source));
        if (index().isExcluded(key)) {
            return linkText;
        }
        JsonNode jsonSource = index().getNode(key);
        Tools5eSources sources = Tools5eSources.findSources(key);
        String resourceName = indexType.decoratedName(jsonSource);
        String creatureType = getMonsterType(jsonSource); // may be missing for partial index
        if (creatureType == null) {
            return linkText;
        }
        boolean isNpc = Json2QuteMonster.isNpc(jsonSource);
        return linkOrText(linkText, key,
                Tools5eQuteBase.monsterPath(isNpc, creatureType),
                Tools5eQuteBase.fixFileName(resourceName, sources));
    }

    default String linkifyVariant(String variant) {
        // "fromVariant": "Action Options",
        // "fromVariant": "Spellcasting|XGE",
        String[] parts = variant.trim().split("\\|");
        String source = valueOrDefault(parts, 1, Tools5eIndexType.variantrule.defaultSourceString());
        if (!index().sourceIncluded(source)) {
            return "<span title=\"%s\">%s</span>".formatted(TtrpgConfig.sourceToLongName(source), parts[0]);
        } else {
            return "[%s](%svariant-rules/%s.md)".formatted(
                    parts[0], index().rulesVaultRoot(),
                    Tools5eQuteBase.fixFileName(parts[0], source, Tools5eIndexType.variantrule));
        }
    }

    default String linkifyItemAttribute(Tools5eIndexType type, String s) {
        String[] parts = s.split("\\|");
        String name = parts[0];
        String linkText = valueOrDefault(parts, 2, name);
        return switch (type) {
            case itemMastery -> {
                ItemMastery mastery = index().findItemMastery(s, getSources());
                yield mastery == null
                        ? linkText
                        : mastery.linkify(linkText);
            }
            case itemProperty -> {
                ItemProperty property = index().findItemProperty(s, getSources());
                yield property == null
                        ? linkText
                        : property.linkify(linkText);
            }
            case itemType -> {
                ItemType itemType = index().findItemType(s, getSources());
                yield itemType == null
                        ? linkText
                        : itemType.linkify(linkText);
            }
            default -> linkText;
        };
    }

    default String handleCitation(String citationTag) {
        // Casting Times|FleeMortals|A
        // Casting Times|FleeMortals|{@sup A}
        String[] parts = citationTag.split("\\|");
        if (parts.length < 3) {
            tui().errorf("Badly formed citation %s in %s", citationTag, getSources().getKey());
            return citationTag;
        }
        String key = index().getAliasOrDefault(Tools5eIndexType.citation.createKey(parts[0], parts[1]));
        String annotation = replaceText(parts[2]).replaceAll("</?sup>", "");
        JsonNode jsonSource = index().getNode(key);
        if (index().isExcluded(key) || jsonSource == null) {
            return annotation;
        }
        String blockRef = "^" + slugify(key);
        List<String> text = new ArrayList<>();
        appendToText(text, jsonSource, null);
        if (text.get(text.size() - 1).startsWith("^")) {
            blockRef = text.get(text.size() - 1);
        } else {
            text.add(blockRef);
        }
        parseState().addCitation(key, String.join("\n", text));
        return "[%s](#%s)".formatted(annotation, blockRef);
    }

    default String decoratedUaName(String name, Tools5eSources sources) {
        Optional<String> uaSource = sources.uaSource();
        if (uaSource.isPresent() && !name.contains("(UA")) {
            return name + " (" + uaSource.get() + ")";
        }
        return name;
    }

    default String getMonsterType(JsonNode node) {
        if (node == null || !node.has("type")) {
            tui().warnf("Monster: Empty type for %s", getSources());
            return null;
        }
        JsonNode typeNode = node.get("type");
        if (typeNode.isTextual()) {
            return typeNode.asText();
        } else if (typeNode.has("type")) {
            // We have an object: type + tags
            return typeNode.get("type").asText();
        }
        return null;
    }

    default String decoratedFeatureTypeName(Tools5eSources valueSources, JsonNode value) {
        String name = valueSources.getName();
        String type = IndexFields.featureType.getTextOrEmpty(value);

        if (!type.isEmpty()) {
            switch (type) {
                case "ED":
                    return "Elemental Discipline: " + name;
                case "EI":
                    return "Eldritch Invocation: " + name;
                case "MM":
                    return "Metamagic: " + name;
                case "MV":
                case "MV:B":
                case "MV:C2-UA":
                    return "Maneuver: " + name;
                case "FS:F":
                case "FS:B":
                case "FS:R":
                case "FS:P":
                    return "Fighting Style: " + name;
                case "AS":
                case "AS:V1-UA":
                case "AS:V2-UA":
                    return "Arcane Shot: " + name;
                case "PB":
                    return "Pact Boon: " + name;
                case "AI":
                    return "Artificer Infusion: " + name;
                case "SHP:H":
                case "SHP:M":
                case "SHP:W":
                case "SHP:F":
                case "SHP:O":
                    return "Ship Upgrade: " + name;
                case "IWM:W":
                    return "Infernal War Machine Variant: " + name;
                case "IWM:A":
                case "IWM:G":
                    return "Infernal War Machine Upgrade: " + name;
                case "OR":
                    return "Onomancy Resonant: " + name;
                case "RN":
                    return "Rune Knight Rune: " + name;
                case "AF":
                    return "Alchemical Formula: " + name;
                default:
                    tui().errorf("Unknown feature type %s for class feature %s", type, name);
            }
        }
        return name;
    }

    default double convertToNumber(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        Matcher m = FRACTIONAL.matcher(text);
        if (m.matches()) {
            double out = Double.parseDouble(m.group(1));
            if (m.group(2) != null) {
                switch (m.group(2)) {
                    case "⅛":
                        out += 0.125;
                        break;
                    case "¼":
                        out += 0.25;
                        break;
                    case "⅜":
                        out += 0.375;
                        break;
                    case "½":
                        out += 0.5;
                        break;
                    case "⅝":
                        out += 0.625;
                        break;
                    case "¾":
                        out += 0.75;
                        break;
                    case "⅞":
                        out += 0.875;
                        break;
                    case "⅓":
                        out += 1 / 3;
                        break;
                    case "⅔":
                        out += 2 / 3;
                        break;
                    case "⅙":
                        out += 1 / 6;
                        break;
                    case "⅚":
                        out += 5 / 6;
                        break;
                    case "":
                        break;
                }
            }
            return out;
        }
        throw new IllegalArgumentException("Unable to convert " + text + " to number");
    }
}
