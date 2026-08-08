package com.iatradex.model;

import java.util.List;

public record StrategyPerformance(
        StrategyType strategy,
        Metrics metrics,
        List<EquityPoint> equity,
        List<Trade> trades
) {}
