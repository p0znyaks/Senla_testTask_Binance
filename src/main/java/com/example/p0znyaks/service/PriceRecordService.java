package com.example.p0znyaks.service;

import com.example.p0znyaks.entity.PriceRecord;
import com.example.p0znyaks.repository.PriceRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PriceRecordService {
    private final PriceRecordRepository priceRecordRepository;

    @Transactional(readOnly = true)
    public PriceRecord read() {
        return priceRecordRepository.findTopByOrderByFetchedAtDesc();
    }
}
