package com.example.p0znyaks.repository;

import com.example.p0znyaks.entity.PriceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public interface PriceRecordRepository extends JpaRepository<PriceRecord, Long> {

    PriceRecord findTopByOrderByFetchedAtDesc();

    List<PriceRecord> findBySymbolAndFetchedAtBetween(String symbol, Instant from, Instant to);

    List<PriceRecord> findBySymbolAndFetchedAtBetweenOrderByFetchedAtAsc(String symbol, Instant from, Instant to);

    Page<PriceRecord> findBySymbolAndFetchedAtBetween(String symbol, Instant from, Instant to, Pageable pageable);

    @Modifying
    @Query(value = """
            INSERT INTO price_record (symbol, price, fetched_at, close_time)
            VALUES (:symbol, :price, :fetchedAt, :closeTime)
            ON CONFLICT (symbol, close_time) DO NOTHING
            """, nativeQuery = true)
    int insertIgnoringDuplicate(@Param("symbol") String symbol,
                                @Param("price") BigDecimal price,
                                @Param("fetchedAt") Instant fetchedAt,
                                @Param("closeTime") Long closeTime);
}
