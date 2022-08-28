package dev.ebullient.json5e.tools5e;

import java.util.ArrayList;
import java.util.List;

public enum Currency {
    sp(10),
    gp(100);

    public final double cpEx;

    Currency(double cpEx) {
        this.cpEx = cpEx;
    }

    public static String coinValue(int cpValue) {
        int gp;
        int sp;

        List<String> coinValue = new ArrayList<>();

        gp = (int) (cpValue / Currency.gp.cpEx);
        cpValue -= (int) (gp * Currency.gp.cpEx);
        if (gp > 0) {
            coinValue.add(String.format("%,d gp", gp));
        }

        sp = (int) (cpValue / Currency.sp.cpEx);
        cpValue -= (int) (sp * Currency.sp.cpEx);
        if (sp > 0) {
            coinValue.add(String.format("%,d sp", sp));
        }

        if (cpValue > 0) {
            coinValue.add(String.format("%,d cp", cpValue));
        }

        return String.join(", ", coinValue);
    }
}
