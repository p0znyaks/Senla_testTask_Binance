package com.example.p0znyaks.scheduler;

import com.example.p0znyaks.exception.BinanceApiException;
import com.example.p0znyaks.service.PriceFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Slf4j
@Component
@ConditionalOnProperty(name = "binance.scheduler.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class PriceRecordScheduler {

    private static final int HTTP_TOO_MANY_REQUESTS = 429;

    private final PriceFetchService priceFetchService;

    private volatile Instant nextAllowedPollAt = Instant.EPOCH;

    @Scheduled(fixedDelayString = "${binance.poll-interval}")
    public void pollBinance() {
        if (Instant.now().isBefore(nextAllowedPollAt)) {
            log.debug("Skipping poll, Binance asked to wait until {}", nextAllowedPollAt);
            return;
        }
        try {
            priceFetchService.fetchAndSave();
        } catch (BinanceApiException e) {
            if (e.getStatusCode() == HTTP_TOO_MANY_REQUESTS && e.getRetryAfterSeconds() != null) {
                nextAllowedPollAt = Instant.now().plusSeconds(e.getRetryAfterSeconds());
                log.warn("Rate limited by Binance, next poll not before {}", nextAllowedPollAt);
            } else {
                log.warn("Binance error: httpStatus={}, binanceCode={}, retryAfterSeconds={}",
                        e.getStatusCode(), e.getBinanceCode(), e.getRetryAfterSeconds(), e);
            }
        } catch (Exception e) {
            log.error("Unexpected error while polling Binance", e);
        }
    }
}