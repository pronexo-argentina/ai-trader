package com.iatradex.model;

import java.util.List;

public record AnalysisResult(
        String marketType,
        String source,
        String symbol,
        String timeframe,
        String period,
        String currency,
        List<Candle> candles,
        TechnicalSnapshot technical,
        MarketQuote quote,
        MarketRegime regime,
        StrategyPerformance primaryStrategy,
        List<StrategyPerformance> strategies,
        StrategyType bestHistoricalStrategy
) {}
