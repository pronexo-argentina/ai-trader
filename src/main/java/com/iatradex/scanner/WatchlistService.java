package com.iatradex.scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class WatchlistService {

    private final ObjectMapper mapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    private final Path storage = Path.of(
            System.getProperty("user.home"),
            ".ia-tradex",
            "watchlists.json"
    );

    private WatchlistState state;

    public WatchlistService() {
        state = load();
    }

    public synchronized List<WatchlistItem> items() {
        return List.copyOf(state.items);
    }

    public synchronized List<WatchlistItem> itemsForMarket(
            String marketType
    ) {
        return state.items.stream()
                .filter(item -> item.marketType().equals(marketType))
                .toList();
    }

    public synchronized void add(WatchlistItem item) throws IOException {
        if (item == null
                || item.symbol() == null
                || item.symbol().isBlank()) {
            throw new IllegalArgumentException(
                    "El activo no puede estar vacío."
            );
        }

        WatchlistItem normalized = new WatchlistItem(
                item.marketType(),
                item.source(),
                item.symbol().trim().toUpperCase(Locale.ROOT),
                item.currency(),
                item.timeframe(),
                item.period()
        );

        boolean exists = state.items.stream()
                .anyMatch(existing ->
                        existing.key().equalsIgnoreCase(normalized.key())
                );

        if (!exists) {
            state.items.add(normalized);
            save();
        }
    }

    public synchronized void remove(WatchlistItem item) throws IOException {
        if (item == null) {
            return;
        }

        state.items.removeIf(existing ->
                existing.key().equalsIgnoreCase(item.key())
        );
        save();
    }

    private WatchlistState load() {
        try {
            if (Files.exists(storage)) {
                WatchlistState loaded = mapper.readValue(
                        storage.toFile(),
                        WatchlistState.class
                );

                if (loaded.items == null) {
                    loaded.items = new ArrayList<>();
                }

                return loaded;
            }
        } catch (Exception ignored) {
        }

        WatchlistState defaults = new WatchlistState();

        defaults.items.add(new WatchlistItem(
                "crypto", "binance", "BTC/USDT", "USD", "1h", "3m"
        ));
        defaults.items.add(new WatchlistItem(
                "crypto", "binance", "ETH/USDT", "USD", "1h", "3m"
        ));

        defaults.items.add(new WatchlistItem(
                "argentina", "open-bymadata", "YPFD", "ARS", "1d", "3m"
        ));
        defaults.items.add(new WatchlistItem(
                "argentina", "open-bymadata", "GGAL", "ARS", "1d", "3m"
        ));
        defaults.items.add(new WatchlistItem(
                "argentina", "open-bymadata", "PAMP", "ARS", "1d", "3m"
        ));

        defaults.items.add(new WatchlistItem(
                "stocks", "yahoo", "AAPL", "USD", "1h", "3m"
        ));
        defaults.items.add(new WatchlistItem(
                "stocks", "yahoo", "MSFT", "USD", "1h", "3m"
        ));
        defaults.items.add(new WatchlistItem(
                "stocks", "yahoo", "NVDA", "USD", "1h", "3m"
        ));

        try {
            state = defaults;
            save();
        } catch (Exception ignored) {
        }

        return defaults;
    }

    private void save() throws IOException {
        Files.createDirectories(storage.getParent());
        mapper.writeValue(storage.toFile(), state);
    }
}
