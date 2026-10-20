package com.example.p0znyaks;

import com.example.p0znyaks.entity.PriceRecord;
import com.example.p0znyaks.repository.PriceRecordRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigDecimal;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = "binance.scheduler.enabled=false")
@AutoConfigureMockMvc
@ExtendWith(PostgresTestContainer.DockerAvailableCondition.class)
class PriceRecordControllerIntegrationTest {

    static PostgreSQLContainer<?> postgres;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PriceRecordRepository priceRecordRepository;

    @DynamicPropertySource
    static void springProperties(DynamicPropertyRegistry registry) {
        postgres = PostgresTestContainer.getInstance();
        registry.add("spring.datasource.url", () -> postgres.getJdbcUrl());
        registry.add("spring.datasource.username", () -> postgres.getUsername());
        registry.add("spring.datasource.password", () -> postgres.getPassword());
        registry.add("binance.poll-interval", () -> "PT10M");
        registry.add("binance.scheduler.enabled", () -> "false");
    }

    @BeforeEach
    void setUp() {
        priceRecordRepository.deleteAll();
    }

    private PriceRecord buildRecord(Instant fetchedAt, BigDecimal price) {
        PriceRecord record = new PriceRecord();
        record.setSymbol("BNBUSDT");
        record.setPrice(price);
        record.setFetchedAt(fetchedAt);
        record.setCloseTime(fetchedAt.toEpochMilli());
        return record;
    }

    @Test
    void latest_returnsLatestRecord() throws Exception {
        priceRecordRepository.save(buildRecord(Instant.parse("2026-09-01T00:00:00Z"), new BigDecimal("100.0")));
        priceRecordRepository.save(buildRecord(Instant.parse("2026-09-02T00:00:00Z"), new BigDecimal("200.0")));
        priceRecordRepository.save(buildRecord(Instant.parse("2026-09-03T00:00:00Z"), new BigDecimal("300.0")));

        mockMvc.perform(get("/api/v1/prices/latest"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BNBUSDT"))
                .andExpect(jsonPath("$.price").value("300.0"))
                .andExpect(jsonPath("$.fetchedAt").exists());
    }

    @Test
    void average_returnsAveragePrice() throws Exception {
        priceRecordRepository.save(buildRecord(Instant.parse("2026-09-01T00:00:00Z"), new BigDecimal("100.0")));
        priceRecordRepository.save(buildRecord(Instant.parse("2026-09-02T00:00:00Z"), new BigDecimal("200.0")));
        priceRecordRepository.save(buildRecord(Instant.parse("2026-09-03T00:00:00Z"), new BigDecimal("300.0")));

        mockMvc.perform(get("/api/v1/prices/average")
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-30T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averagePrice").value("200.0"));
    }

    @Test
    void page_returnsPaginatedItems() throws Exception {
        priceRecordRepository.save(buildRecord(Instant.parse("2026-09-01T00:00:00Z"), new BigDecimal("100.0")));
        priceRecordRepository.save(buildRecord(Instant.parse("2026-09-02T00:00:00Z"), new BigDecimal("200.0")));
        priceRecordRepository.save(buildRecord(Instant.parse("2026-09-03T00:00:00Z"), new BigDecimal("300.0")));

        mockMvc.perform(get("/api/v1/prices")
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-30T00:00:00Z")
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.hasNext").value(true));
    }

    @Test
    void trend_returnsUpward() throws Exception {
        priceRecordRepository.save(buildRecord(Instant.parse("2026-09-01T00:00:00Z"), new BigDecimal("100.0")));
        priceRecordRepository.save(buildRecord(Instant.parse("2026-09-02T00:00:00Z"), new BigDecimal("200.0")));
        priceRecordRepository.save(buildRecord(Instant.parse("2026-09-03T00:00:00Z"), new BigDecimal("300.0")));

        mockMvc.perform(get("/api/v1/prices/trend")
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-30T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BNBUSDT"))
                .andExpect(jsonPath("$.trend").value("UPWARD"))
                .andExpect(jsonPath("$.start_date").value("2026-09-01"))
                .andExpect(jsonPath("$.end_date").value("2026-09-30"));
    }

    @Test
    void average_returnsNotFoundWhenNoRecordsInRange() throws Exception {
        priceRecordRepository.save(buildRecord(Instant.parse("2026-09-01T00:00:00Z"), new BigDecimal("100.0")));

        mockMvc.perform(get("/api/v1/prices/average")
                        .param("from", "2026-10-01T00:00:00Z")
                        .param("to", "2026-10-30T00:00:00Z"))
                .andExpect(status().isNotFound());
    }

    @Test
    void average_returnsBadRequestWhenFromIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/prices/average")
                        .param("to", "2026-09-30T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void average_returnsBadRequestWhenToIsMissing() throws Exception {
        mockMvc.perform(get("/api/v1/prices/average")
                        .param("from", "2026-09-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void page_returnsBadRequestWhenRangeIsReversed() throws Exception {
        mockMvc.perform(get("/api/v1/prices")
                        .param("from", "2026-09-30T00:00:00Z")
                        .param("to", "2026-09-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void trend_returnsBadRequestWhenRangeIsReversed() throws Exception {
        mockMvc.perform(get("/api/v1/prices/trend")
                        .param("from", "2026-09-30T00:00:00Z")
                        .param("to", "2026-09-01T00:00:00Z"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void page_returnsBadRequestForNegativePage() throws Exception {
        mockMvc.perform(get("/api/v1/prices")
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-30T00:00:00Z")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void page_returnsBadRequestForZeroSize() throws Exception {
        mockMvc.perform(get("/api/v1/prices")
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-30T00:00:00Z")
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void page_returnsBadRequestForTooLargeSize() throws Exception {
        mockMvc.perform(get("/api/v1/prices")
                        .param("from", "2026-09-01T00:00:00Z")
                        .param("to", "2026-09-30T00:00:00Z")
                        .param("size", "100000"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    void page_returnsEmptyItemsWhenRangeHasNoRecords() throws Exception {
        mockMvc.perform(get("/api/v1/prices")
                        .param("from", "2026-10-01T00:00:00Z")
                        .param("to", "2026-10-30T00:00:00Z"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.hasNext").value(false));
    }

    @Test
    void trend_returnsNotFoundWhenNoRecordsInRange() throws Exception {
        priceRecordRepository.save(buildRecord(Instant.parse("2026-09-01T00:00:00Z"), new BigDecimal("100.0")));

        mockMvc.perform(get("/api/v1/prices/trend")
                        .param("from", "2026-10-01T00:00:00Z")
                        .param("to", "2026-10-30T00:00:00Z"))
                .andExpect(status().isNotFound());
    }
}
