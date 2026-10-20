package com.example.p0znyaks.dto;

import java.util.List;

public record PricePageResponse(
        List<PriceRecordResponse> items,
        int page,
        int size,
        long totalElements,
        boolean hasNext
) {}