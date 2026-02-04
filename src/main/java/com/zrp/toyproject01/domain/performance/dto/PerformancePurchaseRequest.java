package com.zrp.toyproject01.domain.performance.dto;

import jakarta.validation.constraints.Positive;

public record PerformancePurchaseRequest(
    @Positive(message = "예매 수량은 1장 이상이어야 합니다.")
    int quantity
) {}
