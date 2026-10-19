package com.example.p0znyaks.dto;

import java.math.BigDecimal;

public record AvgPriceResponse(int mins, BigDecimal price, long closeTime) {}