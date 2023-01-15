package dev.ebullient.convert.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public enum Datasource {
    tools5e("5etools", "5e"),
    toolsPf2e("p2fetools", "pf2e");

    public final List<String> format;

    Datasource(String... format) {
        this.format = List.of(format);
    }

    public static Datasource matchDatasource(String input) {
        String key = input.toLowerCase();
        Optional<Datasource> value = Stream.of(Datasource.values())
                .filter((d) -> d.name().equals(key) || d.format.contains(key))
                .findFirst();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Unknown data source: " + input);
        }
        return value.get();
    }

    public static class DatasourceCandidates extends ArrayList<String> {
        DatasourceCandidates() {
            super(List.of("5etools", "p2fetools"));
        }
    }
}
