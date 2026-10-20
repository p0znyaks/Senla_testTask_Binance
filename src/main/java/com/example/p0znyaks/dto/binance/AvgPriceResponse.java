package com.example.p0znyaks.dto.binance;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AvgPriceResponse(BigDecimal price, Long closeTime) {
}