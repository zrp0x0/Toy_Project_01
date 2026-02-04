package com.zrp.toyproject01.domain.performance.dto;

import com.zrp.toyproject01.domain.performance.domain.Performance;

public record PerformanceResponse(
    Long id,
    String name,
    int price,
    int maxStock,
    int stock
) {
    public static PerformanceResponse from(Performance entity) {
        return new PerformanceResponse(
            entity.getId(),
            entity.getName(),
            entity.getPrice(),
            entity.getMaxStock(),
            entity.getStock()
        );
    }
}
