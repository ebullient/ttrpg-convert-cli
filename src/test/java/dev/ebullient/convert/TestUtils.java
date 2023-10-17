package dev.ebullient.convert;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;

import dev.ebullient.convert.io.Tui;
import io.quarkus.test.junit.main.LaunchResult;

public class TestUtils {
    public final static Path PROJECT_PATH = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    public final static Path OUTPUT_ROOT_5E = PROJECT_PATH.resolve("target/test-5e");
    public final static Path OUTPUT_ROOT_PF2 = PROJECT_PATH.resolve("target/test-pf2");

    public final static Path TEST_RESOURCES = PROJECT_PATH.resolve("src/test/resources/");

    // for compile/test purposes. Must clone/sync separately.
    public final static Path TOOLS_PATH_5E = PROJECT_PATH.resolve("sources/5etools-mirror-1.github.io/data");
    public final static Path HOMEBREW_PATH_5E = PROJECT_PATH.resolve("sources/5e-homebrew");
    public final static Path TOOLS_PATH_PF2E = PROJECT_PATH.resolve("sources/Pf2eTools/data");

    public final static Path README = PROJECT_PATH.resolve("README.md").normalize().toAbsolutePath();
    static String USAGE = PROJECT_PATH.resolve("docs/usage").normalize().toAbsolutePath().toString();

    // Obnoxious regular expression because markdown links are complicated:
    // Matches: [link text](vaultPath "title")
    // - link text is optional, and may contain parentheses. Use a negative lookahead for ](
    // - vaultPath is required.
    //   - Slugified file names will not contain spaces
    //   - Anchors can include parenthesis. Spaces will be escaped with %20.
    //   - Stop matching the vaultPath if you encounter a space (precedes an optional title) or a following markdown link
    // - title is optional
    final static String nextLink = "(?!\\]\\()"; // negative lookahead for ](
    final static String linkTitle = "( \".+?\")?"; // optional link title
    final static String linkText = "(" + nextLink + ".)+"; // any sequence of characters except ](
    final static String vaultPath = "(" + nextLink + "[^ ])+"; // any sequence of characters except ]( or space
    final static Pattern markdownLinkPattern = Pattern
            .compile("\\[" + linkText + "\\]\\((" + vaultPath + ")" + linkTitle + "\\)");
    final static Pattern blockRefPattern = Pattern.compile("[^#\\[]+(\\^[^ ]+)");

    final static Map<Path, List<String>> pathHeadings = new HashMap<>();
    final static Map<Path, List<String>> pathBlockReferences = new HashMap<>();

    static void assertContents(Path path1, Path path2, boolean areEqual) throws IOException {
        try (RandomAccessFile randomAccessFile1 = new RandomAccessFile(path1.toFile(), "r");
                RandomAccessFile randomAccessFile2 = new RandomAccessFile(path2.toFile(), "r")) {

            FileChannel ch1 = randomAccessFile1.getChannel();
            FileChannel ch2 = randomAccessFile2.getChannel();
            if (areEqual) {
                assertThat(ch1.size()).isEqualTo(ch2.size());
            } else {
                assertThat(ch1.size()).isNotEqualTo(ch2.size());
            }

            MappedByteBuffer m1 = ch1.map(FileChannel.MapMode.READ_ONLY, 0L, ch1.size());
            MappedByteBuffer m2 = ch2.map(FileChannel.MapMode.READ_ONLY, 0L, ch2.size());

            if (areEqual) {
                assertThat(m1).isEqualTo(m2);
            } else {
                assertThat(m1).isNotEqualTo(m2);
            }
        }
    }

    public static void checkMarkdownLinks(String baseDir, Path p, String line, List<String> errors) {
        Path absPath = p.normalize().toAbsolutePath();
        if (!absPath.toString().endsWith(".md") || absPath.equals(README) || absPath.toString().startsWith(USAGE)) {
            // GH anchor links
            return;
        }
        List<String> e = new ArrayList<>();
        // replace (Level x) or (1st level) or (Firearms) with :whatever: to simplify matching
        Matcher links = markdownLinkPattern.matcher(line);
        links.results().forEach(m -> {
            String path = m.group(2);
            String anchor = null;
            Path resource = p;
            int hash = path.indexOf('#');

            if (path.startsWith("http") && path.contains(" ")) {
                e.add(String.format("HTTP path with space in %s: %s ", p, m.group(0)));
                return;
            } else if (path.startsWith("http") || path.contains("vaultPath")
                    || path.startsWith("{it.") || path.startsWith("{resource.")) {
                // template examples, or other non-file links
                return;
            }

            if (hash == 0) {
                anchor = path.substring(1);
            } else if (hash > 0) {
                anchor = path.substring(hash + 1);
                path = path.substring(0, hash)
                        .replace("%20", " ");
                if (!p.toString().endsWith(path)) {
                    resource = Path.of(baseDir, path);
                }

                if (!resource.toFile().exists()) {
                    resource = p.getParent().resolve(path);
                    if (!resource.toFile().exists()) {
                        e.add(String.format("Unresolvable reference in %s: %s ", p, m.group(0)));
                        return;
                    }
                }
            }

            if (anchor != null) {
                if (!resource.toString().endsWith(".md")) {
                    return;
                }
                if (anchor.startsWith("^")) {
                    List<String> blockRefs = findBlockRefsIn(resource);
                    if (!blockRefs.contains(anchor)) {
                        e.add(String.format("Unresolvable block reference (%s) in %s:  %s", anchor, p, m.group(0)));
                    }
                } else {
                    String heading = simplifyAnchor(anchor).replaceAll("%20", " ");
                    String ghHeading = heading.replace("-", " ");
                    List<String> headings = findHeadingsIn(resource);
                    // obsidian or github style anchors
                    if (!headings.contains(heading) && !headings.contains(ghHeading)) {
                        e.add(String.format("Unresolvable anchor (%s) in %s: %s", heading, p, m.group(0)));
                    }
                }
            }
        });
        errors.addAll(e);
    }

    public static List<String> findHeadingsIn(Path p) {
        if (!p.toString().endsWith(".md")) {
            return List.of();
        }
        return pathHeadings.computeIfAbsent(p, key -> {
            List<String> headings = new ArrayList<>();
            try (Stream<String> lines = Files.lines(key)) {
                lines.forEach(l -> {
                    if (l.startsWith("#")) {
                        if (l.contains(".")) {
                            System.out.println("🔮 Found dot in heading in " + p + ": " + l);
                        }
                        headings.add(simplifyAnchor(l));
                    }
                });
            } catch (UncheckedIOException | IOException e) {
                System.err.println(String.format("🛑 Error finding headings in %s: %s", p, e.toString()));
            }
            return headings;
        });
    }

    private static String simplifyAnchor(String s) {
        return s.replace("#", "")
                .replace(".", "")
                .replace(":", "")
                .replace("(", "")
                .replace(")", "")
                .toLowerCase()
                .trim();
    }

    public static List<String> findBlockRefsIn(Path p) {
        if (!p.toString().endsWith(".md")) {
            return List.of();
        }
        return pathBlockReferences.computeIfAbsent(p, key -> {
            List<String> blockrefs = new ArrayList<>();
            try (Stream<String> lines = Files.lines(key)) {
                lines.forEach(l -> {
                    if (l.startsWith("^")) {
                        blockrefs.add(l.trim());
                    } else {
                        Matcher blockRefs = blockRefPattern.matcher(l);
                        blockRefs.results().forEach(m -> {
                            blockrefs.add(m.group(1));
                        });
                    }
                });
            } catch (UncheckedIOException | IOException e) {
                System.err.println(String.format("🛑 Error finding block references in %s: %s", p, e.toString()));
            }
            return blockrefs;
        });
    }

    /**
     * Common content tests. Will append an error if the text contains an unresolved reference,
     * either &#123;@ or &#123;#.
     *
     * @param p Path of content
     * @param l Line of content
     * @param errors List of errors to append
     */
    public static void commonTests(Path p, String l, List<String> errors) {
        if (l.contains("{@")) {
            errors.add(String.format("Found {@ in %s: %s", p, l));
        }
        if (l.contains("{#")) {
            errors.add(String.format("Found {# in %s: %s", p, l));
        }
        if (l.contains("%% ERROR")) {
            errors.add(String.format("Found template error in %s: %s", p, l));
        }
    }

    /**
     * Consumes a path, and content as a list of strings.
     * For each line in the content, it will apply {@link #commonTests(Path, String, List)}
     *
     * @return array of discovered errors
     */
    static final BiFunction<Path, List<String>, List<String>> checkContents = (p, content) -> {
        List<String> e = new ArrayList<>();
        content.forEach(l -> commonTests(p, l, e));
        return e;
    };

    /**
     * Look at all files in the directory and perform common content checks
     *
     * @param directory Directory to inspect
     * @param tui Text UI for errors
     * @see #assertDirectoryContents(Path, Tui, BiFunction)
     * @see #checkContents
     */
    public static void assertDirectoryContents(Path directory, Tui tui) {
        assertDirectoryContents(directory, tui, checkContents);
    }

    /**
     * Look at all files in the directory and apply the provided content checker to each file
     *
     * @param directory Directory to inspect
     * @param tui Text UI for errors
     * @param checker additional tests to apply to each file in the directory (passed to
     *        {@link #checkDirectoryContents(Path, Tui, BiFunction)})
     * @see #checkDirectoryContents
     */
    public static void assertDirectoryContents(Path directory, Tui tui, BiFunction<Path, List<String>, List<String>> checker) {
        List<String> errors = checkDirectoryContents(directory, tui, checker);
        assertThat(errors).isEmpty();
    }

    static List<String> checkDirectoryContents(Path directory, Tui tui,
            BiFunction<Path, List<String>, List<String>> checker) {
        List<String> errors = new ArrayList<>();

        Path bestiary = directory.resolve("bestiary");
        Path nullDir = bestiary.resolve("null");
        Path compendium = directory.resolve("compendium");
        if (bestiary.toFile().exists() && nullDir.toFile().exists()) {
            errors.add(String.format("Found null directory in bestiary: %s", nullDir));
        }
        if (bestiary.toFile().exists() && compendium.toFile().exists()) {
            errors.add(String.format("Found compendium directory as a peer of bestiary: %s", compendium));
        }

        try (Stream<Path> walk = Files.list(directory)) {
            walk.forEach(p -> {
                if (p.toFile().isDirectory()) {
                    errors.addAll(checkDirectoryContents(p, tui, checker));
                    return;
                }
                if (!p.toString().endsWith(".md")) {
                    if (!p.toString().endsWith(".png")
                            && !p.toString().endsWith(".jpg")
                            && !p.toString().endsWith(".css")
                            && !p.toString().endsWith(".svg")
                            && !p.toString().endsWith(".webp")
                            && !p.toString().endsWith(".json")
                            && !p.toString().endsWith(".yaml")) {
                        errors.add(String.format("Found file that was not markdown: %s", p));
                    }
                    return;
                }
                try {
                    errors.addAll(checker.apply(p, Files.readAllLines(p)));
                } catch (IOException e) {
                    e.printStackTrace();
                    errors.add(String.format("Unable to read lines from %s: %s", p, e));
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
            errors.add(String.format("Unable to parse files in directory %s: %s", directory, e));
        }

        return errors;
    }

    public static String dump(LaunchResult result) {
        return "\n" + result.getOutput() + "\nSystem err:\n" + result.getErrorOutput();
    }

    public static void deleteDir(Path path) {
        if (!path.toFile().exists()) {
            return;
        }

        try (Stream<Path> paths = Files.walk(path)) {
            paths.sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Assertions.assertFalse(path.toFile().exists());
    }

    public static List<String> getFilesFrom(Path directory) {
        try (Stream<Path> paths = Files.walk(directory)) {
            return paths
                    .filter(p -> p.toFile().isFile())
                    .map(Path::toString)
                    .filter(s -> s.endsWith(".json"))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
