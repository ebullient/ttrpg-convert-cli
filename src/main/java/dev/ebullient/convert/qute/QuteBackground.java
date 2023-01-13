package dev.ebullient.convert.qute;

import java.util.List;

import dev.ebullient.convert.tools5e.CompendiumSources;
import io.quarkus.qute.TemplateData;
import io.quarkus.runtime.annotations.RegisterForReflection;

@TemplateData
@RegisterForReflection
public class QuteBackground extends QuteBase {

    final List<ImageRef> images;

    public QuteBackground(CompendiumSources sources,
            String name, String source, String text,
            List<ImageRef> images,
            List<String> tags) {
        super(sources, name, source, text, tags);
        this.images = images;
    }

    @Override
    public String targetPath() {
        return QuteSource.BACKGROUND_PATH;
    }

    @Override
    public List<ImageRef> images() { // not usable by Qute templates
        return images;
    }

    public List<ImageRef> getFluffImages() {
        return images;
    }
}
