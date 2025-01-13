package dev.ebullient.convert.tools.dnd5e.qute;

import static dev.ebullient.convert.StringUtil.joinConjunct;
import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.ArrayList;
import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools background attributes ({@code bastion2md.txt}).
 *
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase}.
 */
@TemplateData
public class QuteBastion extends Tools5eQuteBase {
    /**
     * List of possible hirelings this bastion can have (as {@link dev.ebullient.convert.tools.dnd5e.qute.QuteBastion.Hireling},
     * optional)
     */
    public final List<Hireling> hirelings;
    /** Bastion level (optional) */
    public final String level;
    /** Bastion orders (optional) */
    public final List<String> orders;
    /**
     * List of possible spaces this bastion can occupy (as {@link dev.ebullient.convert.tools.dnd5e.qute.QuteBastion.Space},
     * optional)
     */
    public final List<Space> space;
    /** Type */
    public final String type;
    /** Formatted text listing other prerequisite conditions (optional) */
    public final String prerequisite;

    public QuteBastion(Tools5eSources sources, String name, String source,
            List<Hireling> hirelings, String level, List<String> orders,
            String prerequisite, List<Space> space, String type,
            String text, List<ImageRef> images, Tags tags) {
        super(sources, name, source, images, text, tags);

        this.hirelings = hirelings; // optional
        this.level = level; // optional
        this.orders = orders; // optional
        this.prerequisite = prerequisite; // optional
        this.space = space; // optional
        this.type = type;
    }

    /** Hirelings as a descriptive string (if hirelings is present) */
    public String getHirelingDescription() {
        if (hirelings == null) {
            return "";
        }
        List<String> all = new ArrayList<>();
        for (Hireling h : hirelings) {
            all.add(h.getDescription());
        }
        return joinConjunct(" or ", all);
    }

    /** Space as a descriptive string (if space is present) */
    public String getSpaceDescription() {
        if (space == null) {
            return "";
        }
        List<String> all = new ArrayList<>();
        for (Space s : space) {
            all.add(s.getDescription(type, getName()));
        }
        return joinConjunct(" or ", all);
    }

    /**
     * Hireling information. Either exact or min must be present.
     *
     * @param exact Exact number of hirelings (either exact or min)
     * @param min Minimum number of hirelings (either exact or min)
     * @param max Maximum number of hirelings (optional)
     * @param space Size of bastion space required for these hirelings (optional)
     */
    @TemplateData
    public record Hireling(
            Integer exact,
            Integer min,
            Integer max,
            Space space) {

        /** Formatted string description of the hirelings for a Bastion */
        public String getDescription() {
            String spaceTxt = space == null ? "" : " (%s)".formatted(toTitleCase(space.name()));
            // Either min or exact must be present
            if (exact != null) {
                return "%s%s".formatted(exact, spaceTxt);
            } else if (min != null && max != null) {
                return "%s-%s%s".formatted(min, max, spaceTxt);
            } else if (min != null) {
                return "%s+%s".formatted(min, spaceTxt);
            }
            return "";
        }
    }

    /**
     * @param name Name of this size/space
     * @param squares Maximum number of 5-foot squares a bastion this size can occupy
     * @param cost Cost (GP) of building a bastion of this size
     * @param time Time to construct a bastion of this size
     * @param prevSpace Previous space to enlarge from (optional)
     */
    @TemplateData
    public record Space(
            String name,
            Integer squares,
            Integer cost,
            Integer time,
            Space prevSpace) {

        public Space(String name, Integer squares, Integer cost, Integer time) {
            this(name, squares, cost, time, null);
        }

        /** Formatted string description of the space required for (or occupied by) a Bastion */
        public String getDescription(String type, String facilityName) {
            List<String> more = new ArrayList<>();
            if (squares > 0) {
                more.add(squares + " sq");
            }
            if ("basic".equalsIgnoreCase(type)) {
                String txtCost = cost + " GP and " + time + " days to add";
                if (prevSpace != null) {
                    txtCost += ", or %s GP and %s days to enlarge from a %s %s".formatted(
                            cost - prevSpace.cost, time - prevSpace.time, prevSpace.name(), facilityName);
                }
                more.add("%s GP, %s days ^[%s]".formatted(
                        cost, time, txtCost));
            }
            return name() + (more.isEmpty() ? "" : " (%s)".formatted(String.join("; ", more)));
        }
    }
}
