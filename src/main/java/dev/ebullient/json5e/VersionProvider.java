package dev.ebullient.json5e;

import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() throws Exception {
        String version = getClass().getPackage().getImplementationVersion();
        if (version == null || version.isEmpty()) {
            version = "dev-snapshot";
        }
        return new String[] {
                "${COMMAND-FULL-NAME} version " + version };
    }
}
