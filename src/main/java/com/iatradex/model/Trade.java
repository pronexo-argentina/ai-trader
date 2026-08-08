package com.iatradex.model;

public record Trade(
        long entryTime,
        long exitTime,
        double entryPrice,
        double exitPrice,
        double quantity,
        double pnl,
        String reason
) {}
