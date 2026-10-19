package com.example.p0znyaks.client;

import com.example.p0znyaks.dto.AvgPriceResponse;
import com.example.p0znyaks.exception.BinanceApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class BinanceClient {

    private static final String AVG_PRICE_PATH = "/api/v3/avgPrice";
    private static final int NO_BINANCE_CODE = -1;

    private final RestClient binanceRestClient;
    private final ObjectMapper objectMapper;

    public AvgPriceResponse fetchAvgPrice(String symbol) {
        return binanceRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(AVG_PRICE_PATH)
                        .queryParam("symbol", symbol)
                        .build())
                .retrieve()
                .onStatus(status -> status.isError(), (request, response) -> {
                    throw toBinanceApiException(response);
                })
                .body(AvgPriceResponse.class);
    }

    private BinanceApiException toBinanceApiException(ClientHttpResponse response) throws IOException {
        int statusCode = response.getStatusCode().value();
        Long retryAfterSeconds = parseRetryAfter(response.getHeaders().getFirst("Retry-After"));
        String body = new String(response.getBody().readAllBytes(), StandardCharsets.UTF_8);
        int code = NO_BINANCE_CODE;
        String msg = body;
        try {
            JsonNode json = objectMapper.readTree(body);
            code = json.path("code").asInt(NO_BINANCE_CODE);
            msg = json.path("msg").asText(body);
        } catch (Exception ignored) {
        }
        return new BinanceApiException("Binance API error: " + msg, statusCode, code, retryAfterSeconds);
    }

    private Long parseRetryAfter(String header) {
        if (header == null) {
            return null;
        }
        try {
            return Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}