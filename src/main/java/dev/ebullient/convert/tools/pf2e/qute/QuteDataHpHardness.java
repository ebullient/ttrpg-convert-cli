package dev.ebullient.convert.tools.pf2e.qute;

import io.quarkus.qute.TemplateData;

@TemplateData
public class QuteDataHpHardness {
    public String name;
    public String hpNotes;
    public String hpValue;
    public String hardnessNotes;
    public String hardnessValue;
    public String brokenThreshold;

    public String toString() {
        String n = name == "" ? "" : (name + " ");
        String hardPart = "";
        String btPart = "";
        String hpPart = "";
        String hpNotePart = "";

        if (hardnessValue != null) {
            hardPart = String.format("**%sHardness** %s%s",
                    n, hardnessValue,
                    hardnessNotes == null ? ", " : (" " + hardnessNotes + "; "));
        }
        if (brokenThreshold != null) {
            btPart = " (BT " + brokenThreshold + ")";
        }
        if (hpValue != null) {
            hpPart = String.format("**%sHP** %s", n, hpValue);
        }
        if (hpNotes != null) {
            hpNotePart = " " + hpNotes;
        }
        return hardPart + hpPart + btPart + hpNotePart;
    }
}
