package com.iatradex.market;

import com.fasterxml.jackson.databind.JsonNode;
import com.iatradex.model.AssetSearchResult;
import com.iatradex.util.HttpJsonClient;
import com.iatradex.util.UrlUtil;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class YahooSearchService {

    private final HttpJsonClient http;

    public YahooSearchService(HttpJsonClient http) {
        this.http = http;
    }

    public List<AssetSearchResult> search(
            String query,
            int limit,
            boolean argentina
    ) throws Exception {
        String q = query == null ? "" : query.trim();

        if (q.length() < 2) {
            return List.of();
        }

        int safeLimit = Math.max(1, Math.min(limit, 12));

        if (!argentina) {
            return filterResults(fetch(q, safeLimit * 2), false, safeLimit);
        }

        // Para Argentina hacemos dos búsquedas y combinamos resultados.
        // Yahoo identifica los instrumentos de Buenos Aires con sufijo .BA.
        Map<String, AssetSearchResult> merged = new LinkedHashMap<>();

        for (AssetSearchResult item : fetch(q, safeLimit * 2)) {
            if (isArgentina(item)) {
                merged.put(item.symbol(), item);
            }
        }

        for (AssetSearchResult item : fetch(q + " BA", safeLimit * 2)) {
            if (isArgentina(item)) {
                merged.put(item.symbol(), item);
            }
        }

        return merged.values()
                .stream()
                .limit(safeLimit)
                .toList();
    }

    private List<AssetSearchResult> fetch(String query, int count) throws Exception {
        String url = "https://query2.finance.yahoo.com/v1/finance/search"
                + "?q=" + UrlUtil.enc(query)
                + "&quotesCount=" + count
                + "&newsCount=0"
                + "&enableFuzzyQuery=true";

        JsonNode root = http.get(url);
        JsonNode quotes = root.path("quotes");

        List<AssetSearchResult> results = new ArrayList<>();

        if (!quotes.isArray()) {
            return results;
        }

        for (JsonNode row : quotes) {
            String type = row.path("quoteType").asText("").toUpperCase(Locale.ROOT);

            if (!"EQUITY".equals(type) && !"ETF".equals(type)) {
                continue;
            }

            String symbol = row.path("symbol").asText("").trim();
            if (symbol.isBlank()) {
                continue;
            }

            String name = firstNonBlank(
                    row.path("shortname").asText(""),
                    row.path("longname").asText(""),
                    symbol
            );

            String exchange = firstNonBlank(
                    row.path("exchDisp").asText(""),
                    row.path("exchange").asText(""),
                    ""
            );

            String logoSymbol = symbol.endsWith(".BA")
                    ? symbol.substring(0, symbol.length() - 3)
                    : symbol;

            String logo = "https://assets.parqet.com/logos/symbol/"
                    + UrlUtil.enc(logoSymbol)
                    + "?format=png&size=80";

            results.add(new AssetSearchResult(
                    symbol,
                    name,
                    exchange,
                    type,
                    logo
            ));
        }

        return results;
    }

    private List<AssetSearchResult> filterResults(
            List<AssetSearchResult> source,
            boolean argentina,
            int limit
    ) {
        return source.stream()
                .filter(item -> argentina == isArgentina(item))
                .limit(limit)
                .toList();
    }

    private boolean isArgentina(AssetSearchResult item) {
        String symbol = item.symbol() == null ? "" : item.symbol().toUpperCase(Locale.ROOT);
        String exchange = item.exchange() == null ? "" : item.exchange().toUpperCase(Locale.ROOT);

        return symbol.endsWith(".BA")
                || exchange.contains("BUENOS AIRES")
                || exchange.equals("BUE");
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }

        return "";
    }
}
