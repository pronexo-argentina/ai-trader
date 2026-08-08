package com.iatradex.model;

public enum StrategyType {
    EMA_CROSS("EMA Cross"),
    MOMENTUM("Momentum"),
    MEAN_REVERSION("Mean Reversion"),
    BREAKOUT("Breakout");

    private final String displayName;

    StrategyType(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
