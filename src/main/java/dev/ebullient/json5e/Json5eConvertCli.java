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
import dev.ebullient.json5e.tools5e.Json2MarkdownConverter;
import dev.ebullient.json5e.tools5e.Json5eConfig;
import dev.ebullient.json5e.tools5e.JsonIndex;
import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.IFactory;
import picocli.CommandLine.IParameterExceptionHandler;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.ScopeType;
import picocli.CommandLine.Spec;
import picocli.CommandLine.UnmatchedArgumentException;

@SuppressWarnings("CanBeFinal")
@QuarkusMain
@Command(name = "5e-convert", header = "Convert 5etools data to markdown", subcommands = {
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
}, mixinStandardHelpOptions = true, versionProvider = VersionProvider.class)
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

    @Option(names = { "-d", "--debug" }, description = "Enable debug output", defaultValue = "false", scope = ScopeType.INHERIT)
    boolean debug;

    @Option(names = { "-v", "--verbose" }, description = "Verbose output", defaultValue = "false", scope = ScopeType.INHERIT)
    boolean verbose;

    @Option(names = "-s", description = "Source Books%n  Comma-separated list or multiple declarations (PHB,DMG,...); use ALL for all sources")
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
        if (source.contains("ALL")) {
            source = List.of("*");
        }

        tui.setOutputPath(output);
        tui.verbosef("Writing markdown to %s.\n", output);

        boolean allOk = true;
        JsonIndex index = new JsonIndex(source, tui);
        Path toolsBase = Path.of("").toAbsolutePath();

        for (Path inputPath : input) {
            tui.outPrintf("⏱  Reading %s%n", inputPath);
            if (inputPath.toFile().isDirectory()) {
                toolsBase = inputPath.toAbsolutePath();
                allOk |= tui.read5eTools(toolsBase, index::importTree);
            } else {
                allOk |= tui.readFile(inputPath, index::importTree);
            }
        }

        Json5eConfig extraConfig = index.getExtraConfig();
        for (String adventure : extraConfig.getAdventures()) {
            allOk |= tui.readFile(toolsBase.resolve(adventure), index::importTree);
        }
        for (String book : extraConfig.getBooks()) {
            allOk |= tui.readFile(toolsBase.resolve(book), index::importTree);
        }
        extraConfig.getTemplates().forEach((k, v) -> paths.setCustomTemplate(k, v));

        tui.outPrintln("✅ finished reading 5etools data.");
        index.prepare();

        tui.debugf("Defined templates: %s", tpl);
        tpl.setCustomTemplates(paths);
        tui.verbosef("Custom templates: %s", paths.customTemplates.toString());

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
                .writeAll()
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
    public int run(String... args) {
        return new CommandLine(this, factory)
                .setCaseInsensitiveEnumValuesAllowed(true)
                .setExecutionStrategy(this::executionStrategy)
                .setParameterExceptionHandler(new ShortErrorMessageHandler())
                .execute(args);
    }

    class ShortErrorMessageHandler implements IParameterExceptionHandler {
        public int handleParseException(ParameterException ex, String[] args) {
            CommandLine cmd = ex.getCommandLine();
            CommandSpec spec = cmd.getCommandSpec();
            tui.init(spec, debug, verbose);

            tui.error(ex, ex.getMessage());
            UnmatchedArgumentException.printSuggestions(ex, cmd.getErr());

            cmd.getErr().println(cmd.getHelp().fullSynopsis()); // normal text to error stream

            if (spec.equals(spec.root())) {
                cmd.getErr().println(cmd.getHelp().commandList()); // normal text to error stream
            }
            cmd.getErr().printf("See '%s --help' for more information.%n", spec.qualifiedName());
            cmd.getErr().flush();

            return cmd.getExitCodeExceptionMapper() != null
                    ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                    : spec.exitCodeOnInvalidInput();
        }
    }
}
