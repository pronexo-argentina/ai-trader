package com.iatradex.ui;

import com.iatradex.scanner.ScannerResult;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class ScannerRow {

    private final ScannerResult result;

    private final StringProperty symbol;
    private final StringProperty market;
    private final StringProperty regime;
    private final StringProperty volatility;
    private final StringProperty rsi;
    private final StringProperty strategy;
    private final StringProperty signal;
    private final StringProperty score;
    private final StringProperty detail;

    public ScannerRow(ScannerResult result) {
        this.result = result;

        symbol = new SimpleStringProperty(result.item().symbol());
        market = new SimpleStringProperty(result.item().marketLabel());
        regime = new SimpleStringProperty(result.regime());
        volatility = new SimpleStringProperty(result.volatility());
        rsi = new SimpleStringProperty(
                result.rsi() == null
                        ? "—"
                        : String.format("%.2f", result.rsi())
        );
        strategy = new SimpleStringProperty(
                result.successful()
                        ? result.strategy().displayName()
                        : "—"
        );
        signal = new SimpleStringProperty(result.signal());
        score = new SimpleStringProperty(
                result.successful()
                        ? String.valueOf(result.score())
                        : "—"
        );
        detail = new SimpleStringProperty(
                result.successful()
                        ? result.scoreExplanation()
                        : result.error()
        );
    }

    public ScannerResult result() { return result; }

    public StringProperty symbolProperty() { return symbol; }
    public StringProperty marketProperty() { return market; }
    public StringProperty regimeProperty() { return regime; }
    public StringProperty volatilityProperty() { return volatility; }
    public StringProperty rsiProperty() { return rsi; }
    public StringProperty strategyProperty() { return strategy; }
    public StringProperty signalProperty() { return signal; }
    public StringProperty scoreProperty() { return score; }
    public StringProperty detailProperty() { return detail; }
}
