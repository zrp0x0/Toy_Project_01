package com.zrp.toyproject01.domain.performance.application;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedissonLockPerformanceFacade {
    
    private final RedissonClient redissonClient;
    private final PerformanceService performanceService;

    public void purchase(Long id, int quantity, String email) {
        // 락의 이름 설정(Unique 해야함)
        // 예: performance:1 (1번 공연에 대한 자물쇠)
        RLock lock = redissonClient.getLock("performance:" + id);

        try {
            // 2. 락 획득 시도 (tryLock)
            // waitTime: 락을 얻을 때까지 기다리는 시간 (10초)
            // leaseTime: 락을 얻고 나서 점유하는 시간 (1초 지나면 자동 반납 - 데드락 방지)
            boolean available = lock.tryLock(10, 1, TimeUnit.SECONDS);

            if (!available) {
                System.out.println("락 획득 실패");
                return;
            }

            // 3. 락 획득 성공 시 비즈니스 로직 실행
            performanceService.purchase(id, quantity, email);
        } catch(InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 4. 락 반납 (반드시 finally에서!)
            // isLocked: 락이 걸려있는지 확인
            // isHeldByCurrentThread: 내가 건 락인지 확인 (남의 락을 풀면 안되니깐)
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
