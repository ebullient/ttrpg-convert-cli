package dev.ebullient.convert.io;

import static dev.ebullient.convert.StringUtil.toTitleCase;

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
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Function;
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

    public static final Comparator<IndexEntry> sortEntryByPath = Comparator
            .comparing(IndexEntry::fileName);

    public static final Comparator<IndexEntry> sortEntryByTitle = Comparator
            .comparing(IndexEntry::title)
            .thenComparing(IndexEntry::fileName);

    final Tui tui;
    final Templates templates;
    final Path output;

    public MarkdownWriter(Path output, Templates templates, Tui tui) {
        this.tui = tui;
        this.output = output;
        this.templates = templates;
    }

    public <T extends QuteBase> void writeFiles(Path basePath, List<T> elements, IndexContext ctx) {
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

        // Accumulate index entries and track distinct dirs for parent rollup
        Set<Path> indexedDirs = new LinkedHashSet<>();
        for (FileMap fm : fileMappings) {
            if (fm.renderIndex) {
                ctx.accumulateEntry(fm.dir, new IndexEntry(fm.title, fm.fileName, "./" + fm.fileName));
                indexedDirs.add(fm.dir);
            }
        }

        // Parent rollup: add each subfolder as an entry in its parent dir.
        // e.g., compendium/bestiary/beast → adds "Beast" entry into compendium/bestiary/
        for (Path subdir : indexedDirs) {
            if (subdir.getParent() != null && !subdir.getParent().equals(basePath)) {
                String dirName = subdir.getFileName().toString();
                String title = dirName.substring(0, 1).toUpperCase() + dirName.substring(1);
                ctx.accumulateEntry(subdir.getParent(),
                        new IndexEntry(title, dirName + ".md", "./" + dirName + "/" + dirName + ".md"));
            }
        }

        counts.forEach((k, v) -> tui.printlnf(Msg.OK, "Wrote %s %s files.", v, k));
    }

    <T extends QuteBase> FileMap doWrite(FileMap fileMap, T qs, Map<String, Integer> counts) {
        try {
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

    public void writeNotes(Path dir, Collection<QuteNote> notes, boolean compendium, IndexContext ctx) {
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

            // Accumulate index entries using vault-relative paths
            Path relFd = dir.resolve(n.targetPath()).normalize();
            String dirName = relFd.getFileName().toString();

            // A note "owns" its folder if its slugified name matches the folder name.
            // e.g., traits/traits.md, conditions/conditions.md, pf2e core-rulebook/core-rulebook.md
            boolean isOwnFolderNote = fileName.equals(dirName + ".md") && !relFd.equals(dir);

            if (isOwnFolderNote) {
                // Protect: writeIndexes must not overwrite this note
                ctx.protectDir(relFd);
                // Add it as an entry in its parent (if parent is not the base)
                if (relFd.getParent() != null && !relFd.getParent().equals(dir)) {
                    ctx.accumulateEntry(relFd.getParent(),
                            new IndexEntry(n.title(), dirName + ".md", "./" + dirName + "/" + fileName));
                }
            } else if (!relFd.equals(dir)) {
                // Normal note in a subdirectory — add to that dir's index
                ctx.accumulateEntry(relFd, new IndexEntry(n.title(), fileName, "./" + fileName));
                // Parent rollup: books/<slug>/ch.md → add <slug>.md entry to books/
                if (relFd.getParent() != null && !relFd.getParent().equals(dir)) {
                    String fdDirName = relFd.getFileName().toString();
                    String fdTitle = ctx.toTitle(fdDirName);
                    ctx.accumulateEntry(relFd.getParent(),
                            new IndexEntry(fdTitle, fdDirName + ".md",
                                    "./" + fdDirName + "/" + fdDirName + ".md"));
                }
            }
            // Notes at base (relFd == dir) are not indexed
        }

        tui.printlnf(Msg.OK, "Wrote %s notes to %s.",
                notes.size(),
                compendium ? "compendium" : "rules");
    }

    public void writeIndexes(IndexContext ctx) {
        for (Map.Entry<Path, SortedSet<IndexEntry>> entry : ctx.accumulator.entrySet()) {
            Path relDir = entry.getKey();
            SortedSet<IndexEntry> entries = entry.getValue();
            if (ctx.protectedDirs.contains(relDir) || entries.isEmpty()) {
                continue;
            }
            String fileName = relDir.getFileName().toString();
            String title = ctx.toTitle(fileName);
            String vaultPath = relDir.resolve(fileName).toString().replace('\\', '/');
            try {
                writeFile(new FileMap(title, fileName, relDir, false),
                        templates.renderIndex(title, vaultPath, entries));
            } catch (IOException ex) {
                throw new UncheckedIOException(ex);
            }
        }
    }

    public static String toTitle(String dirName) {
        return toTitleCase(dirName.replace("-", " "), false);
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

    public static class IndexContext {
        // vault-relative path → sorted IndexEntries for that dir's folder note
        final Map<Path, SortedSet<IndexEntry>> accumulator = new HashMap<>();
        // vault-relative dirs where a folder note was already written (must not be overwritten)
        final Set<Path> protectedDirs = new HashSet<>();

        final Function<String, String> titleTransform;
        final Function<Path, Comparator<IndexEntry>> sortOrder;

        public IndexContext(Function<String, String> titleTransform, Function<Path, Comparator<IndexEntry>> sortOrder) {
            this.titleTransform = titleTransform;
            this.sortOrder = sortOrder;
        }

        public String toTitle(String fileName) {
            return this.titleTransform.apply(fileName);
        }

        public void accumulateEntry(Path relativeDir, IndexEntry entry) {
            final var sortBy = sortOrder.apply(relativeDir);
            accumulator.computeIfAbsent(relativeDir, k -> new TreeSet<>(sortBy)).add(entry);
        }

        public void protectDir(Path relativeDir) {
            protectedDirs.add(relativeDir);
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
