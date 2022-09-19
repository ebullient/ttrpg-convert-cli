package dev.ebullient.json5e.qute;

import java.util.List;

public class QuteBackground extends QuteNote {

    public QuteBackground(String name, String source, String text, List<String> tags) {
        super(name, source, text, tags);
    }

    @Override
    public String targetPath() {
        return "backgrounds";
    }
}
