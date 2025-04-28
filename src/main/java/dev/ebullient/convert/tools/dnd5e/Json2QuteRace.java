package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.isPresent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.dnd5e.Json2QuteFeat.FeatFields;
import dev.ebullient.convert.tools.dnd5e.qute.QuteRace;

public class Json2QuteRace extends Json2QuteCommon {

    final static Pattern subraceNamePattern = Pattern.compile("^(.*?)\s*\\((.*?)\\)$");

    Json2QuteRace(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    protected QuteRace buildQuteResource() {
        if (RaceFields._rawName.existsIn(rootNode)) {
            tui().debugf("Skipping output of base race %s", sources.getKey());
            return null;
        }
        String name = linkifier().decoratedName(type, rootNode);
        Tags tags = new Tags(getSources());

        String[] split = name.split("\\(");
        for (int i = 0; i < split.length; i++) {
            split[i] = slugify(split[i].trim());
        }
        tags.addRaw("race", String.join("/", split));

        List<ImageRef> fluffImages = new ArrayList<>();
        String fluff = getFluffDescription(Tools5eIndexType.raceFluff, "###", fluffImages);

        return new QuteRace(sources,
                name,
                getSourceText(sources),
                getRaceAbility(),
                creatureTypes(),
                getSize(rootNode),
                getSpeed(rootNode),
                spellcastingAbility(),
                getText("###"),
                fluff,
                fluffImages,
                tags);
    }

    String getSpeed(JsonNode value) {
        JsonNode speed = value.get("speed");
        try {
            if (speed == null) {
                return "30 ft.";
            } else if (speed.isTextual()) {
                return speed.asText();
            } else if (speed.isIntegralNumber()) {
                return speed.asText() + " ft.";
            } else if (speed.isObject()) {
                List<String> list = new ArrayList<>();
                speed.fields().forEachRemaining(f -> {
                    if (f.getValue().isIntegralNumber()) {
                        list.add(String.format("%s: %s ft.",
                                f.getKey(), f.getValue().asText()));
                    } else if (f.getValue().isBoolean()) {
                        list.add(f.getKey() + " equal to your walking speed");
                    }
                });
                return String.join("; ", list);
            }
        } catch (IllegalArgumentException ignored) {
        }
        tui().errorf("Unable to parse speed for %s from %s", getSources(), speed);
        return "30 ft.";
    }

    String creatureTypes() {
        List<String> types = new ArrayList<>();
        for (JsonNode x : iterableElements(rootNode.get("creatureTypes"))) {
            types.add(x.asText());
        }
        return types.isEmpty()
                ? null
                : String.join(", ", types);
    }

    String spellcastingAbility() {
        if (rootNode.has("additionalSpells") && rootNode.get("additionalSpells").isArray()) {
            JsonNode spells = rootNode.get("additionalSpells").get(0);
            if (spells.has("ability")) {
                JsonNode ability = spells.get("ability");
                if (ability.has("choose")) {
                    List<String> abilities = new ArrayList<>();
                    ability.withArray("choose")
                            .forEach(x -> abilities.add(SkillOrAbility.format(x.asText(), index(), getSources())));
                    return "Choose one of " + String.join(", ", abilities);
                } else {
                    return asAbilityEnum(ability);
                }
            }
        }
        return null;
    }

    String getRaceAbility() {
        // TODO: render.js / handleAbilitiesChoose
        String lineage = RaceFields.lineage.getTextOrEmpty(rootNode);
        if (lineage.equals("VRGR")) {
            // Custom Lineage:
            return "Choose one of: (a) Choose any +2, choose any other +1; (b) Choose any +1, choose any other +1, choose any other +1";
        } else if (lineage.equals("UA1")) {
            // Custom Lineage:
            return "Choose any +2, choose any other +1";
        }

        String ability = SkillOrAbility.getAbilityScore(FeatFields.ability.getFrom(rootNode));
        return isPresent(ability)
                ? ability
                : "None";
    }

    String decoratedAmount(int amount) {
        if (amount >= 0) {
            return "+" + amount;
        }
        return amount + "";
    }

    public static String getSubraceName(String raceName, String subraceName) {
        if (subraceName == null) {
            return raceName;
        }
        Matcher m = subraceNamePattern.matcher(raceName);
        if (m.matches()) {
            raceName = m.group(1);
            subraceName = String.join("; ", List.of(m.group(2), subraceName));
        }
        return String.format("%s (%s)", raceName, subraceName);
    }

    // render.js: mergeSubraces
    public static void prepareBaseRace(Tools5eIndex tools5eIndex, JsonNode jsonSource, Set<JsonNode> subraces) {

        // fix size
        JsonNode size = Tools5eFields.size.getFrom(jsonSource);
        if (size != null && size.isTextual()) {
            Tools5eFields.size.removeFrom(jsonSource);
            Tools5eFields.size.ensureArrayIn(jsonSource).add(size.asText());
        }

        // fix lineage: handled at the moment by getRaceAbility()
        JsonNode lineageNode = RaceFields.lineage.getFrom(jsonSource);
        if (lineageNode != null && lineageNode.isTextual()) {
            String lineage = RaceFields.lineage.getTextOrThrow(jsonSource);

            if (!RaceFields.ability.existsIn(jsonSource)) {
                ArrayNode ability = RaceFields.ability.ensureArrayIn(jsonSource);
                ArrayNode abilities = ability.arrayNode();
                SkillOrAbility.allSkills.forEach(x -> abilities.add(x));

                ObjectNode choice = ability.objectNode();
                ObjectNode weighted = choice.objectNode();
                choice.set("choose", weighted);
                weighted.set("from", abilities);
                weighted.set("weights", weighted.arrayNode().add(2).add(1));

                if (lineage.equalsIgnoreCase("VRGR")) {
                    ability.add(tools5eIndex.copyNode(choice));
                    weighted.set("weights", weighted.arrayNode().add(1).add(1).add(1));
                    ability.add(choice);
                } else if (lineage.equalsIgnoreCase("UA1")) {
                    ability.add(choice);
                }
            }

            ArrayNode entries = SourceField.entries.ensureArrayIn(jsonSource);
            entries.add(entries.objectNode()
                    .put("type", "entries")
                    .put("name", "Languages")
                    .put("entries",
                            "You can speak, read, and write Common and one other language that you and your DM agree is appropriate for your character."));

            if (!RaceFields.languageProficiencies.existsIn(jsonSource)) {
                ArrayNode languageProficiencies = RaceFields.languageProficiencies.ensureArrayIn(jsonSource);
                languageProficiencies.add(languageProficiencies.objectNode()
                        .put("common", true)
                        .put("anyStandard", 1));
            }
        }
    }

    public static void updateBaseRace(Tools5eIndex tools5eIndex, JsonNode jsonSource, Set<JsonNode> inputSubraces,
            List<JsonNode> subraces) {

        if (!RaceFields._isBaseRace.existsIn(jsonSource)) {
            // If one of the original subraces was missing a name, it shares
            // the base race name. Update the base race name to differentiate
            // it from the subrace.
            if (inputSubraces.stream().anyMatch(x -> !SourceField.name.existsIn(x))) {
                String name = SourceField.name.getTextOrThrow(jsonSource);
                RaceFields._rawName.setIn(jsonSource, name);
                SourceField.name.setIn(jsonSource, name + " (Base)");
                tools5eIndex.addAlias(
                        Tools5eIndexType.race.createKey(jsonSource),
                        TtrpgValue.indexKey.getTextOrThrow(jsonSource));
            }

            // subraces.sort((a, b) -> {
            //     String aName = SourceField.name.getTextOrThrow(a);
            //     String bName = SourceField.name.getTextOrThrow(b);
            //     return aName.compareTo(bName);
            // });

            // ArrayNode entries = SourceField.entries.readArrayFrom(jsonSource);

            // ArrayNode subraceList = entries.arrayNode();
            // subraces.forEach(x -> subraceList.add(String.format("{@race %s|%s|%s (%s)}",
            //         SourceField.name.getTextOrThrow(x),
            //         SourceField.source.getTextOrThrow(x),
            //         SourceField.name.getTextOrThrow(x),
            //         SourceField.source.getTextOrThrow(x))));

            // ArrayNode sections = entries.arrayNode()
            //         .add(entries.objectNode()
            //                 .put("type", "section")
            //                 .set("entries", entries.arrayNode()
            //                         .add("This race has multiple subraces, as listed below:")
            //                         .add(entries.objectNode()
            //                                 .put("type", "list")
            //                                 .set("items", subraceList))))
            //         .add(entries.objectNode()
            //                 .put("type", "section")
            //                 .set("entries", entries.objectNode()
            //                         .put("type", "entries")
            //                         .put("name", "Traits")
            //                         .set("entries", entries)));

            // SourceField.entries.setIn(jsonSource, sections);
            RaceFields._isBaseRace.setIn(jsonSource, BooleanNode.TRUE);
        }
    }

    enum RaceFields implements JsonNodeReader {
        _isBaseRace,
        _isSubRace,
        _rawName,
        ability,
        additionalSpells,
        creatureTypes,
        languageProficiencies,
        skillProficiencies,
        lineage,
        race,
        speed,
        raceName,
        raceSource
    }
}
