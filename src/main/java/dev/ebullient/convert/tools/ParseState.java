package dev.ebullient.convert.tools;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.config.CompendiumConfig.DiceRoller;
import dev.ebullient.convert.config.TtrpgConfig;
import dev.ebullient.convert.io.Tui;
import dev.ebullient.convert.qute.SourceAndPage;
import dev.ebullient.convert.tools.dnd5e.Tools5eIndexType;
import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;

public class ParseState {

    enum ParseStateField implements JsonNodeReader {
        page,
        source
    }

    public static class ParseStateInfo {
        boolean inFootnotes;
        boolean inHtmlTable;
        boolean inMarkdownTable;
        boolean inList;
        boolean inTrait;
        int inFeatureType;
        final String listIndent;
        final String src;
        final int page;

        private ParseStateInfo() {
            this(null, 0, "");
        }

        private ParseStateInfo(String src) {
            this(src, 0, "");
        }

        private ParseStateInfo(String src, int page) {
            this(src, page, "");
        }

        private ParseStateInfo(String src, int page, String listIndent) {
            this.src = src;
            this.listIndent = listIndent;
            this.page = page;
        }

        private ParseStateInfo setInList(boolean inList) {
            this.inList = inList;
            return this;
        }

        private ParseStateInfo setInFootnotes(boolean inFootnotes) {
            this.inFootnotes = inFootnotes;
            return this;
        }

        private ParseStateInfo setInHtmlTable(boolean inTable) {
            this.inHtmlTable = inTable;
            return this;
        }

        private ParseStateInfo setInMarkdownTable(boolean inTable) {
            this.inMarkdownTable = inTable;
            return this;
        }

        private ParseStateInfo setInFeatureType(int inFeatureType) {
            this.inFeatureType = inFeatureType;
            return this;
        }

        private ParseStateInfo setInTrait(boolean inTrait) {
            this.inTrait = inTrait;
            return this;
        }

        private ParseStateInfo setTheRest(ParseStateInfo prev) {
            this.setInFootnotes(prev.inFootnotes)
                    .setInHtmlTable(prev.inHtmlTable)
                    .setInMarkdownTable(prev.inMarkdownTable)
                    .setInList(prev.inList)
                    .setInFeatureType(prev.inFeatureType)
                    .setInTrait(prev.inTrait);
            return this;
        }

        private static ParseStateInfo srcAndPage(ParseStateInfo prev, String src, int page) {
            if (prev == null) {
                return new ParseState.ParseStateInfo(src, page);
            }
            return new ParseState.ParseStateInfo(
                    src == null ? prev.src : src,
                    page,
                    prev.listIndent)
                    .setTheRest(prev);
        }

        private static ParseStateInfo changePage(ParseStateInfo prev, int page) {
            if (prev == null) {
                Tui.instance().errorf("Change Page called without someone setting the source first? %s");
                return null;
            }
            return new ParseStateInfo(prev.src, page, prev.listIndent)
                    .setTheRest(prev);
        }

        private static ParseStateInfo inFootnotes(ParseStateInfo prev, boolean inFootnotes) {
            if (prev == null) {
                return new ParseStateInfo().setInFootnotes(inFootnotes);
            }
            return new ParseState.ParseStateInfo(prev.src, prev.page, prev.listIndent)
                    .setTheRest(prev)
                    .setInFootnotes(inFootnotes);
        }

        private static ParseStateInfo inHtmlTable(ParseStateInfo prev, boolean inHtmlTable) {
            if (prev == null) {
                return new ParseStateInfo().setInHtmlTable(inHtmlTable);

            }
            return new ParseState.ParseStateInfo(prev.src, prev.page, prev.listIndent)
                    .setTheRest(prev)
                    .setInHtmlTable(inHtmlTable);
        }

        private static ParseStateInfo inMarkdownTable(ParseStateInfo prev, boolean inMarkdownTable) {
            if (prev == null) {
                return new ParseStateInfo().setInMarkdownTable(inMarkdownTable);

            }
            return new ParseState.ParseStateInfo(prev.src, prev.page, prev.listIndent)
                    .setTheRest(prev)
                    .setInMarkdownTable(inMarkdownTable);
        }

        private static ParseStateInfo indentList(ParseStateInfo prev) {
            if (prev == null) {
                return new ParseStateInfo().setInList(true);
            }
            return new ParseState.ParseStateInfo(prev.src, prev.page, prev.listIndent + "    ")
                    .setTheRest(prev)
                    .setInList(true);
        }

        private static ParseStateInfo indentList(ParseStateInfo prev, String value) {
            if (prev == null) {
                return new ParseStateInfo().setInList(true);
            }
            return new ParseState.ParseStateInfo(prev.src, prev.page, value)
                    .setTheRest(prev)
                    .setInList(true);
        }

        private static ParseStateInfo pushFeatureType(ParseStateInfo prev) {
            if (prev == null) {
                return new ParseStateInfo().setInFeatureType(0);
            }
            return new ParseState.ParseStateInfo(prev.src, prev.page, prev.listIndent)
                    .setTheRest(prev)
                    .setInFeatureType(prev.inFeatureType + 1);
        }

        private static ParseStateInfo pushTrait(ParseStateInfo prev) {
            if (prev == null) {
                return new ParseStateInfo().setInTrait(true);
            }
            return new ParseState.ParseStateInfo(prev.src, prev.page, prev.listIndent)
                    .setTheRest(prev)
                    .setInTrait(true);
        }
    }

    private final Deque<ParseState.ParseStateInfo> stack = new ArrayDeque<>();
    private final Map<String, String> citations = new HashMap<>();

    public boolean push(CompendiumSources sources, JsonNode rootNode) {
        if (rootNode != null && (rootNode.has("page") || rootNode.has("source"))) {
            return push(rootNode);
        }
        return push(sources);
    }

    public boolean push(CompendiumSources sources) {
        if (sources == null) {
            return false;
        }
        return push(sources.findNode());
    }

    public boolean push(JsonNode node) {
        String src = ParseStateField.source.getTextOrNull(node);
        int page = ParseStateField.page.intOrDefault(node, 0);
        return push(src, page);
    }

    public boolean push(String src, int page) {
        if (src == null && page == 0) {
            return false;
        }
        ParseStateInfo current = stack.peek();
        if (current == null) {
            ParseStateInfo info = ParseStateInfo.srcAndPage(current, src, page);
            stack.addFirst(info);
            return true;
        }
        if (src != null && !src.equals(current.src)) {
            ParseState.ParseStateInfo info = ParseStateInfo.srcAndPage(current, src, page);
            stack.addFirst(info);
            return true;
        } else if (page != 0 && page != current.page) {
            ParseStateInfo info = ParseStateInfo.changePage(current, page);
            stack.addFirst(info);
            return true;
        } else
            return false;
    }

    public boolean pushFootnotes(boolean inFootnotes) {
        stack.addFirst(ParseStateInfo.inFootnotes(stack.peek(), inFootnotes));
        return true;
    }

    public boolean pushHtmlTable(boolean inTable) {
        stack.addFirst(ParseStateInfo.inHtmlTable(stack.peek(), inTable));
        return true;
    }

    public boolean pushMarkdownTable(boolean inTable) {
        stack.addFirst(ParseStateInfo.inMarkdownTable(stack.peek(), inTable));
        return true;
    }

    public boolean indentList() {
        stack.addFirst(ParseStateInfo.indentList(stack.peek()));
        return true;
    }

    public boolean indentList(String value) {
        stack.addFirst(ParseStateInfo.indentList(stack.peek(), value));
        return true;
    }

    public boolean pushFeatureType() {
        stack.addFirst(ParseStateInfo.pushFeatureType(stack.peek()));
        return true;
    }

    public boolean pushTrait() {
        stack.addFirst(ParseStateInfo.pushTrait(stack.peek()));
        return true;
    }

    public void pop(boolean pushed) {
        if (pushed) {
            String source = sourcePageString();
            ParseStateInfo removed = stack.removeFirst();
            if (stack.isEmpty() && !citations.isEmpty()) {
                Tui.instance().errorf("%s left unreferenced citations behind", source.isEmpty() ? removed : source);
                citations.clear();
            }
        }
    }

    public String getListIndent() {
        ParseState.ParseStateInfo current = stack.peek();
        return current == null ? "" : current.listIndent;
    }

    public boolean inFootnotes() {
        ParseState.ParseStateInfo current = stack.peek();
        return current != null && current.inFootnotes;
    }

    public boolean inList() {
        ParseState.ParseStateInfo current = stack.peek();
        return current != null && current.inList;
    }

    public boolean inHtmlTable() {
        ParseState.ParseStateInfo current = stack.peek();
        return current != null && current.inHtmlTable;
    }

    public boolean inMarkdownTable() {
        ParseState.ParseStateInfo current = stack.peek();
        return current != null && current.inMarkdownTable;
    }

    public boolean inTable() {
        ParseState.ParseStateInfo current = stack.peek();
        return current != null && (current.inHtmlTable || current.inMarkdownTable);
    }

    public boolean inTrait() {
        ParseState.ParseStateInfo current = stack.peek();
        return current != null && current.inTrait;
    }

    public int featureTypeDepth() {
        ParseState.ParseStateInfo current = stack.peek();
        return current == null ? 0 : current.inFeatureType;
    }

    public String sourcePageString() {
        ParseState.ParseStateInfo current = stack.peek();
        if (current == null || current.page == 0) {
            return "";
        }
        return String.format("%s p. %s",
                current.src, current.page);
    }

    public String sourcePageString(String formatString) {
        ParseState.ParseStateInfo current = stack.peek();
        if (current == null || current.page == 0) {
            return "";
        }
        return String.format(formatString, current.src, current.page);
    }

    public String longSourcePageString(String formatString) {
        ParseState.ParseStateInfo current = stack.peek();
        if (current == null || current.page == 0) {
            return "";
        }
        return String.format(formatString,
                TtrpgConfig.sourceToLongName(current.src), current.page);
    }

    public String getSource() {
        ParseState.ParseStateInfo current = stack.peek();
        return current == null ? null : current.src;
    }

    public String getSource(Tools5eIndexType type) {
        ParseState.ParseStateInfo current = stack.peek();
        if (current == null || current.src == null) {
            return type == null ? null : type.defaultSourceString();
        }
        return current.src;
    }

    public String getSource(Pf2eIndexType type) {
        ParseState.ParseStateInfo current = stack.peek();
        if (current == null || current.src == null) {
            return type == null ? null : type.defaultSourceString();
        }
        return current.src;
    }

    public String getPage() {
        ParseState.ParseStateInfo current = stack.peek();
        return current == null ? null : current.page + "";
    }

    public SourceAndPage toSourceAndPage() {
        return new SourceAndPage(getSource(), getPage());
    }

    public void addCitation(String key, String citationText) {
        String old = citations.put(key, citationText);
        if (old != null && !old.equals(citationText)) {
            Tui.instance().errorf("Duplicate citation text for %s:\nOLD:\n%s\nNEW:\n%s", key, old, citationText);
        }
    }

    public void popCitations(List<String> footerEntries) {
        citations.forEach((k, v) -> {
            if (v.startsWith("|")) { // we have a table, assume noted thing is in the footnote
                footerEntries.add(v);
                return;
            }
            footerEntries.add(String.format("[%s]: %s",
                    k, v));
        });
        citations.clear();
    }

    public DiceFormulaState diceFormulaState() {
        return new DiceFormulaState(this);
    }

    public static class DiceFormulaState {
        public final DiceRoller roller;
        public final boolean suppressInYaml;

        public DiceFormulaState(ParseState parseState) {
            this.roller = TtrpgConfig.getConfig().useDiceRoller();
            this.suppressInYaml = parseState.inTrait() && roller.useFantasyStatblocks();
        }

        /**
         * We can't use dice roller fomulas if the roller is disabled, or if we're
         * in a YAML trait block.
         */
        public boolean noRoller() {
            return !roller.enabled() || suppressInYaml;
        }

        /** In YAML blocks (traits), we avoid all formatting in dice formulas */
        public boolean plainText() {
            return suppressInYaml;
        }
    }
}
