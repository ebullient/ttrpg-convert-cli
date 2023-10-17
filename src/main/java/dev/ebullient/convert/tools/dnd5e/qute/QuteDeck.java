package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.SourceAndPage;
import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.Tags;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * 5eTools deck attributes ({@code deck2md.txt})
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.Tools5eQuteBase}.
 * </p>
 */
@TemplateData
public class QuteDeck extends Tools5eQuteBase {

    /** Image from the back of the card as {@link dev.ebullient.convert.qute.ImageRef} (optional) */
    public final ImageRef cardBack;

    /** List of cards in the deck */
    public final List<Card> cards;

    public QuteDeck(CompendiumSources sources, String name, String source,
            ImageRef cardBack, List<Card> cards, String text, Tags tags) {
        super(sources, name, source, text, tags);
        this.cardBack = cardBack;
        this.cards = cards;
    }

    @TemplateData
    @RegisterForReflection
    public static class Card {
        /** Image from the front of the card as {@link ImageRef} (optional) */
        public final ImageRef face;

        /** Name of the card */
        public final String name;
        /** Text on the front of the card */
        public final String text;

        /** Card suit and value (optional) */
        public final String suitValue;

        /** Source and page containing card definition as {@link dev.ebullient.convert.qute.SourceAndPage} */
        public final SourceAndPage sourceAndPage;

        public Card(String name, ImageRef face, String text, String suitValue, SourceAndPage sourceAndPage) {
            this.name = name;
            this.face = face;
            this.text = text;
            this.suitValue = suitValue;
            this.sourceAndPage = sourceAndPage;
        }
    }
}
