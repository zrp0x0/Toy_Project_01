package com.zrp.toyproject01.domain.performance.application;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OptimisticLockPerformanceFacade {
    
    private final PerformanceService performanceService;

    public void purchase(Long id, int quantity) throws InterruptedException {
        while (true) { // 무한 반복 (성공할 때까지)
            try {
                // 1. 서비스 로직 호출 (여기서 충돌나면 에러 발생)
                performanceService.purchase(id, quantity);

                // 2. 성공하면 반복문 탈출
                break;
            } catch (OptimisticLockingFailureException e) { 
                // 3. 충돌 발생 시 (누군가 먼저 수정함)
                // 잠시 대기했다가 재시도 (서버 부하를 줄이기 위해서 50ms 쉼)
                // ✨ [수정] 충돌(OptimisticLock)일 때만 잡아서 재시도!
                // (import org.springframework.dao.OptimisticLockingFailureException)
                System.out.println("충돌 발생! 재시도합니다.");
                Thread.sleep(50);
            } catch (Exception e) {
                // 다른 에러는 그냥 던져버림 (품절 에러)
                throw e;
            }
        }   
    }

}
