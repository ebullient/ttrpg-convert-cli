package dev.ebullient.convert.io;

public class FontRef {
    /** Font family */
    public final String fontFamily;
    /** Path to font source (unresolved local or remote) */
    public final String sourcePath;

    boolean hasTextReference = false;

    private FontRef(String fontFamily, String sourcePath) {
        this.fontFamily = fontFamily;
        this.sourcePath = sourcePath;
    }

    public void addTextReference() {
        hasTextReference = true;
    }

    public boolean hasTextReference() {
        return hasTextReference;
    }

    @Override
    public String toString() {
        return "FontRef [fontFamily=" + fontFamily + ", sourcePath=" + sourcePath + "]";
    }

    public static String fontFamily(String fontPath) {
        fontPath = fontPath.trim();
        int pos1 = fontPath.lastIndexOf('/');
        int pos2 = fontPath.lastIndexOf('.');
        if (pos1 > 0 && pos2 > 0) {
            fontPath = fontPath.substring(pos1 + 1, pos2);
        } else if (pos1 > 0) {
            fontPath = fontPath.substring(pos1 + 1);
        } else if (pos2 > 0) {
            fontPath = fontPath.substring(0, pos2);
        }
        return fontPath;
    }

    public static FontRef of(String fontString) {
        return of(fontFamily(fontString), fontString);
    }

    public static FontRef of(String fontFamily, String fontString) {
        if (fontString == null || fontString.isEmpty()) {
            return null;
        }
        return new FontRef(fontFamily, fontString);
    }
}
