package com.example.p0znyaks.service;

import com.example.p0znyaks.dto.AveragePriceResponse;
import com.example.p0znyaks.dto.PricePageResponse;
import com.example.p0znyaks.dto.PriceRecordResponse;
import com.example.p0znyaks.dto.TrendResponse;
import com.example.p0znyaks.entity.PriceRecord;
import com.example.p0znyaks.exception.InvalidRangeException;
import com.example.p0znyaks.exception.PriceNotFoundException;
import com.example.p0znyaks.repository.PriceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PriceQueryServiceTest {

    @Mock
    private PriceRecordRepository priceRecordRepository;

    @InjectMocks
    private PriceQueryService priceQueryService;

    private PriceRecord record1;
    private PriceRecord record2;
    private PriceRecord record3;

    @BeforeEach
    void setUp() {
        record1 = new PriceRecord();
        record1.setSymbol("BNBUSDT");
        record1.setPrice(new BigDecimal("100.0"));
        record1.setFetchedAt(Instant.parse("2026-09-01T00:00:00Z"));

        record2 = new PriceRecord();
        record2.setSymbol("BNBUSDT");
        record2.setPrice(new BigDecimal("200.0"));
        record2.setFetchedAt(Instant.parse("2026-09-02T00:00:00Z"));

        record3 = new PriceRecord();
        record3.setSymbol("BNBUSDT");
        record3.setPrice(new BigDecimal("300.0"));
        record3.setFetchedAt(Instant.parse("2026-09-03T00:00:00Z"));
    }

    @Test
    void getLatest_returnsLatestRecord() {
        when(priceRecordRepository.findTopBySymbolOrderByFetchedAtDesc("BNBUSDT")).thenReturn(record3);

        PriceRecordResponse actual = priceQueryService.getLatest("BNBUSDT");

        assertThat(actual.symbol()).isEqualTo("BNBUSDT");
        assertThat(actual.price()).isEqualTo(new BigDecimal("300.0"));
        verify(priceRecordRepository).findTopBySymbolOrderByFetchedAtDesc("BNBUSDT");
    }

    @Test
    void getLatest_throwsWhenNoRecords() {
        when(priceRecordRepository.findTopBySymbolOrderByFetchedAtDesc("BNBUSDT")).thenReturn(null);

        assertThrows(PriceNotFoundException.class, () -> priceQueryService.getLatest("BNBUSDT"));
        verify(priceRecordRepository).findTopBySymbolOrderByFetchedAtDesc("BNBUSDT");
    }

    @Test
    void calculateAverage_throwsWhenRangeIsReversed() {
        Instant from = Instant.parse("2026-09-30T00:00:00Z");
        Instant to = Instant.parse("2026-09-01T00:00:00Z");

        assertThrows(InvalidRangeException.class, () -> priceQueryService.calculateAverage("BNBUSDT", from, to));
        assertThrows(InvalidRangeException.class, () -> priceQueryService.getPage("BNBUSDT", from, to, 0, 20));
        assertThrows(InvalidRangeException.class, () -> priceQueryService.calculateTrend("BNBUSDT", from, to));
    }

    @Test
    void calculateAverage_returnsAverageOfRecords() {
        when(priceRecordRepository.findAveragePrice("BNBUSDT", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z")))
                .thenReturn(new BigDecimal("200.0"));

        AveragePriceResponse actual = priceQueryService.calculateAverage("BNBUSDT", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z"));

        assertThat(actual.averagePrice()).isEqualByComparingTo(new BigDecimal("200.0"));
    }

    @Test
    void calculateAverage_throwsWhenNoRecords() {
        when(priceRecordRepository.findAveragePrice("BNBUSDT", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z")))
                .thenReturn(null);

        assertThrows(PriceNotFoundException.class, () -> priceQueryService.calculateAverage("BNBUSDT", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z")));
    }

    @Test
    void getPage_returnsPageWithItems() {
        Page<PriceRecord> page = new PageImpl<>(List.of(record1, record2),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "fetchedAt")), 2);

        when(priceRecordRepository.findBySymbolAndFetchedAtBetween("BNBUSDT", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z"), PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "fetchedAt"))))
                .thenReturn(page);

        PricePageResponse actual = priceQueryService.getPage("BNBUSDT", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z"), 0, 20);

        assertThat(actual.items()).hasSize(2);
        assertThat(actual.items().get(0).price()).isEqualTo(new BigDecimal("100.0"));
        assertThat(actual.page()).isEqualTo(0);
        assertThat(actual.size()).isEqualTo(20);
        assertThat(actual.totalElements()).isEqualTo(2);
        assertThat(actual.hasNext()).isFalse();
    }


    @Test
    void calculateTrend_returnsUpwardWhenPriceRises() {
        when(priceRecordRepository.findTopBySymbolAndFetchedAtBetweenOrderByFetchedAtAsc("BNBUSDT", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z")))
                .thenReturn(record1);
        when(priceRecordRepository.findTopBySymbolAndFetchedAtBetweenOrderByFetchedAtDesc("BNBUSDT", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z")))
                .thenReturn(record3);

        TrendResponse actual = priceQueryService.calculateTrend("BNBUSDT", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z"));

        assertThat(actual.symbol()).isEqualTo("BNBUSDT");
        assertThat(actual.trend()).isEqualTo(TrendResponse.TREND_UPWARD);
        assertThat(actual.startDate()).isEqualTo(LocalDate.of(2026, 9, 1));
        assertThat(actual.endDate()).isEqualTo(LocalDate.of(2026, 9, 30));
    }

    @Test
    void calculateTrend_returnsDownwardWhenPriceFalls() {
        when(priceRecordRepository.findTopBySymbolAndFetchedAtBetweenOrderByFetchedAtAsc("BNBUSDT", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z")))
                .thenReturn(record3);
        when(priceRecordRepository.findTopBySymbolAndFetchedAtBetweenOrderByFetchedAtDesc("BNBUSDT", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z")))
                .thenReturn(record1);

        TrendResponse actual = priceQueryService.calculateTrend("BNBUSDT", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z"));

        assertThat(actual.trend()).isEqualTo(TrendResponse.TREND_DOWNWARD);
    }

    @Test
    void calculateTrend_throwsWhenNoRecords() {
        when(priceRecordRepository.findTopBySymbolAndFetchedAtBetweenOrderByFetchedAtAsc("BNBUSDT", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z")))
                .thenReturn(null);
        when(priceRecordRepository.findTopBySymbolAndFetchedAtBetweenOrderByFetchedAtDesc("BNBUSDT", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z")))
                .thenReturn(null);

        assertThrows(PriceNotFoundException.class, () -> priceQueryService.calculateTrend("BNBUSDT", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z")));
    }

    @Test
    void calculateTrend_returnsFlatWhenChangeIsBelowThreshold() {
        PriceRecord low = new PriceRecord();
        low.setSymbol("BNBUSDT");
        low.setPrice(new BigDecimal("100.0"));
        low.setFetchedAt(Instant.parse("2026-09-01T00:00:00Z"));

        PriceRecord high = new PriceRecord();
        high.setSymbol("BNBUSDT");
        high.setPrice(new BigDecimal("100.0005"));
        high.setFetchedAt(Instant.parse("2026-09-02T00:00:00Z"));

        when(priceRecordRepository.findTopBySymbolAndFetchedAtBetweenOrderByFetchedAtAsc("BNBUSDT", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z")))
                .thenReturn(low);
        when(priceRecordRepository.findTopBySymbolAndFetchedAtBetweenOrderByFetchedAtDesc("BNBUSDT", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z")))
                .thenReturn(high);

        TrendResponse actual = priceQueryService.calculateTrend("BNBUSDT", Instant.parse("2026-09-01T00:00:00Z"), Instant.parse("2026-09-30T00:00:00Z"));

        assertThat(actual.trend()).isEqualTo(TrendResponse.TREND_FLAT);
    }
}

