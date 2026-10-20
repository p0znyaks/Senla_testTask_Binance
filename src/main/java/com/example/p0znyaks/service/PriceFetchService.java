package com.example.p0znyaks.service;

import com.example.p0znyaks.client.BinanceClient;
import com.example.p0znyaks.config.BinanceProperties;
import com.example.p0znyaks.dto.binance.AvgPriceResponse;
import com.example.p0znyaks.exception.BinanceApiException;
import com.example.p0znyaks.repository.PriceRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class PriceFetchService {

    private final BinanceClient binanceClient;
    private final PriceRecordRepository priceRecordRepository;
    private final BinanceProperties binanceProperties;

    public void fetchAndSave() {
        String symbol = binanceProperties.getSymbol();
        AvgPriceResponse avgPrice = binanceClient.fetchAvgPrice(symbol);
        if (avgPrice == null || avgPrice.price() == null) {
            throw new BinanceApiException("Binance returned no price for " + symbol, 0, -1, null);
        }

        Instant fetchedAt = Instant.now();
        long closeTime = avgPrice.closeTime() != null ? avgPrice.closeTime() : fetchedAt.toEpochMilli();

        int insertedRows = priceRecordRepository.insertIgnoringDuplicate(
                symbol,
                avgPrice.price(),
                fetchedAt,
                closeTime);

        if (insertedRows > 0) {
            log.info("Fetched {} price: {} (closeTime={})", symbol, avgPrice.price(), closeTime);
        } else {
            log.info("Skipped duplicate: {} closeTime={} already stored", symbol, closeTime);
        }
    }
}