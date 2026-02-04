package com.zrp.toyproject01.domain.queue.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.zrp.toyproject01.domain.queue.application.QueueService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j // 로그 찍기
public class QueueScheduler {
    
    private final QueueService queueService;

    private final Long PERFORMANCE_ID = 1L;

    @Scheduled(fixedDelay = 1000) // 1000ms(1초)
    public void enterUser() {

        if (queueService.isSoldOut(PERFORMANCE_ID)) {
            return;
        }

        queueService.allowUser(100);
        log.info("🚪 1초가 지났습니다. 순번이 된 유저들을 입장시켰습니다.");
    }

    public void enterUserForTest(Long performanceId) { 
        if (queueService.isSoldOut(performanceId)) {
            return;
        }    

        queueService.allowUser(100);
        log.info("🚪 1초가 지났습니다. 순번이 된 유저들을 입장시켰습니다.");
    }

}
