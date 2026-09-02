package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.ConfiguratorUtil;
import dev.ebullient.convert.io.Tui;

/**
 * Regression test for https://github.com/ebullient/ttrpg-convert-cli/issues/938:
 * a {@code prefixSuffixStringProp} mod must wrap the existing target string value
 * (e.g. the copied hit dice {@code hp.formula}), not a (nonexistent) {@code value}
 * field on the mod itself.
 */
public class JsonSourceCopierPrefixSuffixStringPropTest {

    Tui tui = new Tui();
    CompendiumConfig config = ConfiguratorUtil.createNewConfig(tui);

    Tools5eIndex index = new Tools5eIndex(config) {
        @Override
        public boolean isIncluded(String name) {
            return true;
        }
    };
    Tools5eJsonSourceCopier copier = new Tools5eJsonSourceCopier(index);

    @Test
    public void testPrefixSuffixStringPropWrapsExistingFormula() {
        ObjectNode origin = Tui.MAPPER.createObjectNode();
        origin.put("name", "Aboleth");
        origin.put("source", "TftYP");
        ObjectNode originHp = origin.putObject("hp");
        originHp.put("average", 135);
        originHp.put("formula", "18d10+36");
        index.addToIndex(Tools5eIndexType.monster, origin);

        ObjectNode copyTo = Tui.MAPPER.createObjectNode();
        copyTo.put("name", "Reduced-Threat Aboleth");
        copyTo.put("source", "TftYP");
        ObjectNode _copy = copyTo.putObject("_copy");
        _copy.put("name", "Aboleth");
        _copy.put("source", "TftYP");
        ObjectNode _mod = _copy.putObject("_mod");
        _mod.putArray("hp")
                .addObject()
                .put("mode", "scalarMultProp")
                .put("prop", "average")
                .put("scalar", 0.5)
                .put("floor", true);
        _mod.withArray("hp")
                .addObject()
                .put("mode", "prefixSuffixStringProp")
                .put("prop", "formula")
                .put("prefix", "floor((")
                .put("suffix", ") ÷ 2)");

        JsonNode result = copier.handleCopy(Tools5eIndexType.monster, copyTo);

        assertThat(result.get("hp").get("formula").asText())
                .isEqualTo("floor((18d10+36) ÷ 2)");
        assertThat(result.get("hp").get("average").asInt()).isEqualTo(67);
    }
}
