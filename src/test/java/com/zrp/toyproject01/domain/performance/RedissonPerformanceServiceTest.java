package com.zrp.toyproject01.domain.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.zrp.toyproject01.domain.account.dao.RefreshTokenRepository;
import com.zrp.toyproject01.domain.account.dao.UserRepository;
import com.zrp.toyproject01.domain.account.domain.User;
import com.zrp.toyproject01.domain.performance.application.PerformanceService;
import com.zrp.toyproject01.domain.performance.application.RedissonLockPerformanceFacade;
import com.zrp.toyproject01.domain.performance.dao.PerformanceRepository;
import com.zrp.toyproject01.domain.performance.domain.Performance;
import com.zrp.toyproject01.domain.performance.dto.PerformanceRegisterRequest;
import com.zrp.toyproject01.domain.post.dao.PostRepository;
import com.zrp.toyproject01.domain.reservation.dao.ReservationRepository;

@SpringBootTest
class RedissonPerformanceServiceTest {

    @Autowired private PerformanceService performanceService;
    @Autowired private PerformanceRepository performanceRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private RedissonLockPerformanceFacade redissonLockPerformanceFacade;
    @Autowired private PostRepository postRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;

    @BeforeEach
    void cleanUp() {
        tearDown();
    }

    @AfterEach
    void tearDown() {
        // [삭제 순서가 생명입니다!]
        // 자식들(참조하는 놈들)을 먼저 싹 지워야 합니다.
        
        refreshTokenRepository.deleteAll(); // 1. 🔑 리프레시 토큰 삭제 (범인 검거)
        reservationRepository.deleteAll();  // 2. 예약 내역 삭제
        postRepository.deleteAll();         // 3. 게시글 삭제
        
        // 자식들이 다 사라졌으니 부모 삭제 가능
        performanceRepository.deleteAll();  // 4. 공연 삭제
        userRepository.deleteAll();         // 5. 유저 삭제 (대장)
    }

    @Test
    @DisplayName("종합 테스트: 100명이 예매하면 재고 0 & 예약 내역 100개 생성")
    void concurrency_test_complete() throws InterruptedException {
        // 1. Given: 공연 생성 (재고 100)
        Long performanceId = performanceService.register(
            new PerformanceRegisterRequest("아이유 콘서트", 100000, 100)
        );

        // 2. Given: 유저 100명 생성 (tester1 ~ tester100)
        // 람다를 이용해 빠르게 생성합니다.
        IntStream.range(1, 101).forEach(i -> {
            userRepository.save(User.create("tester" + i + "@example.com", "1234", "tester" + i));
        });

        // 3. When: 100명이 동시에 요청
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int i = 1; i <= threadCount; i++) {
            String email = "tester" + i + "@example.com";
            executorService.submit(() -> {
                try {
                    // 각자 자기 이메일로 예매 시도
                    redissonLockPerformanceFacade.purchase(performanceId, 1, email);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        // 4. Then
        Performance performance = performanceRepository.findById(performanceId).orElseThrow();
        long reservationCount = reservationRepository.count();

        System.out.println("=========================================");
        System.out.println("🎉 [최종 검증]");
        System.out.println("남은 재고: " + performance.getStock());
        System.out.println("생성된 예약 수: " + reservationCount);
        System.out.println("=========================================");

        assertEquals(0, performance.getStock());
        assertEquals(100, reservationCount);
    }
}