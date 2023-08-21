package dev.ebullient.convert.tools.dnd5e.qute;

import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.Tags;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * 5eTools reward attributes ({@code reward2md.txt})
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.Tools5eQuteBase}.
 * </p>
 */
@TemplateData
@RegisterForReflection
public class QuteReward extends Tools5eQuteBase {

    /**
     * Description of special ability granted by this reward, if defined separately. This is usually included in reward text.
     */
    public final String ability;
    /** Reward detail string (similar to item detail). May include the reward type and rarity if either are defined. */
    public final String detail;
    /** Formatted text describing sigature spells. Not commonly used. */
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
