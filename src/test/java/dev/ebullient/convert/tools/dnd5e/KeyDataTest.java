package dev.ebullient.convert.tools.dnd5e;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * {@link KeyData} fromRefTag -> toKey() tests, using tag examples from
 * {@code sources/5etools-src/data/renderdemo.json}.
 */
public class KeyDataTest {

    // {@race Human}, {@race Aarakocra|eepc}, {@race Aarakocra|eepc|link text}
    @Test
    void testSimpleKeyData_race() {
        assertThat(SimpleKeyData.fromRefTag(Tools5eIndexType.race, "Human").toKey())
                .isEqualTo("race|human|phb");
        assertThat(SimpleKeyData.fromRefTag(Tools5eIndexType.race, "Aarakocra|eepc").toKey())
                .isEqualTo("race|aarakocra|eepc");
        assertThat(SimpleKeyData.fromRefTag(Tools5eIndexType.race,
                "Aarakocra|eepc|and optional link text added with another pipe").toKey())
                .isEqualTo("race|aarakocra|eepc");
    }

    // {@feat Alert}, {@feat Elven Accuracy|xge}, {@feat Elven Accuracy|xge|link text}
    @Test
    void testSimpleKeyData_feat() {
        assertThat(SimpleKeyData.fromRefTag(Tools5eIndexType.feat, "Alert").toKey())
                .isEqualTo("feat|alert|phb");
        assertThat(SimpleKeyData.fromRefTag(Tools5eIndexType.feat, "Elven Accuracy|xge").toKey())
                .isEqualTo("feat|elven accuracy|xge");
        assertThat(SimpleKeyData.fromRefTag(Tools5eIndexType.feat,
                "Elven Accuracy|xge|and optional link text added with another pipe").toKey())
                .isEqualTo("feat|elven accuracy|xge");
    }

    // {@itemProperty F}, {@itemProperty F|PHB}, {@itemProperty F|PHB|link text}
    @Test
    void testItemAbbreviationKeyData_itemProperty() {
        assertThat(ItemProperty.refTagToKey("F").toKey())
                .isEqualTo(Tools5eIndexType.itemProperty.createKey("F", "PHB"));
        assertThat(ItemProperty.refTagToKey("F|PHB").toKey())
                .isEqualTo(Tools5eIndexType.itemProperty.createKey("F", "PHB"));
        assertThat(ItemProperty.refTagToKey("F|PHB|and optional link text added with another pipe").toKey())
                .isEqualTo(Tools5eIndexType.itemProperty.createKey("F", "PHB"));
    }

    // {@classFeature name|className|classSource|level|source|linkText}; class/feature source default to PHB
    @Test
    void testClassFeatureKeyData() {
        assertThat(ClassFeatureKeyData.fromRefTag("Rage|Barbarian||1").toKey())
                .isEqualTo("classfeature|rage|barbarian|phb|1|phb");
        assertThat(ClassFeatureKeyData.fromRefTag("Infuse Item|Artificer|TCE|2").toKey())
                .isEqualTo("classfeature|infuse item|artificer|tce|2|tce");
        assertThat(ClassFeatureKeyData.fromRefTag("Primal Knowledge|Barbarian||3|TCE").toKey())
                .isEqualTo("classfeature|primal knowledge|barbarian|phb|3|tce");
        assertThat(ClassFeatureKeyData.fromRefTag("Rage|Barbarian||1||optional display text").toKey())
                .isEqualTo("classfeature|rage|barbarian|phb|1|phb");
    }

    // {@subclassFeature name|className|classSource|scShortName|scSource|level|source|linkText}
    @Test
    void testSubclassFeatureKeyData() {
        assertThat(SubclassFeatureKeyData.fromRefTag("Path of the Berserker|Barbarian||Berserker||3").toKey())
                .isEqualTo("subclassfeature|path of the berserker|barbarian|phb|berserker|phb|3|phb");
        assertThat(SubclassFeatureKeyData.fromRefTag("Alchemist|Artificer|TCE|Alchemist|TCE|3").toKey())
                .isEqualTo("subclassfeature|alchemist|artificer|tce|alchemist|tce|3|tce");
        assertThat(SubclassFeatureKeyData.fromRefTag("Path of the Battlerager|Barbarian||Battlerager|SCAG|3").toKey())
                .isEqualTo("subclassfeature|path of the battlerager|barbarian|phb|battlerager|scag|3|scag");
        assertThat(SubclassFeatureKeyData.fromRefTag("Blessed Strikes|Cleric||Life||8|TCE").toKey())
                .isEqualTo("subclassfeature|blessed strikes|cleric|phb|life|phb|8|tce");
        assertThat(SubclassFeatureKeyData
                .fromRefTag("Path of the Berserker|Barbarian||Berserker||3||optional display text").toKey())
                .isEqualTo("subclassfeature|path of the berserker|barbarian|phb|berserker|phb|3|phb");
    }

    // {@subclass scShortName|className|classSource|scSource}; short name, not full name -- these
    // two produce alias lookup keys (see allIndex.json "mapping"), resolved to the full-name key
    @Test
    void testSubclassKeyData() {
        assertThat(SubclassKeyData.fromRefTag("Berserker|Barbarian").toKey())
                .isEqualTo("subclass|berserker|barbarian|phb|phb");
        assertThat(SubclassKeyData.fromRefTag("Ancestral Guardian|Barbarian||XGE").toKey())
                .isEqualTo("subclass|ancestral guardian|barbarian|phb|xge");
        assertThat(SubclassKeyData.fromRefTag("Artillerist|Artificer|TCE|TCE").toKey())
                .isEqualTo("subclass|artillerist|artificer|tce|tce");
    }

    // toParentKey()/grandparent chain, using real keys from allIndex.json (barbarian: berserker subclass)
    @Test
    void testToParentKey() {
        assertThat(ClassFeatureKeyData.fromKey("classfeature|rage|barbarian|phb|1|phb").toParentKey().toKey())
                .isEqualTo("classtype|barbarian|phb");

        assertThat(SubclassKeyData.fromKey("subclass|path of the berserker|barbarian|phb|phb").toParentKey().toKey())
                .isEqualTo("classtype|barbarian|phb");

        // subclassFeature keys carry the subclass *short* name (e.g. "berserker"), so toParentKey()
        // here produces the same alias lookup key form as {@subclass} tags (see testSubclassKeyData)
        // -- not the full subclass key -- though className/classSource are unaffected, so the
        // grandparent (classtype) hop still resolves correctly.
        SubclassFeatureKeyData scf = SubclassFeatureKeyData.fromKey(
                "subclassfeature|frenzy|barbarian|phb|berserker|phb|3|phb");
        assertThat(scf.toParentKey().toKey())
                .isEqualTo("subclass|berserker|barbarian|phb|phb");
        assertThat(scf.toParentKey().toParentKey().toKey())
                .isEqualTo("classtype|barbarian|phb");
    }

    // {@deity Gond}, {@deity Gruumsh|nonhuman}, {@deity Ioun|dawn war|dmg}, {@deity Ioun|dawn war|dmg|linkText}
    // pantheon defaults to "Forgotten Realms"
    @Test
    void testDeityKeyData() {
        assertThat(DeityKeyData.fromRefTag("Gond").toKey())
                .isEqualTo("deity|gond|forgotten realms|phb");
        assertThat(DeityKeyData.fromRefTag("Gruumsh|nonhuman").toKey())
                .isEqualTo("deity|gruumsh|nonhuman|phb");
        assertThat(DeityKeyData.fromRefTag("Ioun|dawn war|dmg").toKey())
                .isEqualTo("deity|ioun|dawn war|dmg");
        assertThat(DeityKeyData.fromRefTag("Ioun|dawn war|dmg|and optional link text added with another pipe").toKey())
                .isEqualTo("deity|ioun|dawn war|dmg");
    }

    // {@deck Deck of Many Things}, {@deck Deck of Many Things|DMG}, {@deck Deck of Many Things|DMG|linkText}
    @Test
    void testSimpleKeyData_deck() {
        assertThat(SimpleKeyData.fromRefTag(Tools5eIndexType.deck, "Deck of Many Things").toKey())
                .isEqualTo("deck|deck of many things|dmg");
        assertThat(SimpleKeyData.fromRefTag(Tools5eIndexType.deck, "Deck of Many Things|DMG").toKey())
                .isEqualTo("deck|deck of many things|dmg");
        assertThat(SimpleKeyData.fromRefTag(Tools5eIndexType.deck,
                "Deck of Many Things|DMG|and optional link text added with another pipe").toKey())
                .isEqualTo("deck|deck of many things|dmg");
    }

    // {@card Vizier|Deck of Many Things}, {@card Vizier|Deck of Many Things|DMG}, {@card ...|DMG|linkText}
    @Test
    void testCardKeyData() {
        assertThat(CardKeyData.fromRefTag("Vizier|Deck of Many Things").toKey())
                .isEqualTo("card|vizier|deck of many things|dmg");
        assertThat(CardKeyData.fromRefTag("Vizier|Deck of Many Things|DMG").toKey())
                .isEqualTo("card|vizier|deck of many things|dmg");
        assertThat(CardKeyData.fromRefTag("Vizier|Deck of Many Things|DMG|and optional link text added with another pipe")
                .toKey())
                .isEqualTo("card|vizier|deck of many things|dmg");

        // Card's parent key (the deck) should resolve to the same shape SimpleKeyData produces for `deck`
        assertThat(CardKeyData.fromRefTag("Vizier|Deck of Many Things|DMG").toParentKey().toKey())
                .isEqualTo(SimpleKeyData.fromRefTag(Tools5eIndexType.deck, "Deck of Many Things|DMG").toKey());
    }
}
