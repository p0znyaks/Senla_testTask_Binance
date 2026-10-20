package com.example.p0znyaks.service;

import com.example.p0znyaks.client.BinanceClient;
import com.example.p0znyaks.config.BinanceProperties;
import com.example.p0znyaks.dto.binance.AvgPriceResponse;
import com.example.p0znyaks.exception.BinanceApiException;
import com.example.p0znyaks.repository.PriceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceFetchServiceTest {

    @Mock
    private BinanceClient binanceClient;

    @Mock
    private PriceRecordRepository priceRecordRepository;

    private PriceFetchService priceFetchService;

    @BeforeEach
    void setUp() {
        BinanceProperties properties = new BinanceProperties();
        properties.setSymbol("BNBUSDT");
        priceFetchService = new PriceFetchService(binanceClient, priceRecordRepository, properties);
    }
    @Test
    void fetchAndSave_storesPriceWithBinanceCloseTime() {
        when(binanceClient.fetchAvgPrice("BNBUSDT"))
                .thenReturn(new AvgPriceResponse(new BigDecimal("100.5"), 1_700_000_000_000L));
        when(priceRecordRepository.insertIgnoringDuplicate(any(), any(), any(), anyLong())).thenReturn(1);

        priceFetchService.fetchAndSave();

        ArgumentCaptor<Long> closeTime = ArgumentCaptor.forClass(Long.class);
        verify(priceRecordRepository).insertIgnoringDuplicate(eq("BNBUSDT"), eq(new BigDecimal("100.5")),
                any(), closeTime.capture());
        assertThat(closeTime.getValue()).isEqualTo(1_700_000_000_000L);
    }

    @Test
    void fetchAndSave_fallsBackToFetchTimeWhenCloseTimeIsMissing() {
        when(binanceClient.fetchAvgPrice("BNBUSDT"))
                .thenReturn(new AvgPriceResponse(new BigDecimal("100.5"), null));
        when(priceRecordRepository.insertIgnoringDuplicate(any(), any(), any(), anyLong())).thenReturn(1);

        long before = System.currentTimeMillis();
        priceFetchService.fetchAndSave();

        ArgumentCaptor<Long> closeTime = ArgumentCaptor.forClass(Long.class);
        verify(priceRecordRepository).insertIgnoringDuplicate(eq("BNBUSDT"), any(), any(), closeTime.capture());
        assertThat(closeTime.getValue()).isGreaterThanOrEqualTo(before);
    }

    @Test
    void fetchAndSave_rejectsResponseWithoutPrice() {
        when(binanceClient.fetchAvgPrice("BNBUSDT")).thenReturn(new AvgPriceResponse(null, 1L));

        assertThatThrownBy(() -> priceFetchService.fetchAndSave())
                .isInstanceOf(BinanceApiException.class);

        verify(priceRecordRepository, never()).insertIgnoringDuplicate(any(), any(), any(), anyLong());
    }
}
