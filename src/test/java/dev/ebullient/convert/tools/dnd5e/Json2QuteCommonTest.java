package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.dnd5e.Json2QuteCommon.VulnerabilityFields;

public class Json2QuteCommonTest {

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
