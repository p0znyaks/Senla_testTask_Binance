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
        // The HTTP call is deliberately not wrapped in a transaction, so a slow or
        // failing Binance response does not hold a database connection.
        AvgPriceResponse avgPrice = binanceClient.fetchAvgPrice(symbol);
        if (avgPrice == null || avgPrice.price() == null) {
            throw new BinanceApiException("Binance returned no price for " + symbol, 0, -1, null);
        }

        Instant fetchedAt = Instant.now();
        // closeTime identifies the Binance minute the price belongs to and is used to
        // ignore duplicate polls. If Binance does not send it, fall back to the fetch
        // time so every poll still produces its own row instead of colliding on one value.
        long closeTime = avgPrice.closeTime() != null ? avgPrice.closeTime() : fetchedAt.toEpochMilli();

        // The insert is a single statement; Spring Data runs it in its own transaction.
        int insertedRows = priceRecordRepository.insertIgnoringDuplicate(
                symbol,
                avgPrice.price(),
                fetchedAt,
                closeTime);

        if (insertedRows > 0) {
            log.info("Fetched {} price: {} (closeTime={})", symbol, avgPrice.price(), closeTime);
        } else {
            // Binance keeps the same closeTime for a whole minute, so a poll interval
            // shorter than a minute lands on an already stored minute and is skipped.
            log.info("Skipped duplicate: {} closeTime={} already stored", symbol, closeTime);
        }
    }
}