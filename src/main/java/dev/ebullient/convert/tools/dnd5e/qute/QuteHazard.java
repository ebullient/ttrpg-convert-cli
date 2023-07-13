package dev.ebullient.convert.tools.dnd5e.qute;

import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.Tags;

public class QuteHazard extends Tools5eQuteBase {

    public final String hazardType;

    public QuteHazard(CompendiumSources sources, String name, String source,
            String hazardType,
            String text, Tags tags) {
        super(sources, name, source, text, tags);
        this.hazardType = hazardType;
    }

    @Override
    public String template() {
        return "hazard2md.txt";
    }
}
