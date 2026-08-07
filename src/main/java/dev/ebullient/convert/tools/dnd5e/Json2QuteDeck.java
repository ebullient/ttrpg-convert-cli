package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.qute.SourceAndPage;
import dev.ebullient.convert.tools.JsonNodeReader;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.qute.QuteDeck;
import dev.ebullient.convert.tools.dnd5e.qute.QuteDeck.Card;
import dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase;

public class Json2QuteDeck extends Json2QuteCommon {

    Json2QuteDeck(Tools5eIndex index, Tools5eIndexType type, JsonNode jsonNode) {
        super(index, type, jsonNode);
    }

    @Override
    protected Tools5eQuteBase buildQuteResource() {
        Tags tags = new Tags(getSources());
        List<String> text = new ArrayList<>();
        List<Card> cards = new ArrayList<>();

        appendToText(text, SourceField.entries.getFrom(rootNode), "##");

        boolean hasCardArt = DeckFields.hasCardArt.booleanOrDefault(rootNode, false);
        for (JsonNode cardRef : DeckFields.cards.iterateArrayFrom(rootNode)) {
            final String cardKey;
            if (cardRef.isTextual()) {
                cardKey = Tools5eIndexType.card.fromTagReference(cardRef.asText());
            } else if (cardRef.isObject()) {
                cardKey = Tools5eIndexType.card.fromTagReference(DeckFields.uid.getTextOrThrow(cardRef));
            } else {
                cardKey = null;
            }

            if (cardKey != null) {
                JsonNode cardNode = index.getNode(cardKey);
                if (cardNode == null) {
                    tui().errorf("Unable to find %s referenced from %s", cardKey, getSources().getKey());
                } else {
                    appendCard(hasCardArt, cards, cardNode);
                }
            }
        }

        return new QuteDeck(getSources(),
                getName(),
                getSourceText(getSources()),
                getImage(DeckFields.back, rootNode),
                cards,
                String.join("\n", text),
                tags);
    }

    public void appendCard(boolean hasCardArt, List<Card> cards, JsonNode cardNode) {
        String name = SourceField.name.getTextOrEmpty(cardNode);
        ImageRef face = hasCardArt ? getImage(DeckFields.face, cardNode) : null;
        String cardText = flattenToString(cardNode);
        String suit = DeckFields.suit.getTextOrEmpty(cardNode);
        Optional<Integer> value = DeckFields.value.intFrom(cardNode);
        String valueName = DeckFields.valueName.getTextOrEmpty(cardNode);

        String suitValue = null;
        if (!suit.isEmpty() && (value.isPresent() || !valueName.isEmpty())) {
            suitValue = toTitleCase(valueName.isEmpty() ? numberToText(value.get()) : valueName);
            suitValue += " of " + toTitleCase(suit);

            if (!suitValue.toLowerCase().equals(name.toLowerCase())) {
                cardText = "*" + suitValue + "*\n\n" + cardText;
            }
        }
        cards.add(new Card(name, face, cardText, suitValue, new SourceAndPage(cardNode)));
    }

    ImageRef getImage(JsonNodeReader field, JsonNode imgSource) {
        JsonNode imageRef = field.getFrom(imgSource);
        return imageRef == null ? null : readImageRef(imageRef);
    }

    enum DeckFields implements JsonNodeReader {
        back,
        cards,
        face,
        set,
        suit,
        uid,
        value,
        valueName,
        hasCardArt;
    }
}
