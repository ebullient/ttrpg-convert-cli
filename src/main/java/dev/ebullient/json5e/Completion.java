package dev.ebullient.json5e;

import picocli.AutoComplete.GenerateCompletion;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "completion", version = "generate-completion "
        + CommandLine.VERSION, mixinStandardHelpOptions = true, header = "bash/zsh completion:  source <(${PARENT-COMMAND-FULL-NAME:-$PARENTCOMMAND} ${COMMAND-NAME})", helpCommand = true)
public class Completion extends GenerateCompletion {

}
