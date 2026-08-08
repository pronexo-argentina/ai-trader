package com.iatradex.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.iatradex.model.Candle;
import com.iatradex.util.HttpJsonClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public final class KrakenMarketDataProvider implements MarketDataProvider {

    private final HttpJsonClient http;

    public KrakenMarketDataProvider(HttpJsonClient http) {
        this.http = http;
    }

    @Override
    public List<Candle> fetch(String symbol, String timeframe, String period) throws Exception {
        int interval = switch (timeframe) {
            case "1h" -> 60;
            case "4h" -> 240;
            case "1d" -> 1440;
            default -> throw new IllegalArgumentException("Timeframe no soportado: " + timeframe);
        };

        String pair = normalizePair(symbol);
        long sinceSeconds = Instant.now()
                .minus(MarketPeriods.days(period), ChronoUnit.DAYS)
                .getEpochSecond();

        String url = "https://api.kraken.com/0/public/OHLC"
                + "?pair=" + pair
                + "&interval=" + interval
                + "&since=" + sinceSeconds;

        JsonNode response = http.get(url);

        JsonNode errors = response.path("error");
        if (errors.isArray() && !errors.isEmpty()) {
            throw new IllegalStateException("Kraken: " + errors.toString());
        }

        JsonNode result = response.path("result");
        JsonNode rows = null;

        Iterator<Map.Entry<String, JsonNode>> fields = result.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (!"last".equals(entry.getKey())) {
                rows = entry.getValue();
                break;
            }
        }

        if (rows == null || !rows.isArray()) {
            throw new IllegalStateException("Kraken no devolvió OHLC para " + symbol);
        }

        List<Candle> candles = new ArrayList<>();

        for (JsonNode row : rows) {
            if (!row.isArray() || row.size() < 7) {
                continue;
            }

            candles.add(new Candle(
                    row.get(0).asLong() * 1000L,
                    row.get(1).asDouble(),
                    row.get(2).asDouble(),
                    row.get(3).asDouble(),
                    row.get(4).asDouble(),
                    row.get(6).asDouble()
            ));
        }

        candles.sort(Comparator.comparingLong(Candle::timestamp));

        if (candles.size() < 50) {
            throw new IllegalStateException(
                    "Kraken devolvió solo " + candles.size() + " velas para " + symbol
            );
        }

        return candles;
    }

    private String normalizePair(String symbol) {
        return symbol.toUpperCase()
                .replace("BTC", "XBT")
                .replace("/", "");
    }

    public double livePrice(String symbol) throws Exception {
        String pair = normalizePair(symbol);

        JsonNode response = http.get(
                "https://api.kraken.com/0/public/Ticker?pair=" + pair
        );

        JsonNode errors = response.path("error");
        if (errors.isArray() && !errors.isEmpty()) {
            throw new IllegalStateException(
                    "Kraken: " + errors.toString()
            );
        }

        JsonNode result = response.path("result");

        java.util.Iterator<java.util.Map.Entry<String, JsonNode>> fields =
                result.fields();

        if (!fields.hasNext()) {
            throw new IllegalStateException(
                    "Kraken no devolvió ticker para " + symbol
            );
        }

        JsonNode ticker = fields.next().getValue();
        JsonNode close = ticker.path("c");

        double price = close.isArray() && !close.isEmpty()
                ? close.get(0).asDouble(Double.NaN)
                : Double.NaN;

        if (!Double.isFinite(price) || price <= 0.0) {
            throw new IllegalStateException(
                    "Kraken no devolvió un precio válido para " + symbol
            );
        }

        return price;
    }

}
