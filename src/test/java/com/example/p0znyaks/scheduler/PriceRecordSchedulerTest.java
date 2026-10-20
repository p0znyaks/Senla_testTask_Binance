package com.example.p0znyaks.scheduler;

import com.example.p0znyaks.exception.BinanceApiException;
import com.example.p0znyaks.service.PriceFetchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PriceRecordSchedulerTest {

    @Mock
    private PriceFetchService priceFetchService;

    @Test
    void pollBinance_pausesAfterRateLimit() {
        doThrow(new BinanceApiException("Too many requests", 429, -1003, 30L))
                .when(priceFetchService).fetchAndSave();

        PriceRecordScheduler scheduler = new PriceRecordScheduler(priceFetchService);
        scheduler.pollBinance();
        scheduler.pollBinance();

        // The second poll is skipped because Binance asked us to wait.
        verify(priceFetchService, times(1)).fetchAndSave();
    }

    @Test
    void pollBinance_continuesAfterOtherErrors() {
        doThrow(new BinanceApiException("Server error", 500, -1, null))
                .when(priceFetchService).fetchAndSave();

        PriceRecordScheduler scheduler = new PriceRecordScheduler(priceFetchService);
        scheduler.pollBinance();
        scheduler.pollBinance();

        verify(priceFetchService, times(2)).fetchAndSave();
    }
}
