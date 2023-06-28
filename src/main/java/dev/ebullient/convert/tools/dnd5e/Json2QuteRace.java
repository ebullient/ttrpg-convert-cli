package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndex.Tuple;
import dev.ebullient.convert.tools.dnd5e.qute.QuteRace;

public class Json2QuteRace extends Json2QuteCommon {

    Json2QuteRace(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    protected QuteRace buildQuteResource() {
        String name = decoratedRaceName(node, sources);
        Set<String> tags = new TreeSet<>(sources.getSourceTags());

        String[] split = name.split("\\(");
        for (int i = 0; i < split.length; i++) {
            split[i] = slugify(split[i].trim());
        }
        tags.add("race/" + String.join("/", split));

        List<ImageRef> fluffImages = new ArrayList<>();
        String fluff = getFluffDescription(Tools5eIndexType.racefluff, "###", fluffImages);

        return new QuteRace(sources,
                name,
                sources.getSourceText(index.srdOnly()),
                getRaceAbility(),
                creatureTypes(),
                getSize(node),
                getSpeed(node),
                spellcastingAbility(),
                getText("###"),
                fluff,
                fluffImages,
                tags);
    }

    String creatureTypes() {
        List<String> types = new ArrayList<>();
        for (JsonNode x : iterableElements(node.get("creatureTypes"))) {
            types.add(x.asText());
        }
        return types.isEmpty()
                ? null
                : String.join(", ", types);
    }

    String spellcastingAbility() {
        if (node.has("additionalSpells") && node.get("additionalSpells").isArray()) {
            JsonNode spells = node.get("additionalSpells").get(0);
            if (spells.has("ability")) {
                JsonNode ability = spells.get("ability");
                if (ability.has("choose")) {
                    List<String> abilities = new ArrayList<>();
                    ability.withArray("choose")
                            .forEach(x -> abilities.add(SkillOrAbility.format(x.asText())));
                    return "Choose one of " + String.join(", ", abilities);
                } else {
                    return asAbilityEnum(ability);
                }
            }
        }
        return null;
    }

    String getRaceAbility() {
        if (getTextOrEmpty(node, "lineage").equals("VRGR")) {
            if (getTextOrEmpty(node, "lineage").equals("VRGR")) {
                // Custom Lineage:
                return "Choose one of: (a) Choose any +2, choose any other +1; (b) Choose any +1, choose any other +1, choose any other +1";
            }
        }
        JsonNode ability = node.withArray("ability");
        if (ability.isEmpty()) {
            return "None";
        }

        List<String> list = new ArrayList<>();
        ability.elements().forEachRemaining(skillBonus -> {
            List<String> inner = new ArrayList<>();
            skillBonus.fields().forEachRemaining(f -> {
                if (f.getKey().equals("choose")) {
                    inner.add(skillChoices(f.getValue()));
                } else {
                    inner.add(String.format("%s %s", SkillOrAbility.format(f.getKey()), decoratedAmount(f.getValue().asInt())));
                }
            });
            list.add(String.join(", ", inner));
        });
        return String.join("; or ", list);
    }

    String skillChoices(JsonNode skillBonus) {
        int count = skillBonus.has("count")
                ? skillBonus.get("count").asInt()
                : 1;
        int amount = skillBonus.has("amount")
                ? skillBonus.get("amount").asInt()
                : 1;
        ArrayNode from = skillBonus.withArray("from");
        List<String> choices = new ArrayList<>();
        from.forEach(s -> choices.add(SkillOrAbility.format(s.asText())));
        return String.format("Apply %s to %s of %s",
                decoratedAmount(amount),
                count == 1 ? "one" : count + " (distinct)",
                String.join(", ", choices));
    }

    String decoratedAmount(int amount) {
        if (amount >= 0) {
            return "+" + amount;
        }
        return amount + "";
    }

    public static List<Tuple> findRaceVariants(Tools5eIndex index, Tools5eIndexType type,
            String key, JsonNode jsonSource, JsonSourceCopier copier) {

        List<Tuple> variants = new ArrayList<>();
        variants.add(new Tuple(key, jsonSource));
        // For each subrace derived from the origin...
        Tools5eSources sources = Tools5eSources.constructSources(jsonSource);
        index.originSubraces(sources).forEach(sr -> {
            JsonNode newNode = copier.handleCopy(type, sr);
            // TODO: create new key first?
            Tools5eSources srSources = Tools5eSources.constructSources(newNode);
            variants.add(new Tuple(srSources.getKey(), newNode));
        });
        return variants;
    }
}
