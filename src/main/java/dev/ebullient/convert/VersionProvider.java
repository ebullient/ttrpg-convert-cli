package dev.ebullient.convert;

import java.io.IOException;
import java.util.Properties;

import dev.ebullient.convert.config.TtrpgConfig;
import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() {
        Properties properties = new Properties();
        try {
            properties.load(TtrpgConfig.class.getResourceAsStream("/git.properties"));
            return new String[] {
                    "${COMMAND-FULL-NAME} version " + properties.getProperty("git.build.version"),
                    "Git commit: " + properties.get("git.commit.id.abbrev"),
                    "Build time: " + properties.get("git.build.time")
            };
        } catch (IOException e) {
            return new String[] { "${COMMAND-FULL-NAME} version unknown " };
        }
    }
}
