package dev.ebullient.convert.tools.dnd5e;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.tools.Tags;

enum ItemTag {
    age,
    armor,
    attunement,
    gear,
    mastery,
    property,
    rarity,
    shield,
    tier,
    vehicle,
    weapon,
    wondrous,
    ;

    void add(Tags tags, String... segments) {
        tags.addRaw(build(segments));
    }

    String build(String... segments) {
        return Stream.concat(Stream.of("item", name()), Arrays.stream(segments))
                .map(Tui::slugify)
                .collect(Collectors.joining("/"));
    }
}
