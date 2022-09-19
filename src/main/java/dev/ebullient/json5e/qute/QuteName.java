package dev.ebullient.json5e.qute;

import java.util.List;

public class QuteName implements QuteSource {

    final String name;
    final String source;
    final List<LookupTable> tables;

    public QuteName(String name, String source, List<LookupTable> tables) {
        this.name = name;
        this.source = source;
        this.tables = tables;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSource() {
        return source;
    }

    @Override
    public List<ImageRef> images() {
        return List.of();
    }

    @Override
    public String targetPath() {
        return null;
    }

    @Override
    public String title() {
        return name;
    }

    public List<LookupTable> getTables() {
        return tables;
    }

    public static class LookupTable {
        public final String name;
        public final String diceType;
        public final String blockId;
        public final List<LookupTableRow> rows;

        public LookupTable(String name, String diceType, String blockId, List<LookupTableRow> rows) {
            this.name = name;
            this.diceType = diceType;
            this.blockId = blockId;
            this.rows = rows;
        }
    }

    public static class LookupTableRow {
        public final String min;
        public final String max;
        public final String result;

        public LookupTableRow(String min, String max, String result) {
            this.min = min;
            this.max = max;
            this.result = result;
        }

        public String getRange() {
            if (max == null) {
                return min;
            }
            return String.format("%s-%s", min, max);
        }
    }
}
