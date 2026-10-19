package com.example.p0znyaks.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceItemResponse(String symbol, BigDecimal price, Instant fetchedAt) {}