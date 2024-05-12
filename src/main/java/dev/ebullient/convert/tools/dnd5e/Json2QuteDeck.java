package dev.ebullient.convert.tools.dnd5e;

import static dev.ebullient.convert.StringUtil.toTitleCase;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
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
        Tags tags = new Tags(sources);
        List<String> text = new ArrayList<>();
        List<Card> cards = new ArrayList<>();

        appendToText(text, rootNode, "##");

        boolean hasCardArt = DeckFields.hasCardArt.booleanOrDefault(rootNode, false);
        for (JsonNode cardRef : DeckFields.cards.iterateArrayFrom(rootNode)) {
            final String cardKey;
            if (cardRef.isTextual()) {
                cardKey = Tools5eIndexType.card.fromRawKey(cardRef.asText());
            } else if (cardRef.isObject()) {
                cardKey = Tools5eIndexType.card.fromRawKey(DeckFields.uid.getTextOrThrow(cardRef));
            } else {
                cardKey = null;
            }

            if (cardKey != null) {
                JsonNode cardNode = index.getNode(cardKey);
                if (cardNode == null) {
                    tui().errorf("Unable to find card %s referenced from %s", cardKey, sources.getKey());
                } else {
                    appendCard(hasCardArt, cards, cardNode);
                }
            }
        }

        return new QuteDeck(sources,
                getName(),
                getSourceText(sources),
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
        Optional<Integer> value = DeckFields.value.getIntFrom(cardNode);
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
        if (imageRef != null) {
            try {
                JsonMediaHref mediaHref = mapper().treeToValue(imageRef, JsonMediaHref.class);
                return buildImageRef(mediaHref, getImagePath());
            } catch (JsonProcessingException | IllegalArgumentException e) {
                tui().errorf("Unable to read media reference from %s", imageRef.toPrettyString());
            }
        }
        return null;
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
