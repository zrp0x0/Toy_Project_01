package com.zrp.toyproject01.domain.performance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record PerformanceRegisterRequest(
    @NotBlank(message = "공연명은 필수입니다.")
    String name,

    @Positive(message = "가격은 0원보다 커야 합니다.")
    int price,

    @Positive(message = "좌석 수는 1개 이상이어야합니다.")
    int maxStock
) {}
