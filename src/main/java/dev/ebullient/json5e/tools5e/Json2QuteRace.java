package dev.ebullient.json5e.tools5e;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.json5e.qute.QuteRace;

public class Json2QuteRace extends Json2QuteCommon {

    Json2QuteRace(JsonIndex index, IndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    public QuteRace build() {
        String name = decoratedRaceName();
        List<String> tags = new ArrayList<>(sources.getSourceTags());

        String[] split = name.split("\\(");
        for (int i = 0; i < split.length; i++) {
            split[i] = slugify(split[i].trim());
        }
        tags.add("race/" + String.join("/", split));

        return new QuteRace(
                name,
                sources.getSourceText(),
                getRaceAbility(),
                creatureTypes(),
                getSize(node),
                getSpeed(node),
                spellcastingAbility(),
                getText("###"),
                getFluffDescription(IndexType.racefluff, "###"),
                tags);
    }

    String decoratedRaceName() {
        String raceName = getSources().getName();
        JsonNode raceNameNode = node.get("raceName");
        if (raceNameNode != null) {
            raceName = String.format("%s (%s)", raceNameNode.asText(), raceName);
        }
        return decoratedTypeName(raceName.replace("Variant; ", ""), getSources());
    }

    String creatureTypes() {
        List<String> types = new ArrayList<>();
        node.withArray("creatureTypes").elements()
                .forEachRemaining(x -> types.add(x.asText()));
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

    public static Stream<Map.Entry<String, JsonNode>> findRaceVariants(JsonIndex index, IndexType type,
            String key, JsonNode jsonSource) {
        Map<String, JsonNode> variants = new HashMap<>();
        variants.put(key, jsonSource);
        CompendiumSources sources = index.constructSources(type, jsonSource);
        index.subraces(sources).forEach(sr -> {
            CompendiumSources srSources = index.constructSources(IndexType.subrace, sr);
            variants.put(srSources.getKey(), sr);
        });
        return variants.entrySet().stream();
    }
}
