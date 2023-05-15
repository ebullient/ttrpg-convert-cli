package dev.ebullient.convert.tools.pf2e.qute;

import java.util.Collection;
import java.util.List;

import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;
import io.quarkus.qute.TemplateData;

@TemplateData
public class QuteInlineAttack extends Pf2eQuteNote {
    public final Collection<String> traits;
    public final String meleeOrRanged;
    public final QuteDataActivity activity;
    public final String attack;
    public final String damage;

    public QuteInlineAttack(String name, List<String> text, Collection<String> tags, Collection<String> traits,
            String meleeOrRanged, String attack, String damage, QuteDataActivity activity) {
        super(Pf2eIndexType.syntheticGroup, name, null, text, tags);
        this.traits = traits;
        this.meleeOrRanged = meleeOrRanged;
        this.attack = attack;
        this.damage = damage;
        this.activity = activity;
    }

    @Override
    public String template() {
        return "inline-attack2md.txt";
    }
}
