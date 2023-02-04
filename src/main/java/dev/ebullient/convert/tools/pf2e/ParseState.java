package dev.ebullient.convert.tools.pf2e;

import java.util.ArrayDeque;
import java.util.Deque;

import com.fasterxml.jackson.databind.JsonNode;

public class ParseState {
    public static class ParseStateInfo {
        final boolean inFootnotes;
        final String listIndent;
        final String src;
        final int page;

        private ParseStateInfo() {
            this(null, 0, false, "");
        }

        private ParseStateInfo(String src) {
            this(src, 0, false, "");
        }

        private ParseStateInfo(String src, int page) {
            this(src, page, false, "");
        }

        private ParseStateInfo(boolean inFootnotes) {
            this(null, 0, inFootnotes, "");
        }

        private ParseStateInfo(String src, int page, boolean inFootnotes, String listIndent) {
            this.src = src;
            this.inFootnotes = inFootnotes;
            this.listIndent = listIndent;
            this.page = page;
        }

        private static ParseStateInfo srcAndPage(ParseStateInfo prev, String src, int page) {
            if (prev == null) {
                return new ParseState.ParseStateInfo(src, page);
            }
            return new ParseState.ParseStateInfo(
                    src == null ? prev.src : src,
                    page,
                    prev.inFootnotes,
                    prev.listIndent);
        }

        private static ParseStateInfo changePage(ParseStateInfo prev, int page) {
            if (prev == null) {
                throw new IllegalStateException("Page without source first?");
            }
            return new ParseStateInfo(prev.src, page, prev.inFootnotes, prev.listIndent);
        }

        private static ParseStateInfo inFootnotes(ParseStateInfo prev, boolean inFootnotes) {
            if (prev == null) {
                return new ParseStateInfo(inFootnotes);
            }
            return new ParseState.ParseStateInfo(prev.src, prev.page, inFootnotes, prev.listIndent);
        }

        private static ParseStateInfo indentList(ParseStateInfo prev) {
            if (prev == null) {
                throw new IllegalStateException("List indent outside of src/page?");
            }
            return new ParseState.ParseStateInfo(prev.src, prev.page, prev.inFootnotes,
                    prev.listIndent + "    ");
        }

        private static ParseStateInfo indentList(ParseStateInfo prev, String value) {
            if (prev == null) {
                throw new IllegalStateException("List indent outside of src/page?");
            }
            return new ParseState.ParseStateInfo(prev.src, prev.page, prev.inFootnotes,
                    value);
        }
    }

    private final Deque<ParseState.ParseStateInfo> stack = new ArrayDeque<>();

    public boolean push(Pf2eSources sources) {
        if (sources == null) {
            return false;
        }
        return push(Pf2eIndex.findNode(sources));
    }

    public boolean push(JsonNode node) {
        String src = JsonSource.Field.source.getTextOrNull(node);
        int page = JsonSource.Field.page.intOrDefault(node, 0);
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

    public String sourceAndPage() {
        ParseState.ParseStateInfo current = stack.peek();
        if (current == null || current.page == 0) {
            return "";
        }
        return String.format("<sup>%s p. %s</sup>",
                current.src, current.page);
    }

    public String getSource(Pf2eIndexType type) {
        ParseState.ParseStateInfo current = stack.peek();
        if (current == null || current.src == null) {
            return type == null ? null : type.defaultSource().toString();
        }
        return current.src;
    }
}
