package dev.ebullient.convert.tools.dnd5e.qute;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

public class ImmuneResistTest {

    @Test
    void exposesPlainConditionImmunitiesInSourceOrder() {
        var immuneResist = new ImmuneResist(
                null, null, null, "[blinded](rules/conditions.md#Blinded), [charmed](rules/conditions.md#Charmed)",
                List.of("blinded", "special", "charmed"));

        assertThat(immuneResist.conditionImmuneList)
                .containsExactly("blinded", "special", "charmed");
        assertThat(immuneResist.conditionImmune)
                .isEqualTo("[blinded](rules/conditions.md#Blinded), [charmed](rules/conditions.md#Charmed)");
    }

    @Test
    void exposesEmptyPlainConditionImmunitiesWhenAbsent() {
        var immuneResist = new ImmuneResist(null, null, null, null, List.of());

        assertThat(immuneResist.conditionImmuneList).isEmpty();
    }
}
