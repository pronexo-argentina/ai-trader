package com.iatradex.analysis;

import com.iatradex.model.Candle;
import com.iatradex.model.MarketRegime;
import com.iatradex.model.StrategyType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class MarketRegimeEngine {

    public MarketRegime detect(List<Candle> candles) {
        if (candles == null || candles.size() < 30) {
            return new MarketRegime(
                    "SIN DATOS",
                    "SIN DATOS",
                    "SIN DATOS",
                    0.0,
                    0.0,
                    null,
                    null,
                    "Se necesitan al menos 30 velas para clasificar el régimen.",
                    List.of()
            );
        }

        Candle last = candles.get(candles.size() - 1);
        int lookback = Math.min(20, candles.size() - 1);
        Candle old = candles.get(candles.size() - 1 - lookback);

        double emaSpreadPct = last.emaSlow() == 0.0
                ? 0.0
                : (last.emaFast() / last.emaSlow() - 1.0) * 100.0;

        double return20Pct = old.close() == 0.0
                ? 0.0
                : (last.close() / old.close() - 1.0) * 100.0;

        String trend = classifyTrend(emaSpreadPct, return20Pct);
        String strength = classifyStrength(
                trend,
                Math.abs(emaSpreadPct),
                Math.abs(return20Pct)
        );

        Double atrPct = Double.isFinite(last.atr14()) && last.close() > 0.0
                ? (last.atr14() / last.close()) * 100.0
                : null;

        Double volatilityRatio = volatilityRatio(candles, atrPct);
        String volatility = classifyVolatility(volatilityRatio);

        List<StrategyType> compatible = compatibleStrategies(
                trend,
                volatility
        );

        String explanation = explanation(
                trend,
                volatility,
                strength,
                emaSpreadPct,
                return20Pct,
                atrPct,
                volatilityRatio
        );

        return new MarketRegime(
                trend,
                volatility,
                strength,
                emaSpreadPct,
                return20Pct,
                atrPct,
                volatilityRatio,
                explanation,
                List.copyOf(compatible)
        );
    }

    private String classifyTrend(
            double emaSpreadPct,
            double return20Pct
    ) {
        if (emaSpreadPct >= 0.35 && return20Pct >= 1.0) {
            return "ALCISTA";
        }

        if (emaSpreadPct <= -0.35 && return20Pct <= -1.0) {
            return "BAJISTA";
        }

        return "LATERAL";
    }

    private String classifyStrength(
            String trend,
            double absEmaSpreadPct,
            double absReturn20Pct
    ) {
        if ("LATERAL".equals(trend)) {
            return absReturn20Pct < 2.0
                    ? "BAJA"
                    : "MEDIA";
        }

        double score = absEmaSpreadPct + (absReturn20Pct / 4.0);

        if (score >= 3.0) {
            return "ALTA";
        }

        if (score >= 1.25) {
            return "MEDIA";
        }

        return "BAJA";
    }

    private Double volatilityRatio(
            List<Candle> candles,
            Double currentAtrPct
    ) {
        if (currentAtrPct == null) {
            return null;
        }

        List<Double> history = new ArrayList<>();

        for (Candle candle : candles) {
            if (!Double.isFinite(candle.atr14())
                    || candle.close() <= 0.0) {
                continue;
            }

            double pct = (candle.atr14() / candle.close()) * 100.0;

            if (Double.isFinite(pct) && pct > 0.0) {
                history.add(pct);
            }
        }

        if (history.size() < 10) {
            return null;
        }

        Collections.sort(history);

        double median;
        int middle = history.size() / 2;

        if (history.size() % 2 == 0) {
            median = (
                    history.get(middle - 1)
                            + history.get(middle)
            ) / 2.0;
        } else {
            median = history.get(middle);
        }

        if (median <= 0.0) {
            return null;
        }

        return currentAtrPct / median;
    }

    private String classifyVolatility(Double ratio) {
        if (ratio == null || !Double.isFinite(ratio)) {
            return "SIN DATOS";
        }

        if (ratio >= 1.35) {
            return "ALTA";
        }

        if (ratio <= 0.75) {
            return "BAJA";
        }

        return "MEDIA";
    }

    private List<StrategyType> compatibleStrategies(
            String trend,
            String volatility
    ) {
        List<StrategyType> result = new ArrayList<>();

        switch (trend) {
            case "ALCISTA" -> {
                result.add(StrategyType.MOMENTUM);
                result.add(StrategyType.BREAKOUT);
                result.add(StrategyType.EMA_CROSS);

                if ("BAJA".equals(volatility)) {
                    result.add(StrategyType.MEAN_REVERSION);
                }
            }

            case "BAJISTA" -> {
                // Todas las estrategias actuales son long-only.
                // Mean Reversion es la única que puede tener sentido para
                // estudiar rebotes, pero no se la considera automáticamente
                // una recomendación de compra.
                result.add(StrategyType.MEAN_REVERSION);
            }

            default -> {
                result.add(StrategyType.MEAN_REVERSION);

                if ("ALTA".equals(volatility)) {
                    result.add(StrategyType.BREAKOUT);
                }

                result.add(StrategyType.EMA_CROSS);
            }
        }

        return result;
    }

    private String explanation(
            String trend,
            String volatility,
            String strength,
            double emaSpreadPct,
            double return20Pct,
            Double atrPct,
            Double volatilityRatio
    ) {
        String atrText = atrPct == null
                ? "sin ATR disponible"
                : String.format("ATR %.2f%% del precio", atrPct);

        String ratioText = volatilityRatio == null
                ? "sin referencia suficiente"
                : String.format(
                        "%.2fx su mediana histórica del período",
                        volatilityRatio
                );

        return String.format(
                "Régimen %s con fuerza %s. "
                        + "La EMA rápida está %+.2f%% respecto de la EMA lenta "
                        + "y el precio cambió %+.2f%% en las últimas 20 velas. "
                        + "Volatilidad %s: %s, %s. "
                        + "Clasificación por reglas, no por Machine Learning.",
                trend.toLowerCase(),
                strength.toLowerCase(),
                emaSpreadPct,
                return20Pct,
                volatility.toLowerCase(),
                atrText,
                ratioText
        );
    }
}
