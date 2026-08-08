package com.iatradex.paper;

public record PaperAutoConfig(
        boolean enabled,
        String symbol,
        String marketType,
        String source,
        String currency,
        String timeframe,
        String period,
        String strategy,
        double maxCapital,
        double riskPct,
        double stopLossPct,
        double takeProfitPct
) {
    public static PaperAutoConfig disabled() {
        return new PaperAutoConfig(
                false,
                "",
                "",
                "",
                "USD",
                "1h",
                "3m",
                "MOMENTUM",
                2_000.0,
                1.0,
                2.0,
                4.0
        );
    }
}
