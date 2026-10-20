package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.ConfiguratorUtil;
import dev.ebullient.convert.io.Tui;

public class NumberFormatTest implements JsonSource {

    Tui tui = new Tui();
    CompendiumConfig config = ConfiguratorUtil.createNewConfig(tui);

    Tools5eIndex index = new Tools5eIndex(config) {
        @Override
        public boolean isIncluded(String name) {
            return true;
        }
    };

    @Test
    public void testNumberToTextExistingIntBehaviorUnchanged() {
        // Existing numberToText(int) callers (Json2QuteMonster, Json2QuteDeck, articleFor) must see no change.
        assertThat(numberToText(0)).isEqualTo("zero");
        assertThat(numberToText(1)).isEqualTo("one");
        assertThat(numberToText(20)).isEqualTo("twenty");
        assertThat(numberToText(21)).isEqualTo("21");
        assertThat(numberToText(99)).isEqualTo("99");
        assertThat(numberToText(-5)).isEqualTo("negative five");
        assertThat(numberToText(150)).isEqualTo("150");
    }

    @Test
    public void testArticleForUnchanged() {
        assertThat(articleFor("1 egg")).isEqualTo("an");
        assertThat(articleFor("2 eggs")).isEqualTo("a");
    }

    @Test
    public void testNumberToTextDouble() {
        assertThat(numberToText(0.0)).isEqualTo("zero");
        assertThat(numberToText(1.0)).isEqualTo("one");
        assertThat(numberToText(20.0)).isEqualTo("twenty");
        assertThat(numberToText(21.0)).isEqualTo("twenty-one");
        assertThat(numberToText(30.0)).isEqualTo("thirty");
        assertThat(numberToText(99.0)).isEqualTo("ninety-nine");
        assertThat(numberToText(-5.0)).isEqualTo("negative five");
        assertThat(numberToText(0.5)).isEqualTo("one-half");
        assertThat(numberToText(1.5)).isEqualTo("one and one-half");
        // 1/3-equivalent decimal via tolerance matching
        assertThat(numberToText(1.0 / 3)).isEqualTo("one-third");
        assertThat(numberToText(150.0)).isEqualTo("150");
    }

    @Test
    public void testNumberToVulgar() {
        assertThat(numberToVulgar(5.0)).isEqualTo("5");
        assertThat(numberToVulgar(0.125)).isEqualTo("⅛");
        assertThat(numberToVulgar(0.25)).isEqualTo("¼");
        assertThat(numberToVulgar(0.375)).isEqualTo("⅜");
        assertThat(numberToVulgar(0.5)).isEqualTo("½");
        assertThat(numberToVulgar(0.625)).isEqualTo("⅝");
        assertThat(numberToVulgar(0.75)).isEqualTo("¾");
        assertThat(numberToVulgar(0.875)).isEqualTo("⅞");
        assertThat(numberToVulgar(0.2)).isEqualTo("⅕");
        assertThat(numberToVulgar(0.4)).isEqualTo("⅖");
        assertThat(numberToVulgar(0.6)).isEqualTo("⅗");
        assertThat(numberToVulgar(0.8)).isEqualTo("⅘");
        // thirds/sixths via tolerance match
        assertThat(numberToVulgar(1.0 / 3)).isEqualTo("⅓");
        assertThat(numberToVulgar(2.0 / 3)).isEqualTo("⅔");
        assertThat(numberToVulgar(1.0 / 6)).isEqualTo("⅙");
        assertThat(numberToVulgar(5.0 / 6)).isEqualTo("⅚");
        // mixed whole+fraction
        assertThat(numberToVulgar(1.5)).isEqualTo("1½");
        // negative values
        assertThat(numberToVulgar(-0.5)).isEqualTo("-½");
        assertThat(numberToVulgar(-1.5)).isEqualTo("-1½");
        // unrecognized fraction falls back to plain decimal
        assertThat(numberToVulgar(0.1)).isEqualTo("0.1");
    }

    @Test
    public void testResolveAmountModifiersIndividualCodes() {
        Map<String, Double> amounts = Map.of("amount1", 1.5, "amount2", 0.33, "amount3", 21.0);

        assertThat(resolveAmountModifiers("amount1", "r", amounts)).isEqualTo("2");
        assertThat(resolveAmountModifiers("amount1", "f", amounts)).isEqualTo("1");
        assertThat(resolveAmountModifiers("amount1", "c", amounts)).isEqualTo("2");
        assertThat(resolveAmountModifiers("amount2", "v", amounts)).isEqualTo("⅓");
        assertThat(resolveAmountModifiers("amount3", "x", amounts)).isEqualTo("twenty-one");
        assertThat(resolveAmountModifiers("amount3", "xl", amounts)).isEqualTo("twenty-one");
        assertThat(resolveAmountModifiers("amount3", "xt", amounts)).isEqualTo("Twenty-one");
        assertThat(resolveAmountModifiers("amount3", "xu", amounts)).isEqualTo("TWENTY-ONE");
        assertThat(resolveAmountModifiers("amount3", "xa", amounts)).isEqualTo("a");
    }

    @Test
    public void testResolveAmountModifiersMultiModifier() {
        // {=amount1/cxt} with amount1: 1.5 -> ceiling to 2 -> "two" -> title-case "Two"
        Map<String, Double> amounts = Map.of("amount1", 1.5);
        assertThat(resolveAmountModifiers("amount1", "cxt", amounts)).isEqualTo("Two");

        // {=amount2/xt} with amount2: 6 -> "six" -> title-case "Six"
        Map<String, Double> amounts2 = Map.of("amount2", 6.0);
        assertThat(resolveAmountModifiers("amount2", "xt", amounts2)).isEqualTo("Six");
    }

    @Test
    public void testResolveAmountModifiersNoModifiers() {
        Map<String, Double> amounts = Map.of("amount1", 6.0);
        assertThat(resolveAmountModifiers("amount1", "", amounts)).isEqualTo("6");
    }

    @Test
    public void testResolveAmountModifiersMissingValue() {
        Map<String, Double> amounts = Map.of("amount1", 1.0);
        assertThatThrownBy(() -> resolveAmountModifiers("amount2", "x", amounts))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void testUnitTagWithEmbeddedAmountModifier() {
        // {@unit {=amount1/v}|egg|eggs}: the {=amount1/v} token is resolved first (as the
        // ingredient pre-pass does), then the {@unit} tag selects singular/plural on the result.
        Map<String, Double> amounts = Map.of("amount1", 1.0);
        String resolvedAmount = resolveAmountModifiers("amount1", "v", amounts);
        assertThat(replaceText("{@unit " + resolvedAmount + "|egg|eggs}")).isEqualTo("egg");

        Map<String, Double> amounts6 = Map.of("amount1", 6.0);
        String resolvedAmount6 = resolveAmountModifiers("amount1", "v", amounts6);
        assertThat(replaceText("{@unit " + resolvedAmount6 + "|egg|eggs}")).isEqualTo("eggs");
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
