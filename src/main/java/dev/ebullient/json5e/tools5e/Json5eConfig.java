package dev.ebullient.json5e.tools5e;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Json5eConfig {
    final static TypeReference<List<String>> LIST_STRING = new TypeReference<List<String>>() {
    };
    final static TypeReference<Map<String, String>> MAP_STRING_STRING = new TypeReference<Map<String, String>>() {
    };
    final static String CONVERT = "convert";
    final static String BOOK = "book";
    final static String ADVENTURE = "adventure";
    final static String TEMPLATE = "template";

    final Map<String, String> templates = new HashMap<>();
    final Set<String> adventures = new HashSet<>();
    final Set<String> books = new HashSet<>();

    public void readConfigIfPresent(ObjectMapper mapper, JsonNode node) {
        JsonNode fullSource = node.get(CONVERT);
        if (fullSource != null) {
            if (fullSource.has(ADVENTURE)) {
                List<String> a = mapper.convertValue(fullSource.get(ADVENTURE), LIST_STRING);
                adventures.addAll(a);
            }
            if (fullSource.has(BOOK)) {
                List<String> b = mapper.convertValue(fullSource.get(BOOK), LIST_STRING);
                books.addAll(b);
            }
        }
        if (node.has(TEMPLATE)) {
            Map<String, String> tpl = mapper.convertValue(node.get(TEMPLATE), MAP_STRING_STRING);
            templates.putAll(tpl);
        }
    }

    public Map<String, Path> getTemplates() {
        if (templates.isEmpty()) {
            return Map.of();
        }

        Map<String, Path> result = new HashMap<>();
        for (Entry<String, String> entry : templates.entrySet()) {
            result.put(entry.getKey(), Path.of(entry.getValue()));
        }
        return result;
    }

    public List<String> getBooks() {
        return books.stream()
                .map(b -> {
                    if (b.endsWith(".json")) {
                        return b;
                    }
                    return "book/book-" + b.toLowerCase() + ".json";
                })
                .collect(Collectors.toList());
    }

    public List<String> getAdventures() {
        return adventures.stream()
                .map(a -> {
                    if (a.endsWith(".json")) {
                        return a;
                    }
                    return "adventure/adventure-" + a.toLowerCase() + ".json";
                })
                .collect(Collectors.toList());
    }
}
