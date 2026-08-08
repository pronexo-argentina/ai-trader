package com.iatradex.paper;

public record PaperPortfolioAutoConfig(
        boolean enabled,
        int minScore,
        int maxPositions,
        int maxArgentinaPositions,
        int maxInternationalPositions,
        int maxCryptoPositions,
        double maxGlobalRiskPct,
        double riskPerTradePct,
        double maxCapitalPerTradePct,
        double stopLossPct,
        double takeProfitPct
) {
    public static PaperPortfolioAutoConfig disabled() {
        return new PaperPortfolioAutoConfig(
                false,
                70,
                5,
                2,
                2,
                2,
                5.0,
                1.0,
                25.0,
                2.0,
                4.0
        );
    }

    public PaperPortfolioAutoConfig normalized() {
        PaperPortfolioAutoConfig defaults = disabled();

        return new PaperPortfolioAutoConfig(
                enabled,
                minScore,
                maxPositions <= 0 ? defaults.maxPositions : maxPositions,
                maxArgentinaPositions <= 0
                        ? defaults.maxArgentinaPositions
                        : maxArgentinaPositions,
                maxInternationalPositions <= 0
                        ? defaults.maxInternationalPositions
                        : maxInternationalPositions,
                maxCryptoPositions <= 0
                        ? defaults.maxCryptoPositions
                        : maxCryptoPositions,
                maxGlobalRiskPct <= 0.0
                        ? defaults.maxGlobalRiskPct
                        : maxGlobalRiskPct,
                riskPerTradePct <= 0.0
                        ? defaults.riskPerTradePct
                        : riskPerTradePct,
                maxCapitalPerTradePct <= 0.0
                        ? defaults.maxCapitalPerTradePct
                        : maxCapitalPerTradePct,
                stopLossPct <= 0.0
                        ? defaults.stopLossPct
                        : stopLossPct,
                takeProfitPct <= 0.0
                        ? defaults.takeProfitPct
                        : takeProfitPct
        );
    }

    public int maxForMarket(String marketType) {
        return switch (marketType) {
            case "argentina" -> maxArgentinaPositions;
            case "crypto" -> maxCryptoPositions;
            default -> maxInternationalPositions;
        };
    }
}
