package dev.ebullient.json5e.tools5e;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.json5e.qute.QuteName;
import dev.ebullient.json5e.qute.QuteName.LookupTable;
import dev.ebullient.json5e.qute.QuteName.LookupTableRow;

public class Json2QuteName extends Json2QuteCommon {

    public Json2QuteName(JsonIndex index, JsonNode jsonNode) {
        super(index, IndexType.namelist, jsonNode);
    }

    @Override
    public QuteName build() {
        JsonNode tables = node.get("tables");
        if (tables == null || tables.isEmpty()) {
            index.tui().debugf("Skipped %s; no tables", getName());
            return null;
        }

        boolean isSRD = node.has("srd");
        JsonNode itemSource = node.get("source");
        if (index.excludeItem(itemSource, isSRD)) {
            // skip this item: not from a specified source
            index.tui().debugf("Skipped %s from %s (%s)", getName(), itemSource, isSRD);
            return null;
        }

        List<LookupTable> lookupTables = new ArrayList<>();
        for (Iterator<JsonNode> j = tables.elements(); j.hasNext();) {
            JsonNode t_element = j.next();
            String tableName = t_element.get("option").asText();
            String blockId = index.slugify(tableName);
            String diceType = t_element.get("diceType").asText();

            List<LookupTableRow> rows = new ArrayList<>();
            JsonNode table = t_element.get("table");
            for (Iterator<JsonNode> k = table.elements(); k.hasNext();) {
                JsonNode row = k.next();
                String min = row.get("min").asText();
                String max = row.get("max").asText();
                String result = row.get("result").asText();
                if ("0".equals(max)) {
                    max = "100";
                } else if (min.equals(max)) {
                    max = null;
                }
                rows.add(new LookupTableRow(min, max, result));
            }
            lookupTables.add(new LookupTable(tableName, diceType, blockId, rows));
        }
        return new QuteName(sources.getName(),
                sources.getSourceText(index.srdOnly()),
                lookupTables);
    }
}
