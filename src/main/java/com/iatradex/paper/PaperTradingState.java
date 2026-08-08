package com.iatradex.paper;

import java.util.ArrayList;
import java.util.List;

public final class PaperTradingState {
    public PaperAccount ars = new PaperAccount("ARS", 1_000_000.0, 1_000_000.0);
    public PaperAccount usd = new PaperAccount("USD", 10_000.0, 10_000.0);
    public List<PaperPosition> positions = new ArrayList<>();
    public List<PaperClosedTrade> history = new ArrayList<>();
    public PaperAutoConfig autoConfig = PaperAutoConfig.disabled();
    public PaperPortfolioAutoConfig portfolioAutoConfig = PaperPortfolioAutoConfig.disabled();
    public List<PaperPortfolioCandidate> portfolioRanking = new ArrayList<>();
    public List<PaperAutoLogEntry> autoLog = new ArrayList<>();
}
