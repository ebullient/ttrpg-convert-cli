package dev.ebullient.json5e.tools5e;

import java.util.ArrayList;
import java.util.List;

public enum Currency {
    cp(new double[] { 1, 0.10, 0.02, 0.01, 0.001 }),
    sp(new double[] { 10, 1, 0.2, 0.1, 0.01 }),
    ep(new double[] { 50, 5, 1, 0.5, 0.05 }),
    gp(new double[] { 100, 10, 2, 1, 0.1 }),
    pp(new double[] { 1000, 100, 20, 10, 1 });

    public final double cpEx, spEx, epEx, gpEx, ppEx;

    Currency(double[] exchangeRates) {
        cpEx = exchangeRates[0];
        spEx = exchangeRates[1];
        epEx = exchangeRates[2];
        gpEx = exchangeRates[3];
        ppEx = exchangeRates[4];
    }

    public static String coinValue(int cpValue) {
        int gp = 0;
        int sp = 0;

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
