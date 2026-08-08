package com.iatradex.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.iatradex.model.AssetSearchResult;
import com.iatradex.model.Candle;
import com.iatradex.model.MarketQuote;
import com.iatradex.util.HttpJsonClient;
import com.iatradex.util.UrlUtil;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class OpenBymaDataProvider implements MarketDataProvider {

    private static final String BASE =
            "https://open.bymadata.com.ar/vanoms-be-core/rest/api/bymadata/free";

    private static final String CEDEARS = BASE + "/cedears";
    private static final String LEADING = BASE + "/leading-equity";
    private static final String GENERAL = BASE + "/general-equity";
    private static final String CURRENT_QUOTE =
            BASE + "/bnown/fichatecnica/especies/cotizacion";
    private static final String HISTORICAL =
            BASE + "/chart/historical-series/history";

    private static final String COLLECTION_BODY =
            "{\"excludeZeroPxAndQty\":false,"
                    + "\"T2\":false,"
                    + "\"T1\":true,"
                    + "\"T0\":false,"
                    + "\"Content-Type\":\"application/json\"}";

    private final HttpJsonClient http;

    private List<OpenBymaSecurity> cached = List.of();
    private long cacheUntilMillis = 0L;

    public OpenBymaDataProvider(HttpJsonClient http) {
        this.http = http;
    }

    public List<AssetSearchResult> search(String query, int limit)
            throws Exception {

        String q = query == null
                ? ""
                : query.trim().toUpperCase(Locale.ROOT);

        if (q.length() < 2) {
            return List.of();
        }

        List<AssetSearchResult> results = new ArrayList<>();

        for (OpenBymaSecurity item : securities()) {
            String haystack = (
                    item.symbol()
                            + " "
                            + item.description()
                            + " "
                            + item.category()
            ).toUpperCase(Locale.ROOT);

            if (!haystack.contains(q)) {
                continue;
            }

            String logo = "https://assets.parqet.com/logos/symbol/"
                    + UrlUtil.enc(item.symbol())
                    + "?format=png&size=80";

            results.add(new AssetSearchResult(
                    item.symbol(),
                    item.description().isBlank()
                            ? item.category()
                            : item.description(),
                    "BYMA · " + item.category(),
                    item.category(),
                    logo
            ));

            if (results.size() >= limit) {
                break;
            }
        }

        return results;
    }

    @Override
    public List<Candle> fetch(
            String symbol,
            String timeframe,
            String period
    ) throws Exception {

        if (!"1d".equals(timeframe)) {
            throw new IllegalArgumentException(
                    "Argentina usa histórico diario (1d) en esta etapa."
            );
        }

        int days = MarketPeriods.days(period);
        Instant to = Instant.now();
        Instant from = to.minus(days + 10L, ChronoUnit.DAYS);

        String normalized = normalizeHistorySymbol(symbol);

        String url = HISTORICAL
                + "?symbol=" + UrlUtil.enc(normalized)
                + "&resolution=D"
                + "&from=" + from.getEpochSecond()
                + "&to=" + to.getEpochSecond();

        JsonNode data = http.get(url);

        if ("no_data".equalsIgnoreCase(data.path("s").asText(""))
                || !data.path("t").isArray()) {
            throw new IllegalStateException(
                    "Open BYMADATA no devolvió histórico para "
                            + symbol
                            + "."
            );
        }

        JsonNode times = data.path("t");
        JsonNode opens = data.path("o");
        JsonNode highs = data.path("h");
        JsonNode lows = data.path("l");
        JsonNode closes = data.path("c");
        JsonNode volumes = data.path("v");

        List<Candle> candles = new ArrayList<>();

        for (int i = 0; i < times.size(); i++) {
            if (!numberAt(opens, i)
                    || !numberAt(highs, i)
                    || !numberAt(lows, i)
                    || !numberAt(closes, i)) {
                continue;
            }

            double volume = numberAt(volumes, i)
                    ? volumes.get(i).asDouble()
                    : 0.0;

            candles.add(new Candle(
                    times.get(i).asLong() * 1000L,
                    opens.get(i).asDouble(),
                    highs.get(i).asDouble(),
                    lows.get(i).asDouble(),
                    closes.get(i).asDouble(),
                    volume
            ));
        }

        candles.sort(Comparator.comparingLong(Candle::timestamp));

        long minTimestamp = Instant.now()
                .minus(days, ChronoUnit.DAYS)
                .toEpochMilli();

        candles = candles.stream()
                .filter(c -> c.timestamp() >= minTimestamp)
                .toList();

        if (candles.size() < 20) {
            throw new IllegalStateException(
                    "Open BYMADATA devolvió solo "
                            + candles.size()
                            + " ruedas para "
                            + symbol
                            + ". Probá un período mayor."
            );
        }

        return candles;
    }

    public MarketQuote quote(String symbol) throws Exception {
        String body = "{"
                + "\"symbol\":\"" + escapeJson(symbol) + "\","
                + "\"settlementType\":\"2\","
                + "\"Content-Type\":\"application/json\""
                + "}";

        JsonNode response = http.postJson(CURRENT_QUOTE, body);
        JsonNode rows = unwrapArray(response);

        JsonNode row = null;

        if (rows.isArray() && !rows.isEmpty()) {
            row = rows.get(0);
        }

        // Fallback: la colección pública suele contener la misma información
        // y es útil fuera del horario de mercado.
        if (row == null || row.isMissingNode()) {
            OpenBymaSecurity security = securities().stream()
                    .filter(s -> s.symbol().equalsIgnoreCase(symbol))
                    .findFirst()
                    .orElse(null);

            if (security == null) {
                return null;
            }

            return security.quote();
        }

        Double last = firstNumber(
                row,
                "trade",
                "closingPrice",
                "settlementPrice"
        );

        Double previous = firstNumber(
                row,
                "previousClosingPrice",
                "previousSettlementPrice"
        );

        Double change = null;

        if (positive(last) && positive(previous)) {
            change = (last / previous - 1.0) * 100.0;
        }

        return new MarketQuote(
                firstText(row, "denominationCcy", "currency", "ARS"),
                last,
                change,
                firstNumber(row, "bidPrice", "bestPurchasePrice"),
                firstNumber(row, "quantityBid", "purchaseAmount"),
                firstNumber(row, "offerPrice", "bestSellingPrice"),
                firstNumber(row, "quantityOffer", "sellingAmount"),
                firstNumber(row, "openingPrice", "opening_price"),
                firstNumber(row, "tradingHighPrice", "trading_session_high_price"),
                firstNumber(row, "tradingLowPrice", "trading_session_low_price"),
                firstNumber(row, "tradeVolume", "volume"),
                firstText(row, "tradeHour", "date", ""),
                "Open BYMADATA · 20 min"
        );
    }

    private synchronized List<OpenBymaSecurity> securities() throws Exception {
        long now = System.currentTimeMillis();

        if (!cached.isEmpty() && now < cacheUntilMillis) {
            return cached;
        }

        Map<String, OpenBymaSecurity> unique = new LinkedHashMap<>();

        loadCollection(CEDEARS, "CEDEAR", unique);
        loadCollection(LEADING, "Acción líder", unique);
        loadCollection(GENERAL, "Acción panel general", unique);

        cached = List.copyOf(unique.values());
        cacheUntilMillis = now + (5L * 60L * 1000L);

        return cached;
    }

    private void loadCollection(
            String endpoint,
            String category,
            Map<String, OpenBymaSecurity> target
    ) throws Exception {

        JsonNode response = http.postJson(endpoint, COLLECTION_BODY);
        JsonNode rows = unwrapArray(response);

        if (!rows.isArray()) {
            return;
        }

        for (JsonNode row : rows) {
            String symbol = row.path("symbol").asText("").trim();

            if (symbol.isBlank()) {
                continue;
            }

            String currency = firstText(
                    row,
                    "denominationCcy",
                    "currency",
                    "ARS"
            );

            // Para la pantalla Argentina priorizamos los instrumentos
            // denominados en pesos. Las variantes D/C pueden agregarse luego.
            if (!currency.isBlank()
                    && !"ARS".equalsIgnoreCase(currency)) {
                continue;
            }

            String description = firstText(
                    row,
                    "securityDesc",
                    "securityDescription",
                    ""
            );

            Double last = firstNumber(
                    row,
                    "trade",
                    "closingPrice",
                    "settlementPrice"
            );

            Double previous = firstNumber(
                    row,
                    "previousClosingPrice",
                    "previousSettlementPrice"
            );

            Double change = null;

            if (positive(last) && positive(previous)) {
                change = (last / previous - 1.0) * 100.0;
            }

            MarketQuote quote = new MarketQuote(
                    currency.isBlank() ? "ARS" : currency,
                    last,
                    change,
                    firstNumber(row, "bidPrice"),
                    firstNumber(row, "quantityBid"),
                    firstNumber(row, "offerPrice"),
                    firstNumber(row, "quantityOffer"),
                    firstNumber(row, "openingPrice"),
                    firstNumber(row, "tradingHighPrice"),
                    firstNumber(row, "tradingLowPrice"),
                    firstNumber(row, "tradeVolume", "volume"),
                    firstText(row, "tradeHour", ""),
                    "Open BYMADATA · 20 min"
            );

            target.putIfAbsent(
                    symbol.toUpperCase(Locale.ROOT),
                    new OpenBymaSecurity(
                            symbol,
                            description,
                            category,
                            quote
                    )
            );
        }
    }

    private JsonNode unwrapArray(JsonNode response) {
        if (response == null) {
            return com.fasterxml.jackson.databind.node.JsonNodeFactory
                    .instance.arrayNode();
        }

        if (response.isArray()) {
            return response;
        }

        JsonNode data = response.path("data");

        if (data.isArray()) {
            return data;
        }

        JsonNode result = response.path("result");

        if (result.isArray()) {
            return result;
        }

        return com.fasterxml.jackson.databind.node.JsonNodeFactory
                .instance.arrayNode();
    }

    private String normalizeHistorySymbol(String symbol) {
        String value = symbol == null
                ? ""
                : symbol.trim().toUpperCase(Locale.ROOT);

        if (value.endsWith(" CI")
                || value.endsWith(" 24HS")
                || value.endsWith(" 48HS")) {
            return value;
        }

        return value + " 24HS";
    }

    private boolean numberAt(JsonNode array, int index) {
        return array.isArray()
                && index >= 0
                && index < array.size()
                && !array.get(index).isNull()
                && array.get(index).isNumber()
                && Double.isFinite(array.get(index).asDouble());
    }

    private Double firstNumber(JsonNode row, String... fields) {
        for (String field : fields) {
            JsonNode value = row.path(field);

            if (value.isNumber()) {
                double number = value.asDouble();

                if (Double.isFinite(number)) {
                    return number;
                }
            }

            if (value.isTextual()) {
                try {
                    double number = Double.parseDouble(value.asText());

                    if (Double.isFinite(number)) {
                        return number;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }

        return null;
    }

    private String firstText(JsonNode row, String... fields) {
        String fallback = "";

        for (String field : fields) {
            // Last item may be a literal fallback.
            if (!row.has(field)) {
                fallback = field;
                continue;
            }

            String value = row.path(field).asText("").trim();

            if (!value.isBlank()) {
                return value;
            }
        }

        return fallback;
    }

    private boolean positive(Double value) {
        return value != null
                && Double.isFinite(value)
                && value > 0.0;
    }

    private String escapeJson(String value) {
        return value == null
                ? ""
                : value.replace("\\", "\\\\")
                        .replace("\"", "\\\"");
    }

    private record OpenBymaSecurity(
            String symbol,
            String description,
            String category,
            MarketQuote quote
    ) {}
}
