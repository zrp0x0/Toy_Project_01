package com.zrp.toyproject01.domain.performance.application;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import com.zrp.toyproject01.domain.reservation.dao.ReservationRepository;
import com.zrp.toyproject01.domain.reservation.domain.Reservation;
import com.zrp.toyproject01.global.error.BusinessException;
import com.zrp.toyproject01.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class RedissonLockPerformanceFacade {
    
    private final RedissonClient redissonClient;
    private final PerformanceService performanceService;
    private final ReservationRepository reservationRepository;

    public boolean purchase(Long id, int quantity, String email) {
        // 락의 이름 설정(Unique 해야함)
        // 예: performance:1 (1번 공연에 대한 자물쇠)
        RLock lock = redissonClient.getLock("performance:" + id);
        int maxRetry = 10; // 최대 10번만 다시 들이받아보기

        while (maxRetry > 0) {
            try {
                // 2. 락 획득 시도 (tryLock)
                // waitTime: 락을 얻을 때까지 기다리는 시간 (10초)
                // leaseTime: 락을 얻고 나서 점유하는 시간 (1초 지나면 자동 반납 - 데드락 방지)
                boolean available = lock.tryLock(10, 1, TimeUnit.SECONDS);

                // if (!available) {
                //     System.out.println("락 획득 실패");
                //     return;
                // }

                // 재시도 10번을 위해서
                if (!available) {
                    maxRetry--;
                    continue;
                }

                // 3. 락 획득 성공 시 비즈니스 로직 실행
                performanceService.purchase(id, quantity, email);
                return true;
            } catch (BusinessException e) {
                if (e.getErrorCode() == ErrorCode.PERFORMANCE_SOLD_OUT) {
                    try {
                        Thread.sleep(100); 
                    } catch (InterruptedException ie) {
                        // 쓰레드 상태를 복구하고 예외를 던짐
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("작업 중단 발생", ie);
                    }
                    
                    continue;
                }
                throw e; // 그 외 에러 처리
            } catch(InterruptedException e) {
                Thread.currentThread().interrupt();
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

        return false; // 인내심 바닥 (실패)
        
    }

    public void cancel(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        
        Long performanceId = reservation.getPerformance().getId();
        RLock lock = redissonClient.getLock("performance:" + performanceId);

        try {
            boolean available = lock.tryLock(10, 1, TimeUnit.SECONDS);
            if (!available) return;

            // 2. 락을 잡은 상태에서 서비스의 취소 로직 호출
            performanceService.cancel(reservationId);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();;
            }
        }
    }
}
