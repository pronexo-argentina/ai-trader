package com.iatradex.ui;

import com.iatradex.paper.PaperClosedTrade;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public final class PaperHistoryRow {

    private final StringProperty symbol;
    private final StringProperty quantity;
    private final StringProperty entry;
    private final StringProperty exit;
    private final StringProperty pnl;
    private final StringProperty context;
    private final StringProperty reason;

    public PaperHistoryRow(PaperClosedTrade trade) {
        symbol = new SimpleStringProperty(trade.symbol());
        quantity = new SimpleStringProperty(
                String.format("%.6f", trade.quantity())
                        .replaceAll("0+$", "")
                        .replaceAll("\\.$", "")
        );
        entry = new SimpleStringProperty(money(trade.entryPrice(), trade.currency()));
        exit = new SimpleStringProperty(money(trade.exitPrice(), trade.currency()));
        pnl = new SimpleStringProperty(
                String.format(
                        "%s %+.2f · %+.2f%%",
                        prefix(trade.currency()),
                        trade.pnl(),
                        trade.pnlPct()
                )
        );
        context = new SimpleStringProperty(
                trade.strategyContext() + " · " + trade.regimeContext()
        );
        reason = new SimpleStringProperty(trade.reason());
    }

    public StringProperty symbolProperty() { return symbol; }
    public StringProperty quantityProperty() { return quantity; }
    public StringProperty entryProperty() { return entry; }
    public StringProperty exitProperty() { return exit; }
    public StringProperty pnlProperty() { return pnl; }
    public StringProperty contextProperty() { return context; }
    public StringProperty reasonProperty() { return reason; }

    private static String money(double value, String currency) {
        return String.format("%s %,.2f", prefix(currency), value);
    }

    private static String prefix(String currency) {
        return "ARS".equalsIgnoreCase(currency) ? "AR$" : "US$";
    }
}
