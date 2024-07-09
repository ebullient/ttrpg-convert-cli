package dev.ebullient.convert.tools.pf2e;

import static dev.ebullient.convert.StringUtil.join;
import static dev.ebullient.convert.StringUtil.parenthesize;

import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.pf2e.qute.QuteAbility;

public class Json2QuteAbility extends Json2QuteBase {

    public Json2QuteAbility(Pf2eIndex index, Pf2eIndexType type, JsonNode rootNode) {
        super(index, type, rootNode);
    }

    @Override
    protected QuteAbility buildQuteNote() {
        return Pf2eAbility.createAbility(rootNode, this, sources);
    }

    public enum Pf2eAbility implements Pf2eJsonNodeReader {
        /** e.g. {@code "Dirty Bomb"} */
        name,
        /** @see Pf2eActivity */
        activity,
        /**
         * Activation components. The actual data is much more freeform than the schema indicates, e.g.
         * {@code ["command", "{@action Recall Knowledge}", "1 minute", "{@action Interact} 1 minute"]}.
         */
        components,
        /** e.g. {@code "one batch of infused reagents."} */
        cost,
        /** String array of creatures/archetypes this ability is associated with, e.g. {@code ["Skeleton"]} */
        creature, // TODO
        /** @see dev.ebullient.convert.tools.pf2e.Pf2eJsonNodeReader.Pf2eFrequency */
        frequency,
        /**
         * If present this is a generic ability, defined further in a standalone note.
         *
         * @see Pf2eGenericAbilityReference
         */
        generic,
        /** e.g. {@code "The corpselight is in wisp form and is adjacent to a Medium corpse"} */
        prerequisites,
        /** How to display this block, {@code "compact"} or {@code "full"} */
        style, // TODO
        /** e.g. {@code "the activation takes {@as 2} if the spell normally takes {@as 1} to cast"} */
        note,
        /** @see dev.ebullient.convert.tools.pf2e.Pf2eJsonNodeReader.Pf2eNumberUnitEntry */
        range,
        /** e.g. {@code "Your last action reduced an enemy to 0 Hit Points} */
        requirements,
        /** e.g. {@code "You strike a foe with the blast lance"} */
        trigger,
        /**
         * Listed in the schema as an array of entries, but actual data only ever has a single string in an array.
         * e.g. {@code ["Multiple critical failures might cause the contact to work against the PCs in some way"]}
         */
        special,
        /** Nestable entries for the ability effect. */
        entries;

        private static QuteAbility createAbility(JsonNode node, JsonSource convert, Pf2eSources sources) {
            Tags tags = new Tags();
            Set<String> traits = convert.collectTraitsFrom(node, tags);

            return new QuteAbility(sources,
                    name.getTextFrom(node).map(convert::replaceText).orElse("Activate"),
                    generic.getLinkFrom(node, convert),
                    entries.transformTextFrom(node, "\n", convert),
                    tags,
                    traits,
                    activity.getActivityFrom(node, convert),
                    range.getRangeFrom(node, convert),
                    components.getActivationComponentsFrom(node, traits, convert),
                    requirements.replaceTextFrom(node, convert),
                    prerequisites.replaceTextFrom(node, convert),
                    cost.replaceTextFrom(node, convert)
                            // remove trailing period
                            .replaceFirst("(^[.])\\.$", "$1"),
                    trigger.replaceTextFrom(node, convert)
                            // remove trailing period
                            .replaceFirst("(^[.])\\.$", "$1"),
                    frequency.getFrequencyFrom(node, convert),
                    special.transformTextFrom(node, "\n", convert),
                    note.replaceTextFrom(node, convert),
                    sources == null, convert);
        }

        public static QuteAbility createEmbeddedAbility(JsonNode node, JsonSource convert) {
            return createAbility(node, convert, null);
        }

        public String getLinkFrom(JsonNode node, JsonSource convert) {
            return getObjectFrom(node)
                    .map(n -> convert.linkify(
                            Pf2eIndexType.fromText(Pf2eGenericAbilityReference.tag.getTextOrNull(n)),
                            join("|",
                                    Pf2eGenericAbilityReference.name.getTextFrom(n)
                                            .or(() -> name.getTextFrom(node))
                                            // Add the hash as a parenthesized string after the name
                                            .map(s -> s + Pf2eGenericAbilityReference.add_hash.getTextFrom(n)
                                                    .map(hash -> " " + parenthesize(hash)).orElse(""))
                                            .orElse(null),
                                    Pf2eGenericAbilityReference.source.getTextOrNull(n))))
                    .orElse(null);
        }

        private enum Pf2eGenericAbilityReference implements Pf2eJsonNodeReader {
            /** An extra string to add to an ability for finding its link, e.g. {@code "Rogue"} */
            add_hash,
            /** e.g. {@code "Curse of the Werecreature"}, or {@code "Nimble Dodge (Rogue)"} */
            name,
            /** The type of the resource, e.g. {@code "ability"} or {@code "feat"} */
            tag,
            /** The source, e.g. {@code "B1"} */
            source;
        }
    }
}
