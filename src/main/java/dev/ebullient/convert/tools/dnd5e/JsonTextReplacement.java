package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.NodeReader;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndexType.IndexFields;
import dev.ebullient.convert.tools.dnd5e.qute.QuteSource;

public interface JsonTextReplacement extends NodeReader.Converter<Tools5eIndexType> {

    Pattern linkifyPattern = Pattern.compile(
            "\\{@(background|class|deity|feat|card|deck|item|race|spell|creature|optfeature|classFeature|subclassFeature) ([^}]+)}");

    Pattern dicePattern = Pattern.compile("\\{@(dice|damage) ([^}]+)}");

    Pattern chancePattern = Pattern.compile("\\{@chance ([^}]+)}");
    Pattern quickRefPattern = Pattern.compile("\\{@quickref ([^}]+)}");
    Pattern notePattern = Pattern.compile("\\{@note (\\*|Note:)?\\s?([^}]+)}");
    Pattern condPattern = Pattern.compile("\\{@condition ([^|}]+)\\|?[^}]*}");
    Pattern statusPattern = Pattern.compile("\\{@status ([^|}]+)\\|?[^}]*}");
    Pattern diseasePattern = Pattern.compile("\\{@disease ([^|}]+)\\|?[^}]*}");
    Pattern skillPattern = Pattern.compile("\\{@skill ([^}]+)}");
    Pattern skillCheckPattern = Pattern.compile("\\{@skillCheck ([^}]+) ([^}]+)}"); // {@skillCheck animal_handling 5}
    Pattern sensePattern = Pattern.compile("\\{@sense ([^}]+)}");

    Pattern footnoteReference = Pattern.compile("\\{@sup ([^}]+)}");

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
        if (input == null || input.isEmpty()) {
            return input;
        }

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

            result = dicePattern.matcher(result).replaceAll((match) -> {
                String[] parts = match.group(2).split("\\|");
                if (parts.length > 1) {
                    return parts[1];
                }
                return formatDice(parts[0]);
            });

            result = chancePattern.matcher(result)
                    .replaceAll((match) -> match.group(1) + "% chance");

            result = condPattern.matcher(result)
                    .replaceAll((match) -> linkifyRules(match.group(1), "conditions"));

            result = statusPattern.matcher(result)
                    .replaceAll((match) -> linkifyRules(match.group(1), "status"));

            result = diseasePattern.matcher(result)
                    .replaceAll((match) -> linkifyRules(match.group(1), "diseases"));

            result = sensePattern.matcher(result).replaceAll((match) -> {
                String[] parts = match.group(1).split("\\|");
                return linkifyRules(parts[0], "senses");
            });

            result = skillPattern.matcher(result)
                    .replaceAll((match) -> linkifyRules(match.group(1), "skills"));

            result = skillCheckPattern.matcher(result).replaceAll((match) -> {
                SkillOrAbility skill = SkillOrAbility.fromTextValue(match.group(1));
                return linkifyRules(skill.value(), "skills");
            });

            result = footnoteReference.matcher(result)
                    .replaceAll(this::replaceFootnoteReference);

            result = linkifyPattern.matcher(result)
                    .replaceAll(this::linkify);

            result = quickRefPattern.matcher(result)
                    .replaceAll((match) -> {
                        String[] parts = match.group(1).split("\\|");
                        if (parts.length > 4) {
                            return parts[4];
                        }
                        return parts[0];
                    });

            try {
                result = result
                        .replace("{@hitYourSpellAttack}", "the summoner's spell attack modifier")
                        .replaceAll("\\{@link ([^}|]+)\\|([^}]+)}", "$1 ($2)") // this must come first
                        .replaceAll("\\{@5etools ([^}|]+)\\|?[^}]*}", "$1")
                        .replaceAll("\\{@area ([^|}]+)\\|?[^}]*}", "$1")
                        .replaceAll("\\{@action ([^}]+)\\|?[^}]*}", "$1")
                        .replaceAll("\\{@hazard ([^|}]+)\\|?[^}]*}", "$1")
                        .replaceAll("\\{@reward ([^|}]+)\\|?[^}]*}", "$1")
                        .replaceAll("\\{@vehupgrade ([^|}]+)\\|?[^}]*}", "$1")
                        .replaceAll("\\{@vehicle ([^|}]+)\\|?[^}]*}", "$1")
                        .replaceAll("\\{@dc ([^}]+)}", "DC $1")
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
                        .replaceAll("\\{@atk m}", "*Melee Attack:*")
                        .replaceAll("\\{@atk mw}", "*Melee Weapon Attack:*")
                        .replaceAll("\\{@atk rw}", "*Ranged Weapon Attack:*")
                        .replaceAll("\\{@atk mw,rw}", "*Melee or Ranged Weapon Attack:*")
                        .replaceAll("\\{@atk ms}", "*Melee Spell Attack:*")
                        .replaceAll("\\{@atk rs}", "*Ranged Spell Attack:*")
                        .replaceAll("\\{@atk ms,rs}", "*Melee or Ranged Spell Attack:*")
                        .replaceAll("\\{@b ([^}]+?)}", "**$1**")
                        .replaceAll("\\{@bold ([^}]+?)}", "**$1**")
                        .replaceAll("\\{@i ([^}]+?)}", "_$1_")
                        .replaceAll("\\{@italic ([^}]+)}", "_$1_");
            } catch (Exception e) {
                tui().errorf(e, "Unable to parse string from %s: %s", getSources().getKey(), input);
            }
            result = linkifyPattern.matcher(result)
                    .replaceAll(this::linkify);

            result = notePattern.matcher(result)
                    .replaceAll((match) -> {
                        List<String> text = new ArrayList<>();
                        text.add("> [!note]");
                        for (String line : match.group(2).split("\n")) {
                            text.add("> " + line);
                        }
                        return String.join("\n", text);
                    });

            // after other replacements
            return result.replaceAll("\\{@adventure ([^|}]+)\\|[^}]*}", "$1");

        } catch (IllegalArgumentException e) {
            tui().errorf(e, "Failure replacing text: %s", e.getMessage());
        }

        return input;
    }

    default String replaceFootnoteReference(MatchResult match) {
        return String.format("[^%s]%s", match.group(1),
                parseState.inFootnotes() ? ": " : "");
    }

    default String linkifyRules(String text, String rules) {
        return String.format("[%s](%s%s.md#%s)",
                text, index().rulesVaultRoot(), rules, toAnchorTag(text));
    }

    default String linkify(Tools5eIndexType type, String s) {
        throw new UnsupportedOperationException("Unimplemented method 'linkify'");
    }

    default String linkify(MatchResult match) {
        String matchText = match.group(2);
        switch (match.group(1)) {
            case "background":
                // "Backgrounds:
                // {@background Charlatan} assumes PHB by default,
                // {@background Anthropologist|toa} can have sources added with a pipe,
                // {@background Anthropologist|ToA|and optional link text added with another
                // pipe}.",
                return linkifyType(Tools5eIndexType.background, matchText, QuteSource.BACKGROUND_PATH);
            case "creature":
                // "Creatures:
                // {@creature goblin} assumes MM by default,
                // {@creature cow|vgm} can have sources added with a pipe,
                // {@creature cow|vgm|and optional link text added with another pipe}.",
                return linkifyCreature(matchText);
            case "class":
                return linkifyClass(matchText);
            case "deity":
                return linkifyDeity(matchText);
            case "feat":
                // "Feats:
                // {@feat Alert} assumes PHB by default,
                // {@feat Elven Accuracy|xge} can have sources added with a pipe,
                // {@feat Elven Accuracy|xge|and optional link text added with another pipe}.",
                return linkifyType(Tools5eIndexType.feat, matchText, QuteSource.FEATS_PATH);
            case "card":
                // {@card The Fates|Deck of Many Things}
                // {@card Donjon|Deck of Several Things|LLK}
                return linkifyCardType(matchText, QuteSource.ITEMS_PATH, "dmg");
            case "deck":
                // {@deck Tarokka Deck|CoS|tarokka deck} // like items
            case "item":
                // "Items:
                // {@item alchemy jug} assumes DMG by default,
                // {@item longsword|phb} can have sources added with a pipe,
                // {@item longsword|phb|and optional link text added with another pipe}.",
                return linkifyType(Tools5eIndexType.item, matchText, QuteSource.ITEMS_PATH, "dmg");
            case "race":
                // "Races:
                // {@race Human} assumes PHB by default,
                // {@race Aasimar (Fallen)|VGM}
                // {@race Aasimar|DMG|racial traits for the aasimar}
                // {@race Aarakocra|eepc} can have sources added with a pipe,
                // {@race Aarakocra|eepc|and optional link text added with another pipe}.",
                // {@race dwarf (hill)||Dwarf, hill}
                return linkifyType(Tools5eIndexType.race, matchText, QuteSource.RACES_PATH);
            case "spell":
                // "Spells:
                // {@spell acid splash} assumes PHB by default,
                // {@spell tiny servant|xge} can have sources added with a pipe,
                // {@spell tiny servant|xge|and optional link text added with another pipe}.",
                return linkifyType(Tools5eIndexType.spell, matchText, QuteSource.SPELLS_PATH);
            case "optfeature":
                return linkifyOptionalFeature(matchText);
            case "classFeature":
                return linkifyClassFeature(matchText);
            case "subclassFeature":
                return linkifySubclassFeature(matchText);
        }
        throw new IllegalArgumentException("Unknown group to linkify: " + match.group(1));
    }

    default String linkOrText(String linkText, String key, String dirName, String resourceName) {
        return index().isIncluded(key)
                ? String.format("[%s](%s%s/%s.md)",
                        linkText, index().compendiumVaultRoot(), dirName, slugify(resourceName)
                                .replace("-dmg-dmg", "-dmg")) // bad combo for some race names
                : linkText;
    }

    default String linkifyType(Tools5eIndexType type, String match, String dirName) {
        return linkifyType(type, match, dirName, "phb");
    }

    default String linkifyType(Tools5eIndexType type, String match, String dirName, String defaultSource) {
        String[] parts = match.split("\\|");
        String linkText = parts[0];
        String source = defaultSource;
        if (parts.length > 2) {
            linkText = parts[2];
        }
        if (parts.length > 1) {
            source = parts[1].isBlank() ? source : parts[1];
        }
        String key = index().getAliasOrDefault(type.createKey(parts[0], source));
        if (index().isExcluded(key)) {
            return linkText;
        }
        JsonNode jsonSource = index().getNode(key);
        Tools5eSources sources = Tools5eSources.findSources(key);
        if (type == Tools5eIndexType.background) {
            return linkOrText(linkText, key, dirName,
                    Json2QuteBackground.decoratedBackgroundName(parts[0])
                            + QuteSource.sourceIfNotCore(sources.primarySource()));
        } else if (type == Tools5eIndexType.item) {
            return linkOrText(linkText, key, dirName, parts[0] + QuteSource.sourceIfNotCore(sources.primarySource()));
        } else if (type == Tools5eIndexType.race) {
            return linkOrText(linkText, key, dirName,
                    decoratedRaceName(jsonSource, sources)
                            + QuteSource.sourceIfNotDefault(sources.primarySource(), defaultSource));
        }
        return linkOrText(linkText, key, dirName,
                decoratedTypeName(sources) + QuteSource.sourceIfNotCore(sources.primarySource()));
    }

    default String linkifyCardType(String match, String dirName, String defaultSource) {
        String[] parts = match.split("\\|");
        // {@card Donjon|Deck of Several Things|LLK}
        String cardName = parts[0];
        String deckName = parts[1];
        String source = parts.length < 3 || parts[2].isBlank() ? defaultSource : parts[2];

        String key = index().getAliasOrDefault(Tools5eIndexType.item.createKey(deckName, source));
        if (index().isExcluded(key)) {
            return cardName;
        }
        String resource = slugify(deckName + QuteSource.sourceIfNotCore(source));
        return String.format("[%s](%s%s/%s.md#%s)", cardName,
                index().compendiumVaultRoot(), dirName, resource, cardName.replace(" ", "%20"));
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
        return linkOrText(linkText, key, QuteSource.DEITIES_PATH, QuteSource.getDeityResourceName(parts[0], pantheon));
    }

    default String linkifyClass(String match) {
        // "Classes:
        // {@class fighter} assumes PHB by default,
        // {@class artificer|uaartificer} can have sources added with a pipe,
        // {@class fighter|phb|optional link text added with another pipe},
        // {@class fighter|phb|subclasses added|Eldritch Knight} with another pipe,
        // {@class fighter|phb|and class feature added|Eldritch Knight|phb|2-0} with another pipe
        // (first number is level index (0-19), second number is feature index (0-n)).",
        // {@class Barbarian|phb|Path of the Ancestral Guardian|Ancestral Guardian|xge}
        // {@class Fighter|phb|Samurai|Samurai|xge}
        String[] parts = match.split("\\|");
        String className = parts[0];
        String classSource = parts.length < 2 || parts[1].isEmpty() ? "phb" : parts[1];
        String linkText = parts.length < 3 || parts[2].isEmpty() ? className : parts[2];
        String subclass = parts.length < 4 || parts[3].isEmpty() ? null : parts[3];
        String subclassSource = parts.length < 5 || parts[4].isEmpty() ? classSource : parts[4];

        if (subclass != null) {
            String key = index()
                    .getAliasOrDefault(Tools5eIndexType.getSubclassKey(className, classSource, subclass, subclassSource));
            // "subclass|path of wild magic|barbarian|phb|"
            int first = key.indexOf('|');
            int second = key.indexOf('|', first + 1);
            subclass = key.substring(first + 1, second);
            return linkOrText(linkText, key, QuteSource.CLASSES_PATH,
                    QuteSource.getSubclassResource(subclass, className, subclassSource));
        } else {
            String key = index().getAliasOrDefault(Tools5eIndexType.classtype.createKey(className, classSource));
            return linkOrText(linkText, key, QuteSource.CLASSES_PATH,
                    className + QuteSource.sourceIfNotCore(classSource));
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
        String resource = slugify(className + QuteSource.sourceIfNotCore(classSource));

        return String.format("[%s](%s%s/%s.md#%s)", linkText,
                index().compendiumVaultRoot(), QuteSource.CLASSES_PATH,
                resource, toAnchorTag(headerName));
    }

    default String linkifyOptionalFeature(String match) {
        // "Invocations and Other Optional Features:
        // {@optfeature Agonizing Blast} assumes PHB by default,
        // {@optfeature Aspect of the Moon|xge} can have sources added with a pipe,
        // {@optfeature Aspect of the Moon|xge|and optional link text added with another pipe}.",
        String[] parts = match.split("\\|");
        String linkText = parts[0];
        if (parts.length > 2) {
            linkText = parts[2];
            int pos = match.lastIndexOf("|");
            match = match.substring(0, pos);
        }
        String featureKey = index().getAliasOrDefault(Tools5eIndexType.optionalfeature.fromRawKey(match));
        if (index().isExcluded(featureKey)) {
            return linkText;
        }

        JsonNode featureJson = index().getNode(featureKey);
        Tools5eSources featureSources = Tools5eSources.findSources(featureKey);

        String featureType = getFirstValue(featureJson.get("featureType"));

        return String.format("[%s](%s%s/%s.md#%s)",
                linkText,
                index().compendiumVaultRoot(), QuteSource.CLASSES_PATH,
                featureTypeToClass(featureType),
                featureSources.getName().replace(" ", "%20"));
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

        String classFeatureKey = index().getAliasOrDefault(Tools5eIndexType.subclassfeature.fromRawKey(match));
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

        String resource = slugify(QuteSource.getSubclassResource(subclass, className, subclassSource));

        return String.format("[%s](%s%s/%s.md#%s)",
                linkText,
                index().compendiumVaultRoot(), QuteSource.CLASSES_PATH,
                resource,
                toAnchorTag(headerName));
    }

    default String linkifyCreature(String match) {
        // "Creatures:
        // {@creature goblin} assumes MM by default,
        // {@creature cow|vgm} can have sources added with a pipe,
        // {@creature cow|vgm|and optional link text added with another pipe}.",
        String[] parts = match.trim().split("\\|");
        String source = parts.length < 2 || parts[1].isBlank() ? "mm" : parts[1];
        String linkText = parts.length < 3 || parts[2].isBlank() ? parts[0] : parts[2];

        String key = index().getAliasOrDefault(Tools5eIndexType.monster.createKey(parts[0], source));
        if (index().isExcluded(key)) {
            return linkText;
        }
        JsonNode jsonSource = index().getNode(key);
        Tools5eSources sources = Tools5eSources.findSources(key);
        String resourceName = decoratedMonsterName(sources);
        String type = getMonsterType(jsonSource); // may be missing for partial index
        if (type == null) {
            return linkText;
        }
        boolean isNpc = Json2QuteMonster.isNpc(jsonSource);
        return linkOrText(linkText, key, QuteSource.monsterPath(isNpc, type),
                resourceName + QuteSource.sourceIfNotCore(sources.primarySource()));
    }

    default String decoratedRaceName(JsonNode jsonSource, Tools5eSources sources) {
        String raceName = sources.getName();
        JsonNode raceNameNode = jsonSource.get("raceName");
        if (raceNameNode != null) {
            raceName = String.format("%s (%s)", raceNameNode.asText(), raceName);
        }
        return decoratedTypeName(raceName.replace("Variant; ", ""), getSources());
    }

    default String decoratedMonsterName(Tools5eSources sources) {
        return sources.getName().replace("\"", "");
    }

    default String decoratedTypeName(Tools5eSources sources) {
        return decoratedTypeName(sources.getName(), sources);
    }

    default String decoratedTypeName(String name, Tools5eSources sources) {
        if (sources.isPrimarySource("DMG") && !name.contains("(DMG)")) {
            return name + " (DMG)";
        }
        return name;
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

    // Parser.OPT_FEATURE_TYPE_TO_FULL = {
    //     AI: "Artificer Infusion",
    //     ED: "Elemental Discipline",
    //     EI: "Eldritch Invocation",
    //     MM: "Metamagic",
    //     "MV": "Maneuver",
    //     "MV:B": "Maneuver, Battle Master",
    //     "MV:C2-UA": "Maneuver, Cavalier V2 (UA)",
    //     "AS:V1-UA": "Arcane Shot, V1 (UA)",
    //     "AS:V2-UA": "Arcane Shot, V2 (UA)",
    //     "AS": "Arcane Shot",
    //     OTH: "Other",
    //     "FS:F": "Fighting Style; Fighter",
    //     "FS:B": "Fighting Style; Bard",
    //     "FS:P": "Fighting Style; Paladin",
    //     "FS:R": "Fighting Style; Ranger",
    //     "PB": "Pact Boon",
    //     "OR": "Onomancy Resonant",
    //     "RN": "Rune Knight Rune",
    //     "AF": "Alchemical Formula",
    // };

    default String featureTypeToClass(String type) {
        switch (type) {
            case "AF":
                return "artificer-alchemist-uaartificer";
            case "AI":
                return "artificer-tce";
            case "ED":
                return "monk-way-of-the-four-elements";
            case "EI":
            case "PB":
                return "warlock";
            case "MM":
                return "sorcerer";
            case "AS":
            case "AS:V1-UA":
            case "AS:V2-UA":
                return "fighter-arcane-archer-xge";
            case "FS:F":
                return "fighter";
            case "MV":
            case "MV:B":
                return "fighter-battle-master";
            case "MV:C2-UA":
            case "RN":
                return "fighter-rune-knight-tce";
            case "FS:B":
                return "bard-college-of-swords";
            case "FS:R":
                return "ranger";
            case "FS:P":
                return "paladin";
            case "OR":
                return "wizard-onomancy-ua-uaclericdruidwizard";
            default:
                tui().errorf("Unknown class for feature type %s", type);
                return "unknown";
        }
    }
}
