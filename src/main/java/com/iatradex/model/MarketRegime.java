package com.iatradex.model;

import java.util.List;

public record MarketRegime(
        String trend,
        String volatility,
        String strength,
        double emaSpreadPct,
        double return20Pct,
        Double atrPct,
        Double volatilityRatio,
        String explanation,
        List<StrategyType> compatibleStrategies
) {}
