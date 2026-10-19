package com.example.p0znyaks.service;

import com.example.p0znyaks.dto.AveragePriceResponse;
import com.example.p0znyaks.dto.PriceItemResponse;
import com.example.p0znyaks.dto.PricePageResponse;
import com.example.p0znyaks.dto.TrendResponse;
import com.example.p0znyaks.entity.PriceRecord;
import com.example.p0znyaks.exception.PriceNotFoundException;
import com.example.p0znyaks.repository.PriceRecordRepository;
import jakarta.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PriceQueryService {

    private static final BigDecimal FLAT_THRESHOLD = BigDecimal.valueOf(0.001);
    private static final int AVERAGE_SCALE = 10;

    private final PriceRecordRepository priceRecordRepository;

    @Transactional(readOnly = true)
    public PriceRecord findLatestOrThrow() {
        PriceRecord latest = priceRecordRepository.findTopByOrderByFetchedAtDesc();
        if (latest == null) {
            throw new PriceNotFoundException("No price records available");
        }
        return latest;
    }

    @Transactional(readOnly = true)
    public AveragePriceResponse calculateAverage(String symbol, Instant from, Instant to) {
        List<PriceRecord> records = priceRecordRepository.findBySymbolAndFetchedAtBetween(symbol, from, to);
        if (records.isEmpty()) {
            return new AveragePriceResponse(BigDecimal.ZERO);
        }
        BigDecimal sum = records.stream()
                .map(PriceRecord::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = sum.divide(BigDecimal.valueOf(records.size()), AVERAGE_SCALE, RoundingMode.HALF_UP);
        return new AveragePriceResponse(average);
    }

    @Transactional(readOnly = true)
    public PricePageResponse getPage(String symbol, Instant from, Instant to, int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fetchedAt"));
        Page<PriceRecord> pageData = priceRecordRepository.findBySymbolAndFetchedAtBetween(symbol, from, to, pageable);
        List<PriceItemResponse> items = pageData.getContent().stream()
                .map(r -> new PriceItemResponse(r.getSymbol(), r.getPrice(), r.getFetchedAt()))
                .toList();
        return new PricePageResponse(items, pageData.getNumber(), pageData.getSize(),
                pageData.getTotalElements(), pageData.hasNext());
    }

    @Transactional(readOnly = true)
    public TrendResponse calculateTrend(String symbol, Instant from, Instant to) {
        List<PriceRecord> records = priceRecordRepository
                .findBySymbolAndFetchedAtBetweenOrderByFetchedAtAsc(symbol, from, to);
        if (records.isEmpty()) {
            throw new PriceNotFoundException(
                    "No records for symbol %s in range [%s, %s]".formatted(symbol, from, to));
        }
        String trend = getString(records);
        return new TrendResponse(symbol,
                LocalDate.ofInstant(from, ZoneOffset.UTC),
                LocalDate.ofInstant(to, ZoneOffset.UTC),
                trend);
    }

    @Nonnull
    private static String getString(List<PriceRecord> records) {
        BigDecimal startPrice = records.get(0).getPrice();
        BigDecimal endPrice = records.get(records.size() - 1).getPrice();
        BigDecimal diff = endPrice.subtract(startPrice);
        BigDecimal relative = startPrice.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : diff.abs().divide(startPrice, 6, RoundingMode.HALF_UP);
        String trend;
        if (relative.compareTo(FLAT_THRESHOLD) < 0) {
            trend = TrendResponse.TREND_FLAT;
        } else if (diff.compareTo(BigDecimal.ZERO) > 0) {
            trend = TrendResponse.TREND_UPWARD;
        } else {
            trend = TrendResponse.TREND_DOWNWARD;
        }
        return trend;
    }
}