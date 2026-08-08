package com.iatradex.analysis;

import com.iatradex.market.MarketService;
import com.iatradex.model.AnalysisResult;
import com.iatradex.model.BacktestResult;
import com.iatradex.model.Candle;
import com.iatradex.model.MarketQuote;
import com.iatradex.model.MarketRegime;
import com.iatradex.model.StrategyPerformance;
import com.iatradex.model.StrategyType;
import com.iatradex.model.TechnicalSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class AnalysisService {

    private final MarketService marketService;
    private final IndicatorEngine indicators;
    private final BacktestEngine backtest;
    private final MarketRegimeEngine regimeEngine;

    public AnalysisService(MarketService marketService) {
        this.marketService = marketService;
        this.indicators = new IndicatorEngine();
        this.backtest = new BacktestEngine();
        this.regimeEngine = new MarketRegimeEngine();
    }

    public AnalysisResult analyze(
            String marketType,
            String source,
            String symbol,
            String timeframe,
            String period,
            String currency
    ) throws Exception {
        List<Candle> candles = marketService.fetch(
                marketType,
                source,
                symbol,
                timeframe,
                period
        );

        indicators.apply(candles);
        TechnicalSnapshot snapshot = indicators.snapshot(candles);
        MarketQuote quote = marketService.quote(marketType, symbol);
        MarketRegime regime = regimeEngine.detect(candles);

        List<StrategyPerformance> strategies = new ArrayList<>();

        for (StrategyType strategy : StrategyType.values()) {
            BacktestResult result = backtest.run(
                    candles,
                    marketType,
                    timeframe,
                    strategy,
                    10_000.0,
                    0.001,
                    0.0005,
                    0.01,
                    0.02,
                    0.04
            );

            strategies.add(new StrategyPerformance(
                    strategy,
                    result.metrics(),
                    result.equity(),
                    result.trades()
            ));
        }

        StrategyPerformance primary = strategies.stream()
                .filter(s -> s.strategy() == StrategyType.EMA_CROSS)
                .findFirst()
                .orElseThrow();

        StrategyType best = strategies.stream()
                .max(
                        Comparator.comparingDouble(
                                s -> s.metrics().returnPct()
                        )
                )
                .map(StrategyPerformance::strategy)
                .orElse(StrategyType.EMA_CROSS);

        return new AnalysisResult(
                marketType,
                source,
                symbol,
                timeframe,
                period,
                currency,
                candles,
                snapshot,
                quote,
                regime,
                primary,
                List.copyOf(strategies),
                best
        );
    }
}
