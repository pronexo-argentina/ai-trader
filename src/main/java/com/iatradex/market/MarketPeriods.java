package com.iatradex.market;

import java.time.Duration;

public final class MarketPeriods {
    private MarketPeriods() {}

    public static int days(String period) {
        return switch (period) {
            case "1m" -> 30;
            case "3m" -> 90;
            case "6m" -> 180;
            case "1y" -> 365;
            default -> throw new IllegalArgumentException("Período no soportado: " + period);
        };
    }

    public static long timeframeMillis(String timeframe) {
        return switch (timeframe) {
            case "1h" -> Duration.ofHours(1).toMillis();
            case "4h" -> Duration.ofHours(4).toMillis();
            case "1d" -> Duration.ofDays(1).toMillis();
            default -> throw new IllegalArgumentException("Timeframe no soportado: " + timeframe);
        };
    }
}
