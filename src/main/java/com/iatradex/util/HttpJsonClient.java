package com.iatradex.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class HttpJsonClient {

    private final HttpClient client;
    private final ObjectMapper mapper;

    public HttpJsonClient() {
        this.client = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(12))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.mapper = new ObjectMapper();
    }

    public JsonNode get(String url) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(35))
                .header("Accept", "application/json")
                .header("User-Agent", "Mozilla/5.0 IA-TradeX/0.5")
                .GET()
                .build();

        return send(request, url);
    }

    public JsonNode postJson(String url, String json)
            throws IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(35))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "Mozilla/5.0 IA-TradeX/0.5")
                .POST(HttpRequest.BodyPublishers.ofString(
                        json,
                        StandardCharsets.UTF_8
                ))
                .build();

        return send(request, url);
    }

    private JsonNode send(HttpRequest request, String url)
            throws IOException, InterruptedException {

        HttpResponse<String> response = client.send(
                request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
        );

        if (response.statusCode() / 100 != 2) {
            String text = response.body();

            if (text != null && text.length() > 600) {
                text = text.substring(0, 600) + "...";
            }

            throw new IOException(
                    "HTTP " + response.statusCode()
                            + " consultando " + url
                            + (text == null || text.isBlank()
                            ? ""
                            : " · " + text)
            );
        }

        return mapper.readTree(response.body());
    }
}
