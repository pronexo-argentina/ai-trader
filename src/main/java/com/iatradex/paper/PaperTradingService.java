package com.iatradex.paper;

import com.iatradex.market.MarketService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PaperTradingService {

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Path storage = Path.of(
            System.getProperty("user.home"),
            ".ia-tradex",
            "paper-trading.json"
    );

    private PaperTradingState state;

    public PaperTradingService() {
        state = load();
    }

    public synchronized PaperTradingState state() {
        return state;
    }



    public synchronized PaperPortfolioAutoConfig portfolioAutoConfig() {
        if (state.portfolioAutoConfig == null) {
            state.portfolioAutoConfig =
                    PaperPortfolioAutoConfig.disabled();
        } else {
            state.portfolioAutoConfig =
                    state.portfolioAutoConfig.normalized();
        }
        return state.portfolioAutoConfig;
    }

    public synchronized void setPortfolioAutoConfig(
            PaperPortfolioAutoConfig config
    ) throws IOException {
        state.portfolioAutoConfig = config == null
                ? PaperPortfolioAutoConfig.disabled()
                : config.normalized();
        save();
    }

    public synchronized void setPortfolioRanking(
            List<PaperPortfolioCandidate> ranking
    ) throws IOException {
        state.portfolioRanking = ranking == null
                ? new ArrayList<>()
                : new ArrayList<>(ranking);
        save();
    }

    public synchronized List<PaperPortfolioCandidate> portfolioRanking() {
        if (state.portfolioRanking == null) {
            state.portfolioRanking = new ArrayList<>();
        }
        return List.copyOf(state.portfolioRanking);
    }

    public synchronized List<PaperPosition> positionsSnapshot() {
        return List.copyOf(state.positions);
    }

    public synchronized int openPositionCount() {
        return state.positions.size();
    }

    public synchronized int openPositionCountByMarket(
            String marketType
    ) {
        return (int) state.positions.stream()
                .filter(p ->
                        p.resolvedMarketType()
                                .equalsIgnoreCase(marketType)
                )
                .count();
    }

    public synchronized double marketExposureAmount(
            String currency,
            String marketType
    ) {
        return state.positions.stream()
                .filter(p ->
                        currency.equalsIgnoreCase(p.currency())
                                && p.resolvedMarketType()
                                .equalsIgnoreCase(marketType)
                )
                .mapToDouble(PaperPosition::marketValue)
                .sum();
    }

    public synchronized double totalExposureAmount(
            String currency
    ) {
        return state.positions.stream()
                .filter(p ->
                        currency.equalsIgnoreCase(p.currency())
                )
                .mapToDouble(PaperPosition::marketValue)
                .sum();
    }

    public synchronized double exposurePct(
            String currency
    ) {
        double equity = equity(currency);

        if (equity <= 0.0) {
            return 0.0;
        }

        return totalExposureAmount(currency)
                / equity
                * 100.0;
    }

    public synchronized double openRiskPct(
            String currency
    ) {
        double equity = equity(currency);

        if (equity <= 0.0) {
            return 0.0;
        }

        return openRiskAmount(currency)
                / equity
                * 100.0;
    }

    public synchronized boolean hasOpenPosition(
            String symbol,
            String currency
    ) {
        return state.positions.stream()
                .anyMatch(p ->
                        p.symbol().equalsIgnoreCase(symbol)
                                && p.currency().equalsIgnoreCase(currency)
                );
    }

    public synchronized double openRiskAmount(
            String currency
    ) {
        return state.positions.stream()
                .filter(p ->
                        currency.equalsIgnoreCase(p.currency())
                )
                .mapToDouble(p -> {
                    if (p.stopLoss() == null
                            || p.stopLoss() <= 0.0
                            || p.stopLoss() >= p.entryPrice()) {
                        return 0.0;
                    }

                    return p.quantity()
                            * (p.entryPrice() - p.stopLoss());
                })
                .sum();
    }

    public synchronized PaperAutoConfig autoConfig() {
        if (state.autoConfig == null) {
            state.autoConfig = PaperAutoConfig.disabled();
        }
        return state.autoConfig;
    }

    public synchronized void setAutoConfig(
            PaperAutoConfig config
    ) throws IOException {
        state.autoConfig = config == null
                ? PaperAutoConfig.disabled()
                : config;
        save();
    }

    public synchronized void addAutoLog(
            String level,
            String message
    ) throws IOException {
        if (state.autoLog == null) {
            state.autoLog = new ArrayList<>();
        }

        state.autoLog.add(
                0,
                new PaperAutoLogEntry(
                        Instant.now().toString(),
                        level == null ? "INFO" : level,
                        message == null ? "" : message
                )
        );

        if (state.autoLog.size() > 200) {
            state.autoLog = new ArrayList<>(
                    state.autoLog.subList(0, 200)
            );
        }

        save();
    }

    public synchronized List<PaperAutoLogEntry> autoLog() {
        if (state.autoLog == null) {
            state.autoLog = new ArrayList<>();
        }
        return List.copyOf(state.autoLog);
    }

    public synchronized PaperPosition findOpenPosition(
            String symbol,
            String currency,
            String strategyContext
    ) {
        return state.positions.stream()
                .filter(p ->
                        p.symbol().equalsIgnoreCase(symbol)
                                && p.currency().equalsIgnoreCase(currency)
                                && (
                                strategyContext == null
                                        || strategyContext.equalsIgnoreCase(
                                                p.strategyContext()
                                        )
                        )
                )
                .findFirst()
                .orElse(null);
    }

    public synchronized PaperAccount account(String currency) {
        return "ARS".equalsIgnoreCase(currency)
                ? state.ars
                : state.usd;
    }

    public synchronized void resetAccount(
            String currency,
            double initialCapital
    ) throws IOException {
        if (!Double.isFinite(initialCapital) || initialCapital <= 0.0) {
            throw new IllegalArgumentException(
                    "El capital inicial debe ser mayor que cero."
            );
        }

        boolean hasOpen = state.positions.stream()
                .anyMatch(p -> currency.equalsIgnoreCase(p.currency()));

        if (hasOpen) {
            throw new IllegalStateException(
                    "No podés reiniciar esa cuenta mientras tenga posiciones abiertas."
            );
        }

        PaperAccount account = new PaperAccount(
                currency.toUpperCase(),
                initialCapital,
                initialCapital
        );

        if ("ARS".equalsIgnoreCase(currency)) {
            state.ars = account;
        } else {
            state.usd = account;
        }

        save();
    }

    public synchronized PaperPosition buy(
            String symbol,
            String market,
            String marketType,
            String source,
            String currency,
            double quantity,
            double price,
            Double stopLoss,
            Double takeProfit,
            String strategyContext,
            String regimeContext
    ) throws IOException {
        if (!Double.isFinite(quantity) || quantity <= 0.0) {
            throw new IllegalArgumentException(
                    "La cantidad debe ser mayor que cero."
            );
        }

        if (!Double.isFinite(price) || price <= 0.0) {
            throw new IllegalArgumentException(
                    "El precio debe ser mayor que cero."
            );
        }

        validateLevels(price, stopLoss, takeProfit);

        PaperAccount account = account(currency);
        double cost = quantity * price;

        if (cost > account.cash() + 0.0000001) {
            throw new IllegalStateException(
                    "Saldo insuficiente en la cuenta " + currency + "."
            );
        }

        PaperPosition position = new PaperPosition(
                UUID.randomUUID().toString(),
                symbol,
                market,
                marketType,
                source,
                currency,
                quantity,
                price,
                price,
                stopLoss,
                takeProfit,
                strategyContext == null ? "Manual" : strategyContext,
                regimeContext == null ? "Sin análisis" : regimeContext,
                Instant.now().toString()
        );

        state.positions.add(position);
        setCash(currency, account.cash() - cost);
        save();

        return position;
    }

    public synchronized PaperClosedTrade close(
            String positionId,
            double exitPrice,
            String reason
    ) throws IOException {
        PaperPosition position = state.positions.stream()
                .filter(p -> p.id().equals(positionId))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "La posición ya no existe."
                        )
                );

        if (!Double.isFinite(exitPrice) || exitPrice <= 0.0) {
            throw new IllegalArgumentException(
                    "El precio de salida debe ser mayor que cero."
            );
        }

        double pnl = position.quantity()
                * (exitPrice - position.entryPrice());

        double pnlPct = position.entryPrice() == 0.0
                ? 0.0
                : (exitPrice / position.entryPrice() - 1.0) * 100.0;

        PaperClosedTrade closed = new PaperClosedTrade(
                UUID.randomUUID().toString(),
                position.symbol(),
                position.market(),
                position.currency(),
                position.quantity(),
                position.entryPrice(),
                exitPrice,
                pnl,
                pnlPct,
                position.strategyContext(),
                position.regimeContext(),
                position.openedAt(),
                Instant.now().toString(),
                reason == null ? "Cierre manual" : reason
        );

        PaperAccount account = account(position.currency());
        setCash(
                position.currency(),
                account.cash() + position.quantity() * exitPrice
        );

        state.positions.removeIf(
                p -> p.id().equals(positionId)
        );
        state.history.add(0, closed);
        save();

        return closed;
    }

    public synchronized void updateMarketPrice(
            String symbol,
            String currency,
            double price
    ) throws IOException {
        if (!Double.isFinite(price) || price <= 0.0) {
            return;
        }

        List<PaperPosition> updated = new ArrayList<>();

        for (PaperPosition position : state.positions) {
            if (position.symbol().equalsIgnoreCase(symbol)
                    && position.currency().equalsIgnoreCase(currency)) {
                updated.add(position.withLastPrice(price));
            } else {
                updated.add(position);
            }
        }

        state.positions = updated;
        save();
    }


    public PaperRefreshResult refreshOpenPositions(
            MarketService marketService
    ) throws IOException {
        List<PaperPosition> snapshot;

        synchronized (this) {
            snapshot = List.copyOf(state.positions);
        }

        int updated = 0;
        int closed = 0;
        List<String> messages = new ArrayList<>();

        for (PaperPosition position : snapshot) {
            try {
                double price = marketService.livePrice(
                        position.resolvedMarketType(),
                        position.resolvedSource(),
                        position.symbol()
                );

                String trigger = null;

                if (position.stopLoss() != null
                        && position.stopLoss() > 0.0
                        && price <= position.stopLoss()) {
                    trigger = "Stop Loss automático";
                } else if (position.takeProfit() != null
                        && position.takeProfit() > 0.0
                        && price >= position.takeProfit()) {
                    trigger = "Take Profit automático";
                }

                synchronized (this) {
                    boolean stillOpen = state.positions.stream()
                            .anyMatch(p ->
                                    p.id().equals(position.id())
                            );

                    if (!stillOpen) {
                        continue;
                    }

                    if (trigger != null) {
                        close(position.id(), price, trigger);
                        closed++;
                        messages.add(
                                position.symbol()
                                        + ": "
                                        + trigger
                                        + " @ "
                                        + price
                        );
                    } else {
                        updatePositionPrice(
                                position.id(),
                                price
                        );
                        updated++;
                    }
                }
            } catch (Exception ex) {
                messages.add(
                        position.symbol()
                                + ": "
                                + (
                                ex.getMessage() == null
                                        ? "error al actualizar"
                                        : ex.getMessage()
                        )
                );
            }
        }

        synchronized (this) {
            save();
        }

        return new PaperRefreshResult(
                updated,
                closed,
                List.copyOf(messages),
                Instant.now().toString()
        );
    }

    private void updatePositionPrice(
            String positionId,
            double price
    ) {
        List<PaperPosition> updated = new ArrayList<>();

        for (PaperPosition position : state.positions) {
            if (position.id().equals(positionId)) {
                updated.add(position.withLastPrice(price));
            } else {
                updated.add(position);
            }
        }

        state.positions = updated;
    }

    public synchronized double equity(String currency) {
        PaperAccount account = account(currency);
        double positionsValue = state.positions.stream()
                .filter(p -> currency.equalsIgnoreCase(p.currency()))
                .mapToDouble(PaperPosition::marketValue)
                .sum();

        return account.cash() + positionsValue;
    }

    public synchronized double unrealizedPnl(String currency) {
        return state.positions.stream()
                .filter(p -> currency.equalsIgnoreCase(p.currency()))
                .mapToDouble(PaperPosition::unrealizedPnl)
                .sum();
    }

    private void setCash(String currency, double cash) {
        if ("ARS".equalsIgnoreCase(currency)) {
            state.ars = state.ars.withCash(cash);
        } else {
            state.usd = state.usd.withCash(cash);
        }
    }

    private void validateLevels(
            double price,
            Double stopLoss,
            Double takeProfit
    ) {
        if (stopLoss != null && stopLoss > 0.0 && stopLoss >= price) {
            throw new IllegalArgumentException(
                    "En una compra long, el Stop Loss debe quedar debajo del precio de entrada."
            );
        }

        if (takeProfit != null && takeProfit > 0.0 && takeProfit <= price) {
            throw new IllegalArgumentException(
                    "En una compra long, el Take Profit debe quedar por encima del precio de entrada."
            );
        }
    }

    private PaperTradingState load() {
        try {
            if (!Files.exists(storage)) {
                return new PaperTradingState();
            }

            PaperTradingState loaded = mapper.readValue(
                    storage.toFile(),
                    PaperTradingState.class
            );

            if (loaded.ars == null) {
                loaded.ars = new PaperAccount(
                        "ARS",
                        1_000_000.0,
                        1_000_000.0
                );
            }

            if (loaded.usd == null) {
                loaded.usd = new PaperAccount(
                        "USD",
                        10_000.0,
                        10_000.0
                );
            }

            if (loaded.positions == null) {
                loaded.positions = new ArrayList<>();
            }

            if (loaded.history == null) {
                loaded.history = new ArrayList<>();
            }

            if (loaded.autoConfig == null) {
                loaded.autoConfig = PaperAutoConfig.disabled();
            }

            if (loaded.portfolioAutoConfig == null) {
                loaded.portfolioAutoConfig =
                        PaperPortfolioAutoConfig.disabled();
            } else {
                loaded.portfolioAutoConfig =
                        loaded.portfolioAutoConfig.normalized();
            }

            if (loaded.portfolioRanking == null) {
                loaded.portfolioRanking = new ArrayList<>();
            }

            if (loaded.autoLog == null) {
                loaded.autoLog = new ArrayList<>();
            }

            return loaded;
        } catch (Exception ignored) {
            return new PaperTradingState();
        }
    }

    private void save() throws IOException {
        Files.createDirectories(storage.getParent());
        mapper.writeValue(storage.toFile(), state);
    }
}
