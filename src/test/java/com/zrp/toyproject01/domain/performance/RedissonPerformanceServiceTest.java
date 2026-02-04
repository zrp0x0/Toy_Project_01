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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.transaction.annotation.Transactional;

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
import jakarta.persistence.EntityManager;

@SpringBootTest
class RedissonPerformanceServiceTest {

    @Autowired private PerformanceService performanceService;
    @Autowired private PerformanceRepository performanceRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ReservationRepository reservationRepository;
    @Autowired private RedissonLockPerformanceFacade redissonLockPerformanceFacade;
    @Autowired private PostRepository postRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private StringRedisTemplate redisTemplate;
    @Autowired private EntityManager em;

    @BeforeEach
    void cleanUp() {
        tearDown();
         // ✨ [추가] Redis에 저장된 캐시도 싹 날리고 시작!
        if (redisTemplate != null) {
            redisTemplate.getConnectionFactory().getConnection().serverCommands().flushAll();
        }
        tearDown();
    }

    // @AfterEach
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

    @Test
    @DisplayName("캐싱 적용: 두 번째 조회부터는 조회 속도가 압도적으로 빨라야 한다")
    void caching_performance_test() {
        // 1. 데이터 준비 (공연 5개 등록)
        for (int i = 0; i < 5; i++) {
            performanceService.register(new PerformanceRegisterRequest("공연 " + i, 10000, 100));
        }

        // 2. 첫 번째 조회 (DB 조회 - Cache Miss)
        long start1 = System.currentTimeMillis();
        performanceService.findAll();
        long end1 = System.currentTimeMillis();
        System.out.println("1차 조회 시간 (DB): " + (end1 - start1) + "ms");

        // 3. 두 번째 조회 (Redis 조회 - Cache Hit)
        long start2 = System.currentTimeMillis();
        performanceService.findAll();
        long end2 = System.currentTimeMillis();
        System.out.println("2차 조회 시간 (Redis): " + (end2 - start2) + "ms");

        long start3 = System.currentTimeMillis();
        performanceService.findAll();
        long end3 = System.currentTimeMillis();
        System.out.println("3차 조회 시간 (Redis): " + (end3 - start3) + "ms");

        long start4 = System.currentTimeMillis();
        performanceService.findAll();
        long end4 = System.currentTimeMillis();
        System.out.println("3차 조회 시간 (Redis): " + (end4 - start4) + "ms");

        // 4. 검증 (2차 조회가 훨씬 빨라야 함)
        // (로컬 환경이라 아주 큰 차이는 안 날 수 있지만, 로그에 "DB 조회 중..."이 안 찍혀야 함)
    }

    @Test
    @Transactional // [추가] flush와 clear를 쓰기 위해 반드시 필요합니다!
    @DisplayName("DB 버퍼 풀 테스트: Redis 없이 DB만으로도 두 번째가 빨라지는가?")
    void db_only_performance_test() {
        // 1. 데이터 준비 (데이터가 좀 많아야 차이가 보임 -> 100개 등록)
        // (데이터가 너무 적으면 네트워크 비용 때문에 비슷해 보일 수 있음)
        for (int i = 0; i < 100; i++) {
            performanceRepository.save(Performance.create("공연 " + i, 10000, 100));
        }
        
        // DB에 반영하고, Hibernate 캐시(영속성 컨텍스트)를 싹 비웁니다.
        // 이제 다음 조회는 무조건 DB로 쿼리가 날아갑니다.
        em.flush();
        em.clear(); 

        // -------------------------------------------------------

        // 2. 첫 번째 조회 (Cold - Disk I/O 발생 가능성 높음)
        long start1 = System.currentTimeMillis();
        performanceRepository.findAll(); 
        long end1 = System.currentTimeMillis();
        System.out.println("👉 1차 조회 (Cold DB): " + (end1 - start1) + "ms");

        // 3. 다시 Hibernate 캐시 비우기
        // (이걸 안 하면 DB 안 가고 자바 메모리에서 줘버림. 우리는 DB 속도를 재야 함!)
        em.clear();

        // 4. 두 번째 조회 (Warm - DB Buffer Pool 효과 기대)
        long start2 = System.currentTimeMillis();
        performanceRepository.findAll();
        long end2 = System.currentTimeMillis();
        System.out.println("👉 2차 조회 (Warm DB): " + (end2 - start2) + "ms");

        // 5. 세 번째 조회 (완벽하게 워밍업 됨)
        em.clear();
        long start3 = System.currentTimeMillis();
        performanceRepository.findAll();
        long end3 = System.currentTimeMillis();
        System.out.println("👉 3차 조회 (Hot DB): " + (end3 - start3) + "ms");
    }
}