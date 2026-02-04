package com.zrp.toyproject01.domain.performance.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zrp.toyproject01.domain.performance.application.PerformanceService;
import com.zrp.toyproject01.domain.performance.dto.PerformancePurchaseRequest;
import com.zrp.toyproject01.domain.performance.dto.PerformanceRegisterRequest;
import com.zrp.toyproject01.domain.performance.dto.PerformanceResponse;
import com.zrp.toyproject01.global.common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/performances")
@RequiredArgsConstructor
public class PerformanceController {
    
    private final PerformanceService performanceService;


    // 공연 등록
    @PostMapping // 관리자 기능이지만 일단 오픈
    public ApiResponse<Long> register(
        @RequestBody @Valid PerformanceRegisterRequest request
    ) {
        return ApiResponse.ok(performanceService.register(request));
    }

    // 공연 목록 조회
    @GetMapping
    public ApiResponse<List<PerformanceResponse>> findAll() {
        return ApiResponse.ok(performanceService.findAll());
    }

    // 공연 예매 (핵심 동시성 타켓)
    @PostMapping("/{id}/purchase")
    public ApiResponse<Void> purchase(
        @PathVariable Long id,
        @RequestBody @Valid PerformancePurchaseRequest request
    ) {
        // 원래는 SecurityUtil.getCurrentUserEmail()로 유저 정보도 넘겨야하지만
        // 지금은 동시성 테스트가 목적이므로 재고 감소에만 집중
        performanceService.purchase(id, request.quantity());
        return ApiResponse.ok();
    }

}
