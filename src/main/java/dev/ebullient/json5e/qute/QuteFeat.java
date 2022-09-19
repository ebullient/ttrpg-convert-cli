package dev.ebullient.json5e.qute;

import java.util.List;

public class QuteFeat extends QuteNote {

    public final String level;
    public final String prerequisite;

    public QuteFeat(String name, String source, String prerequisite, String level, String text, List<String> tags) {
        super(name, source, text, tags);
        this.level = level;
        this.prerequisite = prerequisite; // optional
    }

    @Override
    public String targetPath() {
        return "feats";
    }
}
