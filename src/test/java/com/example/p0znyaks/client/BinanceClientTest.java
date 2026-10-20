package com.example.p0znyaks.client;

import com.example.p0znyaks.dto.binance.AvgPriceResponse;
import com.example.p0znyaks.exception.BinanceApiException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BinanceClientTest {

    private MockRestServiceServer server;
    private BinanceClient binanceClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("https://api.binance.com");
        server = MockRestServiceServer.bindTo(builder).build();
        binanceClient = new BinanceClient(builder.build(), new ObjectMapper());
    }

    @Test
    void fetchAvgPrice_returnsParsedResponse() {
        server.expect(requestTo("https://api.binance.com/api/v3/avgPrice?symbol=BNBUSDT"))
                .andRespond(withSuccess("{\"mins\":5,\"price\":\"100.5\",\"closeTime\":1700000000000}",
                        MediaType.APPLICATION_JSON));

        AvgPriceResponse response = binanceClient.fetchAvgPrice("BNBUSDT");

        assertThat(response.price()).isEqualByComparingTo(new BigDecimal("100.5"));
        assertThat(response.closeTime()).isEqualTo(1_700_000_000_000L);
        server.verify();
    }

    @Test
    void fetchAvgPrice_throwsBinanceApiExceptionWithCodeAndRetryAfter() {
        server.expect(requestTo("https://api.binance.com/api/v3/avgPrice?symbol=BNBUSDT"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .header("Retry-After", "30")
                        .body("{\"code\":-1003,\"msg\":\"Too many requests\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> binanceClient.fetchAvgPrice("BNBUSDT"))
                .isInstanceOf(BinanceApiException.class)
                .hasMessageContaining("Too many requests")
                .satisfies(ex -> {
                    BinanceApiException apiException = (BinanceApiException) ex;
                    assertThat(apiException.getStatusCode()).isEqualTo(429);
                    assertThat(apiException.getBinanceCode()).isEqualTo(-1003);
                    assertThat(apiException.getRetryAfterSeconds()).isEqualTo(30L);
                });
    }

    @Test
    void fetchAvgPrice_keepsRawBodyAsMessageWhenErrorIsNotJson() {
        server.expect(requestTo("https://api.binance.com/api/v3/avgPrice?symbol=BNBUSDT"))
                .andRespond(withStatus(HttpStatus.BAD_GATEWAY).body("upstream is down"));

        assertThatThrownBy(() -> binanceClient.fetchAvgPrice("BNBUSDT"))
                .isInstanceOf(BinanceApiException.class)
                .hasMessageContaining("upstream is down")
                .satisfies(ex -> assertThat(((BinanceApiException) ex).getBinanceCode()).isEqualTo(-1));
    }

    @Test
    void fetchAvgPrice_ignoresUnparsableRetryAfterHeader() {
        server.expect(requestTo("https://api.binance.com/api/v3/avgPrice?symbol=BNBUSDT"))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                        .header("Retry-After", "Wed, 21 Oct 2026 07:28:00 GMT")
                        .body("{\"code\":-1003,\"msg\":\"Too many requests\"}")
                        .contentType(MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> binanceClient.fetchAvgPrice("BNBUSDT"))
                .isInstanceOf(BinanceApiException.class)
                .satisfies(ex -> assertThat(((BinanceApiException) ex).getRetryAfterSeconds()).isNull());
    }
}