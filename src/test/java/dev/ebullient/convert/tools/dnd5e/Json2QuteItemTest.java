package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.CompendiumConfig.Configurator;
import dev.ebullient.convert.config.ConfiguratorUtil;
import dev.ebullient.convert.io.Tui;

public class Json2QuteItemTest {

    @AfterEach
    void clearStaticSourceCache() {
        // Tools5eSources.keyToSources is a static cache keyed by name+source; both
        // tests below use the same "Test Apparatus"/"TEST-DMG" key, so without this,
        // the second test to run reuses the first test's cached Tools5eSources instance
        // (and its already-computed inclusion result) instead of building its own.
        Tools5eSources.clear();
    }

    @Test
    void srdItemIncludedOnlyViaReferenceSourcesUsesRealName() throws Exception {
        Tui tui = new Tui();
        CompendiumConfig config = ConfiguratorUtil.createNewConfig(tui);
        new Configurator(config).allowSource("test-wbtw");
        Tools5eIndex index = new Tools5eIndex(config);
        Tools5eLinkifier.instance().reset();

        JsonNode tree = Tui.MAPPER.readTree("""
                {
                  "item": [
                    {
                      "name": "Test Apparatus",
                      "source": "TEST-DMG",
                      "srd": "Test Apparatus (SRD)",
                      "referenceSources": ["TEST-WBTW"]
                    }
                  ]
                }
                """);
        index.importTree("test.json", tree);
        index.prepare();

        String key = Tools5eIndexType.item.createKey("Test Apparatus", "TEST-DMG");
        JsonNode itemNode = index.getOrigin(key);
        assertThat(itemNode).isNotNull();

        Json2QuteItem json2QuteItem = new Json2QuteItem(index, Tools5eIndexType.item, itemNode);
        assertThat(json2QuteItem.itemName(itemNode))
                .describedAs("item included only via referenceSources should still render under its real name")
                .isEqualTo("Test Apparatus");
    }

    @Test
    void srdItemExcludedEntirelyStillUsesSrdNameFallback() throws Exception {
        Tui tui = new Tui();
        CompendiumConfig config = ConfiguratorUtil.createNewConfig(tui);
        new Configurator(config).allowSource("test-unrelated");
        Tools5eIndex index = new Tools5eIndex(config);
        Tools5eLinkifier.instance().reset();

        JsonNode tree = Tui.MAPPER.readTree("""
                {
                  "item": [
                    {
                      "name": "Test Apparatus",
                      "source": "TEST-DMG",
                      "srd": "Test Apparatus (SRD)"
                    }
                  ]
                }
                """);
        index.importTree("test.json", tree);
        index.prepare();

        String key = Tools5eIndexType.item.createKey("Test Apparatus", "TEST-DMG");
        JsonNode itemNode = index.getOrigin(key);
        assertThat(itemNode).isNotNull();

        Json2QuteItem json2QuteItem = new Json2QuteItem(index, Tools5eIndexType.item, itemNode);
        assertThat(json2QuteItem.itemName(itemNode))
                .describedAs("item with no inclusion path at all should keep falling back to its SRD name")
                .isEqualTo("Test Apparatus (SRD)");
    }
}
