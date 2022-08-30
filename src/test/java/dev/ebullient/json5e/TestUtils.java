package dev.ebullient.json5e;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.json5e.io.Json5eTui;
import dev.ebullient.json5e.tools5e.JsonIndex;
import io.quarkus.test.junit.main.LaunchResult;

public class TestUtils {

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

    static void assertFileContent(Path p) throws IOException {
        List<String> errors = new ArrayList<>();
        Files.readAllLines(p)
                .forEach(l -> {
                    if (l.contains("{@")) {
                        errors.add(String.format("Found {@ in %s: %s", p, l));
                    }
                    if (l.contains("{#")) {
                        errors.add(String.format("Found {# in %s: %s", p, l));
                    }
                });
        assertThat(errors).isEmpty();
    }

    public static String dump(LaunchResult result) {
        return "\n" + result.getOutput() + "\nSystem err:\n" + result.getErrorOutput();
    }

    public static void deleteDir(Path path) {
        if (!path.toFile().exists()) {
            return;
        }

        try {
            Files.walk(path)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Assertions.assertFalse(path.toFile().exists());
    }

    public static JsonNode doParse(String resourceName) throws Exception {
        try (InputStream is = Import5eToolsConvertTest.class.getClassLoader().getResourceAsStream(resourceName)) {
            return Json5eTui.MAPPER.readTree(is);
        }
    }

    public static JsonNode doParse(Path resourcePath) throws Exception {
        try (InputStream is = Files.newInputStream(resourcePath, StandardOpenOption.READ)) {
            return Json5eTui.MAPPER.readTree(is);
        }
    }

    static void fullIndex(JsonIndex index, Path resourcePath, Json5eTui tui) throws Exception {
        try (Stream<Path> stream = Files.list(resourcePath)) {
            stream
                    .filter(p -> !p.getFileName().toString().startsWith("adventure"))
                    .filter(p -> !p.getFileName().toString().startsWith("foundry"))
                    .filter(p -> !p.getFileName().toString().startsWith("gen"))
                    .filter(p -> !p.getFileName().toString().startsWith("roll20"))
                    .forEach(p -> {
                        File f = p.toFile();
                        if (f.isDirectory()) {
                            try {
                                fullIndex(index, p, tui);
                            } catch (Exception e) {
                                tui.errorf(e, "Error parsing %s", p);
                                throw new RuntimeException(e);
                            }
                        } else if (f.getName().endsWith(".json")) {
                            try {
                                index.importTree(doParse(p));
                            } catch (Exception e) {
                                tui.errorf(e, "Error parsing %s", p);
                                throw new RuntimeException(e);
                            }
                        }
                    });
        }
    }

    final static Path PROJECT_PATH = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
    // for compile/test purposes. Must clone/sync separately.
    final static Path TOOLS_PATH = PROJECT_PATH.resolve("5etools-mirror-1.github.io/data");
    final static Path TEST_PATH_JSON = PROJECT_PATH.resolve("src/test/resources/paths.json");
    final static Path OUTPUT_ROOT = PROJECT_PATH.resolve("target/test-data");
}
