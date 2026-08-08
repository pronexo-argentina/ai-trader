package com.iatradex.paper;

public record PaperPosition(
        String id,
        String symbol,
        String market,
        String marketType,
        String source,
        String currency,
        double quantity,
        double entryPrice,
        Double lastPrice,
        Double stopLoss,
        Double takeProfit,
        String strategyContext,
        String regimeContext,
        String openedAt
) {
    public PaperPosition withLastPrice(Double price) {
        return new PaperPosition(
                id, symbol, market, marketType, source, currency,
                quantity, entryPrice, price, stopLoss, takeProfit,
                strategyContext, regimeContext, openedAt
        );
    }

    public String resolvedMarketType() {
        if (marketType != null && !marketType.isBlank()) {
            return marketType;
        }

        if ("ARS".equalsIgnoreCase(currency)) {
            return "argentina";
        }

        if (market != null && market.toLowerCase().contains("cripto")) {
            return "crypto";
        }

        return "stocks";
    }

    public String resolvedSource() {
        if (source != null && !source.isBlank()) {
            return source;
        }

        return switch (resolvedMarketType()) {
            case "argentina" -> "open-bymadata";
            case "crypto" -> "binance";
            default -> "yahoo";
        };
    }

    public double marketValue() {
        double px = lastPrice == null ? entryPrice : lastPrice;
        return quantity * px;
    }

    public double unrealizedPnl() {
        double px = lastPrice == null ? entryPrice : lastPrice;
        return quantity * (px - entryPrice);
    }

    public double unrealizedPnlPct() {
        if (entryPrice == 0.0) return 0.0;
        double px = lastPrice == null ? entryPrice : lastPrice;
        return (px / entryPrice - 1.0) * 100.0;
    }
}
