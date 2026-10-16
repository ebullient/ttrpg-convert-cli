package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.ConfiguratorUtil;
import dev.ebullient.convert.io.Tui;

/**
 * A copy-from node's {@code referenceSources} must not leak onto the copy-to
 * node during a {@code _copy} merge unless explicitly preserved via metadata,
 * matching how {@code otherSources} is already protected.
 */
public class JsonSourceCopierReferenceSourcesPreserveTest {

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
    public void testReferenceSourcesNotCopiedWithoutPreserve() {
        ObjectNode origin = Tui.MAPPER.createObjectNode();
        origin.put("name", "Darkling");
        origin.put("source", "TEST-VGM");
        origin.putArray("referenceSources").add("TEST-WBTW");
        index.addToIndex(Tools5eIndexType.monster, origin);

        ObjectNode copyTo = Tui.MAPPER.createObjectNode();
        copyTo.put("name", "Darkling");
        copyTo.put("source", "TEST-MPMM");
        ObjectNode _copy = copyTo.putObject("_copy");
        _copy.put("name", "Darkling");
        _copy.put("source", "TEST-VGM");

        JsonNode result = copier.handleCopy(Tools5eIndexType.monster, copyTo);

        assertThat(result.get("referenceSources")).isNull();
    }

    @Test
    public void testReferenceSourcesCopiedWhenPreserved() {
        ObjectNode origin = Tui.MAPPER.createObjectNode();
        origin.put("name", "Darkling Elder");
        origin.put("source", "TEST-VGM");
        origin.putArray("referenceSources").add("TEST-WBTW");
        index.addToIndex(Tools5eIndexType.monster, origin);

        ObjectNode copyTo = Tui.MAPPER.createObjectNode();
        copyTo.put("name", "Darkling Elder");
        copyTo.put("source", "TEST-MPMM");
        ObjectNode _copy = copyTo.putObject("_copy");
        _copy.put("name", "Darkling Elder");
        _copy.put("source", "TEST-VGM");
        ObjectNode _preserve = _copy.putObject("_preserve");
        _preserve.put("referenceSources", true);

        JsonNode result = copier.handleCopy(Tools5eIndexType.monster, copyTo);

        assertThat(result.get("referenceSources")).isNotNull();
        assertThat(result.get("referenceSources").get(0).asText()).isEqualTo("TEST-WBTW");
    }
}
