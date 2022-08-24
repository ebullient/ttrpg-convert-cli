package dev.ebullient.json5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.ebullient.json5e.io.Json5eTui;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class Import5eToolsConvertTest {
    final static Path PROJECT_PATH = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    final static Path OUTPUT_PATH = PROJECT_PATH.resolve("target/test-cli");

    // for compile/test purposes. Must clone/sync separately.
    final static Path TOOLS_PATH = PROJECT_PATH.resolve("5etools-mirror-1.github.io/data");

    static Json5eTui tui;

    @BeforeAll
    public static void setupDir() {
        tui = new Json5eTui();
        tui.init(null, true, true);
        TestUtils.deleteDir(OUTPUT_PATH);
        OUTPUT_PATH.toFile().mkdirs();
    }

    /**
     * @param result
     */
    @Test
    @Launch({ "--help" })
    void testCommandHelp(LaunchResult result) {
        result.echoSystemOut();
        assertThat(result.getOutput()).contains("Usage: 5e-convert")
                .overridingErrorMessage("Result should contain the CLI help message. Found: %s", TestUtils.dump(result));
    }
}
