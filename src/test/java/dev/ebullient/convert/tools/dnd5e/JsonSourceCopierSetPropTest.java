package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.ConfiguratorUtil;
import dev.ebullient.convert.io.Tui;

/**
 * Regression test for https://github.com/ebullient/ttrpg-convert-cli/issues/933:
 * a {@code setProp} mod on a top-level array property (e.g. fluff {@code entries})
 * must overwrite the copied value, not leave it duplicated alongside a stray field.
 */
public class JsonSourceCopierSetPropTest {

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
    public void testSetPropOverwritesTopLevelArray() {
        ObjectNode origin = Tui.MAPPER.createObjectNode();
        origin.put("name", "Dinosaurs");
        origin.put("source", "MPMM");
        origin.putArray("entries")
                .add("This larger cousin of the velociraptor kills by gripping its target with its claws and feeding.")
                .add("This sail-backed reptile is commonly found in areas where dinosaurs live.");
        index.addToIndex(Tools5eIndexType.monsterFluff, origin);

        ObjectNode copyTo = Tui.MAPPER.createObjectNode();
        copyTo.put("name", "Brontosaurus");
        copyTo.put("source", "MPMM");
        ObjectNode _copy = copyTo.putObject("_copy");
        _copy.put("name", "Dinosaurs");
        _copy.put("source", "MPMM");
        ObjectNode _mod = _copy.putObject("_mod");
        ObjectNode entriesMod = _mod.putObject("entries");
        entriesMod.put("mode", "setProp");
        entriesMod.putArray("value")
                .add("This massive four-legged dinosaur is large enough that most predators leave it alone. "
                        + "Its deadly tail can drive away or kill smaller threats.");

        JsonNode result = copier.handleCopy(Tools5eIndexType.monsterFluff, copyTo);

        assertThat(result.has("/entries")).isFalse();
        assertThat(result.get("entries")).isNotNull();
        assertThat(result.get("entries").isArray()).isTrue();
        assertThat(result.get("entries")).hasSize(1);
        assertThat(result.get("entries").get(0).asText())
                .isEqualTo("This massive four-legged dinosaur is large enough that most predators leave it alone. "
                        + "Its deadly tail can drive away or kill smaller threats.");
    }
}
