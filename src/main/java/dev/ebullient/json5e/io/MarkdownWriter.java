package dev.ebullient.json5e.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import dev.ebullient.json5e.qute.QuteBackground;
import dev.ebullient.json5e.qute.QuteClass;
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

    final Json5eTui tui;
    final Templates templates;
    final Path output;

    public MarkdownWriter(Path output, Templates templates, Json5eTui tui) {
        this.tui = tui;
        this.output = output;
        this.templates = templates;
    }

    public <T extends QuteSource> void writeFiles(List<T> elements, String kind) {
        if (elements.isEmpty()) {
            return;
        }
        Set<FileMap> fileMappings = new TreeSet<>((a, b) -> {
            if (a.dirName.equals(b.dirName)) {
                return a.fileName.compareTo(b.fileName);
            }
            return a.dirName.compareTo(b.dirName);
        });

        elements.forEach(x -> {
            String type = x.getClass().getSimpleName();
            FileMap fileMap = new FileMap(x.getName(), tui.slugify(x.getName()));
            try {
                switch (type) {
                    case "QuteBackground":
                        fileMap.dirName = "backgrounds";
                        writeFile(fileMap, templates.renderBackground((QuteBackground) x));
                        break;
                    case "QuteClass":
                        fileMap.dirName = "classes";
                        writeFile(fileMap, templates.renderClass((QuteClass) x));
                        break;
                    case "QuteFeat":
                        fileMap.dirName = "feats";
                        writeFile(fileMap, templates.renderFeat((QuteFeat) x));
                        break;
                    case "QuteItem":
                        fileMap.dirName = "items";
                        writeFile(fileMap, templates.renderItem((QuteItem) x));
                        break;
                    case "QuteMonster":
                        QuteMonster m = (QuteMonster) x;
                        fileMap = new FileMap(m.getName(),
                                tui.slugify(m.getName()),
                                QuteMonster.getSubdir(m));
                        writeFile(fileMap, templates.renderMonster(m));
                        break;
                    case "QuteRace":
                        fileMap.dirName = "races";
                        writeFile(fileMap, templates.renderRace((QuteRace) x));
                        break;
                    case "QuteSpell":
                        fileMap.dirName = "spells";
                        writeFile(fileMap, templates.renderSpell((QuteSpell) x));
                        break;
                    case "QuteSubclass":
                        QuteSubclass s = (QuteSubclass) x;
                        String title = s.parentClass + ": " + s.getName();
                        fileMap = new FileMap(title, tui.slugify(title), "classes");
                        writeFile(fileMap, templates.renderSubclass(s));
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown file type:" + type);
                }
                fileMappings.add(fileMap);
            } catch (IOException e) {
                throw new WrappedIOException(e);
            }
        });
        fileMappings.stream()
                .collect(Collectors.groupingBy(fm -> fm.dirName))
                .forEach((dirName, value) -> {
                    int lastIndex = dirName.lastIndexOf("/");
                    String fileName = lastIndex > 0 ? dirName.substring(lastIndex + 1) : dirName;
                    String title = fileName.substring(0, 1).toUpperCase() + fileName.substring(1);
                    try {
                        writeFile(new FileMap(title, fileName, dirName), templates.renderIndex(title, value));
                    } catch (IOException ex) {
                        throw new WrappedIOException(ex);
                    }
                });
        tui.outPrintf("✅ Wrote %s %s files.%n", (fileMappings.size() + 1), kind == null ? "markdown" : kind);
    }

    void writeFile(FileMap fileMap, String content) throws IOException {
        Path targetDir = Paths.get(output.toString(), fileMap.dirName);
        targetDir.toFile().mkdirs();

        Path target = targetDir.resolve(fileMap.fileName);

        Files.write(target, content.getBytes(StandardCharsets.UTF_8));
    }

    public void writeNotes(String dirName, Collection<QuteNote> notes) {
        Path targetDir = Paths.get(output.toString(), dirName);
        targetDir.toFile().mkdirs();

        for (QuteNote n : notes) {
            String fileName = tui.slugify(n.getName()) + ".md";
            writeNote(targetDir, fileName, n);
        }

        tui.outPrintf("✅ Wrote %s notes (rules and tables).%n", notes.size());
    }

    public void writeNotes(String dirName, Map<String, QuteNote> notes) {
        Path rootDir = Paths.get(output.toString(), dirName);
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

    public void writeNames(String dirName, Collection<QuteName> names) {
        Path rootDir = Paths.get(output.toString(), dirName);
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
        public String dirName;

        public FileMap(String title, String fileName) {
            this.title = title;
            this.fileName = fileName + (fileName.endsWith(".md") ? "" : ".md");
        }

        public FileMap(String title, String fileName, String dirName) {
            this.title = title;
            this.fileName = fileName + (fileName.endsWith(".md") ? "" : ".md");
            this.dirName = dirName;
        }
    }

    // IOException -> RuntimeException .. for working w/in stream/function
    public static class WrappedIOException extends RuntimeException {
        WrappedIOException(IOException cause) {
            super(cause);
        }
    }
}
