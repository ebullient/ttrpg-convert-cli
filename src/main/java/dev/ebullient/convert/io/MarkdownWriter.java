package dev.ebullient.convert.io;

import java.io.IOException;
import java.io.UncheckedIOException;
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

import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.qute.QuteNote;
import io.quarkus.qute.TemplateData;

public class MarkdownWriter {
    static final Comparator<FileMap> fileSort = (a, b) -> {
        if (a.dir.equals(b.dir)) {
            return a.fileName.compareTo(b.fileName);
        }
        return a.dir.compareTo(b.dir);
    };

    final Tui tui;
    final Templates templates;
    final Path output;

    public MarkdownWriter(Path output, Templates templates, Tui tui) {
        this.tui = tui;
        this.output = output;
        this.templates = templates;
    }

    public <T extends QuteBase> void writeFiles(Path basePath, List<T> elements) {
        if (elements.isEmpty()) {
            return;
        }

        // Counts and sorted lists (to write index)
        Map<String, Integer> counts = new HashMap<>();
        Set<FileMap> fileMappings = new TreeSet<>(fileSort);

        // Find duplicates
        Map<FileMap, List<T>> pathMap = new HashMap<>();
        for (T qs : elements) {
            Path path = basePath.resolve(qs.targetPath()).normalize();
            FileMap fileMap = new FileMap(qs.title(),
                    qs.targetFile(),
                    path,
                    qs.createIndex());

            pathMap.computeIfAbsent(fileMap, k -> new ArrayList<>()).add(qs);

            Collection<QuteBase> inlineNotes = qs.inlineNotes();
            for (QuteBase n : inlineNotes) {
                FileMap fm = new FileMap(n.title(),
                        n.targetFile(),
                        path,
                        false);

                pathMap.computeIfAbsent(fm, k -> new ArrayList<>()).add(qs);
            }
        }

        for (Map.Entry<FileMap, List<T>> pathEntry : pathMap.entrySet()) {
            if (pathEntry.getValue().size() > 1) {
                tui.warnf("Conflict: several entries would write to the same file: (%s)\n  %s",
                        pathEntry.getKey().fileName,
                        pathEntry.getValue().stream().map(x -> String.format("[%s]: %s",
                                x.getName(),
                                x.sources().getKey()))
                                .collect(Collectors.joining("\n  ")));
            }
            fileMappings.add(doWrite(pathEntry.getKey(), pathEntry.getValue().get(0), counts));
        }

        fileMappings.stream()
                .filter(fm -> fm.renderIndex)
                .collect(Collectors.groupingBy(fm -> fm.dir))
                .forEach((dir, value) -> {
                    String fileName = dir.getFileName().toString();
                    String title = fileName.substring(0, 1).toUpperCase() + fileName.substring(1);
                    List<IndexEntry> entries = value.stream()
                            .map(fm -> new IndexEntry(fm.title, fm.fileName, "./" + fm.fileName)) // folder note
                            .collect(Collectors.toList());
                    try {
                        String vaultPath = dir.resolve(fileName).toString();
                        writeFile(new FileMap(title, fileName, dir, false), templates.renderIndex(title, vaultPath, entries));
                    } catch (IOException ex) {
                        throw new UncheckedIOException(ex);
                    }
                });

        counts.forEach((k, v) -> tui.printlnf(Msg.OK, "Wrote %s %s files.", v, k));
    }

    <T extends QuteBase> FileMap doWrite(FileMap fileMap, T qs, Map<String, Integer> counts) {
        try {
            qs.vaultPath(fileMap.dir + "/" + fileMap.fileName);
            writeFile(fileMap, templates.render(qs));
            counts.compute(qs.indexType().name(), (k, v) -> (v == null) ? 1 : v + 1);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return fileMap;
    }

    void writeFile(FileMap fileMap, String content) throws IOException {
        Path targetDir = Paths.get(output.toString(), fileMap.dir.toString());
        targetDir.toFile().mkdirs();

        Path target = targetDir.resolve(fileMap.fileName);
        Files.write(target, content.getBytes(StandardCharsets.UTF_8));
    }

    public void writeNotes(Path dir, Collection<QuteNote> notes, boolean compendium) {
        if (notes.isEmpty()) {
            return;
        }
        Path targetDir = output.resolve(dir);

        for (QuteNote n : notes) {
            String fn = n.targetFile();
            Path fd = targetDir.resolve(n.targetPath()).normalize();
            fd.toFile().mkdirs();
            String fileName = Tui.slugify(fn) + (fn.endsWith(".md") ? "" : ".md");
            String relative = dir.resolve(n.targetPath()).normalize().toString().replace("\\", "/");
            if (relative.isBlank() || relative.equals(".")) {
                relative = "";
            } else {
                relative += "/";
            }
            n.vaultPath(relative + fileName);
            writeNote(fd, fileName, n);
        }

        tui.printlnf(Msg.OK, "Wrote %s notes to %s.",
                notes.size(),
                compendium ? "compendium" : "rules");
    }

    private void writeNote(Path targetDir, String fileName, QuteNote n) {
        Path target = targetDir.resolve(fileName);
        String content = templates.render(n);
        try {
            Files.write(target, content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @TemplateData
    public record IndexEntry(String title, String fileName, String relativePath) {

        @Override
        public String toString() {
            return "IndexEntry [title=" + title + ", fileName=" + fileName + ", relativePath=" + relativePath + "]";
        }
    }

    @TemplateData
    public static class FileMap {

        public final String title;
        public final String fileName;
        public final Path dir;
        public final boolean renderIndex;

        public FileMap(String title, String fileName, Path dirName, boolean renderIndex) {
            this.title = title;
            this.fileName = Tui.slugify(fileName) + (fileName.endsWith(".md") ? "" : ".md");
            this.dir = dirName;
            this.renderIndex = renderIndex;
        }

        @Override
        public String toString() {
            return "FileMap [title=" + title + ", fileName=" + fileName + ", dir=" + dir + "]";
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
}
