package com.iatradex.ui;

import com.iatradex.model.StrategyPerformance;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class StrategyRow {

    private final StrategyPerformance performance;
    private final StringProperty strategy;
    private final StringProperty returnPct;
    private final StringProperty profitFactor;
    private final StringProperty drawdown;
    private final StringProperty sharpe;
    private final StringProperty trades;
    private final StringProperty historical;

    public StrategyRow(
            StrategyPerformance performance,
            boolean bestHistorical
    ) {
        this.performance = performance;

        this.strategy = new SimpleStringProperty(
                performance.strategy().displayName()
        );

        this.returnPct = new SimpleStringProperty(
                String.format("%+.2f%%", performance.metrics().returnPct())
        );

        this.profitFactor = new SimpleStringProperty(
                performance.metrics().profitFactor() == null
                        ? "—"
                        : String.format("%.2f", performance.metrics().profitFactor())
        );

        this.drawdown = new SimpleStringProperty(
                String.format("%+.2f%%", performance.metrics().maxDrawdownPct())
        );

        this.sharpe = new SimpleStringProperty(
                performance.metrics().sharpeRatio() == null
                        ? "—"
                        : String.format("%.2f", performance.metrics().sharpeRatio())
        );

        this.trades = new SimpleStringProperty(
                String.valueOf(performance.metrics().trades())
        );

        this.historical = new SimpleStringProperty(
                bestHistorical
                        ? "MEJOR HISTÓRICO"
                        : ""
        );
    }

    public StrategyPerformance performance() {
        return performance;
    }

    public StringProperty strategyProperty() { return strategy; }
    public StringProperty returnPctProperty() { return returnPct; }
    public StringProperty profitFactorProperty() { return profitFactor; }
    public StringProperty drawdownProperty() { return drawdown; }
    public StringProperty sharpeProperty() { return sharpe; }
    public StringProperty tradesProperty() { return trades; }
    public StringProperty historicalProperty() { return historical; }
}
