package com.zrp.toyproject01.domain.performance;


import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.zrp.toyproject01.domain.performance.application.OptimisticLockPerformanceFacade;
import com.zrp.toyproject01.domain.performance.application.PerformanceService;
import com.zrp.toyproject01.domain.performance.application.RedissonLockPerformanceFacade;
import com.zrp.toyproject01.domain.performance.dao.PerformanceRepository;
import com.zrp.toyproject01.domain.performance.domain.Performance;
import com.zrp.toyproject01.domain.performance.dto.PerformanceRegisterRequest;

@SpringBootTest
class PerformanceServiceTest {

    @Autowired
    private PerformanceService performanceService;

    @Autowired
    private PerformanceRepository performanceRepository;

    @Autowired
    private OptimisticLockPerformanceFacade performanceFacade;

    @Autowired
    private RedissonLockPerformanceFacade redissonLockPerformanceFacade;

    // 테스트가 끝날 때마다 실행되는 청소부 🧹
    @AfterEach
    void tearDown() {
        performanceRepository.deleteAll();
    }

    @Test
    @DisplayName("동시에 100명이 1개씩 예매하면 재고가 0이 되어야 한다")
    void concurrency_test_100_users() throws InterruptedException {
        // 1. Given: 100개의 재고를 가진 공연 생성
        int initialStock = 100;
        PerformanceRegisterRequest request = new PerformanceRegisterRequest("아이유 콘서트", 100000, initialStock);
        Long performanceId = performanceService.register(request);

        // 2. When: 100명의 사용자가 동시에 1개씩 주문
        int threadCount = 100;
        
        // ExecutorService: 비동기 작업을 단순하게 처리해주는 자바의 스레드 관리 도구 - 이거 모름
        ExecutorService executorService = Executors.newFixedThreadPool(32); 
        
        // CountDownLatch: 100개의 요청이 끝날 때까지 기다리게 해주는 도구
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    performanceService.purchase(performanceId, 1, " ");
                } finally {
                    latch.countDown(); // 작업 하나 끝나면 카운트 감소
                }
            });
        }

        latch.await(); // 100개 작업이 모두 끝날 때까지 대기

        // 3. Then: 재고 확인
        Performance performance = performanceRepository.findById(performanceId).orElseThrow();
        
        // 로그로 결과 출력
        System.out.println("=========================================");
        System.out.println("기대 재고: 0");
        System.out.println("실제 재고: " + performance.getStock());
        System.out.println("=========================================");

        // [중요] 우리는 이 테스트가 '실패'할 것을 알고 있습니다.
        // 동시성 제어가 안 되어 있다면 재고는 0이 아닐 것입니다.
        // 따라서 현재 상태에서는 0이 아니어야(실패해야) 정상입니다.
        assertNotEquals(0, performance.getStock());
    }

    @Test
    @DisplayName("비관적 락 적용: 동시에 100명이 1개씩 예매하면 재고가 0이 되어야 한다")
    void concurrency_test_with_pessimistic_lock() throws InterruptedException {
        // 1. Given (초기 재고 100개)
        int initialStock = 100;
        PerformanceRegisterRequest request = new PerformanceRegisterRequest("아이유 콘서트", 100000, initialStock);
        Long performanceId = performanceService.register(request);

        // 2. When (100명 동시 요청)
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    performanceService.purchase(performanceId, 1, " ");
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드가 끝날 때까지 대기

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 3. Then (검증)
        Performance performance = performanceRepository.findById(performanceId).orElseThrow();
        
        System.out.println("최종 재고: " + performance.getStock());
        System.out.println("총 소요 시간: " + duration + "ms");
        
        // [핵심] 이제는 0이 되어야만 성공입니다!
        assertEquals(0, performance.getStock());
    }

    @Test
    @DisplayName("낙관적 락 적용: 동시에 100명이 1개씩 예매하면 재고가 0이 되어야 한다")
    void concurrency_test_with_optimistic_lock() throws InterruptedException {
        // 1. Given (초기 재고 100개)
        int initialStock = 100;
        PerformanceRegisterRequest request = new PerformanceRegisterRequest("아이유 콘서트", 100000, initialStock);
        Long performanceId = performanceService.register(request);

        // 2. When (100명 동시 요청)
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    performanceFacade.purchase(performanceId, 1, " ");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드가 끝날 때까지 대기

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 3. Then (검증)
        Performance performance = performanceRepository.findById(performanceId).orElseThrow();
        
        System.out.println("최종 재고: " + performance.getStock());
        System.out.println("총 소요 시간: " + duration + "ms");
        
        // [핵심] 이제는 0이 되어야만 성공입니다!
        assertEquals(0, performance.getStock());
    }


    @Test
    @DisplayName("Redis 락 적용: 동시에 100명이 1개씩 예매하면 재고가 0이 되어야 한다")
    void concurrency_test_with_redissonLock_lock() throws InterruptedException {
        // 1. Given (초기 재고 100개)
        int initialStock = 100;
        PerformanceRegisterRequest request = new PerformanceRegisterRequest("아이유 콘서트", 100000, initialStock);
        Long performanceId = performanceService.register(request);

        // 2. When (100명 동시 요청)
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        long startTime = System.currentTimeMillis();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    redissonLockPerformanceFacade.purchase(performanceId, 1, " ");
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(); // 모든 스레드가 끝날 때까지 대기

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // 3. Then (검증)
        Performance performance = performanceRepository.findById(performanceId).orElseThrow();
        
        System.out.println("최종 재고: " + performance.getStock());
        System.out.println("총 소요 시간: " + duration + "ms");
        
        // [핵심] 이제는 0이 되어야만 성공입니다!
        assertEquals(0, performance.getStock());
    }
}
