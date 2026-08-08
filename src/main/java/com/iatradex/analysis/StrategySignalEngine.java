package com.iatradex.analysis;

import com.iatradex.model.Candle;
import com.iatradex.model.StrategyType;

import java.util.List;

public final class StrategySignalEngine {

    public boolean entrySignal(
            List<Candle> candles,
            int index,
            StrategyType strategy
    ) {
        if (index <= 0 || index >= candles.size()) {
            return false;
        }

        return switch (strategy) {
            case EMA_CROSS -> emaEntry(candles, index);
            case MOMENTUM -> momentumEntry(candles, index);
            case MEAN_REVERSION -> meanReversionEntry(candles, index);
            case BREAKOUT -> breakoutEntry(candles, index);
        };
    }

    public boolean exitSignal(
            List<Candle> candles,
            int index,
            StrategyType strategy
    ) {
        if (index <= 0 || index >= candles.size()) {
            return false;
        }

        return switch (strategy) {
            case EMA_CROSS -> emaExit(candles, index);
            case MOMENTUM -> momentumExit(candles, index);
            case MEAN_REVERSION -> meanReversionExit(candles, index);
            case BREAKOUT -> breakoutExit(candles, index);
        };
    }

    private boolean emaEntry(List<Candle> candles, int index) {
        return candles.get(index).cross() > 0;
    }

    private boolean emaExit(List<Candle> candles, int index) {
        return candles.get(index).cross() < 0;
    }

    private boolean momentumEntry(List<Candle> candles, int index) {
        int lookback = 20;

        if (index < lookback) {
            return false;
        }

        Candle row = candles.get(index);
        double oldClose = candles.get(index - lookback).close();

        if (oldClose <= 0.0 || !Double.isFinite(row.rsi14())) {
            return false;
        }

        double momentum = row.close() / oldClose - 1.0;

        return momentum >= 0.03
                && row.emaFast() > row.emaSlow()
                && row.rsi14() >= 55.0
                && row.rsi14() < 75.0;
    }

    private boolean momentumExit(List<Candle> candles, int index) {
        int lookback = 10;

        if (index < lookback) {
            return false;
        }

        Candle row = candles.get(index);
        double oldClose = candles.get(index - lookback).close();
        double momentum = oldClose <= 0.0
                ? 0.0
                : row.close() / oldClose - 1.0;

        return momentum < 0.0
                || row.emaFast() < row.emaSlow()
                || (Double.isFinite(row.rsi14()) && row.rsi14() < 45.0);
    }

    private boolean meanReversionEntry(List<Candle> candles, int index) {
        Candle row = candles.get(index);

        if (!Double.isFinite(row.rsi14())
                || !Double.isFinite(row.emaSlow())
                || row.emaSlow() <= 0.0) {
            return false;
        }

        double distanceFromMean = row.close() / row.emaSlow() - 1.0;

        return row.rsi14() <= 35.0
                && distanceFromMean <= -0.02;
    }

    private boolean meanReversionExit(List<Candle> candles, int index) {
        Candle row = candles.get(index);

        if (!Double.isFinite(row.rsi14())
                || !Double.isFinite(row.emaSlow())) {
            return false;
        }

        return row.rsi14() >= 55.0
                || row.close() >= row.emaSlow();
    }

    private boolean breakoutEntry(List<Candle> candles, int index) {
        int lookback = 20;

        if (index < lookback) {
            return false;
        }

        double priorHigh = highestHigh(
                candles,
                index - lookback,
                index - 1
        );

        Candle row = candles.get(index);

        return row.close() > priorHigh
                && row.emaFast() > row.emaSlow();
    }

    private boolean breakoutExit(List<Candle> candles, int index) {
        int lookback = 10;

        if (index < lookback) {
            return false;
        }

        double priorLow = lowestLow(
                candles,
                index - lookback,
                index - 1
        );

        Candle row = candles.get(index);

        return row.close() < priorLow
                || row.emaFast() < row.emaSlow();
    }

    private double highestHigh(
            List<Candle> candles,
            int fromInclusive,
            int toInclusive
    ) {
        double result = Double.NEGATIVE_INFINITY;

        for (int i = fromInclusive; i <= toInclusive; i++) {
            result = Math.max(result, candles.get(i).high());
        }

        return result;
    }

    private double lowestLow(
            List<Candle> candles,
            int fromInclusive,
            int toInclusive
    ) {
        double result = Double.POSITIVE_INFINITY;

        for (int i = fromInclusive; i <= toInclusive; i++) {
            result = Math.min(result, candles.get(i).low());
        }

        return result;
    }
}
