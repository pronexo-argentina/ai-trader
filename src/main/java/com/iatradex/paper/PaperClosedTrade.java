package com.iatradex.paper;

public record PaperClosedTrade(
        String id,
        String symbol,
        String market,
        String currency,
        double quantity,
        double entryPrice,
        double exitPrice,
        double pnl,
        double pnlPct,
        String strategyContext,
        String regimeContext,
        String openedAt,
        String closedAt,
        String reason
) {}
