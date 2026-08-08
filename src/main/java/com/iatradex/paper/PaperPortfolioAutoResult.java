package com.iatradex.paper;

import java.util.List;

public record PaperPortfolioAutoResult(
        int scanned,
        int entries,
        int exits,
        int skipped,
        int errors,
        List<String> messages,
        List<PaperPortfolioCandidate> ranking,
        String timestamp
) {}
