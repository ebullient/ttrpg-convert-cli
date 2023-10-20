package dev.ebullient.convert.qute;

import java.util.Collection;
import java.util.Comparator;
import java.util.Set;
import java.util.TreeSet;

import io.quarkus.qute.TemplateData;

/**
 * Holder of a name or category and associated descriptive text.
 *
 * <p>
 * This attribute will render itself as labeled elements
 * if you reference it directly.
 * </p>
 */
@TemplateData
public class NamedText {
    public final static Comparator<NamedText> comparator = Comparator.comparing(NamedText::getKey);

    /** Name */
    public final String name;
    /** Pre-formatted text description */
    public final String desc;

    public NamedText(String name, String desc) {
        this.name = name;
        this.desc = desc;
    }

    public NamedText(String name, Collection<String> text) {
        this.name = name == null ? "" : name;
        String body = text == null ? "" : String.join("\n", text);
        if (body.startsWith(">")) {
            body = "\n" + body;
        }
        this.desc = body;
    }

    public boolean hasContent() {
        return !(name.isBlank() && desc.isBlank());
    }

    /** Alternate accessor for the name */
    public String getKey() {
        return name;
    }

    /** Alternate accessor for the name */
    public String getCategory() {
        return name;
    }

    /** Alternate accessor for formatted/descriptive text */
    public String getText() {
        return desc;
    }

    /** Alternate accessor for formatted/descriptive text */
    public String getValue() {
        return desc;
    }

    public String toString() {
        if (name.isBlank()) {
            return desc;
        }
        return String.format("**%s** %s", name, desc);
    }

    public static class SortedBuilder {
        Set<NamedText> list = new TreeSet<>(comparator);

        public SortedBuilder add(String name, String text) {
            list.add(new NamedText(name, text));
            return this;
        }

        public boolean isEmpty() {
            return list.isEmpty();
        }

        /** Returns a modifiable collection of NamedText (sorted order) */
        public Collection<NamedText> build() {
            return list;
        }
    }
}
