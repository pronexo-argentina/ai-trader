package com.iatradex.ui;

import com.iatradex.paper.PaperPosition;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class PaperPositionRow {

    private final PaperPosition position;
    private final StringProperty symbol;
    private final StringProperty quantity;
    private final StringProperty entry;
    private final StringProperty current;
    private final StringProperty pnl;
    private final StringProperty stop;
    private final StringProperty take;
    private final StringProperty context;

    public PaperPositionRow(PaperPosition position) {
        this.position = position;
        this.symbol = new SimpleStringProperty(position.symbol());
        this.quantity = new SimpleStringProperty(
                String.format("%.6f", position.quantity())
                        .replaceAll("0+$", "")
                        .replaceAll("\\.$", "")
        );
        this.entry = new SimpleStringProperty(money(position.entryPrice(), position.currency()));
        this.current = new SimpleStringProperty(
                money(
                        position.lastPrice() == null
                                ? position.entryPrice()
                                : position.lastPrice(),
                        position.currency()
                )
        );
        this.pnl = new SimpleStringProperty(
                String.format(
                        "%s %+.2f · %+.2f%%",
                        prefix(position.currency()),
                        position.unrealizedPnl(),
                        position.unrealizedPnlPct()
                )
        );
        this.stop = new SimpleStringProperty(
                position.stopLoss() == null
                        ? "—"
                        : money(position.stopLoss(), position.currency())
        );
        this.take = new SimpleStringProperty(
                position.takeProfit() == null
                        ? "—"
                        : money(position.takeProfit(), position.currency())
        );
        this.context = new SimpleStringProperty(
                position.strategyContext()
                        + " · "
                        + position.regimeContext()
        );
    }

    public PaperPosition position() { return position; }
    public StringProperty symbolProperty() { return symbol; }
    public StringProperty quantityProperty() { return quantity; }
    public StringProperty entryProperty() { return entry; }
    public StringProperty currentProperty() { return current; }
    public StringProperty pnlProperty() { return pnl; }
    public StringProperty stopProperty() { return stop; }
    public StringProperty takeProperty() { return take; }
    public StringProperty contextProperty() { return context; }

    private static String money(double value, String currency) {
        return String.format("%s %,.2f", prefix(currency), value);
    }

    private static String prefix(String currency) {
        return "ARS".equalsIgnoreCase(currency) ? "AR$" : "US$";
    }
}
