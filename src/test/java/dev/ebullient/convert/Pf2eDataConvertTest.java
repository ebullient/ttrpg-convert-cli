package dev.ebullient.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import dev.ebullient.convert.io.Tui;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class Pf2eDataConvertTest {
    static Path testOutputRoot;
    static Tui tui;

    Path testOutput;

    @BeforeAll
    public static void setupDir() {
        setupDir("test-cli");
    }

    public static void setupDir(String name) {
        tui = new Tui();
        tui.init(null, false, false);
        testOutputRoot = TestUtils.OUTPUT_ROOT_PF2.resolve(name);
        testOutputRoot.toFile().mkdirs();
    }

    @AfterAll
    public static void cleanup() {
        System.out.println("Done.");
    }

    @BeforeEach
    public void setup() {
        testOutput = null; // test should set this to something readable
    }

    @AfterEach
    public void clear() throws IOException {
        assertThat(testOutput).isNotNull(); // make sure test set this

        Path logFile = Path.of("ttrpg-convert.out.txt");
        if (Files.exists(logFile)) {
            Path filePath = testOutput.resolve(logFile);
            Files.move(logFile, filePath, StandardCopyOption.REPLACE_EXISTING);

            String content = Files.readString(filePath, StandardCharsets.UTF_8);
            if (content.contains("Exception")) {
                tui.errorf("Exception found in %s", filePath);
            }
        }
        TestUtils.cleanupReferences();
    }

    @Test
    void testLiveData_Pf2eAllSources(QuarkusMainLauncher launcher) {
        testOutput = testOutputRoot.resolve("all-index");
        if (TestUtils.PATH_PF2E_TOOLS_DATA.toFile().exists()) {
            // All, I mean it. Really for real.. ALL.
            TestUtils.deleteDir(testOutput);

            List<String> args = new ArrayList<>(List.of("--index", "--log",
                    "-o", testOutput.toString(),
                    "-g", "pf2e",
                    TestUtils.TEST_RESOURCES.resolve("sources-from-all.json").toString(),
                    TestUtils.PATH_PF2E_TOOLS_DATA.toString()));

            args.addAll(TestUtils.getFilesFrom(TestUtils.PATH_PF2E_TOOLS_DATA.resolve("adventure"))
                    .stream()
                    .filter(x -> !x.endsWith("-id.json"))
                    .toList());
            args.addAll(TestUtils.getFilesFrom(TestUtils.PATH_PF2E_TOOLS_DATA.resolve("book")));

            LaunchResult result = launcher.launch(args.toArray(new String[0]));
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            Tui tui = new Tui();
            tui.init(null, false, false);

            TestUtils.assertDirectoryContents(testOutput, tui, (p, content) -> {
                List<String> errors = new ArrayList<>();
                content.forEach(l -> TestUtils.checkMarkdownLink(testOutput.toString(), p, l, errors));
                return errors;
            });
        }
    }
}
