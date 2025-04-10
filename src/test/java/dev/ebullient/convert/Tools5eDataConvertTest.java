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
public class Tools5eDataConvertTest {
    static Path rootTestOutput;
    static Tui tui;

    Path testOutput;

    @BeforeAll
    public static void setupDir() {
        setupDir("test-cli");
    }

    public static void setupDir(String name) {
        tui = new Tui();
        tui.init(null, false, false);
        rootTestOutput = TestUtils.OUTPUT_ROOT_5E.resolve(name);
        rootTestOutput.toFile().mkdirs();

        tui.infof("5eTools sources (%s): %s",
                TestUtils.PATH_5E_TOOLS_DATA.toFile().exists(),
                TestUtils.PATH_5E_TOOLS_DATA);
        tui.infof("5eTools images (%s): %s",
                TestUtils.PATH_5E_TOOLS_IMAGES.toFile().exists(),
                TestUtils.PATH_5E_TOOLS_IMAGES);
        tui.infof("5eTools homebrew (%s): %s",
                TestUtils.PATH_5E_HOMEBREW.toFile().exists(),
                TestUtils.PATH_5E_HOMEBREW);
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
            String content = Files.readString(logFile, StandardCharsets.UTF_8);

            Path filePath = testOutput.resolve(logFile);
            Files.move(logFile, filePath, StandardCopyOption.REPLACE_EXISTING);

            if (content.contains("Exception")) {
                tui.errorf("Exception found in %s", filePath);
            }
        }
        TestUtils.cleanupReferences();
    }

    @Test
    void testLiveData_defaultSrd(QuarkusMainLauncher launcher) {
        testOutput = rootTestOutput.resolve("default-index");
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {
            // SRD
            TestUtils.deleteDir(testOutput);

            Tui.instance().infof("--- Default content ----- ");

            LaunchResult result = launcher.launch("--log", "--index",
                    "-o", testOutput.toString(),
                    TestUtils.TEST_RESOURCES.resolve("5e/images-remote.json").toString(),
                    TestUtils.TEST_RESOURCES.resolve("dice-roller.json").toString(),
                    TestUtils.PATH_5E_TOOLS_DATA.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);
            TestUtils.assertDirectoryContents(testOutput, tui, (p, content) -> {
                List<String> errors = new ArrayList<>();
                content.forEach(l -> {
                    TestUtils.checkMarkdownLink(testOutput.toString(), p, l, errors);
                    TestUtils.commonTests(p, l, errors);
                });
                return errors;
            });
        }
    }

    @Test
    void testLiveData_2014Srd(QuarkusMainLauncher launcher) {
        testOutput = rootTestOutput.resolve("srd-2014-index");
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {
            // SRD 2014
            Tui.instance().infof("--- 2014 SRD ----- ");
            TestUtils.deleteDir(testOutput);

            LaunchResult result = launcher.launch("--log", "--index",
                    "-o", testOutput.toString(),
                    "-c", TestUtils.TEST_RESOURCES.resolve("5e/sources-2014-srd.yaml").toString(),
                    TestUtils.TEST_RESOURCES.resolve("5e/images-remote.json").toString(),
                    TestUtils.PATH_5E_TOOLS_DATA.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);
            TestUtils.assertDirectoryContents(testOutput, tui, (p, content) -> {
                List<String> errors = new ArrayList<>();
                content.forEach(l -> {
                    TestUtils.checkMarkdownLink(testOutput.toString(), p, l, errors);
                    TestUtils.commonTests(p, l, errors);
                });
                return errors;
            });
        }
    }

    @Test
    void testLiveData_2024Srd(QuarkusMainLauncher launcher) {
        testOutput = rootTestOutput.resolve("srd-2024-index");
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {
            // SRD 2024
            Tui.instance().infof("--- 2024 SRD ----- ");
            TestUtils.deleteDir(testOutput);

            LaunchResult result = launcher.launch("--log", "--index",
                    "-o", testOutput.toString(),
                    "-c", TestUtils.TEST_RESOURCES.resolve("5e/sources-2024-srd.yaml").toString(),
                    TestUtils.TEST_RESOURCES.resolve("5e/images-remote.json").toString(),
                    TestUtils.PATH_5E_TOOLS_DATA.toString());

            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);
            TestUtils.assertDirectoryContents(testOutput, tui, (p, content) -> {
                List<String> errors = new ArrayList<>();
                content.forEach(l -> {
                    TestUtils.checkMarkdownLink(testOutput.toString(), p, l, errors);
                    TestUtils.commonTests(p, l, errors);
                });
                return errors;
            });
        }
    }

    @Test
    void testLiveData_5eAllSources(QuarkusMainLauncher launcher) {
        testOutput = rootTestOutput.resolve("all-index");
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {
            // All, I mean it. Really for real.. ALL.
            TestUtils.deleteDir(testOutput);

            List<String> args = new ArrayList<>(List.of("--log", "--index",
                    "-c", TestUtils.TEST_RESOURCES.resolve("5e/sources-images.yaml").toString(),
                    "-o", testOutput.toString(),
                    TestUtils.PATH_5E_TOOLS_DATA.toString()));

            if (TestUtils.PATH_5E_TOOLS_IMAGES.toFile().exists()) {
                args.add(TestUtils.TEST_RESOURCES.resolve("5e/images-from-local.json").toString());
            } else {
                args.add(TestUtils.TEST_RESOURCES.resolve("5e/images-remote.json").toString());
            }

            args.addAll(TestUtils.getFilesFrom(TestUtils.PATH_5E_TOOLS_DATA.resolve("adventure")));
            args.addAll(TestUtils.getFilesFrom(TestUtils.PATH_5E_TOOLS_DATA.resolve("book")));

            LaunchResult result = launcher.launch(args.toArray(new String[0]));
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            TestUtils.assertDirectoryContents(testOutput, tui, (p, content) -> {
                List<String> errors = new ArrayList<>();
                content.forEach(l -> {
                    TestUtils.checkMarkdownLink(testOutput.toString(), p, l, errors);
                    TestUtils.commonTests(p, l, errors);
                });
                return errors;
            });
        }
    }

    @Test
    void testLiveData_5eOneSource(QuarkusMainLauncher launcher) {
        testOutput = rootTestOutput.resolve("erlw");
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {
            TestUtils.deleteDir(testOutput);

            // No basics
            LaunchResult result = launcher.launch("--log", "--index",
                    "-o", testOutput.toString(),
                    "-c", TestUtils.TEST_RESOURCES.resolve("5e/sources-single.yaml").toString(),
                    TestUtils.TEST_RESOURCES.resolve("5e/images-remote.json").toString(),
                    TestUtils.PATH_5E_TOOLS_DATA.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            TestUtils.assertDirectoryContents(testOutput, tui, (p, content) -> {
                List<String> errors = new ArrayList<>();
                content.forEach(l -> {
                    TestUtils.checkMarkdownLink(testOutput.toString(), p, l, errors);
                    TestUtils.commonTests(p, l, errors);
                    if (l.matches(".*-ua[^.]\\.md.*$")) {
                        errors.add(String.format("Found UA resources in %s: %s", p.toString(), l));
                    }
                });
                return errors;
            });
        }
    }

    @Test
    void testLiveData_5eHomebrew(QuarkusMainLauncher launcher) {
        testOutput = rootTestOutput.resolve("homebrew");
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists() && TestUtils.PATH_5E_HOMEBREW.toFile().exists()) {
            TestUtils.deleteDir(testOutput);

            List<String> args = new ArrayList<>(List.of("--index", "--log",
                    "-c", TestUtils.TEST_RESOURCES.resolve("5e/sources-homebrew.json").toString(),
                    "-o", testOutput.toString(),
                    TestUtils.PATH_5E_TOOLS_DATA.toString()));

            if (TestUtils.PATH_5E_TOOLS_IMAGES.toFile().exists()) {
                args.add(TestUtils.TEST_RESOURCES.resolve("5e/images-from-local.json").toString());
            } else {
                args.add(TestUtils.TEST_RESOURCES.resolve("5e/images-remote.json").toString());
            }

            LaunchResult result = launcher.launch(args.toArray(new String[0]));
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            assertThat(testOutput.resolve("compendium/adventures/a-diamond-in-the-rough")).isDirectory();
            assertThat(testOutput.resolve("compendium/adventures/book-of-lairs")).isDirectory();
            assertThat(testOutput.resolve("compendium/adventures/call-from-the-deep")).isDirectory();
            assertThat(testOutput.resolve("compendium/adventures/tavern-of-the-lost")).isDirectory();
            assertThat(testOutput.resolve("compendium/books/arkadia")).isDirectory();
            assertThat(testOutput.resolve("compendium/books/hamunds-herbalism-handbook")).isDirectory();
            assertThat(testOutput.resolve("compendium/books/plane-shift-amonkhet")).isDirectory();

            assertThat(testOutput.resolve("compendium/backgrounds/cook-variant-dndwiki-bestbackgrounds.md"))
                    .isRegularFile();
            assertThat(testOutput.resolve("compendium/classes/alchemist-dynamo-engineer-vss.md")).isRegularFile();

            TestUtils.assertDirectoryContents(testOutput, tui, (p, content) -> {
                List<String> errors = new ArrayList<>();
                content.forEach(l -> {
                    TestUtils.checkMarkdownLink(testOutput.toString(), p, l, errors);
                    TestUtils.commonTests(p, l, errors);
                });
                return errors;
            });
        }
    }

    @Test
    void testLiveData_5eUA(QuarkusMainLauncher launcher) {
        testOutput = rootTestOutput.resolve("ua");
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists() && TestUtils.PATH_5E_HOMEBREW.toFile().exists()) {
            TestUtils.deleteDir(testOutput);

            LaunchResult result = launcher.launch("--log", "--index",
                    "-c", TestUtils.TEST_RESOURCES.resolve("5e/sources-ua.json").toString(),
                    "-o", testOutput.toString(),
                    TestUtils.TEST_RESOURCES.resolve("5e/images-remote.json").toString(),
                    TestUtils.PATH_5E_TOOLS_DATA.toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana - Downtime.json").toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana - Encounter Building.json").toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana - Into the Wild.json").toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana - Quick Characters.json").toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana - Traps Revisited.json").toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana - When Armies Clash.json").toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana 2022 - Character Origins.json")
                            .toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana 2022 - Expert Classes.json").toString(),
                    TestUtils.PATH_5E_UA
                            .resolve("collection/Unearthed Arcana 2022 - The Cleric and Revised Species.json")
                            .toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana 2023 - Bastions and Cantrips.json")
                            .toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana 2023 - Druid & Paladin.json").toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana 2023 - Player's Handbook Playtest 5.json")
                            .toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana 2023 - Player's Handbook Playtest 6.json")
                            .toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana 2023 - Player's Handbook Playtest 7.json")
                            .toString());

            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);
            TestUtils.assertDirectoryContents(testOutput, tui, (p, content) -> {
                List<String> errors = new ArrayList<>();
                content.forEach(l -> {
                    TestUtils.checkMarkdownLink(testOutput.toString(), p, l, errors);
                    TestUtils.commonTests(p, l, errors);
                });
                return errors;
            });
        }
    }

    @Test
    void testLiveData_5eBookAdventureInJson(QuarkusMainLauncher launcher) {
        testOutput = rootTestOutput.resolve("json-book-adventure");
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {
            TestUtils.deleteDir(testOutput);

            // Make sure we find the data directory if the src dir is provided
            LaunchResult result = launcher.launch("--log", "--index",
                    "-o", testOutput.toString(),
                    TestUtils.TEST_RESOURCES.resolve("5e/images-remote.json").toString(),
                    TestUtils.PATH_5E_TOOLS_SRC.toString(),
                    TestUtils.TEST_RESOURCES.resolve("5e/sources-book-adventure.json").toString());

            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            List<Path> dirs = List.of(testOutput.resolve("compend ium/adventures/the-wild-beyond-the-witchlight"),
                    testOutput.resolve("compend ium/books/players-handbook-2014"));

            dirs.forEach(d -> {
                assertThat(d).isDirectory();
            });

            List<Path> files = List.of(testOutput.resolve("compend ium/backgrounds/witchlight-hand-wbtw.md"),
                    testOutput.resolve("compend ium/backgrounds/folk-hero.md"));

            files.forEach(f -> {
                assertThat(f).isRegularFile();
            });

            TestUtils.assertDirectoryContents(testOutput, tui, (p, content) -> {
                List<String> errors = new ArrayList<>();
                content.forEach(l -> {
                    TestUtils.checkMarkdownLink(testOutput.toString(), p, l, errors);
                    TestUtils.commonTests(p, l, errors);
                    if (l.contains("/ru les/")) {
                        errors.add("Found '/ru les/' " + p); // not escaped
                    }
                    if (l.contains("/compend ium/")) {
                        errors.add("Found '/compend ium/' " + p); // not escaped
                    }
                });
                return errors;
            });
        }
    }

    @Test
    void testLiveData_5eBookAdventureMinimalYaml(QuarkusMainLauncher launcher) {
        testOutput = rootTestOutput.resolve("yaml-adventure");
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {
            TestUtils.deleteDir(testOutput);

            LaunchResult result = launcher.launch("--log", "--index",
                    "-o", testOutput.toString(),
                    "-c", TestUtils.TEST_RESOURCES.resolve("5e/sources-no-phb.yaml").toString(),
                    TestUtils.TEST_RESOURCES.resolve("5e/images-remote.json").toString(),
                    TestUtils.PATH_5E_TOOLS_DATA.toString());

            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            List<Path> dirs = List.of(
                    testOutput.resolve("compendium/adventures/lost-mine-of-phandelver"),
                    testOutput.resolve("compendium/adventures/waterdeep-dragon-heist"),
                    testOutput.resolve("compendium/books/volos-guide-to-monsters"));

            dirs.forEach(d -> {
                assertThat(d).isDirectory();
            });
        }
    }

    @Test
    void testLiveData_Sample(QuarkusMainLauncher launcher) {
        testOutput = rootTestOutput.resolve("sample");
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {

            TestUtils.deleteDir(testOutput);

            Tui.instance().infof("--- Sample content ----- ");

            LaunchResult result = launcher.launch("--log", "--index",
                    "-o", testOutput.toString(),
                    "-c", TestUtils.TEST_RESOURCES.resolve("5e/sample.yaml").toString(),
                    TestUtils.PATH_5E_TOOLS_DATA.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);
        }
    }
}
