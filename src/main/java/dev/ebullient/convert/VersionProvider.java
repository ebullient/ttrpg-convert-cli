package dev.ebullient.convert;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import dev.ebullient.convert.config.TtrpgConfig;
import picocli.CommandLine;

public class VersionProvider implements CommandLine.IVersionProvider {

    @Override
    public String[] getVersion() {
        Properties properties = new Properties();
        try (InputStream in = TtrpgConfig.class.getResourceAsStream("/git.properties")) {
            properties.load(in);
            return new String[] {
                    "${COMMAND-FULL-NAME} version " + properties.getProperty("git.build.version"),
                    "Git commit: " + properties.get("git.commit.id.abbrev")
            };
        } catch (IOException e) {
            return new String[] { "${COMMAND-FULL-NAME} version unknown " };
        }
    }
}
