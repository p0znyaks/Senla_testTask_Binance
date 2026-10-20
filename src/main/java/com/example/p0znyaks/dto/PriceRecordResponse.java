package com.example.p0znyaks.dto;

import com.example.p0znyaks.entity.PriceRecord;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * @param fetchedAt moment the price was received from Binance, ISO-8601 in UTC
 */
public record PriceRecordResponse(String symbol, BigDecimal price,
                                   @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "uuuu-MM-dd'T'HH:mm:ss'Z'", timezone = "UTC")
                                   Instant fetchedAt) {

    public static PriceRecordResponse from(PriceRecord record) {
        return new PriceRecordResponse(record.getSymbol(), record.getPrice(), record.getFetchedAt());
    }
}