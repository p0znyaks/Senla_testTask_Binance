package com.example.p0znyaks.service;

import com.example.p0znyaks.dto.AveragePriceResponse;
import com.example.p0znyaks.dto.PricePageResponse;
import com.example.p0znyaks.dto.PriceRecordResponse;
import com.example.p0znyaks.dto.TrendResponse;
import com.example.p0znyaks.entity.PriceRecord;
import com.example.p0znyaks.exception.InvalidRangeException;
import com.example.p0znyaks.exception.PriceNotFoundException;
import com.example.p0znyaks.repository.PriceRecordRepository;
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
    public PriceRecordResponse getLatest(String symbol) {
        PriceRecord latest = priceRecordRepository.findTopBySymbolOrderByFetchedAtDesc(symbol);
        if (latest == null) {
            throw new PriceNotFoundException("No price records available for " + symbol);
        }
        return PriceRecordResponse.from(latest);
    }

    @Transactional(readOnly = true)
    public AveragePriceResponse calculateAverage(String symbol, Instant from, Instant to) {
        validateRange(from, to);
        BigDecimal average = priceRecordRepository.findAveragePrice(symbol, from, to);
        if (average == null) {
            throw new PriceNotFoundException(
                    "No records for symbol %s in range [%s, %s]".formatted(symbol, from, to));
        }
        return new AveragePriceResponse(average.setScale(AVERAGE_SCALE, RoundingMode.HALF_UP));
    }

    @Transactional(readOnly = true)
    public PricePageResponse getPage(String symbol, Instant from, Instant to, int page, int size) {
        validateRange(from, to);
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "fetchedAt"));
        Page<PriceRecord> pageData = priceRecordRepository.findBySymbolAndFetchedAtBetween(symbol, from, to, pageable);
        List<PriceRecordResponse> items = pageData.getContent().stream()
                .map(PriceRecordResponse::from)
                .toList();
        return new PricePageResponse(items, pageData.getNumber(), pageData.getSize(),
                pageData.getTotalElements(), pageData.hasNext());
    }

    @Transactional(readOnly = true)
    public TrendResponse calculateTrend(String symbol, Instant from, Instant to) {
        validateRange(from, to);
        PriceRecord first = priceRecordRepository
                .findTopBySymbolAndFetchedAtBetweenOrderByFetchedAtAsc(symbol, from, to);
        PriceRecord last = priceRecordRepository
                .findTopBySymbolAndFetchedAtBetweenOrderByFetchedAtDesc(symbol, from, to);
        if (first == null || last == null) {
            throw new PriceNotFoundException(
                    "No records for symbol %s in range [%s, %s]".formatted(symbol, from, to));
        }
        return new TrendResponse(symbol,
                LocalDate.ofInstant(from, ZoneOffset.UTC),
                LocalDate.ofInstant(to, ZoneOffset.UTC),
                resolveTrend(first.getPrice(), last.getPrice()));
    }

    /**
     * A range must start before it ends, otherwise the query would match nothing.
     */
    private static void validateRange(Instant from, Instant to) {
        if (from.isAfter(to)) {
            throw new InvalidRangeException("`from` must not be after `to`");
        }
    }

    private static String resolveTrend(BigDecimal startPrice, BigDecimal endPrice) {
        BigDecimal diff = endPrice.subtract(startPrice);
        BigDecimal relative = startPrice.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : diff.abs().divide(startPrice, 6, RoundingMode.HALF_UP);
        if (relative.compareTo(FLAT_THRESHOLD) < 0) {
            return TrendResponse.TREND_FLAT;
        }
        return diff.compareTo(BigDecimal.ZERO) > 0
                ? TrendResponse.TREND_UPWARD
                : TrendResponse.TREND_DOWNWARD;
    }
}