package dev.ebullient.convert.tools.dnd5e.qute;

import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.Tags;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class QuteReward extends Tools5eQuteBase {

    public final String ability;
    public final String detail;
    public final String signatureSpells;

    public QuteReward(CompendiumSources sources, String name, String source,
            String ability, String detail, String signatureSpells,
            String text, Tags tags) {
        super(sources, name, source, text, tags);
        this.ability = ability;
        this.detail = detail;
        this.signatureSpells = signatureSpells;
    }

    @Override
    public String template() {
        return "reward2md.txt";
    }
}
