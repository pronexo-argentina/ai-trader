package com.iatradex.model;

import java.util.List;

public record BacktestResult(
        Metrics metrics,
        List<EquityPoint> equity,
        List<Trade> trades
) {}
