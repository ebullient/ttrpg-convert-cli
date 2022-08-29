package dev.ebullient.json5e;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import dev.ebullient.json5e.io.Json5eTui;
import dev.ebullient.json5e.io.MarkdownWriter;
import dev.ebullient.json5e.io.TemplatePaths;
import dev.ebullient.json5e.io.Templates;
import dev.ebullient.json5e.tools5e.IndexType;
import dev.ebullient.json5e.tools5e.Json2MarkdownConverter;
import dev.ebullient.json5e.tools5e.JsonIndex;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.IFactory;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;

@QuarkusMain
@Command(name = "5e-convert", mixinStandardHelpOptions = true, header = "Convert 5etools data to markdown", subcommands = {
        Completion.class,
}, description = {
        "%n%nThis will read from a 5etools json file (or the 5etools data directory) and will produce xml or markdown documents (based on options).",
}, footer = {
        "",
        "Use the sources option to filter converted items by source. If no sources",
        "are specified, only items from the SRD will be included.",
        "Specify values as they appear in the exported json, e.g. -s PHB -s DMG.",
        "Only include items from sources you own.",
        "",
        "You can describe sources and specify specific identifiers to be excluded in a json file, e.g.",
        "",
        "{",
        "  \"from\" : [",
        "    \"PHB\",",
        "    \"DMG\",",
        "    \"SCAG\",",
        "  ]",
        "  \"exclude\" : [",
        "    \"background|sage|phb\",",
        "  ]",
        "  \"excludePattern\" : [",
        "    \"race|.*|dmg\",",
        "  ]",
        "}",
        "",
        "Pass this file in as another input source. Use the identifiers from the generated index files in the list of excluded rules.",
        "",
})
public class Json5eConvertCli implements Callable<Integer>, QuarkusApplication {

    List<Path> input;
    Path output;

    @Inject
    IFactory factory;

    @Inject
    Templates tpl;

    @Inject
    Json5eTui tui;

    @Spec
    private CommandSpec spec;

    @Option(names = { "-d", "--debug" }, description = "Enable debug output", scope = ScopeType.INHERIT)
    boolean debug;

    @Option(names = { "-v", "--verbose" }, description = "Verbose output", scope = ScopeType.INHERIT)
    boolean verbose;

    @Option(names = "-s", description = "Source Books%n  Comma-separated list or multiple declarations (PHB,DMG,...)")
    List<String> source = Collections.emptyList();

    @Option(names = "--index", description = "Create index of keys that can be used to exclude entries")
    boolean filterIndex;

    @ArgGroup(exclusive = false)
    TemplatePaths paths = new TemplatePaths();

    @Option(names = "-o", description = "Output directory", required = true)
    void setOutputPath(File outputDir) {
        output = outputDir.toPath().toAbsolutePath().normalize();
        if (output.toFile().exists() && output.toFile().isFile()) {
            throw new ParameterException(spec.commandLine(),
                    "Specified output path exists and is a file: " + output.toString());
        }
    }

    @Parameters(description = "Source file(s)")
    void setInput(List<File> inputFile) {
        input = new ArrayList<>(inputFile.size());
        for (File f : inputFile) {
            input.add(f.toPath().toAbsolutePath().normalize());
        }
    }

    @Override
    public Integer call() {
        if (input == null || input.isEmpty()) {
            throw new CommandLine.MissingParameterException(spec.commandLine(), spec.args(),
                    "Must specify an input file");
        }
        if (!output.toFile().exists() && !output.toFile().mkdirs()) {
            tui.errorf("Unable to create output directory: %s", output);
            return ExitCode.USAGE;
        }
        if (source.size() == 1 && source.get(0).contains(",")) {
            String tmp = source.remove(0);
            source = List.of(tmp.split(","));
        }

        tui.outPrintf("Importing/Converting items from 5e tools %s to %s.\n",
                input, output);

        boolean allOk = true;
        JsonIndex index = new JsonIndex(source, tui);

        for (Path inputPath : input) {
            tui.outPrintf("⏱  Reading 5eTools data from %s%n", inputPath);
            try {
                if (inputPath.toFile().isDirectory()) {
                    List<String> inputs = List.of(
                            "bestiary", "class", "spells",
                            "conditionsdiseases.json", "skills.json", "loot.json",
                            "backgrounds.json", "fluff-backgrounds.json",
                            "feats.json", "optionalfeatures.json",
                            "items.json", "items-base.json", "fluff-items.json", "magicvariants.json",
                            "races.json", "fluff-races.json",
                            "names.json", "variantrules.json");
                    for (String input : inputs) {
                        Path p = inputPath.resolve(input);
                        if (p.toFile().isFile()) {
                            index.readFile(p);
                        } else {
                            index.readDirectory(p);
                        }
                    }
                } else {
                    index.readFile(inputPath);
                }
            } catch (IOException e) {
                tui.error(e, "  Exception: " + e.getMessage());
                allOk = false;
            }
            tui.outPrintln("✅ finished reading 5etools data.");
        }

        tui.debugf("Custom templates: %s", paths.customTemplates.toString());
        tpl.setCustomTemplates(paths);
        tui.debugf("Defined templates: %s", tpl);

        tui.outPrintln("💡 Writing files to " + output);

        if (filterIndex) {
            try {
                index.writeIndex(output.resolve("all-index.json"));
                index.writeSourceIndex(output.resolve("src-index.json"));
            } catch (IOException e) {
                tui.error(e, "  Exception: " + e.getMessage());
                allOk = false;
            }
        }

        MarkdownWriter writer = new MarkdownWriter(output, tpl, tui);
        tui.outPrintln("💡 Writing files to " + output);
        new Json2MarkdownConverter(index, writer)
                .writeFiles(IndexType.background)
                .writeFiles(IndexType.classtype)
                .writeFiles(IndexType.feat)
                .writeFiles(IndexType.item)
                .writeFiles(IndexType.monster)
                .writeFiles(IndexType.namelist)
                .writeFiles(IndexType.race)
                .writeFiles(IndexType.spell)
                .writeRulesAndTables();

        return allOk ? ExitCode.OK : ExitCode.SOFTWARE;
    }

    private int executionStrategy(ParseResult parseResult) {
        try {
            init(parseResult);
            return new CommandLine.RunLast().execute(parseResult);
        } finally {
            shutdown();
        }
    }

    private void init(ParseResult parseResult) {
        tui.init(spec, debug, verbose);
    }

    private void shutdown() {
        tui.close();
    }

    @Override
    public int run(String... args) throws Exception {
        return new CommandLine(this, factory)
                .setCaseInsensitiveEnumValuesAllowed(true)
                .setExecutionStrategy(this::executionStrategy)
                .execute(args);
    }
}
