package com.iatradex.model;

public record TechnicalSnapshot(
        double lastPrice,
        double emaFast,
        double emaSlow,
        Double rsi14,
        Double atr14,
        String trend,
        String momentum,
        String signal,
        String explanation
) {}
