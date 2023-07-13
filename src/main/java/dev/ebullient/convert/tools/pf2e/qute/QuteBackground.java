package dev.ebullient.convert.tools.pf2e.qute;

import java.util.List;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import io.quarkus.qute.TemplateData;

@TemplateData
public class QuteBackground extends Pf2eQuteBase {

    public QuteBackground(Pf2eSources sources, List<String> text, Tags tags) {
        super(sources, text, tags);
    }
}
