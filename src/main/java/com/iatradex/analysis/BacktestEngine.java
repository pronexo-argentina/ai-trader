package com.iatradex.analysis;

import com.iatradex.model.BacktestResult;
import com.iatradex.model.Candle;
import com.iatradex.model.EquityPoint;
import com.iatradex.model.Metrics;
import com.iatradex.model.StrategyType;
import com.iatradex.model.Trade;

import java.util.ArrayList;
import java.util.List;

public final class BacktestEngine {

    private final StrategySignalEngine signals = new StrategySignalEngine();

    public BacktestResult run(
            List<Candle> candles,
            String marketType,
            String timeframe,
            StrategyType strategy,
            double initialCash,
            double feePct,
            double slippagePct,
            double riskPerTrade,
            double stopLossPct,
            double takeProfitPct
    ) {
        double cash = initialCash;
        double qty = 0.0;
        double entryPrice = Double.NaN;
        long entryTime = 0L;

        List<Trade> trades = new ArrayList<>();
        List<EquityPoint> equity = new ArrayList<>();

        for (int i = 1; i < candles.size(); i++) {
            Candle signalCandle = candles.get(i - 1);
            Candle row = candles.get(i);

            long ts = row.timestamp();
            double open = row.open();
            double high = row.high();
            double low = row.low();
            double close = row.close();

            // La señal se calcula únicamente con información disponible
            // hasta la vela anterior y se ejecuta en la apertura actual.
            if (qty == 0.0
                    && signals.entrySignal(candles, i - 1, strategy)) {
                double entry = open * (1.0 + slippagePct);
                double newQty = positionSize(
                        cash,
                        entry,
                        stopLossPct,
                        riskPerTrade
                );

                double cost = newQty * entry;
                double entryFee = cost * feePct;

                if (newQty > 0.0 && cost + entryFee <= cash) {
                    cash -= cost + entryFee;
                    qty = newQty;
                    entryPrice = entry;
                    entryTime = ts;
                }
            }

            if (qty > 0.0) {
                double stop = entryPrice * (1.0 - stopLossPct);
                double target = entryPrice * (1.0 + takeProfitPct);

                String reason = null;
                double exitPrice = 0.0;

                // Conservador: si stop y target ocurren en la misma vela,
                // asumimos que se ejecutó primero el stop.
                if (low <= stop) {
                    exitPrice = stop * (1.0 - slippagePct);
                    reason = "stop_loss";
                } else if (high >= target) {
                    exitPrice = target * (1.0 - slippagePct);
                    reason = "take_profit";
                } else if (signals.exitSignal(candles, i - 1, strategy)) {
                    exitPrice = open * (1.0 - slippagePct);
                    reason = "signal_exit";
                }

                if (reason != null) {
                    double proceeds = qty * exitPrice;
                    double exitFee = proceeds * feePct;
                    cash += proceeds - exitFee;

                    double entryFee = entryPrice * qty * feePct;
                    double pnl = ((exitPrice - entryPrice) * qty)
                            - entryFee
                            - exitFee;

                    trades.add(new Trade(
                            entryTime,
                            ts,
                            entryPrice,
                            exitPrice,
                            qty,
                            pnl,
                            reason
                    ));

                    qty = 0.0;
                    entryPrice = Double.NaN;
                    entryTime = 0L;
                }
            }

            double equityValue = cash + (qty > 0.0 ? qty * close : 0.0);
            equity.add(new EquityPoint(ts, equityValue));
        }

        if (qty > 0.0) {
            Candle row = candles.get(candles.size() - 1);
            long ts = row.timestamp();
            double exitPrice = row.close() * (1.0 - slippagePct);

            double proceeds = qty * exitPrice;
            double exitFee = proceeds * feePct;
            cash += proceeds - exitFee;

            double entryFee = entryPrice * qty * feePct;
            double pnl = ((exitPrice - entryPrice) * qty)
                    - entryFee
                    - exitFee;

            trades.add(new Trade(
                    entryTime,
                    ts,
                    entryPrice,
                    exitPrice,
                    qty,
                    pnl,
                    "end_of_data"
            ));

            if (!equity.isEmpty()) {
                equity.set(
                        equity.size() - 1,
                        new EquityPoint(ts, cash)
                );
            }
        }

        double runningMax = initialCash;
        double maxDd = 0.0;

        for (EquityPoint point : equity) {
            runningMax = Math.max(runningMax, point.equity());
            maxDd = Math.min(maxDd, point.equity() / runningMax - 1.0);
        }

        List<Trade> winners = trades.stream()
                .filter(t -> t.pnl() > 0)
                .toList();

        List<Trade> losers = trades.stream()
                .filter(t -> t.pnl() < 0)
                .toList();

        double grossProfit = winners.stream()
                .mapToDouble(Trade::pnl)
                .sum();

        double grossLoss = Math.abs(
                losers.stream()
                        .mapToDouble(Trade::pnl)
                        .sum()
        );

        Double profitFactor = grossLoss == 0.0
                ? null
                : grossProfit / grossLoss;

        Double avgWin = winners.isEmpty()
                ? null
                : grossProfit / winners.size();

        Double avgLoss = losers.isEmpty()
                ? null
                : losers.stream().mapToDouble(Trade::pnl).sum() / losers.size();

        double first = candles.get(0).open() * (1.0 + slippagePct);
        double last = candles.get(candles.size() - 1).close() * (1.0 - slippagePct);

        double buyHold = (
                (last * (1.0 - feePct))
                        / (first * (1.0 + feePct))
                        - 1.0
        ) * 100.0;

        Metrics metrics = new Metrics(
                initialCash,
                cash,
                (cash / initialCash - 1.0) * 100.0,
                maxDd * 100.0,
                trades.size(),
                winners.size(),
                trades.isEmpty()
                        ? 0.0
                        : ((double) winners.size() / trades.size()) * 100.0,
                profitFactor,
                avgWin,
                avgLoss,
                sharpeRatio(equity, timeframe, marketType),
                buyHold
        );

        return new BacktestResult(metrics, equity, trades);
    }

    private double positionSize(
            double cash,
            double entryPrice,
            double stopLossPct,
            double riskPerTrade
    ) {
        double riskBudget = cash * riskPerTrade;
        double riskPerUnit = entryPrice * stopLossPct;

        if (riskPerUnit <= 0.0) {
            return 0.0;
        }

        return Math.max(
                0.0,
                Math.min(
                        riskBudget / riskPerUnit,
                        cash / entryPrice
                )
        );
    }

    private Double sharpeRatio(
            List<EquityPoint> equity,
            String timeframe,
            String marketType
    ) {
        if (equity.size() < 3) {
            return null;
        }

        List<Double> returns = new ArrayList<>();

        for (int i = 1; i < equity.size(); i++) {
            double previous = equity.get(i - 1).equity();
            double current = equity.get(i).equity();

            if (previous != 0.0) {
                returns.add(current / previous - 1.0);
            }
        }

        if (returns.size() < 2) {
            return null;
        }

        double mean = returns.stream()
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(0.0);

        double sumSquared = 0.0;

        for (double value : returns) {
            double delta = value - mean;
            sumSquared += delta * delta;
        }

        double std = Math.sqrt(sumSquared / (returns.size() - 1));

        if (std == 0.0 || !Double.isFinite(std)) {
            return null;
        }

        return (mean / std) * Math.sqrt(
                periodsPerYear(timeframe, marketType)
        );
    }

    private double periodsPerYear(String timeframe, String marketType) {
        if ("crypto".equals(marketType)) {
            return switch (timeframe) {
                case "1h" -> 24.0 * 365.0;
                case "4h" -> 6.0 * 365.0;
                case "1d" -> 365.0;
                default -> 365.0;
            };
        }

        return switch (timeframe) {
            case "1h" -> 6.5 * 252.0;
            case "4h" -> 2.0 * 252.0;
            case "1d" -> 252.0;
            default -> 252.0;
        };
    }
}
