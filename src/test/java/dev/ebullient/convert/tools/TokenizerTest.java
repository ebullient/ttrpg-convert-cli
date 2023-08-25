package dev.ebullient.convert.tools;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig;
import dev.ebullient.convert.io.Tui;
import io.quarkus.arc.Arc;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class TokenizerTest {
    protected static Tui tui;
    protected static JsonTextConverter<IndexType> tokenizer;
    protected static ToolsIndex index;

    @BeforeAll
    public static void prepare() {
        tui = Arc.container().instance(Tui.class).get();
        tui.init(null, true, false);
        tokenizer = new JsonTextConverter<IndexType>() {

            @Override
            public void appendToText(List<String> inner, JsonNode target, String heading) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'appendToText'");
            }

            @Override
            public CompendiumConfig cfg() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'cfg'");
            }

            @Override
            public String linkify(IndexType type, String s) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'linkify'");
            }

            @Override
            public String replaceText(String s) {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'replaceText'");
            }

            @Override
            public Tui tui() {
                // TODO Auto-generated method stub
                throw new UnsupportedOperationException("Unimplemented method 'tui'");
            }

        };
    }

    @Test
    public void testSimpleString() {
        String input = "{@atk mw} {@hit 2} to hit, reach 5 ft., one target. {@h}4 ({@damage 1d8}) bludgeoning damage, or 5 ({@damage 1d10 + 1}) bludgeoning damage if used with two hands to make a melee attack.";
        String expected = "%atk% %hit% to hit, reach 5 ft., one target. %h%4 (%damage%) bludgeoning damage, or 5 (%damage%) bludgeoning damage if used with two hands to make a melee attack.";

        assertThat(tokenizer.replaceTokens(input, (s, b) -> s.replaceAll("\\{@(\\w+).*?}", "%$1%"))).isEqualTo(expected);

    }

    @Test
    public void testNestedString() {
        String input = "The elder dinosaur can use its Frightful Presence. It then makes five attacks: three with its bite, {@footnote one with its stomp, and one with its tail|{@note This statblock does not have these actions available.}}. It can use its Swallow instead of a bite.";
        String expected = "The elder dinosaur can use its Frightful Presence. It then makes five attacks: three with its bite, ^[one with its stomp, and one with its tail|^[This statblock does not have these actions available.]]. It can use its Swallow instead of a bite.";

        assertThat(tokenizer.replaceTokens(input, (s, b) -> s.replaceAll("\\{@\\w+ (.*?)}", "^[$1]"))).isEqualTo(expected);

        input = "The events of {@adventure Hoard of the Dragon Queen|HotDQ} lead directly into {@i {@i The Rise of Tiamat}.} The shape of this adventure is defined by the meetings of the Council of Waterdeep, which divide the adventure into four stages.";
        expected = "The events of ^[Hoard of the Dragon Queen|HotDQ] lead directly into ^[^[The Rise of Tiamat].] The shape of this adventure is defined by the meetings of the Council of Waterdeep, which divide the adventure into four stages.";

        assertThat(tokenizer.replaceTokens(input, (s, b) -> s.replaceAll("\\{@\\w+ (.*?)}", "^[$1]"))).isEqualTo(expected);
    }

}
