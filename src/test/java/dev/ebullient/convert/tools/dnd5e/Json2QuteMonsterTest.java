package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.ConfiguratorUtil;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.ToolsIndex.TtrpgValue;
import dev.ebullient.convert.tools.dnd5e.qute.QuteMonster;

public class Json2QuteMonsterTest {

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
        // Avoid "Programmer error: Called isSrdBasicFreeOnly while indexing"
        // for sources resolved outside the normal index/prepare flow.
        index.prepared.set(true);
    }

    @Test
    void choosableTypeRendersAsChooseOneOf() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Empyrean",
                  "source": "TEST",
                  "type": {"type": {"choose": ["celestial", "fiend"]}}
                }
                """);

        Json2QuteMonster json2Qute = new Json2QuteMonster(index, Tools5eIndexType.monster, source);

        assertThat(json2Qute.creatureType).isEqualTo("Choose one of celestial or fiend");

        QuteMonster monster = (QuteMonster) json2Qute.build();
        assertThat(monster.type).isEqualTo("Choose one of celestial or fiend");
    }

    @Test
    void choosableTypeCategoryRemainsMiscellaneous() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Empyrean",
                  "source": "TEST",
                  "type": {"type": {"choose": ["celestial", "fiend"]}}
                }
                """);

        Json2QuteMonster.MonsterType category = Json2QuteMonster.MonsterType.fromNode(source, index);

        assertThat(category).isEqualTo(Json2QuteMonster.MonsterType.miscellaneous);
    }

    @Test
    void findMonsterVariantsGeneratesTypedSiblingsForChooseWithNoVersions() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Empyrean",
                  "source": "TEST",
                  "type": {"type": {"choose": ["celestial", "fiend"]}}
                }
                """);
        String key = Tools5eIndexType.monster.createKey(source);

        List<JsonNode> variants = Json2QuteMonster.findMonsterVariants(
                index, Tools5eIndexType.monster, key, source);

        assertThat(variants).hasSize(3);
        assertThat(variants.get(0)).isSameAs(source);

        JsonNode celestial = variants.get(1);
        assertThat(celestial.get("name").asText()).isEqualTo("Empyrean (Celestial)");
        assertThat(celestial.get("type").asText()).isEqualTo("celestial");
        assertThat(TtrpgValue.indexParentKey.getTextOrNull(celestial)).isEqualTo(key);
        assertThat(Tools5eIndexType.monster.createKey(celestial))
                .isEqualTo(Tools5eIndexType.monster.createKey("Empyrean (Celestial)", "TEST"));

        JsonNode fiend = variants.get(2);
        assertThat(fiend.get("name").asText()).isEqualTo("Empyrean (Fiend)");
        assertThat(fiend.get("type").asText()).isEqualTo("fiend");
        assertThat(TtrpgValue.indexParentKey.getTextOrNull(fiend)).isEqualTo(key);
        assertThat(Tools5eIndexType.monster.createKey(fiend))
                .isEqualTo(Tools5eIndexType.monster.createKey("Empyrean (Fiend)", "TEST"));

        assertThat(Json2QuteMonster.MonsterType.fromNode(celestial, index))
                .isEqualTo(Json2QuteMonster.MonsterType.celestial);
        assertThat(Json2QuteMonster.MonsterType.fromNode(fiend, index))
                .isEqualTo(Json2QuteMonster.MonsterType.fiend);
    }

    @Test
    void findMonsterVariantsAliasesFluffAndTokenToParent() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Empyrean",
                  "source": "TEST",
                  "type": {"type": {"choose": ["celestial", "fiend"]}},
                  "hasToken": true,
                  "hasFluff": true,
                  "hasFluffImages": true
                }
                """);
        String key = Tools5eIndexType.monster.createKey(source);

        List<JsonNode> variants = Json2QuteMonster.findMonsterVariants(
                index, Tools5eIndexType.monster, key, source);

        JsonNode celestial = variants.get(1);
        assertThat(celestial.get("name").asText()).isEqualTo("Empyrean (Celestial)");
        assertThat(Json2QuteMonster.MonsterFields.original.getTextOrNull(celestial)).isEqualTo("Empyrean");
        assertThat(TtrpgValue.indexFluffKey.getTextOrNull(celestial))
                .isEqualTo(Tools5eIndexType.monsterFluff.createKey("Empyrean", "TEST"));

        JsonNode fiend = variants.get(2);
        assertThat(Json2QuteMonster.MonsterFields.original.getTextOrNull(fiend)).isEqualTo("Empyrean");
        assertThat(TtrpgValue.indexFluffKey.getTextOrNull(fiend))
                .isEqualTo(Tools5eIndexType.monsterFluff.createKey("Empyrean", "TEST"));
    }

    @Test
    void findMonsterVariantsLeavesNoChooseNoVersionsUnchanged() throws Exception {
        JsonNode source = Tui.MAPPER.readTree("""
                {
                  "name": "Fiend",
                  "source": "TEST",
                  "type": "fiend"
                }
                """);
        String key = Tools5eIndexType.monster.createKey(source);

        List<JsonNode> variants = Json2QuteMonster.findMonsterVariants(
                index, Tools5eIndexType.monster, key, source);

        assertThat(variants).hasSize(1);
        assertThat(variants.get(0)).isSameAs(source);
    }
}
