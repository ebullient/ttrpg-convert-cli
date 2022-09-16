package dev.ebullient.json5e;

import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        String version = getClass().getPackage().getImplementationVersion();
        return new String[] {
                "${COMMAND-FULL-NAME} version " + version };
    }
}
