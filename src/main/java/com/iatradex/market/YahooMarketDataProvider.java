package com.iatradex.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.iatradex.model.Candle;
import com.iatradex.util.HttpJsonClient;
import com.iatradex.util.UrlUtil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class YahooMarketDataProvider implements MarketDataProvider {

    private final HttpJsonClient http;

    public YahooMarketDataProvider(HttpJsonClient http) {
        this.http = http;
    }

    @Override
    public List<Candle> fetch(String symbol, String timeframe, String period) throws Exception {
        if (!List.of("1h", "4h", "1d").contains(timeframe)) {
            throw new IllegalArgumentException("Timeframe no soportado: " + timeframe);
        }

        int days = MarketPeriods.days(period);
        Instant end = Instant.now();
        Instant start = end.minus(days, ChronoUnit.DAYS);

        List<Candle> raw;

        if ("1d".equals(timeframe)) {
            raw = fetchChunk(symbol, "1d", start, end.plus(1, ChronoUnit.DAYS));
        } else {
            // Yahoo es más confiable para intradía cuando se consulta por tramos.
            Map<Long, Candle> merged = new LinkedHashMap<>();
            Instant cursor = start;

            while (cursor.isBefore(end)) {
                Instant chunkEnd = cursor.plus(30, ChronoUnit.DAYS);
                if (chunkEnd.isAfter(end)) {
                    chunkEnd = end;
                }

                for (Candle candle : fetchChunk(
                        symbol,
                        "60m",
                        cursor,
                        chunkEnd.plus(1, ChronoUnit.DAYS)
                )) {
                    merged.put(candle.timestamp(), candle);
                }

                cursor = chunkEnd;
            }

            raw = new ArrayList<>(merged.values());
            raw.sort(Comparator.comparingLong(Candle::timestamp));
        }

        List<Candle> result = "4h".equals(timeframe)
                ? resampleFourHours(raw)
                : raw;

        if (result.size() < 50) {
            throw new IllegalStateException(
                    "Solo se obtuvieron " + result.size()
                            + " velas para " + symbol
                            + ". Se requieren al menos 50."
            );
        }

        return result;
    }

    private List<Candle> fetchChunk(
            String symbol,
            String interval,
            Instant start,
            Instant end
    ) throws Exception {
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/"
                + UrlUtil.enc(symbol)
                + "?period1=" + start.getEpochSecond()
                + "&period2=" + end.getEpochSecond()
                + "&interval=" + interval
                + "&includePrePost=false"
                + "&events=div%2Csplits"
                + "&includeAdjustedClose=true";

        JsonNode root = http.get(url);
        JsonNode chart = root.path("chart");

        if (!chart.path("error").isNull() && !chart.path("error").isMissingNode()) {
            throw new IllegalStateException(
                    "Yahoo: " + chart.path("error").path("description").asText("error desconocido")
            );
        }

        JsonNode result = chart.path("result");
        if (!result.isArray() || result.isEmpty()) {
            return List.of();
        }

        JsonNode node = result.get(0);
        JsonNode timestamps = node.path("timestamp");
        JsonNode quote = node.path("indicators").path("quote").path(0);

        if (!timestamps.isArray() || quote.isMissingNode()) {
            return List.of();
        }

        JsonNode opens = quote.path("open");
        JsonNode highs = quote.path("high");
        JsonNode lows = quote.path("low");
        JsonNode closes = quote.path("close");
        JsonNode volumes = quote.path("volume");

        List<Candle> candles = new ArrayList<>();

        for (int i = 0; i < timestamps.size(); i++) {
            if (!validNumber(opens, i)
                    || !validNumber(highs, i)
                    || !validNumber(lows, i)
                    || !validNumber(closes, i)) {
                continue;
            }

            double volume = validNumber(volumes, i)
                    ? volumes.get(i).asDouble()
                    : 0.0;

            candles.add(new Candle(
                    timestamps.get(i).asLong() * 1000L,
                    opens.get(i).asDouble(),
                    highs.get(i).asDouble(),
                    lows.get(i).asDouble(),
                    closes.get(i).asDouble(),
                    volume
            ));
        }

        return candles;
    }

    private boolean validNumber(JsonNode array, int index) {
        return array.isArray()
                && index < array.size()
                && !array.get(index).isNull()
                && array.get(index).isNumber()
                && Double.isFinite(array.get(index).asDouble());
    }

    private List<Candle> resampleFourHours(List<Candle> hourly) {
        if (hourly.isEmpty()) {
            return List.of();
        }

        List<Candle> result = new ArrayList<>();

        long bucketMs = 4L * 60L * 60L * 1000L;
        long bucket = -1;
        double open = 0;
        double high = 0;
        double low = 0;
        double close = 0;
        double volume = 0;

        for (Candle candle : hourly) {
            long currentBucket = (candle.timestamp() / bucketMs) * bucketMs;

            if (bucket != currentBucket) {
                if (bucket >= 0) {
                    result.add(new Candle(bucket, open, high, low, close, volume));
                }

                bucket = currentBucket;
                open = candle.open();
                high = candle.high();
                low = candle.low();
                close = candle.close();
                volume = candle.volume();
            } else {
                high = Math.max(high, candle.high());
                low = Math.min(low, candle.low());
                close = candle.close();
                volume += candle.volume();
            }
        }

        if (bucket >= 0) {
            result.add(new Candle(bucket, open, high, low, close, volume));
        }

        return result;
    }

    public double livePrice(String symbol) throws Exception {
        String url = "https://query1.finance.yahoo.com/v8/finance/chart/"
                + UrlUtil.enc(symbol)
                + "?range=1d&interval=1m&includePrePost=false";

        JsonNode root = http.get(url);
        JsonNode chart = root.path("chart");

        if (!chart.path("error").isNull()
                && !chart.path("error").isMissingNode()) {
            throw new IllegalStateException(
                    "Yahoo: "
                            + chart.path("error")
                                    .path("description")
                                    .asText("error desconocido")
            );
        }

        JsonNode result = chart.path("result");

        if (!result.isArray() || result.isEmpty()) {
            throw new IllegalStateException(
                    "Yahoo no devolvió cotización para " + symbol
            );
        }

        JsonNode meta = result.get(0).path("meta");

        double price = meta.path("regularMarketPrice")
                .asDouble(Double.NaN);

        if (!Double.isFinite(price) || price <= 0.0) {
            JsonNode closes = result.get(0)
                    .path("indicators")
                    .path("quote")
                    .path(0)
                    .path("close");

            for (int i = closes.size() - 1; i >= 0; i--) {
                if (closes.get(i).isNumber()) {
                    double candidate = closes.get(i).asDouble();

                    if (Double.isFinite(candidate) && candidate > 0.0) {
                        price = candidate;
                        break;
                    }
                }
            }
        }

        if (!Double.isFinite(price) || price <= 0.0) {
            throw new IllegalStateException(
                    "Yahoo no devolvió un precio válido para " + symbol
            );
        }

        return price;
    }

}
