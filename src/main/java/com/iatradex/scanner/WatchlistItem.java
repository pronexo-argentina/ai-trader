package com.iatradex.scanner;

public record WatchlistItem(
        String marketType,
        String source,
        String symbol,
        String currency,
        String timeframe,
        String period
) {
    public String key() {
        return marketType + "|" + source + "|" + symbol;
    }

    public String marketLabel() {
        return switch (marketType) {
            case "argentina" -> "Argentina";
            case "crypto" -> "Crypto";
            default -> "Internacional";
        };
    }
}
