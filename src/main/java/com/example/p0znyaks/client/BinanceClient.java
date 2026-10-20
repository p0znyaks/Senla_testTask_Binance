package com.example.p0znyaks.client;

import com.example.p0znyaks.dto.binance.AvgPriceResponse;
import com.example.p0znyaks.dto.binance.BinanceErrorResponse;
import com.example.p0znyaks.exception.BinanceApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Thin HTTP client for the Binance public API.
 *
 * <p>Every failure is translated into a {@link BinanceApiException} that carries the HTTP
 * status, the Binance error code and the {@code Retry-After} hint, so the scheduler can
 * react to rate limiting without knowing anything about HTTP.
 */
@Component
@RequiredArgsConstructor
public class BinanceClient {

    private static final String AVG_PRICE_PATH = "/api/v3/avgPrice";
    private static final String RETRY_AFTER_HEADER = "Retry-After";

    private final RestClient binanceRestClient;
    private final ObjectMapper objectMapper;

    /**
     * Fetches the average price of the given symbol.
     *
     * @throws BinanceApiException if Binance answers with an error status or an unreadable body
     */
    public AvgPriceResponse fetchAvgPrice(String symbol) {
        return binanceRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path(AVG_PRICE_PATH)
                        .queryParam("symbol", symbol)
                        .build())
                .exchange((request, response) -> {
                    // The body is read exactly once and reused for both the success
                    // and the error branch: a stream cannot be read twice.
                    int statusCode = response.getStatusCode().value();
                    byte[] body = readBody(response, statusCode);
                    if (response.getStatusCode().is2xxSuccessful()) {
                        return deserialize(body, AvgPriceResponse.class, statusCode);
                    }
                    throw toBinanceApiException(response, statusCode, body);
                });
    }

    private <T> T deserialize(byte[] body, Class<T> type, int statusCode) {
        try {
            return objectMapper.readValue(body, type);
        } catch (IOException e) {
            throw new BinanceApiException(
                    "Could not parse Binance response: " + e.getMessage(),
                    statusCode, BinanceErrorResponse.UNKNOWN_CODE, null);
        }
    }

    private BinanceApiException toBinanceApiException(ConvertibleClientHttpResponse response,
                                                     int statusCode,
                                                     byte[] body) {
        Long retryAfterSeconds = parseRetryAfter(response.getHeaders().getFirst(RETRY_AFTER_HEADER));
        BinanceErrorResponse error = parseError(body);
        return new BinanceApiException(
                "Binance API error: " + error.msg(),
                statusCode,
                error.codeOrUnknown(),
                retryAfterSeconds);
    }

    /**
     * Parses the Binance error payload. A body that is missing or not a JSON object is not
     * an error by itself: the raw text is used as the message and the code stays unknown.
     */
    private BinanceErrorResponse parseError(byte[] body) {
        String rawBody = new String(body, StandardCharsets.UTF_8);
        if (rawBody.isBlank()) {
            return new BinanceErrorResponse(null, "empty response body");
        }
        try {
            BinanceErrorResponse parsed = objectMapper.readValue(rawBody, BinanceErrorResponse.class);
            return new BinanceErrorResponse(parsed.code(), parsed.msg() != null ? parsed.msg() : rawBody);
        } catch (IOException e) {
            return new BinanceErrorResponse(null, rawBody);
        }
    }

    private byte[] readBody(ConvertibleClientHttpResponse response, int statusCode) {
        try {
            return response.getBody().readAllBytes();
        } catch (IOException e) {
            throw new BinanceApiException(
                    "Could not read Binance response body: " + e.getMessage(),
                    statusCode, BinanceErrorResponse.UNKNOWN_CODE, null);
        }
    }

    /**
     * @return the number of seconds to wait, or {@code null} if the header is absent or not a number
     */
    private Long parseRetryAfter(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(header.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}