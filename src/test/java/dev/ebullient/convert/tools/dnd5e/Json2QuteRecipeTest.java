package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.ConfiguratorUtil;
import dev.ebullient.convert.io.Tui;

public class Json2QuteRecipeTest implements JsonSource {

    Tui tui = new Tui();
    CompendiumConfig config = ConfiguratorUtil.createNewConfig(tui);

    Tools5eIndex index = new Tools5eIndex(config) {
        @Override
        public boolean isIncluded(String name) {
            return true;
        }
    };

    @Test
    public void testAppendIngredientResolvesPlaceholders() throws Exception {
        JsonNode node = Tui.MAPPER.readTree("""
                {
                  "type": "ingredient",
                  "entry": "{=amount1/c} garlic cloves, pressed or grated (about {=amount2/v} teaspoons)",
                  "amount1": 1.5,
                  "amount2": 1.5
                }
                """);

        List<String> text = new ArrayList<>();
        appendIngredient(text, node);

        assertThat(text).hasSize(1);
        assertThat(text.get(0)).isEqualTo("2 garlic cloves, pressed or grated (about 1½ teaspoons)");
        assertThat(text.get(0)).doesNotContain("{=");
    }

    @Test
    public void testAppendIngredientPlainAmountNoModifiers() throws Exception {
        JsonNode node = Tui.MAPPER.readTree("""
                {
                  "type": "ingredient",
                  "entry": "{=amount1} eggs",
                  "amount1": 6
                }
                """);

        List<String> text = new ArrayList<>();
        appendIngredient(text, node);

        assertThat(text).containsExactly("6 eggs");
    }

    @Test
    public void testAppendIngredientWithUnitTag() throws Exception {
        JsonNode node = Tui.MAPPER.readTree("""
                {
                  "type": "ingredient",
                  "entry": "{=amount1/c} large {@unit {=amount1/v}|egg|eggs}",
                  "amount1": 6
                }
                """);

        List<String> text = new ArrayList<>();
        appendIngredient(text, node);

        assertThat(text).containsExactly("6 large eggs");
    }

    @Test
    public void testAppendListNestedIngredientGroupHasNoBlankLines() throws Exception {
        // A captioned sub-group of ingredient entries (e.g. "Cinnamon Whipped Cream") should
        // render as a tight nested list, not blank-line-separated like prose paragraphs.
        ArrayNode ingredients = (ArrayNode) Tui.MAPPER.readTree("""
                [
                  {
                    "type": "entries",
                    "name": "Cinnamon Whipped Cream",
                    "entries": [
                      { "type": "ingredient", "entry": "{=amount1/v} cup heavy cream", "amount1": 0.5 },
                      { "type": "ingredient", "entry": "{=amount1/v} tablespoon sugar", "amount1": 1 }
                    ]
                  },
                  { "type": "ingredient", "entry": "{=amount1/v} cup dark rum", "amount1": 0.5 }
                ]
                """);

        List<String> text = new ArrayList<>();
        appendList(text, ingredients, ListType.unordered);

        assertThat(text).containsExactly(
                "- **Cinnamon Whipped Cream.** ½ cup heavy cream  ",
                "    1 tablespoon sugar  ",
                "- ½ cup dark rum  ");
    }

    @Test
    public void testAppendListIngredientGroupBlankLinesUnaffectedForNonIngredientEntries() throws Exception {
        // A captioned entries-group whose children are NOT ingredient-typed must keep its
        // existing (prose) blank-line behavior; the fix is scoped to ingredient groups only.
        ArrayNode items = (ArrayNode) Tui.MAPPER.readTree("""
                [
                  {
                    "type": "entries",
                    "name": "Notes",
                    "entries": [
                      "First paragraph.",
                      "Second paragraph."
                    ]
                  }
                ]
                """);

        List<String> text = new ArrayList<>();
        appendList(text, items, ListType.unordered);

        assertThat(text).containsExactly(
                "- **Notes.** First paragraph.  ",
                "",
                "    Second paragraph.  ");
    }

    @Override
    public Tools5eIndex index() {
        return index;
    }

    @Override
    public Tools5eSources getSources() {
        return Tools5eSources.findOrTemporary(Tui.MAPPER.createObjectNode());
    }
}
