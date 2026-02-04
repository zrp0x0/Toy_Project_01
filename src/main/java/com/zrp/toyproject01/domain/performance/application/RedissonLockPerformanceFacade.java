package com.zrp.toyproject01.domain.performance.application;

import java.util.concurrent.TimeUnit;

import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import com.zrp.toyproject01.domain.queue.application.QueueService;
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

    // 입장권 검사를 위해 QueueService 주입하기
    private final QueueService queueService;

    // public boolean purchase(Long id, int quantity, String email) {
    //     // 락의 이름 설정(Unique 해야함)
    //     // 예: performance:1 (1번 공연에 대한 자물쇠)
    //     RLock lock = redissonClient.getLock("performance:" + id);
    //     int maxRetry = 50; // 최대 10번만 다시 들이받아보기

    //     while (maxRetry > 0) {
    //         try {
    //             // 2. 락 획득 시도 (tryLock)
    //             // waitTime: 락을 얻을 때까지 기다리는 시간 (10초)
    //             // leaseTime: 락을 얻고 나서 점유하는 시간 (1초 지나면 자동 반납 - 데드락 방지)
    //             // - 근데 이거 하면 안될 듯? 일단 왜냐하면 점유 시간이 끝났는데 작업은 안끝나면 안되니깐
    //             boolean available = lock.tryLock(1, TimeUnit.SECONDS);

    //             // if (!available) {
    //             //     System.out.println("락 획득 실패");
    //             //     return;
    //             // }

    //             // 재시도 10번을 위해서
    //             if (!available) {
    //                 maxRetry--;
    //                 continue;
    //             }

    //             // 3. 락 획득 성공 시 비즈니스 로직 실행
    //             performanceService.purchase(id, quantity, email);
    //             return true;
    //         } catch (BusinessException e) {
    //             if (e.getErrorCode() == ErrorCode.PERFORMANCE_SOLD_OUT) {
    //                 try {
    //                     Thread.sleep(100); 
    //                 } catch (InterruptedException ie) {
    //                     // 쓰레드 상태를 복구하고 예외를 던짐
    //                     Thread.currentThread().interrupt();
    //                     throw new RuntimeException("작업 중단 발생", ie);
    //                 }
                    
    //                 continue;
    //             }
    //             throw e; // 그 외 에러 처리
    //         } catch(InterruptedException e) {
    //             Thread.currentThread().interrupt();
    //             throw new RuntimeException(e);
    //         } finally {
    //             // 4. 락 반납 (반드시 finally에서!)
    //             // isLocked: 락이 걸려있는지 확인
    //             // isHeldByCurrentThread: 내가 건 락인지 확인 (남의 락을 풀면 안되니깐)
    //             if (lock.isLocked() && lock.isHeldByCurrentThread()) {
    //                 lock.unlock();
    //             }
    //         }
    //     }

    //     return false; // 인내심 바닥 (실패)
        
    // }

    public void cancel(Long reservationId) {
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));
        
        Long performanceId = reservation.getPerformance().getId();
        RLock lock = redissonClient.getLock("performance:" + performanceId);

        try {
            boolean available = lock.tryLock(5, TimeUnit.SECONDS);
            if (!available) {
                log.error("취소 락 획득 실패! ID: {}", reservationId);
                throw new RuntimeException("락 획득 실패로 취소 처리를 못했습니다.");
            }

            // 2. 락을 잡은 상태에서 서비스의 취소 로직 호출
            performanceService.cancel(reservationId);
            queueService.removeSoldOut(performanceId);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock();;
            }
        }   
    }


    // 대기열
    public boolean purchase(Long id, int quantity, String email) {

        // 매진 여부 확인 (Redis 조회 속도 빠름)
        if (queueService.isSoldOut(id)) {
            throw new BusinessException(ErrorCode.PERFORMANCE_SOLD_OUT);
        }

        // 입장 권한 체크
        if (!queueService.isAllowed(email)) { 
            // 입장 명단에 없으면
            
            // 대기열 ZSet에 등록 (이미 있으면 순서 유지됨)
            queueService.addQueue(email);

            Long rank = queueService.getRank(email);
            long waitingNumber = (rank != null) ? rank + 1 : 0;

            log.info("접근 불가! 대기열로 이동합니다. 대기 순번: {}등", waitingNumber);


            // 실패 처리 (프론트에서 대기 중입니다 화면)
            return false;
        }

        // 입장 권한이 있는 경우 (기존 로직 실행)
        log.info("🎉 입장 성공! 티켓 구매를 시도합니다. User: {}", email);

        RLock lock = redissonClient.getLock("performance:" + id);

        try {
            boolean available = lock.tryLock(2, 5, TimeUnit.SECONDS);

            if (!available) {
                return false; // 시스템 혼잡으로 실패
            }

            performanceService.purchase(id, quantity, email);
            
            return true;

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread())
                lock.unlock();
        }
    }

}
