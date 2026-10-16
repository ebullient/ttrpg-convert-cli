package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.config.ConfiguratorUtil;
import dev.ebullient.convert.io.Tui;
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
}
