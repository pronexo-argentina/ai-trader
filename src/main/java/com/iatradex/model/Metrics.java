package com.iatradex.model;

public record Metrics(
        double capitalInitial,
        double capitalFinal,
        double returnPct,
        double maxDrawdownPct,
        int trades,
        int winners,
        double winRatePct,
        Double profitFactor,
        Double avgWin,
        Double avgLoss,
        Double sharpeRatio,
        double buyHoldReturnPct
) {}
