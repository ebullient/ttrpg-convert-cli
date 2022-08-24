package dev.ebullient.json5e.tools5e;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.json5e.io.MarkdownWriter;
import dev.ebullient.json5e.qute.QuteSource;

public class Json2MarkdownConverter {
    final JsonIndex index;
    final MarkdownWriter writer;

    public Json2MarkdownConverter(JsonIndex index, MarkdownWriter writer) {
        this.index = index;
        this.writer = writer;
    }

    public Json2MarkdownConverter writeFiles(IndexType type, String title) {
        String prefix = type + "|";
        Map<String, JsonNode> variants = StreamSupport.stream(index.elements().spliterator(), false)
                .filter(e -> e.getKey().startsWith(prefix))
                .flatMap(e -> findVariants(type, e.getKey(), e.getValue()))
                .filter(e -> index.keyIsIncluded(e.getKey()))
                .filter(e -> !JsonSource.isReprinted(index, e.getKey(), e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        List<QuteSource> nodes = new ArrayList<>();

        for (JsonNode e : variants.values()) {
            QuteSource converted = json2qute(type, e);
            if (converted != null) {
                nodes.add(converted);
            }
        }

        try {
            writer.writeFiles(nodes, title);
        } catch (IOException e) {
            index.tui().error("⛔️ Exception: " + e.getCause().getMessage());
        }
        return this;
    }

    Stream<Map.Entry<String, JsonNode>> findVariants(IndexType type, String key, JsonNode jsonSource) {
        return Map.of(key, jsonSource).entrySet().stream();
    }

    private QuteSource json2qute(IndexType type, JsonNode jsonNode) {
        switch (type) {
            case background:
                return new Json2QuteBackground(index, type, jsonNode).build();
            case feat:
                return new Json2QuteFeat(index, type, jsonNode).build();
            case namelist:
                return new Json2QuteName(index, jsonNode).build();
            default:
                throw new IllegalArgumentException("Unsupported type " + type);
        }
    }
}
