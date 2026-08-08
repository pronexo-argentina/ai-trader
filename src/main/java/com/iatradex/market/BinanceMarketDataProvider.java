package com.iatradex.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.iatradex.model.Candle;
import com.iatradex.util.HttpJsonClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BinanceMarketDataProvider implements MarketDataProvider {

    private final HttpJsonClient http;

    public BinanceMarketDataProvider(HttpJsonClient http) {
        this.http = http;
    }

    @Override
    public List<Candle> fetch(String symbol, String timeframe, String period) throws Exception {
        String apiSymbol = symbol.replace("/", "").toUpperCase();
        long now = System.currentTimeMillis();
        long since = Instant.now()
                .minus(MarketPeriods.days(period), ChronoUnit.DAYS)
                .toEpochMilli();

        long cursor = since;
        long tfMillis = MarketPeriods.timeframeMillis(timeframe);
        Map<Long, Candle> unique = new LinkedHashMap<>();

        for (int page = 0; page < 100 && cursor < now; page++) {
            String url = "https://api.binance.com/api/v3/klines"
                    + "?symbol=" + apiSymbol
                    + "&interval=" + timeframe
                    + "&startTime=" + cursor
                    + "&endTime=" + now
                    + "&limit=1000";

            JsonNode rows = http.get(url);

            if (!rows.isArray() || rows.isEmpty()) {
                break;
            }

            long lastTs = cursor;

            for (JsonNode row : rows) {
                long ts = row.get(0).asLong();
                if (ts < since) {
                    continue;
                }

                Candle candle = new Candle(
                        ts,
                        row.get(1).asDouble(),
                        row.get(2).asDouble(),
                        row.get(3).asDouble(),
                        row.get(4).asDouble(),
                        row.get(5).asDouble()
                );

                unique.put(ts, candle);
                lastTs = Math.max(lastTs, ts);
            }

            long next = lastTs + tfMillis;
            if (next <= cursor) {
                break;
            }

            cursor = next;

            if (rows.size() < 1000) {
                break;
            }
        }

        List<Candle> candles = new ArrayList<>(unique.values());
        candles.sort(Comparator.comparingLong(Candle::timestamp));

        if (candles.size() < 50) {
            throw new IllegalStateException(
                    "Binance devolvió solo " + candles.size() + " velas para " + symbol
            );
        }

        return candles;
    }

    public double livePrice(String symbol) throws Exception {
        String apiSymbol = symbol.replace("/", "").toUpperCase();

        JsonNode response = http.get(
                "https://api.binance.com/api/v3/ticker/price?symbol="
                        + apiSymbol
        );

        double price = response.path("price").asDouble(Double.NaN);

        if (!Double.isFinite(price) || price <= 0.0) {
            throw new IllegalStateException(
                    "Binance no devolvió un precio válido para " + symbol
            );
        }

        return price;
    }

}
