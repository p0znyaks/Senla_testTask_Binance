package com.example.p0znyaks.controller;

import com.example.p0znyaks.dto.AveragePriceResponse;
import com.example.p0znyaks.dto.PricePageResponse;
import com.example.p0znyaks.dto.PriceRecordResponse;
import com.example.p0znyaks.dto.TrendResponse;
import com.example.p0znyaks.config.BinanceProperties;
import com.example.p0znyaks.entity.PriceRecord;
import com.example.p0znyaks.service.PriceQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/prices")
@RequiredArgsConstructor
public class PriceRecordController {

    private final PriceQueryService priceQueryService;
    private final BinanceProperties binanceProperties;

    @GetMapping("/latest")
    public PriceRecordResponse getLatest() {
        PriceRecord latest = priceQueryService.findLatestOrThrow();
        return new PriceRecordResponse(latest.getSymbol(), latest.getPrice(), latest.getFetchedAt());
    }

    @GetMapping("/average")
    public AveragePriceResponse getAverage(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return priceQueryService.calculateAverage(binanceProperties.getSymbol(), from, to);
    }

    @GetMapping
    public PricePageResponse getPage(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return priceQueryService.getPage(binanceProperties.getSymbol(), from, to, page, size);
    }

    @GetMapping("/trend")
    public TrendResponse getTrend(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return priceQueryService.calculateTrend(binanceProperties.getSymbol(), from, to);
    }
}
