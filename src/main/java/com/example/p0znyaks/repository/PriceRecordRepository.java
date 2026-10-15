package com.example.p0znyaks.repository;

import com.example.p0znyaks.entity.PriceRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceRecordRepository extends JpaRepository<PriceRecord, Long> {
    PriceRecord findTopByOrderByFetchedAtDesc();
}
