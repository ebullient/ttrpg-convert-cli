package dev.ebullient.convert.tools.pf2e.qute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.Pf2eSources;
import dev.ebullient.convert.tools.pf2e.qute.QuteSpell.QuteSpellTarget;
import io.quarkus.qute.TemplateData;

@TemplateData
public class QuteRitual extends Pf2eQuteBase {

    public final String level;
    public final String ritualType;
    public final Collection<String> traits;
    public final List<String> aliases;

    public final QuteRitualCasting casting;
    public final QuteRitualChecks checks;
    public final QuteSpellTarget targeting;

    public final String requirements;
    public final String duration;

    public final Map<String, String> heightened;

    public QuteRitual(Pf2eSources sources, List<String> text, Tags tags,
            String level, String ritualType, Collection<String> traits, List<String> aliases,
            QuteRitualCasting casting, QuteRitualChecks checks, QuteSpellTarget targeting,
            String requirements, String duration, Map<String, String> heightened) {
        super(sources, text, tags);

        this.level = level;
        this.ritualType = ritualType;
        this.traits = traits;
        this.aliases = aliases;
        this.casting = casting;
        this.checks = checks;
        this.targeting = targeting;
        this.requirements = requirements;
        this.duration = duration;
        this.heightened = heightened;
    }

    @TemplateData
    public static class QuteRitualCasting {
        public String cast;
        public String cost;
        public String secondaryCasters;

        public String toString() {
            List<String> parts = new ArrayList<>();

            parts.add(cast);

            if (cost != null) {
                parts.add(String.format("**Cost** %s", cost));
            }
            if (secondaryCasters != null) {
                parts.add(String.format("**Secondary Casters** %s", secondaryCasters));
            }

            return String.join("\n- ", parts);
        }
    }

    @TemplateData
    public static class QuteRitualChecks {
        public String primaryChecks;
        public String secondaryChecks;

        public String toString() {
            List<String> parts = new ArrayList<>();
            parts.add(String.format("**Primary Checks** %s", primaryChecks));

            if (secondaryChecks != null) {
                parts.add(String.format("**Secondary Checks** %s", secondaryChecks));
            }

            return String.join("\n- ", parts);
        }
    }

}
