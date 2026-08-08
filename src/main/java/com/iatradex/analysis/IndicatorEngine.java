package com.iatradex.analysis;

import com.iatradex.model.Candle;
import com.iatradex.model.TechnicalSnapshot;

import java.util.List;

public final class IndicatorEngine {

    public void apply(List<Candle> candles) {
        if (candles.isEmpty()) {
            return;
        }

        applyEma(candles, 12, true);
        applyEma(candles, 26, false);
        applyRsi(candles, 14);
        applyAtr(candles, 14);

        int previousRegime = 0;

        for (int i = 0; i < candles.size(); i++) {
            Candle candle = candles.get(i);
            int regime = candle.emaFast() > candle.emaSlow() ? 1 : -1;
            candle.regime(regime);

            if (i == 0) {
                candle.cross(0);
            } else {
                candle.cross(regime - previousRegime);
            }

            previousRegime = regime;
        }
    }

    private void applyEma(List<Candle> candles, int span, boolean fast) {
        double alpha = 2.0 / (span + 1.0);
        double ema = candles.get(0).close();

        for (int i = 0; i < candles.size(); i++) {
            Candle candle = candles.get(i);

            if (i > 0) {
                ema = alpha * candle.close() + (1.0 - alpha) * ema;
            }

            if (fast) {
                candle.emaFast(ema);
            } else {
                candle.emaSlow(ema);
            }
        }
    }

    private void applyRsi(List<Candle> candles, int period) {
        if (candles.size() <= period) {
            return;
        }

        double avgGain = 0.0;
        double avgLoss = 0.0;

        for (int i = 1; i <= period; i++) {
            double delta = candles.get(i).close() - candles.get(i - 1).close();
            avgGain += Math.max(delta, 0.0);
            avgLoss += Math.max(-delta, 0.0);
        }

        avgGain /= period;
        avgLoss /= period;
        candles.get(period).rsi14(rsi(avgGain, avgLoss));

        for (int i = period + 1; i < candles.size(); i++) {
            double delta = candles.get(i).close() - candles.get(i - 1).close();
            double gain = Math.max(delta, 0.0);
            double loss = Math.max(-delta, 0.0);

            avgGain = ((avgGain * (period - 1)) + gain) / period;
            avgLoss = ((avgLoss * (period - 1)) + loss) / period;

            candles.get(i).rsi14(rsi(avgGain, avgLoss));
        }
    }

    private double rsi(double avgGain, double avgLoss) {
        if (avgLoss == 0.0) {
            return avgGain == 0.0 ? 50.0 : 100.0;
        }

        double rs = avgGain / avgLoss;
        return 100.0 - (100.0 / (1.0 + rs));
    }

    private void applyAtr(List<Candle> candles, int period) {
        if (candles.size() <= period) {
            return;
        }

        double atr = 0.0;

        for (int i = 1; i <= period; i++) {
            atr += trueRange(candles.get(i), candles.get(i - 1).close());
        }

        atr /= period;
        candles.get(period).atr14(atr);

        for (int i = period + 1; i < candles.size(); i++) {
            double tr = trueRange(candles.get(i), candles.get(i - 1).close());
            atr = ((atr * (period - 1)) + tr) / period;
            candles.get(i).atr14(atr);
        }
    }

    private double trueRange(Candle candle, double previousClose) {
        return Math.max(
                candle.high() - candle.low(),
                Math.max(
                        Math.abs(candle.high() - previousClose),
                        Math.abs(candle.low() - previousClose)
                )
        );
    }

    public TechnicalSnapshot snapshot(List<Candle> candles) {
        Candle row = candles.get(candles.size() - 1);

        String trend = row.emaFast() > row.emaSlow()
                ? "ALCISTA"
                : "BAJISTA";

        Double rsi = Double.isFinite(row.rsi14())
                ? row.rsi14()
                : null;

        Double atr = Double.isFinite(row.atr14())
                ? row.atr14()
                : null;

        String momentum;

        if (rsi == null) {
            momentum = "SIN DATOS";
        } else if (rsi >= 70) {
            momentum = "SOBRECOMPRADO";
        } else if (rsi <= 30) {
            momentum = "SOBREVENDIDO";
        } else if (rsi >= 50) {
            momentum = "POSITIVO";
        } else {
            momentum = "NEGATIVO";
        }

        String signal;
        String explanation;

        if ("ALCISTA".equals(trend)
                && rsi != null
                && rsi >= 50
                && rsi < 70) {
            signal = "OBSERVAR POSIBLE COMPRA";
            explanation = "EMA rápida sobre EMA lenta y RSI positivo sin sobrecompra. "
                    + "Es una regla técnica, no IA.";
        } else if ("BAJISTA".equals(trend)) {
            signal = "ESPERAR";
            explanation = "EMA rápida debajo de EMA lenta. "
                    + "La estrategia base evita nuevas posiciones largas.";
        } else {
            signal = "ESPERAR";
            explanation = "Las condiciones técnicas no cumplen simultáneamente "
                    + "las reglas mínimas.";
        }

        return new TechnicalSnapshot(
                row.close(),
                row.emaFast(),
                row.emaSlow(),
                rsi,
                atr,
                trend,
                momentum,
                signal,
                explanation
        );
    }
}
