package dev.ebullient.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import dev.ebullient.convert.io.Tui;
import io.quarkus.test.junit.main.LaunchResult;
import io.quarkus.test.junit.main.QuarkusMainLauncher;
import io.quarkus.test.junit.main.QuarkusMainTest;

@QuarkusMainTest
public class Tools5eDataConvertTest {
    static Path testOutput;
    static Tui tui;

    @BeforeAll
    public static void setupDir() {
        setupDir("Tools5eDataConvertTest");
    }

    public static void setupDir(String root) {
        tui = new Tui();
        tui.init(null, false, false);
        testOutput = TestUtils.OUTPUT_ROOT_5E.resolve(root).resolve("test-cli");
        testOutput.toFile().mkdirs();
    }

    @Test
    void testLiveData_5e(QuarkusMainLauncher launcher) {
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {
            // SRD
            final Path srd_index = testOutput.resolve("srd-index");
            TestUtils.deleteDir(srd_index);

            LaunchResult result = launcher.launch("--index",
                    "-o", srd_index.toString(), TestUtils.PATH_5E_TOOLS_DATA.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            // Subset
            final Path subset_index = testOutput.resolve("subset-index");
            TestUtils.deleteDir(subset_index);

            result = launcher.launch("--index", "-s", "PHB,DMG,XGE,SCAG",
                    "-o", subset_index.toString(), TestUtils.PATH_5E_TOOLS_DATA.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);
        }
    }

    @Test
    void testLiveData_5eAllSources(QuarkusMainLauncher launcher) {
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {
            // All, I mean it. Really for real.. ALL.
            final Path allIndex = testOutput.resolve("all-index");
            TestUtils.deleteDir(allIndex);

            List<String> args = new ArrayList<>(List.of("--index", "--debug",
                    "-c", TestUtils.TEST_RESOURCES.resolve("sources-images.yaml").toString(),
                    "-o", allIndex.toString(),
                    TestUtils.PATH_5E_TOOLS_DATA.toString()));

            args.addAll(TestUtils.getFilesFrom(TestUtils.PATH_5E_TOOLS_DATA.resolve("adventure")));
            args.addAll(TestUtils.getFilesFrom(TestUtils.PATH_5E_TOOLS_DATA.resolve("book")));

            LaunchResult result = launcher.launch(args.toArray(new String[0]));
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            Tui tui = new Tui();
            tui.init(null, false, false);
            TestUtils.assertDirectoryContents(allIndex, tui, (p, content) -> {
                List<String> errors = new ArrayList<>();
                content.forEach(l -> {
                    TestUtils.checkMarkdownLinks(allIndex.toString(), p, l, errors);
                    TestUtils.commonTests(p, l, errors);
                });
                return errors;
            });
        }
    }

    @Test
    void testLiveData_5eOneSource(QuarkusMainLauncher launcher) {
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {
            Path target = testOutput.resolve("erlw");
            TestUtils.deleteDir(target);

            // No basics
            LaunchResult result = launcher.launch("-s", "ERLW", "--debug",
                    "-o", target.toString(), TestUtils.PATH_5E_TOOLS_DATA.toString());
            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            TestUtils.assertDirectoryContents(target, tui, (p, content) -> {
                List<String> errors = new ArrayList<>();
                content.forEach(l -> {
                    TestUtils.checkMarkdownLinks(target.toString(), p, l, errors);
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
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists() && TestUtils.PATH_5E_HOMEBREW.toFile().exists()) {
            Path target = testOutput.resolve("homebrew");
            TestUtils.deleteDir(target);

            LaunchResult result = launcher.launch("--debug", "--index",
                    "-c", TestUtils.TEST_RESOURCES.resolve("sources-homebrew.json").toString(),
                    "-o", target.toString(),
                    TestUtils.PATH_5E_TOOLS_DATA.toString(),
                    TestUtils.TEST_RESOURCES.resolve("psion.json").toString(),
                    TestUtils.TEST_RESOURCES.resolve("ermis-bg.json").toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("adventure/Anthony Joyce; The Blood Hunter Adventure.json").toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("adventure/JVC Parry; Call from the Deep.json").toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("adventure/Kobold Press; Book of Lairs.json").toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("background/D&D Wiki; Featured Quality Backgrounds.json").toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("book/Ghostfire Gaming; Grim Hollow Campaign Guide.json").toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("book/Ghostfire Gaming; Stibbles Codex of Companions.json").toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("book/MCDM Productions; Arcadia Issue 3.json").toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("class/D&D Wiki; Swashbuckler.json").toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("class/Foxfire94; Vampire.json").toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("class/badooga; Badooga's Psion.json").toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("class/Matthew Mercer; Blood Hunter (2022).json").toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("collection/Arcana Games; Arkadia.json").toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("collection/Ghostfire Gaming; Grim Hollow - The Monster Grimoire.json")
                            .toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("collection/Keith Baker; Exploring Eberron.json").toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("collection/Kobold Press; Deep Magic 14 Elemental Magic.json")
                            .toString(),
                    TestUtils.PATH_5E_HOMEBREW
                            .resolve("collection/Kobold Press; Deep Magic Backgrounds, Feats, & Subclasses.json").toString(),
                    TestUtils.PATH_5E_HOMEBREW
                            .resolve("collection/MCDM Productions; The Talent and Psionics Open Playtest Round 2.json")
                            .toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("creature/Kobold Press; Creature Codex.json")
                            .toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("creature/Kobold Press; Tome of Beasts.json")
                            .toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("creature/MCDM Productions; Flee, Mortals! preview.json").toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("creature/MCDM Productions; Flee, Mortals!.json").toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("creature/Nerzugal Role-Playing; Nerzugal's Extended Bestiary.json")
                            .toString(),
                    TestUtils.PATH_5E_HOMEBREW.resolve("deity/Frog God Games; The Lost Lands.json").toString());

            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            TestUtils.assertDirectoryContents(target, tui, (p, content) -> {
                List<String> errors = new ArrayList<>();
                content.forEach(l -> {
                    TestUtils.checkMarkdownLinks(target.toString(), p, l, errors);
                    TestUtils.commonTests(p, l, errors);
                });
                return errors;
            });
        }
    }

    @Test
    void testLiveData_5eUA(QuarkusMainLauncher launcher) {
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists() && TestUtils.PATH_5E_HOMEBREW.toFile().exists()) {
            Path target = testOutput.resolve("ua");
            TestUtils.deleteDir(target);

            LaunchResult result = launcher.launch("--debug", "--index",
                    "-c", TestUtils.TEST_RESOURCES.resolve("sources-ua.json").toString(),
                    "-o", target.toString(),
                    TestUtils.PATH_5E_TOOLS_DATA.toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana - Downtime.json").toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana - Encounter Building.json").toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana - Into the Wild.json").toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana - Quick Characters.json").toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana - Traps Revisited.json").toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana - When Armies Clash.json").toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana 2022 - Character Origins.json").toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana 2022 - Expert Classes.json").toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana 2022 - The Cleric and Revised Species.json")
                            .toString(),
                    TestUtils.PATH_5E_UA.resolve("collection/Unearthed Arcana 2023 - Bastions and Cantrips.json").toString(),
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
            TestUtils.assertDirectoryContents(target, tui, (p, content) -> {
                List<String> errors = new ArrayList<>();
                content.forEach(l -> {
                    TestUtils.checkMarkdownLinks(target.toString(), p, l, errors);
                    TestUtils.commonTests(p, l, errors);
                });
                return errors;
            });
        }
    }

    @Test
    void testCommand_5eBookAdventureInJson(QuarkusMainLauncher launcher) {
        if (TestUtils.PATH_5E_TOOLS_DATA.toFile().exists()) {
            Path target = testOutput.resolve("json-book-adventure");
            TestUtils.deleteDir(target);

            LaunchResult result = launcher.launch("--index",
                    "-o", target.toString(),
                    TestUtils.PATH_5E_TOOLS_DATA.toString(),
                    TestUtils.TEST_RESOURCES.resolve("sources-book-adventure.json").toString());

            assertThat(result.exitCode())
                    .withFailMessage("Command failed. Output:%n%s", TestUtils.dump(result))
                    .isEqualTo(0);

            List<Path> dirs = List.of(target.resolve("compend ium/adventures/the-wild-beyond-the-witchlight"),
                    target.resolve("compend ium/books/players-handbook"));

            dirs.forEach(d -> {
                assertThat(d).isDirectory();
            });

            List<Path> files = List.of(target.resolve("compend ium/backgrounds/witchlight-hand-wbtw.md"),
                    target.resolve("compend ium/backgrounds/folk-hero.md"));

            files.forEach(f -> {
                assertThat(f).isRegularFile();
            });

            TestUtils.assertDirectoryContents(target, tui, (p, content) -> {
                List<String> errors = new ArrayList<>();
                content.forEach(l -> {
                    TestUtils.checkMarkdownLinks(target.toString(), p, l, errors);
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
}
