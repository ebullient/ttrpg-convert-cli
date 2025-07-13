package dev.ebullient.convert.qute;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class TtrpgTemplateExtensionTest {

    @Test
    public void testTitleCase() {
        // Basic cases
        assertThat(TtrpgTemplateExtension.capitalized("hello world")).isEqualTo("Hello World");
        assertThat(TtrpgTemplateExtension.capitalized("**hello** world")).isEqualTo("**Hello** World");
        assertThat(TtrpgTemplateExtension.capitalized(null)).isNull();
        assertThat(TtrpgTemplateExtension.capitalized("")).isEmpty();

        // With markdown links
        assertThat(TtrpgTemplateExtension.capitalized("thing 1, [thing 2](uRl), And thing 3"))
                .isEqualTo("Thing 1, [Thing 2](uRl), and Thing 3");
    }

    @Test
    public void testCapitalizedList() {
        // With markdown links
        assertThat(TtrpgTemplateExtension
                .capitalizedList("thing 1, [thing 2](uRl), and thing 3; Other thing (with additional stuff)"))
                .isEqualTo("Thing 1, [Thing 2](uRl), and Thing 3; Other thing (with additional stuff)");
    }

    @Test
    public void testUppercaseFirst() {
        // Basic cases
        assertThat(TtrpgTemplateExtension.uppercaseFirst("hello world")).isEqualTo("Hello world");
        assertThat(TtrpgTemplateExtension.uppercaseFirst("**hello** world")).isEqualTo("**Hello** world");
        assertThat(TtrpgTemplateExtension.uppercaseFirst(null)).isNull();
        assertThat(TtrpgTemplateExtension.uppercaseFirst("")).isEmpty();

        // With markdown links, and no changes to the case of middle words..
        assertThat(TtrpgTemplateExtension.uppercaseFirst("thing 1, [thing 2](url), And thing 3"))
                .isEqualTo("Thing 1, [thing 2](url), And thing 3");

        assertThat(TtrpgTemplateExtension.uppercaseFirst("[thing 2](url), thing 1, And thing 3"))
                .isEqualTo("[Thing 2](url), thing 1, And thing 3");
    }

    @Test
    public void testLowercase() {
        // Basic cases
        assertThat(TtrpgTemplateExtension.lowercase("HELLO WORLD")).isEqualTo("hello world");
        assertThat(TtrpgTemplateExtension.lowercase(null)).isNull();
        assertThat(TtrpgTemplateExtension.lowercase("")).isEmpty();

        // With markdown links
        assertThat(TtrpgTemplateExtension.lowercase("THING 1, [THING 2](URL), And THING 3"))
                .isEqualTo("thing 1, [thing 2](URL), and thing 3");
    }

    @Test
    public void testAsBonus() {
        assertThat(TtrpgTemplateExtension.asBonus(5)).isEqualTo("+5");
        assertThat(TtrpgTemplateExtension.asBonus(-3)).isEqualTo("-3");
        assertThat(TtrpgTemplateExtension.asBonus(0)).isEqualTo("+0");
    }
}
