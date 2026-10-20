package com.example.p0znyaks.dto.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Response of the Binance {@code /api/v3/avgPrice} endpoint.
 *
 * <p>Unknown fields are ignored so that Binance can add attributes without breaking the poll.
 *
 * @param price     average price of the pair
 * @param closeTime close time of the last minute the price belongs to, {@code null} if Binance did not send it
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record AvgPriceResponse(BigDecimal price, Long closeTime) {
}