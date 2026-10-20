package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.Tags;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools recipe attributes ({@code recipe2md.txt})
 *
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase}.
 */
@TemplateData
public class QuteRecipe extends Tools5eQuteBase {

    /** Lines describing time categories (e.g. total/cooking/preparation), each rendered as plain minutes. */
    public final List<String> time;
    /** How much this recipe makes (exact or min-max count, with optional note), as formatted text. May be null. */
    public final String makes;
    /** How many this recipe serves (exact or min-max count, with optional note), as formatted text. May be null. */
    public final String serves;
    /** Ingredient list, rendered as a markdown bullet list with placeholders substituted. */
    public final String ingredients;
    /** Equipment list, rendered as a markdown bullet list with placeholders substituted. May be null. */
    public final String equipment;
    /** Cook's notes. May be empty. */
    public final List<String> noteCook;
    /** Instructions for preparing the recipe. */
    public final List<String> instructions;

    public QuteRecipe(CompendiumSources sources, String name, String source,
            List<ImageRef> images, String text, Tags tags,
            List<String> time, String makes, String serves,
            String ingredients, String equipment, List<String> noteCook, List<String> instructions) {
        super(sources, name, source, images, text, tags);
        withTemplate("recipe2md.txt");
        this.time = time;
        this.makes = makes;
        this.serves = serves;
        this.ingredients = ingredients;
        this.equipment = equipment;
        this.noteCook = noteCook;
        this.instructions = instructions;
    }
}
