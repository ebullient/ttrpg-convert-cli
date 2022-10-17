package dev.ebullient.json5e.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import dev.ebullient.json5e.qute.QuteBackground;
import dev.ebullient.json5e.qute.QuteClass;
import dev.ebullient.json5e.qute.QuteDeity;
import dev.ebullient.json5e.qute.QuteFeat;
import dev.ebullient.json5e.qute.QuteItem;
import dev.ebullient.json5e.qute.QuteMonster;
import dev.ebullient.json5e.qute.QuteName;
import dev.ebullient.json5e.qute.QuteNote;
import dev.ebullient.json5e.qute.QuteRace;
import dev.ebullient.json5e.qute.QuteSource;
import dev.ebullient.json5e.qute.QuteSpell;
import dev.ebullient.json5e.qute.QuteSubclass;
import io.quarkus.qute.TemplateData;

public class MarkdownWriter {
    static final Comparator<FileMap> fileSort = (a, b) -> {
        if (a.dir.equals(b.dir)) {
            return a.fileName.compareTo(b.fileName);
        }
        return a.dir.compareTo(b.dir);
    };

    final Json5eTui tui;
    final Templates templates;
    final Path output;

    public MarkdownWriter(Path output, Templates templates, Json5eTui tui) {
        this.tui = tui;
        this.output = output;
        this.templates = templates;
    }

    public <T extends QuteSource> void writeFiles(List<T> elements, Path compendiumPath) {
        if (elements.isEmpty()) {
            return;
        }

        // Counts and sorted lists (to write index)
        Map<String, Integer> counts = new HashMap<>();
        Set<FileMap> fileMappings = new TreeSet<>(fileSort);

        // Find duplicates
        Map<FileMap, List<T>> pathMap = new HashMap<>();
        for (T qs : elements) {
            FileMap fileMap = new FileMap(qs.title(),
                    qs.targetFile(),
                    compendiumPath.resolve(qs.targetPath()).normalize());

            pathMap.computeIfAbsent(fileMap, k -> new ArrayList<>()).add(qs);
        }

        for (Map.Entry<FileMap, List<T>> pathEntry : pathMap.entrySet()) {
            if (pathEntry.getValue().size() > 1) {
                tui.warnf("Conflict: several entries would write to the same file:\n  %s",
                        pathEntry.getValue().stream().map(QuteSource::key)
                                .collect(Collectors.joining("\n  ")));
            }
            fileMappings.add(doWrite(pathEntry.getKey(), pathEntry.getValue().get(0), counts));
        }

        fileMappings.stream()
                .collect(Collectors.groupingBy(fm -> fm.dir))
                .forEach((dir, value) -> {
                    String fileName = dir.getFileName().toString();
                    String title = fileName.substring(0, 1).toUpperCase() + fileName.substring(1);
                    try {
                        writeFile(new FileMap(title, fileName, dir), templates.renderIndex(title, value));
                    } catch (IOException ex) {
                        throw new WrappedIOException(ex);
                    }
                });

        counts.forEach((k, v) -> tui.outPrintf("✅ Wrote %s files to %s.%n", v, k));
    }

    <T extends QuteSource> FileMap doWrite(FileMap fileMap, T qs, Map<String, Integer> counts) {
        String type = qs.getClass().getSimpleName();

        try {
            switch (type) {
                case "QuteBackground":
                    writeFile(fileMap, templates.renderBackground((QuteBackground) qs));
                    counts.compute("backgrounds", (k, v) -> (v == null) ? 1 : v + 1);
                    break;
                case "QuteClass":
                    writeFile(fileMap, templates.renderClass((QuteClass) qs));
                    counts.compute(QuteSource.CLASSES_PATH, (k, v) -> (v == null) ? 1 : v + 1);
                    break;
                case "QuteDeity":
                    writeFile(fileMap, templates.renderDeity((QuteDeity) qs));
                    counts.compute(QuteSource.DEITIES_PATH, (k, v) -> (v == null) ? 1 : v + 1);
                    break;
                case "QuteFeat":
                    writeFile(fileMap, templates.renderFeat((QuteFeat) qs));
                    counts.compute(QuteSource.FEATS_PATH, (k, v) -> (v == null) ? 1 : v + 1);
                    break;
                case "QuteItem":
                    writeFile(fileMap, templates.renderItem((QuteItem) qs));
                    counts.compute(QuteSource.ITEMS_PATH, (k, v) -> (v == null) ? 1 : v + 1);
                    break;
                case "QuteMonster":
                    writeFile(fileMap, templates.renderMonster((QuteMonster) qs));
                    counts.compute(QuteSource.MONSTERS_BASE_PATH, (k, v) -> (v == null) ? 1 : v + 1);
                    break;
                case "QuteRace":
                    writeFile(fileMap, templates.renderRace((QuteRace) qs));
                    counts.compute("races", (k, v) -> (v == null) ? 1 : v + 1);
                    break;
                case "QuteSpell":
                    writeFile(fileMap, templates.renderSpell((QuteSpell) qs));
                    counts.compute(QuteSource.SPELLS_PATH, (k, v) -> (v == null) ? 1 : v + 1);
                    break;
                case "QuteSubclass":
                    writeFile(fileMap, templates.renderSubclass((QuteSubclass) qs));
                    counts.compute(QuteSource.CLASSES_PATH, (k, v) -> (v == null) ? 1 : v + 1);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown file type:" + type);
            }
        } catch (IOException e) {
            throw new WrappedIOException(e);
        }
        return fileMap;
    }

    void writeFile(FileMap fileMap, String content) throws IOException {
        Path targetDir = Paths.get(output.toString(), fileMap.dir.toString());
        targetDir.toFile().mkdirs();

        Path target = targetDir.resolve(fileMap.fileName);
        Files.write(target, content.getBytes(StandardCharsets.UTF_8));
    }

    public void writeNotes(Path dir, Collection<QuteNote> notes) {
        Path targetDir = output.resolve(dir);
        targetDir.toFile().mkdirs();

        for (QuteNote n : notes) {
            String fileName = tui.slugify(n.getName()) + ".md";
            writeNote(targetDir, fileName, n);
        }

        tui.outPrintf("✅ Wrote %s notes (rules and tables).%n", notes.size());
    }

    public void writeNotes(Path dir, Map<String, QuteNote> notes) {
        Path rootDir = output.resolve(dir);
        rootDir.toFile().mkdirs();

        notes.forEach((k, v) -> {
            Path fullPath = rootDir.resolve(k);
            Path targetDir = fullPath.getParent();
            String fileName = fullPath.getFileName().toString();
            targetDir.toFile().mkdirs();
            writeNote(targetDir, fileName, v);
        });

        tui.outPrintf("✅ Wrote %s notes (rules and tables).%n", notes.size());
    }

    public void writeNote(Path targetDir, String fileName, QuteNote n) {
        Path target = targetDir.resolve(fileName);
        String content = templates.renderNote(n);
        try {
            Files.write(target, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new WrappedIOException(e);
        }
    }

    public void writeNames(Path dir, Collection<QuteName> names) {
        Path rootDir = output.resolve(dir);
        rootDir.toFile().mkdirs();

        names.forEach(n -> {
            Path target = rootDir.resolve("names-" + tui.slugify(n.getName()) + ".md");
            String content = templates.renderName(n);
            try {
                Files.write(target, content.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                throw new WrappedIOException(e);
            }
        });
        tui.outPrintf("✅ Wrote %s name tables.%n", names.size());
    }

    @TemplateData
    public static class FileMap {

        public final String title;
        public final String fileName;
        public final Path dir;

        public FileMap(String title, String fileName, Path dirName) {
            this.title = title;
            this.fileName = Json5eTui.slugifier().slugify(fileName) + (fileName.endsWith(".md") ? "" : ".md");
            this.dir = dirName;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
            result = prime * result + ((dir == null) ? 0 : dir.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;

            FileMap other = (FileMap) obj;
            if (fileName == null) {
                if (other.fileName != null) {
                    return false;
                }
            } else if (!fileName.equals(other.fileName)) {
                return false;
            }

            if (dir == null) {
                return other.dir == null;
            }
            return dir.equals(other.dir);
        }
    }

    // IOException -> RuntimeException .. for working w/in stream/function
    public static class WrappedIOException extends RuntimeException {
        WrappedIOException(IOException cause) {
            super(cause);
        }
    }
}
