package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.ConfiguratorUtil;
import dev.ebullient.convert.io.Tui;

/**
 * Regression test for https://github.com/ebullient/ttrpg-convert-cli/issues/820:
 * a {@code <$variable$>} placeholder token embedded inside a longer sentence must be
 * spliced into its original position, not used to replace the entire string.
 */
public class JsonSourceCopierResolveDynamicTextTest {

    Tui tui = new Tui();
    CompendiumConfig config = ConfiguratorUtil.createNewConfig(tui);

    Tools5eIndex index = new Tools5eIndex(config) {
        @Override
        public boolean isIncluded(String name) {
            return true;
        }
    };
    Tools5eJsonSourceCopier copier = new Tools5eJsonSourceCopier(index);

    void indexBaseCreature() throws Exception {
        JsonNode copyFrom = Tui.MAPPER.readTree("""
                {
                  "name": "Mock Base",
                  "source": "TEST"
                }
                """);
        index.addToIndex(Tools5eIndexType.monster, copyFrom);
    }

    void indexMockTemplate() throws Exception {
        JsonNode template = Tui.MAPPER.readTree(
                """
                        {
                          "name": "Mock Template",
                          "source": "TEST",
                          "apply": {
                            "_mod": {
                              "trait": {
                                "mode": "appendArr",
                                "items": [
                                  {
                                    "name": "Mock Trait",
                                    "entries": [
                                      "<$title_short_name$> has advantage on saving throws against being charmed, and magic can't put <$short_name$> to sleep."
                                    ]
                                  }
                                ]
                              }
                            }
                          }
                        }
                        """);
        index.addToIndex(Tools5eIndexType.monsterTemplate, template);
    }

    @Test
    public void testTokenAtNonZeroOffsetIsSplicedInPlace() throws Exception {
        indexBaseCreature();
        ObjectNode copyTo = (ObjectNode) Tui.MAPPER.readTree(
                """
                        {
                          "name": "Pip Pebblewhistle",
                          "source": "TEST",
                          "isNamedCreature": true,
                          "cr": "1",
                          "_copy": {
                            "name": "Mock Base",
                            "source": "TEST",
                            "_mod": {
                              "trait": {
                                "mode": "appendArr",
                                "items": [
                                  {
                                    "name": "Mock Trait",
                                    "entries": [
                                      "<$title_short_name$> has advantage on saving throws against being charmed, and magic can't put <$short_name$> to sleep."
                                    ]
                                  }
                                ]
                              }
                            }
                          }
                        }
                        """);

        JsonNode result = copier.handleCopy(Tools5eIndexType.monster, copyTo);

        assertThat(result.get("trait").get(0).get("entries").get(0).asText())
                .isEqualTo("Pip has advantage on saving throws against being charmed, "
                        + "and magic can't put Pip to sleep.");
    }

    @Test
    public void testTwoDistinctTokenTypesInOneStringAreSpliced() throws Exception {
        indexBaseCreature();
        ObjectNode copyTo = (ObjectNode) Tui.MAPPER.readTree(
                """
                        {
                          "name": "Pip Pebblewhistle",
                          "source": "TEST",
                          "isNamedCreature": true,
                          "cr": "1",
                          "cha": 16,
                          "_copy": {
                            "name": "Mock Base",
                            "source": "TEST",
                            "_mod": {
                              "spellcasting": {
                                "mode": "appendArr",
                                "items": [
                                  {
                                    "name": "Mock Spellcasting",
                                    "headerEntries": [
                                      "<$title_short_name$>'s innate spellcasting ability is Charisma (spell save DC <$spell_dc__cha$>). <$title_short_name$> can innately cast spells."
                                    ]
                                  }
                                ]
                              }
                            }
                          }
                        }
                        """);

        JsonNode result = copier.handleCopy(Tools5eIndexType.monster, copyTo);

        assertThat(result.get("spellcasting").get(0).get("headerEntries").get(0).asText())
                .isEqualTo("Pip's innate spellcasting ability is Charisma (spell save DC 13). "
                        + "Pip can innately cast spells.");
    }

    @Test
    public void testRepeatedTokenInOneStringIsSplicedEveryOccurrence() throws Exception {
        indexBaseCreature();
        ObjectNode copyTo = (ObjectNode) Tui.MAPPER.readTree(
                """
                        {
                          "name": "Pip Pebblewhistle",
                          "source": "TEST",
                          "isNamedCreature": true,
                          "cr": "1",
                          "_copy": {
                            "name": "Mock Base",
                            "source": "TEST",
                            "_mod": {
                              "trait": {
                                "mode": "appendArr",
                                "items": [
                                  {
                                    "name": "Mock Trait",
                                    "entries": [
                                      "<$title_short_name$> can communicate telepathically. These creatures understand <$title_short_name$>'s words, though <$title_short_name$> has no ability to speak."
                                    ]
                                  }
                                ]
                              }
                            }
                          }
                        }
                        """);

        JsonNode result = copier.handleCopy(Tools5eIndexType.monster, copyTo);

        assertThat(result.get("trait").get(0).get("entries").get(0).asText())
                .isEqualTo("Pip can communicate telepathically. "
                        + "These creatures understand Pip's words, though Pip has no ability to speak.");
    }

    @Test
    public void testStringWithNoTokenIsUnchanged() throws Exception {
        indexBaseCreature();
        ObjectNode copyTo = (ObjectNode) Tui.MAPPER.readTree("""
                {
                  "name": "Pip Pebblewhistle",
                  "source": "TEST",
                  "isNamedCreature": true,
                  "cr": "1",
                  "_copy": {
                    "name": "Mock Base",
                    "source": "TEST",
                    "_mod": {
                      "trait": {
                        "mode": "appendArr",
                        "items": [
                          {
                            "name": "Mock Trait",
                            "entries": [
                              "This trait has no placeholder tokens at all."
                            ]
                          }
                        ]
                      }
                    }
                  }
                }
                """);

        JsonNode result = copier.handleCopy(Tools5eIndexType.monster, copyTo);

        assertThat(result.get("trait").get(0).get("entries").get(0).asText())
                .isEqualTo("This trait has no placeholder tokens at all.");
    }

    @Test
    public void testUnresolvableTokenLeavesRawTextWhileOtherTokenStillSplices() throws Exception {
        // "dc" mode requires a target attribute named by its param; omit it so resolution fails for that token.
        indexBaseCreature();
        ObjectNode copyTo = (ObjectNode) Tui.MAPPER.readTree("""
                {
                  "name": "Pip Pebblewhistle",
                  "source": "TEST",
                  "isNamedCreature": true,
                  "cr": "1",
                  "_copy": {
                    "name": "Mock Base",
                    "source": "TEST",
                    "_mod": {
                      "trait": {
                        "mode": "appendArr",
                        "items": [
                          {
                            "name": "Mock Trait",
                            "entries": [
                              "<$short_name$> has a save DC of <$dc__missingAbility$> for this trait."
                            ]
                          }
                        ]
                      }
                    }
                  }
                }
                """);

        JsonNode result = copier.handleCopy(Tools5eIndexType.monster, copyTo);

        assertThat(result.get("trait").get(0).get("entries").get(0).asText())
                .isEqualTo("Pip has a save DC of <$dc__missingAbility$> for this trait.");
    }

    @Test
    public void testTemplateInjectedTraitTokenIsSplicedInPlace() throws Exception {
        indexBaseCreature();
        indexMockTemplate();
        ObjectNode copyTo = (ObjectNode) Tui.MAPPER.readTree("""
                {
                  "name": "Pip Pebblewhistle",
                  "source": "TEST",
                  "isNamedCreature": true,
                  "cr": "1",
                  "_copy": {
                    "name": "Mock Base",
                    "source": "TEST",
                    "_templates": [
                      {
                        "name": "Mock Template",
                        "source": "TEST"
                      }
                    ]
                  }
                }
                """);

        JsonNode result = copier.handleCopy(Tools5eIndexType.monster, copyTo);

        assertThat(result.get("trait").get(0).get("entries").get(0).asText())
                .isEqualTo("Pip has advantage on saving throws against being charmed, "
                        + "and magic can't put Pip to sleep.");
    }

    @Test
    public void testTemplateInjectedTraitTokenIsSplicedInPlaceWithoutWildcardReplaceTxtMod() throws Exception {
        // Mirrors a creature that has no "*" replaceTxt mod at all (unlike the test above, which has none either,
        // but this adds an unrelated "*" replaceTxt mod to confirm its presence/absence doesn't change splicing).
        indexBaseCreature();
        indexMockTemplate();
        ObjectNode copyTo = (ObjectNode) Tui.MAPPER.readTree("""
                {
                  "name": "Pip Pebblewhistle",
                  "source": "TEST",
                  "isNamedCreature": true,
                  "cr": "1",
                  "_copy": {
                    "name": "Mock Base",
                    "source": "TEST",
                    "_templates": [
                      {
                        "name": "Mock Template",
                        "source": "TEST"
                      }
                    ],
                    "_mod": {
                      "*": {
                        "mode": "replaceTxt",
                        "replace": "the mock base",
                        "with": "Pip",
                        "flags": "i"
                      }
                    }
                  }
                }
                """);

        JsonNode result = copier.handleCopy(Tools5eIndexType.monster, copyTo);

        assertThat(result.get("trait").get(0).get("entries").get(0).asText())
                .isEqualTo("Pip has advantage on saving throws against being charmed, "
                        + "and magic can't put Pip to sleep.");
    }
}
