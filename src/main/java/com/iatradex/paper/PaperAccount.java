package com.iatradex.paper;

public record PaperAccount(
        String currency,
        double initialCapital,
        double cash
) {
    public PaperAccount withInitialCapital(double value) {
        return new PaperAccount(currency, value, value);
    }

    public PaperAccount withCash(double value) {
        return new PaperAccount(currency, initialCapital, value);
    }
}
