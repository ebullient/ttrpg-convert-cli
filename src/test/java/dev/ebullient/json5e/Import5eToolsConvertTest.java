package dev.ebullient.json5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.ebullient.json5e.io.Json5eTui;
import io.quarkus.test.junit.main.Launch;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class Import5eToolsConvertTest {
    static final Path outputPath = TestUtils.OUTPUT_ROOT.resolve("test-cli");
    static Json5eTui tui;

    @BeforeAll
    public static void setupDir() {
        tui = new Json5eTui();
        tui.init(null, true, true);
        outputPath.toFile().mkdirs();
    }

    @Test
    @Launch({ "--help" })
    void testCommandHelp(LaunchResult result) {
        result.echoSystemOut();
        assertThat(result.getOutput())
                .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                .contains("Usage: 5e-convert");
    }

    @Test
    void testCommandLiveData(QuarkusMainLauncher launcher) {
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            // SRD
            LaunchResult result = launcher.launch("--index", "-v",
                    "-o", outputPath.resolve("srd-index").toString(), TestUtils.TOOLS_PATH.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            // Subset
            result = launcher.launch("--index", "-s", "PHB,DMG,XGE,SCAG", "-v",
                    "-o", outputPath.resolve("subset-index").toString(), TestUtils.TOOLS_PATH.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            // All
            result = launcher.launch("--index", "-s", "*", "-v",
                    "-o", outputPath.resolve("all-index").toString(), TestUtils.TOOLS_PATH.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);
        }
    }

    @Test
    void testCommandLiveDataOneSource(QuarkusMainLauncher launcher) {
        if (TestUtils.TOOLS_PATH.toFile().exists()) {
            // No basics
            LaunchResult result = launcher.launch("-s", "ERLW", "-v",
                    "-o", outputPath.resolve("erlw").toString(), TestUtils.TOOLS_PATH.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);
        }
    }
}
