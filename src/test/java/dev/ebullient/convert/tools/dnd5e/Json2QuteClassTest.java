package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.CompendiumConfig.Configurator;
import dev.ebullient.convert.config.ConfiguratorUtil;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.dnd5e.qute.QuteClass;

public class Json2QuteClassTest {

    @AfterEach
    void clearStaticSourceCache() {
        Tools5eSources.clear();
        Json2QuteClass.keyToClassFeature.clear();
    }

    @Test
    void classFeatureChoiceOptionsAreLinkedAndRenderedAsSections() throws Exception {
        Tui tui = new Tui();
        CompendiumConfig config = ConfiguratorUtil.createNewConfig(tui);
        new Configurator(config).allowSource("test-phb");
        Tools5eIndex index = new Tools5eIndex(config);
        Tools5eLinkifier.instance().reset();

        JsonNode tree = Tui.MAPPER.readTree("""
                {
                  "class": [
                    {
                      "name": "TestClass",
                      "source": "TEST-PHB",
                      "classFeatures": [
                        "Test Chooser|TestClass|TEST-PHB|1"
                      ]
                    }
                  ],
                  "classFeature": [
                    {
                      "name": "Test Chooser",
                      "source": "TEST-PHB",
                      "className": "TestClass",
                      "classSource": "TEST-PHB",
                      "level": 1,
                      "entries": [
                        {
                          "type": "options",
                          "count": 1,
                          "entries": [
                            { "type": "refClassFeature", "classFeature": "Test Choice A|TestClass|TEST-PHB|1" },
                            { "type": "refClassFeature", "classFeature": "Test Choice B|TestClass|TEST-PHB|1" }
                          ]
                        }
                      ]
                    },
                    {
                      "name": "Test Choice A",
                      "source": "TEST-PHB",
                      "className": "TestClass",
                      "classSource": "TEST-PHB",
                      "level": 1,
                      "entries": ["You gain Choice A benefits."]
                    },
                    {
                      "name": "Test Choice B",
                      "source": "TEST-PHB",
                      "className": "TestClass",
                      "classSource": "TEST-PHB",
                      "level": 1,
                      "entries": ["You gain Choice B benefits."]
                    }
                  ]
                }
                """);
        index.importTree("test.json", tree);
        index.prepare();

        String key = Tools5eIndexType.classtype.createKey("TestClass", "TEST-PHB");
        JsonNode classNode = index.getOrigin(key);
        assertThat(classNode).isNotNull();

        Json2QuteClass json2QuteClass = new Json2QuteClass(index, Tools5eIndexType.classtype, classNode);
        QuteClass qute = json2QuteClass.buildQuteResource();

        // The options list should link to each choice rather than inlining its full text there
        assertThat(qute.text)
                .describedAs("options list should contain links to the choice features, not their full bodies inline")
                .contains("[Test Choice A](#Test%20Choice%20A%20(Level%201))")
                .contains("[Test Choice B](#Test%20Choice%20B%20(Level%201))");

        // Each linked choice must have a real heading to resolve against (this is the anchor
        // text a link above points at: "Test Choice A (Level 1)")
        assertThat(qute.text)
                .describedAs("each choice referenced from the options list must get its own linkable heading")
                .contains("### Test Choice A (Level 1)")
                .contains("### Test Choice B (Level 1)")
                .contains("You gain Choice A benefits.")
                .contains("You gain Choice B benefits.");
    }

    @Test
    void genericOptionsListIsUnaffectedByClassFeatureChoiceHandling() throws Exception {
        // Generic "options" entries (as found in regular book/adventure text, e.g. a choice of
        // mundane equipment or effects) have nothing to do with refClassFeature/refSubclassFeature,
        // and must keep rendering as a plain bullet list: no headings injected, no links generated.
        Tui tui = new Tui();
        CompendiumConfig config = ConfiguratorUtil.createNewConfig(tui);
        new Configurator(config).allowSource("test-phb");
        Tools5eIndex index = new Tools5eIndex(config);
        Tools5eLinkifier.instance().reset();

        JsonNode tree = Tui.MAPPER.readTree("""
                {
                  "class": [
                    {
                      "name": "TestClass",
                      "source": "TEST-PHB",
                      "classFeatures": [
                        "Test Generic Choice|TestClass|TEST-PHB|1"
                      ]
                    }
                  ],
                  "classFeature": [
                    {
                      "name": "Test Generic Choice",
                      "source": "TEST-PHB",
                      "className": "TestClass",
                      "classSource": "TEST-PHB",
                      "level": 1,
                      "entries": [
                        {
                          "type": "options",
                          "count": 1,
                          "entries": [
                            "A plain text option with no heading of its own.",
                            "Another plain text option with no heading of its own."
                          ]
                        }
                      ]
                    }
                  ]
                }
                """);
        index.importTree("test.json", tree);
        index.prepare();

        String key = Tools5eIndexType.classtype.createKey("TestClass", "TEST-PHB");
        JsonNode classNode = index.getOrigin(key);
        assertThat(classNode).isNotNull();

        Json2QuteClass json2QuteClass = new Json2QuteClass(index, Tools5eIndexType.classtype, classNode);
        QuteClass qute = json2QuteClass.buildQuteResource();

        assertThat(qute.text)
                .describedAs("generic options entries render as plain bullets, not headings or links")
                .contains("- A plain text option with no heading of its own.")
                .contains("- Another plain text option with no heading of its own.")
                // only the enclosing feature itself gets a heading; the options are not promoted
                .containsOnlyOnce("###")
                .doesNotContain("[A plain text option");
    }
}
