package dev.ebullient.convert.io;

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

import dev.ebullient.convert.qute.QuteBase;
import dev.ebullient.convert.qute.QuteNote;
import dev.ebullient.convert.tools.dnd5e.qute.QuteName;
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
            FileMap fileMap = new FileMap(qs.title(),
                    qs.targetFile(),
                    basePath.resolve(qs.targetPath()).normalize());

            pathMap.computeIfAbsent(fileMap, k -> new ArrayList<>()).add(qs);
        }

        for (Map.Entry<FileMap, List<T>> pathEntry : pathMap.entrySet()) {
            if (pathEntry.getValue().size() > 1) {
                tui.warnf("Conflict: several entries would write to the same file:\n  %s",
                        pathEntry.getValue().stream().map(x -> String.format("%s: %s", x.getName(), x.source))
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

    <T extends QuteBase> FileMap doWrite(FileMap fileMap, T qs, Map<String, Integer> counts) {
        try {
            writeFile(fileMap, templates.render(qs));
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

        for (QuteNote n : notes) {
            String fn = n.targetFile();
            Path fd = targetDir.resolve(n.targetPath()).normalize();
            fd.toFile().mkdirs();
            String fileName = Tui.slugify(fn) + (fn.endsWith(".md") ? "" : ".md");
            writeNote(fd, fileName, n);
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
        String content = templates.render(n);
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
            Path target = rootDir.resolve("names-" + Tui.slugify(n.getName()) + ".md");
            String content = templates.render(n);
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
            this.fileName = Tui.slugify(fileName) + (fileName.endsWith(".md") ? "" : ".md");
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
