package com.iatradex.paper;

import com.iatradex.analysis.AnalysisService;
import com.iatradex.analysis.StrategySignalEngine;
import com.iatradex.market.MarketService;
import com.iatradex.model.AnalysisResult;
import com.iatradex.model.Candle;
import com.iatradex.model.StrategyType;

import java.time.Instant;
import java.util.List;

public final class PaperAutoTradingEngine {

    private final AnalysisService analysisService;
    private final MarketService marketService;
    private final PaperTradingService paperService;
    private final StrategySignalEngine signals =
            new StrategySignalEngine();

    public PaperAutoTradingEngine(
            AnalysisService analysisService,
            MarketService marketService,
            PaperTradingService paperService
    ) {
        this.analysisService = analysisService;
        this.marketService = marketService;
        this.paperService = paperService;
    }

    public PaperAutoResult runOnce(
            PaperAutoConfig config
    ) throws Exception {

        if (config == null || !config.enabled()) {
            return result("PAUSED", "Automatización pausada.");
        }

        StrategyType strategy = StrategyType.valueOf(
                config.strategy()
        );

        // Primero actualiza precios y ejecuta Stop/Take si corresponde.
        paperService.refreshOpenPositions(marketService);

        AnalysisResult analysis = analysisService.analyze(
                config.marketType(),
                config.source(),
                config.symbol(),
                config.timeframe(),
                config.period(),
                config.currency()
        );

        List<Candle> candles = analysis.candles();

        if (candles.size() < 3) {
            return result(
                    "WAIT",
                    config.symbol()
                            + " · historial insuficiente para evaluar señal."
            );
        }

        // Se usa la última vela cerrada, no la vela potencialmente en formación.
        int signalIndex = candles.size() - 2;

        PaperPosition open = paperService.findOpenPosition(
                config.symbol(),
                config.currency(),
                strategy.displayName()
        );

        double livePrice = marketService.livePrice(
                config.marketType(),
                config.source(),
                config.symbol()
        );

        if (open != null) {
            if (signals.exitSignal(
                    candles,
                    signalIndex,
                    strategy
            )) {
                paperService.close(
                        open.id(),
                        livePrice,
                        "Salida automática por señal · "
                                + strategy.displayName()
                );

                String message = config.symbol()
                        + " · "
                        + strategy.displayName()
                        + " · salida por señal @ "
                        + formatPrice(livePrice);

                paperService.addAutoLog("EXIT", message);
                return result("EXIT", message);
            }

            String message = config.symbol()
                    + " · "
                    + strategy.displayName()
                    + " · posición abierta · sin señal de salida.";

            paperService.addAutoLog("HOLD", message);
            return result("HOLD", message);
        }

        if (!signals.entrySignal(
                candles,
                signalIndex,
                strategy
        )) {
            String message = config.symbol()
                    + " · "
                    + strategy.displayName()
                    + " · sin señal de entrada.";

            paperService.addAutoLog("WAIT", message);
            return result("WAIT", message);
        }

        double stopPct = config.stopLossPct() / 100.0;
        double takePct = config.takeProfitPct() / 100.0;
        double riskPct = config.riskPct() / 100.0;

        if (stopPct <= 0.0) {
            throw new IllegalArgumentException(
                    "El Stop Loss automático debe ser mayor que 0%."
            );
        }

        PaperAccount account = paperService.account(
                config.currency()
        );

        double allowedCapital = Math.min(
                Math.max(0.0, config.maxCapital()),
                account.cash()
        );

        double riskBudget = Math.min(
                account.cash() * riskPct,
                allowedCapital * riskPct
        );

        double riskPerUnit = livePrice * stopPct;

        double quantityByRisk = riskPerUnit <= 0.0
                ? 0.0
                : riskBudget / riskPerUnit;

        double quantityByCapital = livePrice <= 0.0
                ? 0.0
                : allowedCapital / livePrice;

        double quantity = Math.min(
                quantityByRisk,
                quantityByCapital
        );

        // Para BYMA usamos unidades enteras. En crypto/global se permiten
        // fracciones porque esto es una simulación neutral respecto del broker.
        if ("argentina".equals(config.marketType())) {
            quantity = Math.floor(quantity);
        }

        if (!Double.isFinite(quantity) || quantity <= 0.0) {
            String message = config.symbol()
                    + " · señal detectada, pero el capital/riesgo "
                    + "no permite abrir posición.";

            paperService.addAutoLog("SKIP", message);
            return result("SKIP", message);
        }

        double stopLoss = livePrice * (1.0 - stopPct);
        double takeProfit = livePrice * (1.0 + takePct);

        String regime = analysis.regime() == null
                ? "Sin régimen"
                : analysis.regime().trend()
                        + " / "
                        + analysis.regime().volatility();

        paperService.buy(
                config.symbol(),
                humanMarket(config.marketType()),
                config.marketType(),
                config.source(),
                config.currency(),
                quantity,
                livePrice,
                stopLoss,
                takeProfit,
                strategy.displayName(),
                regime
        );

        String message = config.symbol()
                + " · "
                + strategy.displayName()
                + " · compra automática "
                + formatQuantity(quantity)
                + " @ "
                + formatPrice(livePrice)
                + " · Stop "
                + formatPrice(stopLoss)
                + " · Take "
                + formatPrice(takeProfit);

        paperService.addAutoLog("BUY", message);
        return result("BUY", message);
    }

    private PaperAutoResult result(
            String action,
            String message
    ) {
        return new PaperAutoResult(
                action,
                message,
                Instant.now().toString()
        );
    }

    private String humanMarket(String marketType) {
        return switch (marketType) {
            case "crypto" -> "Criptomonedas";
            case "argentina" -> "Argentina";
            default -> "Internacional";
        };
    }

    private String formatPrice(double value) {
        return String.format("%.4f", value);
    }

    private String formatQuantity(double value) {
        return String.format("%.6f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }
}
