package com.zrp.toyproject01.domain.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

// import org.junit.jupiter.api.AfterEach;
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
import com.zrp.toyproject01.domain.queue.application.QueueService;
import com.zrp.toyproject01.domain.queue.scheduler.QueueScheduler;
import com.zrp.toyproject01.domain.reservation.dao.ReservationRepository;
import com.zrp.toyproject01.domain.reservation.domain.Reservation;
import com.zrp.toyproject01.domain.reservation.domain.ReservationStatus;

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

    @Autowired QueueScheduler queueScheduler; // 스케줄러 주입
    @Autowired QueueService queueService;

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

    @Test
    @DisplayName("시나리오 테스트: 150명 시도(재고 100) -> 20명 취소 -> 대기자 중 20명 추가 낙찰")
    void catching_canceled_tickets_test() throws InterruptedException {
        // 1. Given: 변수 설정
        int initialStock = 100;
        int totalParticipants = 150; // 총 구매 시도 인원
        int cancelCount = 20;        // 취소할 인원

        // 테스트용 데이터 세팅 (공연 및 유저)
        Long performanceId = performanceService.register(new PerformanceRegisterRequest("취소표 대전", 50000, initialStock));
        for (int i = 1; i <= totalParticipants; i++) {
            userRepository.save(User.create("hyena" + i + "@test.com", "1234", "하이에나" + i));
        }

        AtomicInteger totalSuccessCount = new AtomicInteger(0);
        AtomicInteger totalFailCount = new AtomicInteger(0);

        // 🚨 Latch 개수 = 구매 시도(150) + 취소 시도(20) = 170
        CountDownLatch latch = new CountDownLatch(totalParticipants + cancelCount);

        // 🧵 [핵심 수정 1] 스레드 풀 분리
        // 구매자용 풀: 고정된 스레드 개수로 부하를 줌 (대기열 발생 시뮬레이션)
        ExecutorService purchaseExecutor = Executors.newFixedThreadPool(100);
        // 취소자용 풀: 즉시 실행되어야 하므로 CachedThreadPool 사용 (혹은 별도 생성)
        ExecutorService cancelExecutor = Executors.newCachedThreadPool();

        // 2. When: 150명 구매 시도 (purchaseExecutor 사용)
        for (int i = 1; i <= totalParticipants; i++) {
            String email = "hyena" + i + "@test.com";
            purchaseExecutor.submit(() -> {
                try {
                    // 락 획득 및 구매 로직 시도
                    boolean isSuccess = redissonLockPerformanceFacade.purchase(performanceId, 1, email);
                    if (isSuccess) {
                        totalSuccessCount.incrementAndGet();
                    } else {
                        totalFailCount.incrementAndGet();
                    }
                } catch (Exception e) {
                    System.err.println("구매 에러: " + e.getMessage());
                } finally {
                    latch.countDown(); // 작업 완료 카운트
                }
            });
        }

        // 3. 중간 이벤트 모니터링: 100개가 다 팔릴 때까지 대기
        long startTime = System.currentTimeMillis();
        long maxWaitTime = 10000; // 10초

        while (true) {
            long currentCount = reservationRepository.count();
            
            if (currentCount >= initialStock) {
                System.out.println("🎉 예약 100개 달성 완료! (현재: " + currentCount + "개) -> 취소 작업 준비");
                break; 
            }

            if (System.currentTimeMillis() - startTime > maxWaitTime) {
                // 디버깅을 위해 현재 상태 출력 후 종료
                System.err.println("⚠️ 타임아웃 발생! 현재 예약 수: " + currentCount);
                purchaseExecutor.shutdownNow();
                cancelExecutor.shutdownNow();
                throw new RuntimeException("시간 초과: 10초가 지나도 예약이 다 차지 않았습니다.");
            }

            Thread.sleep(100); // 0.1초 간격 폴링
        }

        // 예약 데이터 조회 (취소 대상 선정을 위해)
        List<Reservation> currentReservations = reservationRepository.findAll();
        System.out.println("📢 취소 로직 실행 직전 예약 수: " + currentReservations.size());

        // 4. 취소 작업 시작 (cancelExecutor 사용)
        // 🚨 [핵심 수정 2] 꽉 찬 purchaseExecutor 대신 별도 스레드에서 실행
        System.out.println("🚀 취소 스레드 가동 시작...");
        for (int i = 0; i < cancelCount; i++) {
            Long resId = currentReservations.get(i).getId();
            
            cancelExecutor.submit(() -> {
                try {
                    redissonLockPerformanceFacade.cancel(resId);
                    System.out.println("✅ 예약 취소 완료: ID " + resId);
                } catch (Exception e) {
                    System.err.println("취소 에러: " + e.getMessage());
                } finally {
                    latch.countDown(); // 취소 작업도 카운트 감소 필수
                }
            });
        }

        // 5. 모든 작업(170개)이 끝날 때까지 대기
        // 넉넉하게 30초 대기 (테스트 환경 고려)
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        
        // 스레드 풀 정리
        purchaseExecutor.shutdown();
        cancelExecutor.shutdown();

        if (!completed) {
            System.err.println("⚠️ 테스트가 시간 내에 완전히 종료되지 않았습니다. (남은 카운트: " + latch.getCount() + ")");
        }

        // 6. Then: 결과 검증
        Performance performance = performanceRepository.findById(performanceId).orElseThrow();
        long finalReservedCount = reservationRepository.findAll().stream()
                .filter(r -> r.getStatus() == ReservationStatus.RESERVED)
                .count();

        System.out.println("=========================================");
        System.out.println("📊 최종 테스트 리포트");
        System.out.println("총 시도: " + (totalParticipants + cancelCount));
        System.out.println("구매 성공(누적): " + totalSuccessCount.get()); 
        System.out.println("구매 실패: " + totalFailCount.get());
        System.out.println("최종 유효 예약 수: " + finalReservedCount); // 100이어야 함
        System.out.println("최종 재고: " + performance.getStock());      // 0이어야 함
        System.out.println("=========================================");

        // 검증 1: 최종 예약된 티켓 수는 초기 재고(100)와 같아야 함 (취소된 만큼 다시 팔렸으므로)
        assertEquals(initialStock, finalReservedCount);
        
        // 검증 2: DB 재고는 0이어야 함
        assertEquals(0, performance.getStock());
        
        // 검증 3: '누적' 성공 횟수는 최소 120회 이상이어야 함
        // (처음 100명 성공 + 취소 후 재진입하여 20명 성공 = 120)
        assertTrue(totalSuccessCount.get() >= initialStock + cancelCount, 
            "누적 성공 횟수가 120회 이상이어야 합니다. (실제: " + totalSuccessCount.get() + ")");
    }

    @Test
    @DisplayName("통합 시나리오: 매진 -> 스케줄러 휴식 -> 취소표 발생 -> 스케줄러 가동 -> 이삭줍기 성공")
    void sold_out_and_cancel_scenario_test() throws InterruptedException {
        // 1. [준비] 재고 1개로 시작 -> 누군가 바로 구매해서 '매진' 상태로 만듦
        Long performanceId = performanceService.register(new PerformanceRegisterRequest("아이유 콘서트", 50000, 1));
        User winner = userRepository.save(User.create("winner@test.com", "1234", "승리자"));
        User hyena = userRepository.save(User.create("hyena@test.com", "1234", "하이에나"));
        
        // 승리자가 1개를 사버림 -> 재고 0 -> SoldOut Flag = true
        redissonLockPerformanceFacade.purchase(performanceId, 1, "winner@test.com");
        queueScheduler.enterUserForTest(performanceId); 
        redissonLockPerformanceFacade.purchase(performanceId, 1, "winner@test.com");
        
        // 검증 1: 매진 플래그가 서 있어야 함
        assertTrue(queueService.isSoldOut(performanceId));
        System.out.println("✅ 1. 초기 매진 상태 확인 완료 (Flag=True)");

        // // 2. [대기] 하이에나가 늦게 들어와서 대기열에 갇힘
        // boolean purchaseResult = redissonLockPerformanceFacade.purchase(performanceId, 1, "hyena@test.com");
        // assertFalse(purchaseResult); // 구매 실패 (대기열 진입)
        
        // // 하이에나가 대기열(Waiting Queue)에 있는지 확인
        // Long rank = queueService.getRank("hyena@test.com");
        // assertNotNull(rank);
        // System.out.println("✅ 2. 하이에나 대기열 진입 확인 (순번: " + rank + ")");

        // 3. [스케줄러 테스트] 매진 상태에서 스케줄러를 강제로 실행해봄
        // (기대결과: Flag가 True이므로 아무도 입장시키지 않아야 함)
        queueScheduler.enterUserForTest(performanceId); 
        
        // // 여전히 대기열에 있어야 함 (입장 못함)
        // assertFalse(queueService.isAllowed("hyena@test.com"));
        // System.out.println("✅ 3. 매진 중 스케줄러 작동 안 함 확인 (하이에나 여전히 대기 중)");

        // 4. [이벤트] 승리자가 예약을 취소함!
        Reservation reservation = reservationRepository.findAll().get(0);
        redissonLockPerformanceFacade.cancel(reservation.getId());

        // 검증 4: 취소하자마자 매진 플래그가 사라져야 함
        assertFalse(queueService.isSoldOut(performanceId));
        System.out.println("✅ 4. 취소 후 매진 플래그 제거 확인 (Flag=False)");

        // 5. [재가동] 이제 스케줄러가 돌면 하이에나가 입장해야 함
        redissonLockPerformanceFacade.purchase(performanceId, 1, "hyena@test.com");
        queueScheduler.enterUserForTest(performanceId); 
        
        // 검증 5: 하이에나가 입장열(Active Queue)로 이동했는지
        assertTrue(queueService.isAllowed("hyena@test.com"));
        System.out.println("✅ 5. 스케줄러가 하이에나를 입장시킴");

        // 6. [이삭줍기] 하이에나가 다시 구매 시도 -> 성공해야 함
        boolean finalResult = redissonLockPerformanceFacade.purchase(performanceId, 1, "hyena@test.com");
        
        assertTrue(finalResult);
        System.out.println("✅ 6. 하이에나 취소표 구매 성공!");
        
        // 7. [최종 확인] 다시 매진되었는지
        assertTrue(queueService.isSoldOut(performanceId));
        System.out.println("✅ 7. 재구매 후 다시 매진 플래그 설정됨");
    }
}
