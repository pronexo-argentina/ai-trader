package com.iatradex.paper;

import com.iatradex.analysis.StrategySignalEngine;
import com.iatradex.market.MarketService;
import com.iatradex.model.AnalysisResult;
import com.iatradex.model.Candle;
import com.iatradex.model.StrategyType;
import com.iatradex.scanner.ScannerEngine;
import com.iatradex.scanner.ScannerResult;
import com.iatradex.scanner.WatchlistItem;
import com.iatradex.scanner.WatchlistService;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class PaperPortfolioAutoEngine {

    private static final String CONTEXT_PREFIX = "AUTO Scanner · ";

    private final ScannerEngine scannerEngine;
    private final WatchlistService watchlistService;
    private final MarketService marketService;
    private final PaperTradingService paperService;
    private final StrategySignalEngine signals = new StrategySignalEngine();

    public PaperPortfolioAutoEngine(
            ScannerEngine scannerEngine,
            WatchlistService watchlistService,
            MarketService marketService,
            PaperTradingService paperService
    ) {
        this.scannerEngine = scannerEngine;
        this.watchlistService = watchlistService;
        this.marketService = marketService;
        this.paperService = paperService;
    }

    public PaperPortfolioAutoResult runOnce(
            PaperPortfolioAutoConfig config
    ) throws Exception {

        if (config == null || !config.enabled()) {
            return result(
                    0, 0, 0, 0, 0,
                    List.of("Cartera AUTO pausada."),
                    List.of()
            );
        }

        validate(config);

        PaperRefreshResult refresh =
                paperService.refreshOpenPositions(marketService);

        List<String> messages = new ArrayList<>(
                refresh.messages()
        );

        int exits = manageSignalExits(messages);

        List<WatchlistItem> items = watchlistService.items();
        List<ScannerResult> scanned = new ArrayList<>();
        int errors = 0;

        for (WatchlistItem item : items) {
            ScannerResult result = scannerEngine.scan(item);
            scanned.add(result);

            if (!result.successful()) {
                errors++;
                messages.add(
                        item.symbol() + " · ERROR · " + result.error()
                );
            }
        }

        scanned.sort(
                Comparator.comparingInt(
                        ScannerResult::score
                ).reversed()
        );

        int entries = 0;
        int skipped = 0;
        List<PaperPortfolioCandidate> ranking =
                new ArrayList<>();

        for (ScannerResult candidate : scanned) {
            if (!candidate.successful()) {
                ranking.add(toCandidate(
                        candidate,
                        "ERROR"
                ));
                continue;
            }

            if (!"ENTRADA".equals(candidate.signal())) {
                ranking.add(toCandidate(
                        candidate,
                        "SIN ENTRADA"
                ));
                continue;
            }

            if (candidate.score() < config.minScore()) {
                ranking.add(toCandidate(
                        candidate,
                        "SCORE BAJO"
                ));
                continue;
            }

            if (paperService.openPositionCount() >= config.maxPositions()) {
                skipped++;
                ranking.add(toCandidate(
                        candidate,
                        "LÍMITE TOTAL"
                ));
                messages.add(
                        candidate.item().symbol()
                                + " · omitido: límite de posiciones."
                );
                continue;
            }

            WatchlistItem item = candidate.item();

            int openInMarket = paperService.openPositionCountByMarket(
                    item.marketType()
            );

            if (openInMarket >= config.maxForMarket(
                    item.marketType()
            )) {
                skipped++;
                ranking.add(toCandidate(
                        candidate,
                        "LÍMITE " + item.marketLabel()
                ));
                messages.add(
                        item.symbol()
                                + " · omitido: límite para "
                                + item.marketLabel()
                                + "."
                );
                continue;
            }

            if (paperService.hasOpenPosition(
                    item.symbol(),
                    item.currency()
            )) {
                skipped++;
                ranking.add(toCandidate(
                        candidate,
                        "DUPLICADA"
                ));
                continue;
            }

            double equity = paperService.equity(item.currency());

            if (equity <= 0.0) {
                skipped++;
                ranking.add(toCandidate(
                        candidate,
                        "SIN EQUITY"
                ));
                continue;
            }

            double usedRisk = paperService.openRiskAmount(
                    item.currency()
            );

            double globalRiskBudget =
                    equity * config.maxGlobalRiskPct() / 100.0;

            double remainingRisk =
                    Math.max(0.0, globalRiskBudget - usedRisk);

            double tradeRiskBudget = Math.min(
                    equity * config.riskPerTradePct() / 100.0,
                    remainingRisk
            );

            if (tradeRiskBudget <= 0.0) {
                skipped++;
                ranking.add(toCandidate(
                        candidate,
                        "RIESGO GLOBAL"
                ));
                messages.add(
                        item.symbol()
                                + " · omitido: riesgo global agotado."
                );
                continue;
            }

            double livePrice = marketService.livePrice(
                    item.marketType(),
                    item.source(),
                    item.symbol()
            );

            double stopPct = config.stopLossPct() / 100.0;
            double takePct = config.takeProfitPct() / 100.0;

            double riskPerUnit = livePrice * stopPct;
            double maxCapital =
                    equity * config.maxCapitalPerTradePct() / 100.0;

            double quantityByRisk =
                    tradeRiskBudget / riskPerUnit;
            double quantityByCapital =
                    maxCapital / livePrice;

            double quantity = Math.min(
                    quantityByRisk,
                    quantityByCapital
            );

            if ("argentina".equals(item.marketType())) {
                quantity = Math.floor(quantity);
            }

            if (!Double.isFinite(quantity) || quantity <= 0.0) {
                skipped++;
                ranking.add(toCandidate(
                        candidate,
                        "CANTIDAD 0"
                ));
                messages.add(
                        item.symbol()
                                + " · omitido: cantidad resultó cero."
                );
                continue;
            }

            double cost = quantity * livePrice;
            PaperAccount account =
                    paperService.account(item.currency());

            if (cost > account.cash()) {
                quantity = account.cash() / livePrice;

                if ("argentina".equals(item.marketType())) {
                    quantity = Math.floor(quantity);
                }
            }

            if (!Double.isFinite(quantity) || quantity <= 0.0) {
                skipped++;
                ranking.add(toCandidate(
                        candidate,
                        "SIN EFECTIVO"
                ));
                continue;
            }

            double stopLoss =
                    livePrice * (1.0 - stopPct);
            double takeProfit =
                    livePrice * (1.0 + takePct);

            String strategyName =
                    candidate.strategy().displayName();

            String regime = candidate.analysis().regime() == null
                    ? "Sin régimen"
                    : candidate.analysis().regime().trend()
                    + " / "
                    + candidate.analysis().regime().volatility();

            paperService.buy(
                    item.symbol(),
                    item.marketLabel(),
                    item.marketType(),
                    item.source(),
                    item.currency(),
                    quantity,
                    livePrice,
                    stopLoss,
                    takeProfit,
                    CONTEXT_PREFIX + strategyName,
                    regime
            );

            entries++;

            String message =
                    item.symbol()
                            + " · BUY · score "
                            + candidate.score()
                            + " · "
                            + strategyName
                            + " · qty "
                            + format(quantity)
                            + " @ "
                            + format(livePrice);

            paperService.addAutoLog(
                    "PORTFOLIO_BUY",
                    message
            );
            messages.add(message);
            ranking.add(toCandidate(
                    candidate,
                    "ABIERTA"
            ));
        }

        paperService.setPortfolioRanking(ranking);

        String summary =
                "Cartera AUTO · "
                        + scanned.size()
                        + " escaneados · "
                        + entries
                        + " entradas · "
                        + exits
                        + " salidas";

        paperService.addAutoLog(
                "PORTFOLIO",
                summary
        );

        return result(
                scanned.size(),
                entries,
                exits,
                skipped,
                errors,
                messages,
                ranking
        );
    }

    private int manageSignalExits(
            List<String> messages
    ) {
        int exits = 0;

        List<PaperPosition> positions =
                paperService.positionsSnapshot().stream()
                        .filter(p ->
                                p.strategyContext() != null
                                        && p.strategyContext()
                                        .startsWith(CONTEXT_PREFIX)
                        )
                        .toList();

        for (PaperPosition position : positions) {
            try {
                WatchlistItem item =
                        findWatchlistItem(position);

                if (item == null) {
                    continue;
                }

                ScannerResult scan =
                        scannerEngine.scan(item);

                if (!scan.successful()) {
                    continue;
                }

                StrategyType strategy =
                        strategyFromContext(
                                position.strategyContext()
                        );

                AnalysisResult analysis =
                        scan.analysis();

                List<Candle> candles =
                        analysis.candles();

                if (candles.size() < 3) {
                    continue;
                }

                int signalIndex =
                        candles.size() - 2;

                if (!signals.exitSignal(
                        candles,
                        signalIndex,
                        strategy
                )) {
                    continue;
                }

                double livePrice =
                        marketService.livePrice(
                                position.resolvedMarketType(),
                                position.resolvedSource(),
                                position.symbol()
                        );

                paperService.close(
                        position.id(),
                        livePrice,
                        "Cartera AUTO · salida por señal · "
                                + strategy.displayName()
                );

                exits++;

                String message =
                        position.symbol()
                                + " · EXIT · "
                                + strategy.displayName()
                                + " @ "
                                + format(livePrice);

                paperService.addAutoLog(
                        "PORTFOLIO_EXIT",
                        message
                );
                messages.add(message);
            } catch (Exception ex) {
                messages.add(
                        position.symbol()
                                + " · error al evaluar salida: "
                                + safeMessage(ex)
                );
            }
        }

        return exits;
    }

    private WatchlistItem findWatchlistItem(
            PaperPosition position
    ) {
        return watchlistService.items().stream()
                .filter(item ->
                        item.symbol().equalsIgnoreCase(
                                position.symbol()
                        )
                                && item.currency().equalsIgnoreCase(
                                position.currency()
                        )
                )
                .findFirst()
                .orElse(null);
    }

    private StrategyType strategyFromContext(
            String context
    ) {
        String name = context.substring(
                CONTEXT_PREFIX.length()
        );

        for (StrategyType type : StrategyType.values()) {
            if (type.displayName().equalsIgnoreCase(name)) {
                return type;
            }
        }

        return StrategyType.MOMENTUM;
    }

    private void validate(
            PaperPortfolioAutoConfig config
    ) {
        if (config.minScore() < 0
                || config.minScore() > 100) {
            throw new IllegalArgumentException(
                    "Score mínimo debe estar entre 0 y 100."
            );
        }

        if (config.maxPositions() < 1) {
            throw new IllegalArgumentException(
                    "Máximo de posiciones debe ser al menos 1."
            );
        }

        if (config.maxArgentinaPositions() < 1
                || config.maxInternationalPositions() < 1
                || config.maxCryptoPositions() < 1) {
            throw new IllegalArgumentException(
                    "Los límites por mercado deben ser al menos 1."
            );
        }

        if (config.maxGlobalRiskPct() <= 0.0
                || config.maxGlobalRiskPct() > 100.0) {
            throw new IllegalArgumentException(
                    "Riesgo global inválido."
            );
        }

        if (config.riskPerTradePct() <= 0.0
                || config.riskPerTradePct()
                > config.maxGlobalRiskPct()) {
            throw new IllegalArgumentException(
                    "Riesgo por operación inválido."
            );
        }

        if (config.maxCapitalPerTradePct() <= 0.0
                || config.maxCapitalPerTradePct() > 100.0) {
            throw new IllegalArgumentException(
                    "Capital por operación inválido."
            );
        }

        if (config.stopLossPct() <= 0.0
                || config.stopLossPct() >= 100.0) {
            throw new IllegalArgumentException(
                    "Stop Loss inválido."
            );
        }

        if (config.takeProfitPct() <= 0.0) {
            throw new IllegalArgumentException(
                    "Take Profit inválido."
            );
        }
    }

    private PaperPortfolioAutoResult result(
            int scanned,
            int entries,
            int exits,
            int skipped,
            int errors,
            List<String> messages,
            List<PaperPortfolioCandidate> ranking
    ) {
        return new PaperPortfolioAutoResult(
                scanned,
                entries,
                exits,
                skipped,
                errors,
                List.copyOf(messages),
                List.copyOf(ranking),
                Instant.now().toString()
        );
    }

    private PaperPortfolioCandidate toCandidate(
            ScannerResult candidate,
            String decision
    ) {
        String strategy = candidate.successful()
                ? candidate.strategy().displayName()
                : "—";

        return new PaperPortfolioCandidate(
                candidate.item().symbol(),
                candidate.item().marketLabel(),
                candidate.score(),
                candidate.signal(),
                strategy,
                candidate.regime(),
                decision
        );
    }

    private String format(double value) {
        return String.format("%.4f", value)
                .replaceAll("0+$", "")
                .replaceAll("\\.$", "");
    }

    private String safeMessage(Exception ex) {
        return ex.getMessage() == null
                ? ex.getClass().getSimpleName()
                : ex.getMessage();
    }
}
