package com.example.p0znyaks.service;

import com.example.p0znyaks.client.BinanceClient;
import com.example.p0znyaks.config.BinanceProperties;
import com.example.p0znyaks.dto.AvgPriceResponse;
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

        int insertedRows = priceRecordRepository.insertIgnoringDuplicate(
                symbol,
                avgPrice.price(),
                Instant.now(),
                avgPrice.closeTime());

        if (insertedRows > 0) {
            log.info("Fetched {} price: {} (closeTime={})", symbol, avgPrice.price(), avgPrice.closeTime());
        } else {
            log.debug("Skipped duplicate: {} closeTime={} already stored", symbol, avgPrice.closeTime());
        }
    }
}