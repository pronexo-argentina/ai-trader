package com.iatradex.ui;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class TradeRow {

    private final StringProperty time;
    private final DoubleProperty entry;
    private final DoubleProperty exit;
    private final DoubleProperty pnl;
    private final StringProperty reason;

    public TradeRow(
            String time,
            double entry,
            double exit,
            double pnl,
            String reason
    ) {
        this.time = new SimpleStringProperty(time);
        this.entry = new SimpleDoubleProperty(entry);
        this.exit = new SimpleDoubleProperty(exit);
        this.pnl = new SimpleDoubleProperty(pnl);
        this.reason = new SimpleStringProperty(reason);
    }

    public StringProperty timeProperty() { return time; }
    public DoubleProperty entryProperty() { return entry; }
    public DoubleProperty exitProperty() { return exit; }
    public DoubleProperty pnlProperty() { return pnl; }
    public StringProperty reasonProperty() { return reason; }
}
