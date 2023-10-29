package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.JsonTextConverter;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndex.OptionalFeatureType;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndexType.IndexFields;
import dev.ebullient.convert.tools.dnd5e.qute.AbilityScores;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public interface JsonTextReplacement extends JsonTextConverter<Tools5eIndexType> {
    Pattern FRACTIONAL = Pattern.compile("^(\\d+)?([⅛¼⅜½⅝¾⅞⅓⅔⅙⅚])?$");

    Pattern linkifyPattern = Pattern.compile(
            "\\{@(action|background|class|condition|creature|deity|disease|feat|card|deck|hazard|item|legroup|object|race|reward|sense|skill|spell|status|table|variantrule|vehicle|optfeature|classFeature|subclassFeature|trap) ([^}]+)}");

    Pattern dicePattern = Pattern.compile("\\{@(dice|damage) ([^{}]+)}");

    Pattern chancePattern = Pattern.compile("\\{@chance ([^}]+)}");
    Pattern fontPattern = Pattern.compile("\\{@font ([^}]+)}");
    Pattern homebrewPattern = Pattern.compile("\\{@homebrew ([^}]+)}");
    Pattern quickRefPattern = Pattern.compile("\\{@quickref ([^}]+)}");
    Pattern notePattern = Pattern.compile("\\{@note (\\*|Note:)?\\s?([^}]+)}");
    Pattern footnotePattern = Pattern.compile("\\{@footnote ([^}]+)}");
    Pattern abilitySavePattern = Pattern.compile("\\{@(ability|savingThrow) ([^}]+)}"); // {@ability str 20}
    Pattern skillCheckPattern = Pattern.compile("\\{@skillCheck ([^}]+)}"); // {@skillCheck animal_handling 5}
    Pattern optionalFeaturesFilter = Pattern.compile("\\{@filter ([^|}]+)\\|optionalfeatures\\|([^}]+)*}");
    Pattern featureTypePattern = Pattern.compile("(?:[Ff]eature )?[Tt]ype=([^|}]+)");
    Pattern featureSourcePattern = Pattern.compile("source=([^|}]+)");
    Pattern superscriptCitationPattern = Pattern.compile("\\{@(sup|cite) ([^}]+)}");

    Tools5eIndex index();

    Tools5eSources getSources();

    default Tui tui() {
        return cfg().tui();
    }

    default CompendiumConfig cfg() {
        return index().cfg();
    }

    default ObjectMapper mapper() {
        return Tui.MAPPER;
    }

    default boolean isPresent(String s) {
        return s != null && !s.isBlank();
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
        return replaceTokens(input, (s, b) -> this._replaceTokenText(s, b));
    }

    default String _replaceTokenText(String input, boolean nested) {
        String result = input;

        try {
            result = result
                    .replace("#$prompt_number:title=Enter Alert Level$#", "Alert Level")
                    .replace("#$prompt_number:title=Enter Charisma Modifier$#", "Charisma modifier")
                    .replace("#$prompt_number:title=Enter Lifestyle Modifier$#", "Charisma modifier")
                    .replace("#$prompt_number:title=Enter a Modifier$#", "Modifier")
                    .replace("#$prompt_number:title=Enter a Modifier,default=10$#", "Modifier (default 10)")
                    .replaceAll("#\\$prompt_number.*default=(.*)\\$#", "$1");

            // Dice roller tags; {@dice 1d2-2+2d3+5} for regular dice rolls
            // - {@dice 1d6;2d6} for multiple options;
            // - {@dice 1d6 + #$prompt_number:min=1,title=Enter a Number!,default=123$#} for input prompts
            // - {@dice 1d20+2|display text} and {@dice 1d20+2|display text|rolled by name}
            if (cfg().alwaysUseDiceRoller()) {
                result = replaceWithDiceRoller(result);
            }

            // @dice or @damage
            result = dicePattern.matcher(result).replaceAll((match) -> {
                String[] parts = match.group(2).split("\\|");
                if (parts.length > 1) {
                    return parts[1];
                }
                return formatDice(parts[0]);
            });

            result = chancePattern.matcher(result).replaceAll((match) -> {
                String[] parts = match.group(1).split("\\|");
                return parts[0] + "% chance";
            });

            result = abilitySavePattern.matcher(result).replaceAll(this::replaceSkillOrAbility);
            result = skillCheckPattern.matcher(result).replaceAll(this::replaceSkillCheck);

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
                // {@homebrew changes|modifications}, {@homebrew additions} or {@homebrew |removals}
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

            result = linkifyPattern.matcher(result)
                    .replaceAll(this::linkify);

            result = fontPattern.matcher(result)
                    .replaceAll((match) -> {
                        String[] parts = match.group(1).split("\\|");
                        String fontFamily = Tools5eSources.getFontReference(parts[1]);
                        if (fontFamily != null) {
                            return String.format("<span style=\"font-family: %s\">%s</span>",
                                    fontFamily, parts[0]);
                        }
                        return parts[0];
                    });

            try {
                result = result
                        .replace("{@hitYourSpellAttack}", "the summoner's spell attack modifier")
                        // "Internal links: {@5etools This Is Your Life|lifegen.html}",
                        // "External links: {@link https://discord.gg/5etools} or {@link Discord|https://discord.gg/5etools}"
                        .replaceAll("\\{@link ([^}|]+)\\|([^}]+)}", "$1 ($2)") // this must come first
                        .replaceAll("\\{@link ([^}|]+)}", "$1") // this must come first
                        .replaceAll("\\{@5etools ([^}|]+)\\|?[^}]*}", "$1")
                        .replaceAll("\\{@loader ([^}|]+)\\|([^}]+)}", "$1 ^[$2]")
                        .replaceAll("\\{@area ([^|}]+)\\|?[^}]*}", "$1")
                        .replaceAll("\\{@vehupgrade ([^|}]+)\\|?[^}]*}", "$1") // TODO: vehicle upgrade type
                        .replaceAll("\\{@dc ([^}]+)}", "DC $1")
                        .replaceAll("\\{@d20 ([^}]+?)}", "$1") // when not using dice roller
                        .replaceAll("\\{@recharge ([^}]+?)}", "(Recharge $1-6)")
                        .replaceAll("\\{@recharge}", "(Recharge 6)")
                        .replaceAll("\\{@coinflip ([^|}]+)\\|?[^}]*}", "$1")
                        .replaceAll("\\{@coinflip}", "flip a coin")
                        .replaceAll("\\{@(scaledice|scaledamage) [^|]+\\|[^|]+\\|([^|}]+)[^}]*}", "$2")
                        .replaceAll("\\{@filter ([^|}]+)\\|?[^}]*}", "$1")
                        .replaceAll("\\{@boon ([^|}]+)\\|([^|}]+)\\|[^|}]*}", "$2")
                        .replaceAll("\\{@boon ([^|}]+)\\|[^}]*}", "$1")
                        .replaceAll("\\{@boon ([^|}]+)}", "$1")
                        .replaceAll("\\{@charoption ([^|}]+)\\|([^|}]+)\\|[^|}]*}", "$2")
                        .replaceAll("\\{@charoption ([^|}]+)\\|[^}]*}", "$1")
                        .replaceAll("\\{@charoption ([^|}]+)}", "$1")
                        .replaceAll("\\{@cult ([^|}]+)\\|([^|}]+)\\|[^|}]*}", "$2")
                        .replaceAll("\\{@cult ([^|}]+)\\|[^}]*}", "$1")
                        .replaceAll("\\{@cult ([^|}]+)}", "$1")
                        .replaceAll("\\{@language ([^|}]+)\\|?[^}]*}", "$1")
                        .replaceAll("\\{@book ([^}|]+)\\|?[^}]*}", "\"$1\"")
                        .replaceAll("\\{@(hit|h) ([+-][^}<]+)}", "$2")
                        .replaceAll("\\{@(hit|h) ([^}<]+)}", "+$2")
                        .replaceAll("\\{@h}", "*Hit:* ")
                        .replaceAll("\\{@m}", "*Miss:* ")
                        .replaceAll("\\{@atk a}", "*Area Attack:*")
                        .replaceAll("\\{@atk aw}", "*Area Weapon Attack:*")
                        .replaceAll("\\{@atk g}", "*Magical Attack:*")
                        .replaceAll("\\{@atk m}", "*Melee Attack:*")
                        .replaceAll("\\{@atk r}", "*Ranged Attack:*")
                        .replaceAll("\\{@atk mw}", "*Melee Weapon Attack:*")
                        .replaceAll("\\{@atk rw}", "*Ranged Weapon Attack:*")
                        .replaceAll("\\{@atk m,r}", "*Melee or Ranged Attack:*")
                        .replaceAll("\\{@atk mw,rw}", "*Melee or Ranged Weapon Attack:*")
                        .replaceAll("\\{@atk mp}", "*Melee Power Attack:*")
                        .replaceAll("\\{@atk rp}", "*Ranged Power Attack:*")
                        .replaceAll("\\{@atk mp,rp}", "*Melee or Ranged Power Attack:*")
                        .replaceAll("\\{@atk ms}", "*Melee Spell Attack:*")
                        .replaceAll("\\{@atk rs}", "*Ranged Spell Attack:*")
                        .replaceAll("\\{@atk ms,rs}", "*Melee or Ranged Spell Attack:*")
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
                        .replaceAll("\\{@kbd ([^}]+?)}", "`$1`")
                        .replaceAll("\\{@b}", " ")
                        .replaceAll("\\{@i}", " ");
            } catch (Exception e) {
                tui().errorf(e, "Unable to parse string from %s: %s", getSources().getKey(), input);
            }

            result = footnotePattern.matcher(result).replaceAll((match) -> {
                // {@footnote directly in text|This is primarily for homebrew purposes, as the official texts (so far) avoid using footnotes},
                // {@footnote optional reference information|This is the footnote. References are free text.|Footnote 1, page 20}.",
                // We're converting these to _inline_ markdown footnotes, as numbering is difficult to track
                String[] parts = match.group(1).split("\\|");
                if (parts[0].contains("<sup>")) {
                    // This already assumes what the footnote name will be
                    // TODO: Note content is lost on this path at the moment
                    return String.format("%s", parts[0]);
                }
                if (parts.length > 2) {
                    return String.format("%s ^[%s, _%s_]", parts[0], parts[1], parts[2]);
                }
                return String.format("%s ^[%s]", parts[0], parts[1]);
            });

            result = notePattern.matcher(result).replaceAll((match) -> {
                if (nested) {
                    return "***Note:** " + match.group(2).trim() + "*";
                } else {
                    List<String> text = new ArrayList<>();
                    text.add("> [!note]");
                    for (String line : match.group(2).split("\n")) {
                        text.add("> " + line);
                    }
                    return String.join("\n", text);
                }
            });

            // after other replacements
            return result.replaceAll("\\{@adventure ([^|}]+)\\|[^}]*}", "$1");

        } catch (IllegalArgumentException e) {
            tui().errorf(e, "Failure replacing text: %s", e.getMessage());
        }

        return input;
    }

    default String replaceSkillOrAbility(MatchResult match) {
        // format: {@ability str 20} or {@ability str 20|Display Text}
        //      or {@ability str 20|Display Text|Roll Name Text}
        // format: {@savingThrow str 5} or {@savingThrow str 5|Display Text}
        //      or {@savingThrow str 5|Display Text|Roll Name Text}
        String[] parts = match.group(2).split("\\|");
        String[] score = parts[0].split(" ");
        SkillOrAbility ability = index().findSkillOrAbility(score[0], getSources());
        boolean abilityCheck = match.group(1).equals("ability");
        String text = parts.length > 1 && !parts[1].isBlank() ? parts[1] : null;

        if (!abilityCheck && !score[1].matches("\\d+")) { // saving throw: +PB
            return text == null ? ability.value() + " " + score[1] : text;
        } else {
            int value = Integer.parseInt(score[1]);
            String mod = abilityCheck
                    ? "" + AbilityScores.getModifier(value)
                    : (value >= 0 ? "+" : "") + value;
            return String.format("%s (%s)", text == null ? ability.value() : text, mod);
        }
    }

    default String replaceSkillCheck(MatchResult match) {
        // format: {@skillCheck animal_handling 5} or {@skillCheck animal_handling 5|Display Text}
        //      or {@skillCheck animal_handling 5|Display Text|Roll Name Text}
        String[] parts = match.group(1).split("\\|");
        String[] score = parts[0].split(" ");
        SkillOrAbility skill = index().findSkillOrAbility(score[0], getSources());
        String text = parts.length > 1 && !parts[1].isBlank()
                ? parts[1]
                : linkifyRules(Tools5eIndexType.skill, skill.value(), "skills");

        String dice = score[1];
        if (score[1].matches("\\d+")) {
            int value = Integer.parseInt(score[1]);
            dice = (value >= 0 ? "+" : "") + value;
        }
        return String.format("%s (%s)", text, dice);
    }

    default String linkifyRules(Tools5eIndexType type, String text, String rules) {
        // {@condition stunned} assumes PHB by default,
        // {@condition stunned|PHB} can have sources added with a pipe (not that it's ever useful),
        // {@condition stunned|PHB|and optional link text added with another pipe}.",

        String[] parts = text.split("\\|");
        String heading = parts[0];
        String source = parts.length > 1 ? parts[1] : "PHB";
        String linkText = parts.length > 2 ? parts[2] : heading;

        String key = index().getAliasOrDefault(type.createKey(heading, source));
        if (index().isExcluded(key)) {
            return linkText;
        }
        return String.format("[%s](%s%s.md#%s)",
                linkText, index().rulesVaultRoot(), rules, toAnchorTag(heading));
    }

    default String linkify(MatchResult match) {
        Tools5eIndexType type = Tools5eIndexType.fromText(match.group(1));
        if (type == null) {
            throw new IllegalArgumentException("Unable to linkify " + match.group(0));
        }
        return linkify(type, match.group(2));
    }

    default String linkify(Tools5eIndexType type, String s) {
        return switch (type) {
            // {@background Charlatan} assumes PHB by default,
            // {@background Anthropologist|toa} can have sources added with a pipe,
            // {@background Anthropologist|ToA|and optional link text added with another pipe}.",
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
            // {@optfeature Aspect of the Moon|xge|and optional link text added with another pipe}.",
            // {@race Human} assumes PHB by default,
            // {@race Aasimar (Fallen)|VGM}
            // {@race Aasimar|DMG|racial traits for the aasimar}
            // {@race Aarakocra|eepc} can have sources added with a pipe,
            // {@race Aarakocra|eepc|and optional link text added with another pipe}.",
            // {@race dwarf (hill)||Dwarf, hill}
            // {@reward Blessing of Health} assumes DMG by default,
            // {@reward Blessing of Health} can have sources added with a pipe,
            // {@reward Blessing of Health|DMG|and optional link text added with another pipe}.",
            // {@spell acid splash} assumes PHB by default,
            // {@spell tiny servant|xge} can have sources added with a pipe,
            // {@spell tiny servant|xge|and optional link text added with another pipe}.",
            // {@table 25 gp Art Objects} assumes DMG by default,
            // {@table Adventuring Gear|phb} can have sources added with a pipe,
            // {@table Adventuring Gear|phb|and optional link text added with another pipe}.",
            // {@trap falling net} assumes DMG by default,
            // {@trap falling portcullis|xge} can have sources added with a pipe,
            // {@trap falling portcullis|xge|and optional link text added with another pipe}.",
            // {@vehicle Galley} assumes GoS by default,
            // {@vehicle Galley|UAOfShipsAndSea} can have sources added with a pipe,
            // {@vehicle Galley|GoS|and optional link text added with another pipe}.",
            case background,
                    feat,
                    deck,
                    hazard,
                    item,
                    legendaryGroup,
                    object,
                    optionalfeature,
                    race,
                    reward,
                    spell,
                    table,
                    trap,
                    vehicle ->
                linkifyType(type, s);
            case action -> linkifyRules(type, s, "actions");
            case condition, status -> linkifyRules(type, s, "conditions");
            case disease -> linkifyRules(type, s, "diseases");
            case sense -> linkifyRules(type, s, "senses");
            case skill -> linkifyRules(type, s, "skills");
            case monster -> linkifyCreature(s);
            case subclass, classtype -> linkifyClass(s);
            case deity -> linkifyDeity(s);
            case card -> linkifyCardType(s);
            case classfeature -> linkifyClassFeature(s);
            case subclassFeature -> linkifySubclassFeature(s);
            case variantrule -> linkifyVariant(s);
            default -> throw new IllegalArgumentException("Unknown type to linkify: " + type);
        };
    }

    default String linkOrText(String linkText, String key, String dirName, String resourceName) {
        return index().isIncluded(key)
                ? String.format("[%s](%s%s/%s.md)",
                        linkText, index().compendiumVaultRoot(), dirName, slugify(resourceName)
                                .replace("-dmg-dmg", "-dmg")) // bad combo for some race names
                : linkText;
    }

    default String linkifyType(Tools5eIndexType type, String match) {
        String[] parts = match.split("\\|");
        String source = parts.length > 1 && !parts[1].isBlank() ? parts[1] : type.defaultSourceString();
        String linkText = parts.length > 2 ? parts[2] : parts[0];

        String key = index().getAliasOrDefault(type.createKey(parts[0], source));
        return linkifyType(type, key, linkText);
    }

    default String linkifyType(Tools5eIndexType type, String key, String linkText) {
        String dirName = type.getRelativePath();
        JsonNode jsonSource = index().getNode(key);
        if (index().isExcluded(key) || jsonSource == null) {
            return linkText;
        }
        Tools5eSources linkSource = Tools5eSources.findSources(jsonSource);
        return linkOrText(linkText, key, dirName,
                type.decoratedName(jsonSource) + Tools5eQuteBase.sourceIfNotDefault(linkSource));
    }

    default String linkifyCardType(String match) {
        String dirName = Tools5eIndexType.card.getRelativePath();
        // {@card The Fates|Deck of Many Things}
        // {@card Donjon|Deck of Several Things|LLK}
        String[] parts = match.split("\\|");
        String cardName = parts[0];
        String deckName = parts[1];
        String source = parts.length < 3 || parts[2].isBlank() ? Tools5eIndexType.card.defaultSourceString() : parts[2];

        String key = index().getAliasOrDefault(Tools5eIndexType.deck.createKey(deckName, source));
        if (index().isExcluded(key)) {
            return cardName;
        }
        String resource = slugify(deckName + Tools5eQuteBase.sourceIfNotDefault(source, Tools5eIndexType.card));
        return String.format("[%s](%s%s/%s.md#%s)", cardName,
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
        String source = "phb";
        String linkText = deity;
        String pantheon = "Faerûnian";

        if (parts.length > 3) {
            linkText = parts[3];
        }
        if (parts.length > 2) {
            source = parts[2];
        }
        if (parts.length > 1) {
            pantheon = parts[1];
        }
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
        // {@class fighter|phb|and class feature added|Eldritch Knight|phb|2-0} with another pipe
        // {@class Barbarian|phb|Path of the Ancestral Guardian|Ancestral Guardian|xge}
        // {@class Fighter|phb|Samurai|Samurai|xge}
        String[] parts = match.split("\\|");
        String className = parts[0];
        String classSource = parts.length < 2 || parts[1].isEmpty() ? "phb" : parts[1];
        String linkText = parts.length < 3 || parts[2].isEmpty() ? className : parts[2];
        String subclass = parts.length < 4 || parts[3].isEmpty() ? null : parts[3];
        String subclassSource = parts.length < 5 || parts[4].isEmpty() ? classSource : parts[4];

        String relativePath = Tools5eIndexType.classtype.getRelativePath();
        if (subclass != null) {
            String key = index()
                    .getAliasOrDefault(Tools5eIndexType.getSubclassKey(className, classSource, subclass, subclassSource));
            // "subclass|path of wild magic|barbarian|phb|"
            int first = key.indexOf('|');
            int second = key.indexOf('|', first + 1);
            subclass = key.substring(first + 1, second);
            return linkOrText(linkText, key, relativePath,
                    Tools5eQuteBase.getSubclassResource(subclass, className, subclassSource));
        } else {
            String key = index().getAliasOrDefault(Tools5eIndexType.classtype.createKey(className, classSource));
            return linkOrText(linkText, key, relativePath,
                    Tools5eQuteBase.getClassResource(className, classSource));
        }
    }

    default String linkifyClassFeature(String match) {
        // "Class Features: Class source is assumed to be PHB, class feature source is assumed to be the same as class source"
        // {@classFeature Rage|Barbarian||1},
        // {@classFeature Infuse Item|Artificer|TCE|2},
        // {@classFeature Survival Instincts|Barbarian||2|UAClassFeatureVariants},
        // {@classFeature Rage|Barbarian||1||optional display text}.
        String[] parts = match.split("\\|");
        String linkText = parts[0];
        String className = parts[1];
        String classSource = parts[2].isBlank() ? "phb" : parts[2];
        String level = parts[3];
        if (parts.length > 5) {
            linkText = parts[5];
            int pos = match.lastIndexOf("|");
            match = match.substring(0, pos);
        }
        String classFeatureKey = index().getAliasOrDefault(Tools5eIndexType.classfeature.fromRawKey(match));
        if (index().isExcluded(classFeatureKey)) {
            return linkText;
        }

        JsonNode featureJson = index().getNode(classFeatureKey);
        Tools5eSources featureSources = Tools5eSources.findSources(classFeatureKey);

        String headerName = decoratedFeatureTypeName(featureSources, featureJson) + " (Level " + level + ")";
        String resource = Tools5eQuteBase.getClassResource(className, classSource);

        String relativePath = Tools5eIndexType.classtype.getRelativePath();
        return String.format("[%s](%s%s/%s.md#%s)", linkText,
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

        OptionalFeatureType oft = index().getOptionalFeatureTypes(featureType, featureSource);
        if (oft == null) {
            return linkText;
        }
        return linkOrText(linkText, oft.getKey(),
                Tools5eIndexType.optionalFeatureTypes.getRelativePath(),
                oft.getFilename());
    }

    default String linkifySubclassFeature(String match) {
        //"Subclass Features:
        // {@subclassFeature Path of the Berserker|Barbarian||Berserker||3},
        // {@subclassFeature Alchemist|Artificer|TCE|Alchemist|TCE|3},
        // {@subclassFeature Path of the Battlerager|Barbarian||Battlerager|SCAG|3},  --> "barbarian-path-of-the-... "
        // {@subclassFeature Blessed Strikes|Cleric||Life||8|UAClassFeatureVariants}, --> "-domain"
        // {@subclassFeature Blessed Strikes|Cleric|PHB|Twilight|TCE|8|TCE}
        // {@subclassFeature Path of the Berserker|Barbarian||Berserker||3||optional display text}.
        // Class source is assumed to be PHB.
        // Subclass source is assumed to be PHB.
        // Subclass feature source is assumed to be the same as subclass source.",

        String[] parts = match.split("\\|");
        String linkText = parts[0];
        String className = parts[1];
        String classSource = parts[2].isBlank() ? "phb" : parts[2];
        String subclass = parts[3];
        String subclassSource = parts[4].isBlank() ? classSource : parts[4];
        String level = parts[5];

        if (parts.length > 7) {
            linkText = parts[7];
            int pos = match.lastIndexOf("|");
            match = match.substring(0, pos);
        }

        String classFeatureKey = index().getAliasOrDefault(Tools5eIndexType.subclassFeature.fromRawKey(match));
        if (index().isExcluded(classFeatureKey)) {
            return linkText;
        }

        // look up alias for subclass so link is correct, e.g.
        // "subclass|redemption|paladin|phb|" : "subclass|oath of redemption|paladin|phb|",
        // "subclass|twilight|cleric|phb|tce"    : "subclass|twilight domain|cleric|phb|tce"
        String subclassKey = index()
                .getAliasOrDefault(Tools5eIndexType.getSubclassKey(className, classSource, subclass, subclassSource));
        int first = subclassKey.indexOf('|');
        subclass = subclassKey.substring(first + 1, subclassKey.indexOf('|', first + 1));

        JsonNode featureJson = index().getNode(classFeatureKey);
        Tools5eSources featureSources = Tools5eSources.findSources(classFeatureKey);
        String headerName = decoratedFeatureTypeName(featureSources, featureJson) + " (Level " + level + ")";

        String resource = slugify(Tools5eQuteBase.getSubclassResource(subclass, className, subclassSource));

        String relativePath = Tools5eIndexType.classtype.getRelativePath();
        return String.format("[%s](%s%s/%s.md#%s)",
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
        String source = parts.length > 1 && !parts[1].isBlank() ? parts[1] : indexType.defaultSourceString();
        String linkText = parts.length > 2 ? parts[2] : parts[0];

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
        return linkOrText(linkText, key, Tools5eQuteBase.monsterPath(isNpc, creatureType),
                resourceName + Tools5eQuteBase.sourceIfNotDefault(sources));
    }

    default String linkifyVariant(String variant) {
        // "fromVariant": "Action Options",
        // "fromVariant": "Spellcasting|XGE",
        String[] parts = variant.trim().split("\\|");
        String source = parts.length > 1 ? parts[1] : Tools5eIndexType.variantrule.defaultSourceString();
        if (!index().sourceIncluded(source)) {
            return variant + " from " + TtrpgConfig.sourceToLongName(source);
        } else {
            return String.format("[%s](%svariant-rules/%s.md)",
                    parts[0], index().rulesVaultRoot(),
                    slugify(parts[0]) + Tools5eQuteBase.sourceIfNotDefault(source, Tools5eIndexType.variantrule));
        }
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
        return String.format("[%s](#%s)", annotation, blockRef);
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
            tui().warn("Monster: Empty type for " + getSources());
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
