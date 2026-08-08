package com.iatradex.paper;

public record PaperPortfolioCandidate(
        String symbol,
        String market,
        int score,
        String signal,
        String strategy,
        String regime,
        String decision
) {}
