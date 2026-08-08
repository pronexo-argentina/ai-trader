package com.iatradex.model;

public record MarketQuote(
        String currency,
        Double lastPrice,
        Double changePct,
        Double bidPrice,
        Double bidSize,
        Double askPrice,
        Double askSize,
        Double open,
        Double high,
        Double low,
        Double volume,
        String date,
        String dataMode
) {}
