package com.iatradex.scanner;

import com.iatradex.model.AnalysisResult;
import com.iatradex.model.StrategyType;

public record ScannerResult(
        WatchlistItem item,
        AnalysisResult analysis,
        String regime,
        String volatility,
        Double rsi,
        String trend,
        StrategyType strategy,
        String signal,
        int score,
        String scoreExplanation,
        String error
) {
    public boolean successful() {
        return error == null || error.isBlank();
    }
}
