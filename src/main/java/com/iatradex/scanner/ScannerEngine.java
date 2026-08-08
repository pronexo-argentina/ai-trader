package com.iatradex.scanner;

import com.iatradex.analysis.AnalysisService;
import com.iatradex.analysis.StrategySignalEngine;
import com.iatradex.model.AnalysisResult;
import com.iatradex.model.Candle;
import com.iatradex.model.MarketRegime;
import com.iatradex.model.StrategyPerformance;
import com.iatradex.model.StrategyType;
import com.iatradex.model.TechnicalSnapshot;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ScannerEngine {

    private final AnalysisService analysisService;
    private final StrategySignalEngine signals =
            new StrategySignalEngine();

    public ScannerEngine(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    public ScannerResult scan(WatchlistItem item) {
        try {
            AnalysisResult analysis = analysisService.analyze(
                    item.marketType(),
                    item.source(),
                    item.symbol(),
                    item.timeframe(),
                    item.period(),
                    item.currency()
            );

            return score(item, analysis);
        } catch (Exception ex) {
            return new ScannerResult(
                    item,
                    null,
                    "—",
                    "—",
                    null,
                    "—",
                    StrategyType.EMA_CROSS,
                    "ERROR",
                    0,
                    "No se pudo analizar el activo.",
                    ex.getMessage() == null
                            ? ex.getClass().getSimpleName()
                            : ex.getMessage()
            );
        }
    }

    private ScannerResult score(
            WatchlistItem item,
            AnalysisResult analysis
    ) {
        TechnicalSnapshot technical = analysis.technical();
        MarketRegime regime = analysis.regime();
        List<Candle> candles = analysis.candles();
        int signalIndex = Math.max(1, candles.size() - 2);

        List<StrategyType> entryStrategies = new ArrayList<>();

        for (StrategyType type : StrategyType.values()) {
            if (signals.entrySignal(candles, signalIndex, type)) {
                entryStrategies.add(type);
            }
        }

        StrategyType selectedStrategy;

        if (!entryStrategies.isEmpty()) {
            selectedStrategy = entryStrategies.stream()
                    .max(Comparator.comparingDouble(
                            type -> historicalReturn(analysis, type)
                    ))
                    .orElse(entryStrategies.get(0));
        } else {
            selectedStrategy = regime.compatibleStrategies().stream()
                    .max(Comparator.comparingDouble(
                            type -> historicalReturn(analysis, type)
                    ))
                    .orElse(analysis.bestHistoricalStrategy());
        }

        int score = 0;
        List<String> why = new ArrayList<>();

        if ("ALCISTA".equalsIgnoreCase(regime.trend())) {
            score += 24;
            why.add("+24 tendencia alcista");
        } else if ("LATERAL".equalsIgnoreCase(regime.trend())) {
            score += 10;
            why.add("+10 mercado lateral");
        } else {
            why.add("+0 tendencia bajista");
        }

        if ("ALTA".equalsIgnoreCase(regime.strength())) {
            score += 14;
            why.add("+14 fuerza alta");
        } else if ("MEDIA".equalsIgnoreCase(regime.strength())) {
            score += 9;
            why.add("+9 fuerza media");
        } else {
            score += 3;
            why.add("+3 fuerza baja");
        }

        Double rsi = technical.rsi14();

        if (rsi != null && rsi >= 50.0 && rsi < 70.0) {
            score += 16;
            why.add("+16 RSI favorable");
        } else if (rsi != null && rsi >= 35.0 && rsi < 50.0) {
            score += 8;
            why.add("+8 RSI neutral");
        } else if (rsi != null && rsi <= 35.0
                && selectedStrategy == StrategyType.MEAN_REVERSION) {
            score += 15;
            why.add("+15 RSI de reversión");
        } else {
            why.add("+0 RSI sin ventaja clara");
        }

        if (regime.compatibleStrategies().contains(selectedStrategy)) {
            score += 14;
            why.add("+14 estrategia compatible");
        }

        boolean entry = entryStrategies.contains(selectedStrategy);

        if (entry) {
            score += 22;
            why.add("+22 señal de entrada");
        }

        double histReturn = historicalReturn(
                analysis,
                selectedStrategy
        );

        if (histReturn > 0.0) {
            int historicalPoints = (int) Math.min(
                    10,
                    Math.round(histReturn)
            );
            score += historicalPoints;
            why.add(
                    "+" + historicalPoints
                            + " retorno histórico positivo"
            );
        }

        if ("ALTA".equalsIgnoreCase(regime.volatility())) {
            score -= 5;
            why.add("-5 volatilidad alta");
        } else if ("BAJA".equalsIgnoreCase(regime.volatility())) {
            score += 4;
            why.add("+4 volatilidad baja");
        }

        score = Math.max(0, Math.min(100, score));

        String signal = entry
                ? "ENTRADA"
                : technical.signal().contains("COMPRA")
                ? "OBSERVAR"
                : "ESPERAR";

        return new ScannerResult(
                item,
                analysis,
                regime.trend(),
                regime.volatility(),
                rsi,
                technical.trend(),
                selectedStrategy,
                signal,
                score,
                String.join(" · ", why),
                null
        );
    }

    private double historicalReturn(
            AnalysisResult analysis,
            StrategyType type
    ) {
        return analysis.strategies().stream()
                .filter(s -> s.strategy() == type)
                .map(StrategyPerformance::metrics)
                .mapToDouble(m -> m.returnPct())
                .findFirst()
                .orElse(0.0);
    }
}
