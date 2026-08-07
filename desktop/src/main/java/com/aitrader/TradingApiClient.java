package com.aitrader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public class TradingApiClient {

    private static final URI URI_ANALYSIS =
            URI.create("http://127.0.0.1:7000/analysis");

    private final HttpClient client = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    private final ObjectMapper mapper = new ObjectMapper();


    public JsonNode searchSymbols(String query) throws Exception {
        String encoded = URLEncoder.encode(
                query == null ? "" : query.trim(),
                StandardCharsets.UTF_8
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(
                        "http://127.0.0.1:7000/symbols/search?q="
                                + encoded
                                + "&limit=8"
                ))
                .timeout(Duration.ofSeconds(15))
                .header("Accept", "application/json")
                .GET()
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException(
                    "Backend respondió "
                            + response.statusCode()
                            + ": "
                            + response.body()
            );
        }

        return mapper.readTree(response.body());
    }

    public JsonNode analyze(
            String marketType,
            String source,
            String symbol,
            String timeframe,
            String period
    ) throws Exception {

        ObjectNode json = mapper.createObjectNode();
        json.put("market_type", marketType);
        json.put("source", source);
        json.put("symbol", symbol);
        json.put("timeframe", timeframe);
        json.put("period", period);
        json.put("initial_cash", 10000.0);
        json.put("fee_pct", 0.001);
        json.put("slippage_pct", 0.0005);
        json.put("risk_per_trade", 0.01);
        json.put("stop_loss_pct", 0.02);
        json.put("take_profit_pct", 0.04);

        byte[] body = mapper.writeValueAsBytes(json);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI_ANALYSIS)
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                .build();

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() / 100 != 2) {
            throw new IllegalStateException(
                    "Backend respondió " + response.statusCode() + ": " + response.body()
            );
        }

        return mapper.readTree(response.body());
    }
}
