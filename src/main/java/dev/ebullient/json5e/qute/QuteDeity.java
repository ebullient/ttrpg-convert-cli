package dev.ebullient.json5e.qute;

import java.util.Collection;
import java.util.List;

public class QuteDeity extends QuteNote {

    public final List<String> altNames;
    public final String pantheon;
    public final String alignment;
    public final String title;
    public final String category;
    public final String domains;
    public final String province;
    public final String symbol;
    final ImageRef symbolImg;

    public QuteDeity(String name, String source,
            List<String> altNames, String pantheon, String alignment,
            String title, String cateogry, String domains,
            String province, String symbol, ImageRef symbolImg,
            String text, Collection<String> tags) {
        super(name, source, text, tags);
        this.altNames = altNames;
        this.pantheon = pantheon;
        this.alignment = alignment;
        this.title = title;
        this.category = cateogry;
        this.domains = domains;
        this.province = province;
        this.symbol = symbol;
        this.symbolImg = symbolImg;
    }

    public String getImage() {
        if (symbolImg == null) {
            return null;
        }
        return symbolImg.link;
    }

    @Override
    public List<ImageRef> images() {
        if (symbolImg == null) {
            return List.of();
        }
        return List.of(symbolImg);
    }

    @Override
    public String targetPath() {
        return "deities";
    }
}
