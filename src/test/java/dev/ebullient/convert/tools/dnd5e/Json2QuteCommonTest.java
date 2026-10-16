package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.ConfiguratorUtil;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.dnd5e.Json2QuteCommon.VulnerabilityFields;

public class Json2QuteCommonTest {

    static Tools5eIndex index;

    @BeforeAll
    static void setupIndex() {
        Tui tui = new Tui();
        CompendiumConfig config = ConfiguratorUtil.createNewConfig(tui);
        index = new Tools5eIndex(config) {
            @Override
            public boolean isIncluded(String name) {
                return true;
            }
        };
        index.prepared.set(true);
    }

    @Test
    void rendersListNameAsBoldCaption() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Monster Lists",
                  "source": "TEST",
                  "entries": [
                    "Example text before",
                    {
                      "type": "list",
                      "style": "list-hang-notitle",
                      "name": "Aberration",
                      "items": [
                        "[Aboleth](/compendium/bestiary/aberration/aboleth-xmm.md)",
                        "[Beholder](/compendium/bestiary/aberration/beholder-xmm.md)"
                      ]
                    },
                    "Example text after"
                  ]
                }
                """);

        Json2QuteMonster json2Qute = new Json2QuteMonster(index, Tools5eIndexType.monster, source);

        assertThat(json2Qute.getText(null)).isEqualTo(
                """
                        Example text before

                        **Aberration**

                        - [Aboleth](/compendium/bestiary/aberration/aboleth-xmm.md)\s\s
                        - [Beholder](/compendium/bestiary/aberration/beholder-xmm.md)\s\s

                        Example text after"""); // trailing spaces are intentional (markdown hard line breaks)
    }

    @Test
    void rendersNoListCaptionWhenNameIsAbsentOrBlank() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Monster Lists",
                  "source": "TEST",
                  "entries": [
                    "Example text before",
                    {
                      "type": "list",
                      "style": "list-hang-notitle",
                      "name": "   ",
                      "items": [
                        "[Aboleth](/compendium/bestiary/aberration/aboleth-xmm.md)"
                      ]
                    },
                    "Example text after"
                  ]
                }
                """);

        Json2QuteMonster json2Qute = new Json2QuteMonster(index, Tools5eIndexType.monster, source);

        assertThat(json2Qute.getText(null)).isEqualTo(
                """
                        Example text before

                        - [Aboleth](/compendium/bestiary/aberration/aboleth-xmm.md)\s\s

                        Example text after""");
    }

    @Test
    void nestsUncaptionedNonFirstNestedListUnderPreviousItem() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Credits",
                  "source": "TEST",
                  "entries": [
                    "Example text before",
                    {
                      "type": "list",
                      "style": "list-hang-notitle",
                      "items": [
                        "Lead Designer",
                        {
                          "type": "list",
                          "style": "list-hang-notitle",
                          "items": [
                            "Writer One",
                            "Writer Two"
                          ]
                        }
                      ]
                    },
                    "Example text after"
                  ]
                }
                """);

        Json2QuteMonster json2Qute = new Json2QuteMonster(index, Tools5eIndexType.monster, source);

        assertThat(json2Qute.getText(null)).isEqualTo(
                """
                        Example text before

                        - Lead Designer\s\s
                            - Writer One\s\s
                            - Writer Two\s\s

                        Example text after""");
    }

    @Test
    void rendersFirstItemUncaptionedNestedListAtCurrentLevel() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Credits",
                  "source": "TEST",
                  "entries": [
                    "Example text before",
                    {
                      "type": "list",
                      "style": "list-hang-notitle",
                      "items": [
                        {
                          "type": "list",
                          "style": "list-hang-notitle",
                          "items": [
                            "Writer One",
                            "Writer Two"
                          ]
                        }
                      ]
                    },
                    "Example text after"
                  ]
                }
                """);

        Json2QuteMonster json2Qute = new Json2QuteMonster(index, Tools5eIndexType.monster, source);

        assertThat(json2Qute.getText(null)).isEqualTo(
                """
                        Example text before

                        - Writer One\s\s
                        - Writer Two\s\s

                        Example text after""");
    }

    @Test
    void rendersNestedOptionsWithoutDoubledIndentation() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Feature",
                  "source": "TEST",
                  "entries": [
                    "Example text before",
                    {
                      "type": "options",
                      "count": 1,
                      "entries": [
                        {
                          "type": "options",
                          "entries": [
                            "Athletics",
                            "Acrobatics"
                          ]
                        }
                      ]
                    },
                    "Example text after"
                  ]
                }
                """);

        Json2QuteMonster json2Qute = new Json2QuteMonster(index, Tools5eIndexType.monster, source);

        assertThat(json2Qute.getText(null)).isEqualTo(
                """
                        Example text before

                        Options (choose 1):

                        - Options:\s\s
                            - Athletics\s\s
                            - Acrobatics\s\s

                        Example text after""");
    }

    @Test
    void rendersOptionsNestedInListWithoutMergingHeadingOntoBullet() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Feature",
                  "source": "TEST",
                  "entries": [
                    "Example text before",
                    {
                      "type": "list",
                      "style": "list-hang-notitle",
                      "items": [
                        "Choose a skill:",
                        {
                          "type": "options",
                          "count": 1,
                          "entries": [
                            "Athletics",
                            "Acrobatics"
                          ]
                        }
                      ]
                    },
                    "Example text after"
                  ]
                }
                """);

        Json2QuteMonster json2Qute = new Json2QuteMonster(index, Tools5eIndexType.monster, source);

        assertThat(json2Qute.getText(null)).isEqualTo(
                """
                        Example text before

                        - Choose a skill:\s\s

                            Options (choose 1):

                            - Athletics\s\s
                            - Acrobatics\s\s

                        Example text after""");
    }

    @Test
    void nestsCaptionedListAsItemInOuterGroupingList() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Monster Lists",
                  "source": "TEST",
                  "entries": [
                    "Example text before",
                    {
                      "type": "list",
                      "style": "list-hang-notitle",
                      "items": [
                        {
                          "type": "list",
                          "style": "list-hang-notitle",
                          "name": "Aberration",
                          "items": [
                            "[Aboleth](/compendium/bestiary/aberration/aboleth-xmm.md)",
                            "[Beholder](/compendium/bestiary/aberration/beholder-xmm.md)"
                          ]
                        },
                        {
                          "type": "list",
                          "style": "list-hang-notitle",
                          "name": "Beast",
                          "items": [
                            "[Ape](/compendium/bestiary/beast/ape-xmm.md)"
                          ]
                        }
                      ]
                    },
                    "Example text after"
                  ]
                }
                """);

        Json2QuteMonster json2Qute = new Json2QuteMonster(index, Tools5eIndexType.monster, source);

        assertThat(json2Qute.getText(null)).isEqualTo(
                """
                        Example text before

                        **Aberration**

                        - [Aboleth](/compendium/bestiary/aberration/aboleth-xmm.md)\s\s
                        - [Beholder](/compendium/bestiary/aberration/beholder-xmm.md)\s\s

                        **Beast**

                        - [Ape](/compendium/bestiary/beast/ape-xmm.md)\s\s

                        Example text after""");
    }

    @Test
    void collectsPlainConditionImmunitiesBeforeLinkification() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Test Monster",
                  "source": "TEST",
                  "conditionImmune": [
                    "blinded",
                    {"special": "see details"},
                    {"conditionImmune": ["charmed", "frightened"], "note": "while transformed"}
                  ]
                }
                """);
        assertThat(Json2QuteCommon.collectPlainImmunities(source, VulnerabilityFields.conditionImmune))
                .containsExactly("blinded", "special", "charmed", "frightened");
    }
}
