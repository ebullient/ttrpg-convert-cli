package dev.ebullient.convert.tools;

import java.util.ArrayDeque;
import java.util.Deque;

import com.fasterxml.jackson.databind.JsonNode;

import dev.ebullient.convert.tools.dnd5e.Tools5eIndexType;
import dev.ebullient.convert.tools.pf2e.Pf2eIndexType;

public class ParseState {

    enum ParseStateField implements NodeReader {
        footnotes,
        page,
        source
    }

    public static class ParseStateInfo {
        boolean inFootnotes;
        boolean inList;
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

        private ParseStateInfo(boolean inFootnotes) {
            this(null, 0, "");
            this.inFootnotes = inFootnotes;
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

        private static ParseStateInfo srcAndPage(ParseStateInfo prev, String src, int page) {
            if (prev == null) {
                return new ParseState.ParseStateInfo(src, page);
            }
            return new ParseState.ParseStateInfo(
                    src == null ? prev.src : src,
                    page,
                    prev.listIndent)
                    .setInFootnotes(prev.inFootnotes)
                    .setInList(prev.inList);
        }

        private static ParseStateInfo changePage(ParseStateInfo prev, int page) {
            if (prev == null) {
                throw new IllegalStateException("Page without source first?");
            }
            return new ParseStateInfo(prev.src, page, prev.listIndent)
                    .setInFootnotes(prev.inFootnotes)
                    .setInList(prev.inList);
        }

        private static ParseStateInfo inFootnotes(ParseStateInfo prev, boolean inFootnotes) {
            if (prev == null) {
                return new ParseStateInfo(inFootnotes);
            }
            return new ParseState.ParseStateInfo(prev.src, prev.page, prev.listIndent)
                    .setInFootnotes(prev.inFootnotes)
                    .setInList(prev.inList);
        }

        private static ParseStateInfo indentList(ParseStateInfo prev) {
            if (prev == null) {
                return new ParseStateInfo().setInList(true);
            }
            return new ParseState.ParseStateInfo(prev.src, prev.page, prev.listIndent + "    ")
                    .setInFootnotes(prev.inFootnotes)
                    .setInList(true);
        }

        private static ParseStateInfo indentList(ParseStateInfo prev, String value) {
            if (prev == null) {
                return new ParseStateInfo().setInList(true);
            }
            return new ParseState.ParseStateInfo(prev.src, prev.page, value)
                    .setInFootnotes(prev.inFootnotes)
                    .setInList(true);
        }
    }

    private final Deque<ParseState.ParseStateInfo> stack = new ArrayDeque<>();

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
        if (src != null && !current.src.equals(src)) {
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

    public boolean push(boolean inFootnotes) {
        stack.addFirst(ParseStateInfo.inFootnotes(stack.peek(), inFootnotes));
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

    public void pop(boolean pushed) {
        if (pushed) {
            stack.removeFirst();
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

    public String sourceAndPage() {
        ParseState.ParseStateInfo current = stack.peek();
        if (current == null || current.page == 0) {
            return "";
        }
        return String.format("<sup>%s p. %s</sup>",
                current.src, current.page);
    }

    public String getSource(Tools5eIndexType type) {
        ParseState.ParseStateInfo current = stack.peek();
        if (current == null || current.src == null) {
            return type == null ? null : type.defaultSource().toString();
        }
        return current.src;
    }

    public String getSource(Pf2eIndexType type) {
        ParseState.ParseStateInfo current = stack.peek();
        if (current == null || current.src == null) {
            return type == null ? null : type.defaultSource().toString();
        }
        return current.src;
    }
}
