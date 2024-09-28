package dev.ebullient.convert.tools.dnd5e.qute;

import java.util.Collection;

import dev.ebullient.convert.qute.NamedText;
import dev.ebullient.convert.tools.CompendiumSources;
import dev.ebullient.convert.tools.Tags;
import io.quarkus.qute.TemplateData;

/**
 * 5eTools psionic talent attributes ({@code psionic2md.txt})
 *
 * Extension of {@link dev.ebullient.convert.tools.dnd5e.qute.Tools5eQuteBase}.
 */
@TemplateData
public class QutePsionic extends Tools5eQuteBase {

    /** Psionic type and order (string) */
    public final String typeOrder;
    /** Psionic focus (string) */
    public final String focus;
    /** Psionic mode as list of {@link dev.ebullient.convert.qute.NamedText} */
    public final Collection<NamedText> modes;

    public QutePsionic(CompendiumSources sources, String name, String source,
            String typeOrder, String focus, Collection<NamedText> modes,
            String text, Tags tags) {
        super(sources, name, source, text, tags);
        this.typeOrder = typeOrder;
        this.focus = focus;
        this.modes = modes;
    }
}
