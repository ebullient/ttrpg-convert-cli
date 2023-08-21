package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.List;

import dev.ebullient.convert.qute.ImageRef;
import dev.ebullient.convert.tools.Tags;
import dev.ebullient.convert.tools.dnd5e.Tools5eSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * 5eTools deity attributes ({@code deity2md.txt})
 * <p>
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.Tools5eQuteBase Tools5eQuteBase}.
 * </p>
 */
@TemplateData
@RegisterForReflection
public class QuteDeity extends Tools5eQuteBase {

    /** List of alternative names */
    public final List<String> altNames;
    /** Pantheon to which this deity belongs: Celtic */
    public final String pantheon;
    /** Alignment of this deity */
    public final String alignment;
    /** Title of this deity */
    public final String title;
    /** Category of this deity: Lesser Idols, Prime Deities */
    public final String category;
    /** Category of this deity: Nature, Tempest */
    public final String domains;
    /** Province of this deity: Discovery, Luck, Storms, Travel, ... */
    public final String province;
    /** Text description of deity's symbol: Wave of white water on green */
    public final String symbol;
    /** Image or symbol representing this deity (as {@link dev.ebullient.convert.qute.ImageRef ImageRef}) */
    public final ImageRef image;

    public QuteDeity(Tools5eSources sources, String name, String source,
            List<String> altNames, String pantheon, String alignment,
            String title, String cateogry, String domains,
            String province, String symbol, ImageRef symbolImg,
            String text, Tags tags) {
        super(sources, name, source, text, tags);
        this.altNames = altNames;
        this.pantheon = pantheon;
        this.alignment = alignment;
        this.title = title;
        this.category = cateogry;
        this.domains = domains;
        this.province = province;
        this.symbol = symbol;
        this.image = symbolImg;
    }

    @Override
    public String targetFile() {
        return Tools5eQuteBase.getDeityResourceName(name, sources().primarySource(), pantheon);
    }
}
