package com.example.p0znyaks.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record TrendResponse(
        String symbol,
        @JsonProperty("start_date") LocalDate startDate,
        @JsonProperty("end_date") LocalDate endDate,
        String trend
) {
    public static final String TREND_UPWARD = "UPWARD";
    public static final String TREND_DOWNWARD = "DOWNWARD";
    public static final String TREND_FLAT = "FLAT";
}