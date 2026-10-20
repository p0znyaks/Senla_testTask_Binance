package com.example.p0znyaks.controller;

import com.example.p0znyaks.config.BinanceProperties;
import com.example.p0znyaks.dto.AveragePriceResponse;
import com.example.p0znyaks.dto.PricePageResponse;
import com.example.p0znyaks.dto.PriceRangeRequest;
import com.example.p0znyaks.dto.PriceRecordResponse;
import com.example.p0znyaks.dto.TrendResponse;
import com.example.p0znyaks.service.PriceQueryService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/prices")
@Validated
@RequiredArgsConstructor
public class PriceRecordController {

    private final PriceQueryService priceQueryService;
    private final BinanceProperties binanceProperties;

    @GetMapping("/latest")
    public PriceRecordResponse getLatest() {
        return priceQueryService.getLatest(binanceProperties.getSymbol());
    }

    @GetMapping("/average")
    public AveragePriceResponse getAverage(@Valid PriceRangeRequest range) {
        return priceQueryService.calculateAverage(binanceProperties.getSymbol(), range.from(), range.to());
    }

    @GetMapping
    public PricePageResponse getPage(@Valid PriceRangeRequest range,
                                     @RequestParam(defaultValue = "0") @Min(0) int page,
                                     @RequestParam(defaultValue = "20") @Min(1) @Max(200) int size) {
        return priceQueryService.getPage(binanceProperties.getSymbol(), range.from(), range.to(), page, size);
    }

    @GetMapping("/trend")
    public TrendResponse getTrend(@Valid PriceRangeRequest range) {
        return priceQueryService.calculateTrend(binanceProperties.getSymbol(), range.from(), range.to());
    }
}
