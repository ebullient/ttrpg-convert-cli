package dev.ebullient.convert.tools.pf2e.qute;

import java.util.ArrayList;
import java.util.List;

import dev.ebullient.convert.qute.QuteUtil;
import io.quarkus.qute.TemplateData;

/**
 * Pf2eTools Hit Points and Hardiness attributes
 */
@TemplateData
public class QuteDataHpHardness implements QuteUtil {
    public String name;
    public String hpNotes;
    public String hpValue;
    public String hardnessNotes;
    public String hardnessValue;
    public String brokenThreshold;

    public String toString() {
        String n = isPresent(name) ? (name.trim() + " ") : "";
        String btPart = "";
        String hpNotePart = "";
        boolean hasHardnessNotes = isPresent(hardnessNotes);

        List<String> hardParts = new ArrayList<>();

        if (isPresent(hardnessValue)) {
            hardParts.add(String.format("**%sHardness** %s%s",
                    n,
                    hardnessValue,
                    isPresent(hardnessNotes) ? (" " + hardnessNotes) : ""));
        }
        if (isPresent(hpValue)) {
            hardParts.add(String.format("**%sHP** %s", n, hpValue));
        }
        if (isPresent(brokenThreshold)) {
            btPart = " (BT " + brokenThreshold + ")";
        }
        if (isPresent(hpNotes)) {
            hpNotePart = " " + hpNotes;
        }
        return String.join(hasHardnessNotes ? "; " : ", ", hardParts)
                + btPart + hpNotePart;
    }
}
