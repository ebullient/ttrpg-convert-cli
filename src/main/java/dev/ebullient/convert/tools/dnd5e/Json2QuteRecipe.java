package dev.ebullient.convert.tools.dnd5e;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.StringUtil;
import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.QuteRecipe;

public class Json2QuteRecipe extends Json2QuteCommon {

    Json2QuteRecipe(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    protected QuteRecipe buildQuteResource() {
        Tags tags = new Tags(getSources());

        List<ImageRef> images = new ArrayList<>();
        List<String> text = getFluff(Tools5eIndexType.recipeFluff, "##", images);

        return new QuteRecipe(getSources(),
                getName(),
                getSourceText(getSources()),
                images,
                String.join("\n", text),
                tags,
                getTime(),
                getMakes(),
                getServes(),
                getIngredientList(RecipeFields.ingredients),
                getIngredientList(RecipeFields.equipment),
                getNoteCook(),
                getInstructions());
    }

    private String getIngredientList(RecipeFields field) {
        JsonNode arrayNode = field.getFrom(rootNode);
        if (arrayNode == null) {
            return null;
        }
        List<String> text = new ArrayList<>();
        appendList(text, field.readArrayFrom(rootNode), ListType.unordered);
        while (!text.isEmpty() && text.get(0).isBlank()) {
            text.remove(0);
        }
        return String.join("\n", text);
    }

    private List<String> getTime() {
        JsonNode timeNode = RecipeFields.time.getFrom(rootNode);
        if (timeNode == null) {
            return List.of();
        }
        List<String> time = new ArrayList<>();
        for (var field : timeNode.properties()) {
            time.add("**%s:** %s min".formatted(StringUtil.toTitleCase(field.getKey()), field.getValue().asText()));
        }
        return time;
    }

    private String getMakes() {
        JsonNode makesNode = RecipeFields.makes.getFrom(rootNode);
        return makesNode == null ? null : makesNode.asText();
    }

    private String getServes() {
        JsonNode servesNode = RecipeFields.serves.getFrom(rootNode);
        if (servesNode == null) {
            return null;
        }
        String count = servesNode.has("exact")
                ? servesNode.get("exact").asText()
                : "%s–%s".formatted(servesNode.get("min").asText(), servesNode.get("max").asText());
        String note = servesNode.has("note") ? replaceText(servesNode.get("note").asText()) : null;
        return note == null ? count : "%s (%s)".formatted(count, note);
    }

    private List<String> getNoteCook() {
        List<String> list = new ArrayList<>();
        appendToText(list, RecipeFields.noteCook.getFrom(rootNode), null);
        return list;
    }

    private List<String> getInstructions() {
        List<String> list = new ArrayList<>();
        appendToText(list, RecipeFields.instructions.getFrom(rootNode), null);
        return list;
    }

    enum RecipeFields implements JsonNodeReader {
        allergenGroups,
        diet,
        dishTypes,
        equipment,
        ingredients,
        instructions,
        makes,
        miscTags,
        noteCook,
        serves,
        time
    }
}
