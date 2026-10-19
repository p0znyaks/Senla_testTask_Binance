package com.example.p0znyaks.scheduler;

import com.example.p0znyaks.exception.BinanceApiException;
import com.example.p0znyaks.service.PriceFetchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PriceRecordScheduler {

    private final PriceFetchService priceFetchService;

    @Scheduled(fixedDelayString = "${binance.poll-interval}")
    public void pollBinance() {
        try {
            priceFetchService.fetchAndSave();
        } catch (BinanceApiException e) {
            log.warn("Binance error: httpStatus={}, binanceCode={}, retryAfterSeconds={}",
                    e.getStatusCode(), e.getBinanceCode(), e.getRetryAfterSeconds(), e);
        } catch (Exception e) {
            log.error("Unexpected error while polling Binance", e);
        }
    }
}