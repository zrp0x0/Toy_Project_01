package com.zrp.toyproject01.domain.queue.application;

import java.util.Set;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QueueService {
    
    // 1. Redis와 소통하는 창구 <Key 타입, Value 타입>
    private final RedisTemplate<String, String> redisTemplate;

    // 2. redis에 저장할 키(변수명)을 상수로 정의함
    private static final String WAITING_KEY = "waiting_queue"; // 대기열 (ZSet 사용)
    private static final String ACTIVE_KEY = "active_queue"; // 입장렬 (Set 사용)
    private static final String SOLD_OUT_KEY = "sold_out:";

    // 기능 1. 대기열 등록 (줄 서기)
    // - 유저가 예매하기 버튼을 누르면 이 메소드가 실행됨
    public void addQueue(String email) {
        // 3. 현재 시간을 가져오기
        // 낮을 수록 먼저 온 사람
        long timeStamp = System.currentTimeMillis();

        // Redis의 ZSet(Sorted Set)에 데이터를 넣음
        // opsForZSet(): "나 ZSet 쓸 거야!"라고 선언
        // add(키이름, 값(유저), 점수(시간)): 유저를 시간 점수로 등록합니다.
        // Redis가 알아서 시간순으로 줄을 쫙 세워줍니다.
        redisTemplate.opsForZSet().add(WAITING_KEY, email, timeStamp);
    }

    // 기능 2 입장 가능 여부 확인 (문지기)
    // 유저가 결제 등을 시도할 때 너 입장권 있어?라고 확인하는 용도
    public boolean isAllowed(String email) {
        // 5. Redis의 Set에 이 유저가 있는지 확인함
        // opsForSet(): "나 Set 쓸 거야!"라고 선언
        // isMember(키이름, 값): 이 유저가 ACTIVE_KEY 목록에 포함되어 있니? (True/False)
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(ACTIVE_KEY, email));
    }

    // 기능 3 내 대기 순번 확인 (옵션)
    public Long getRank(String email) {
        // 6. ZSet에서 내 등수를 확인합니다.
        // rank(키이름, 값): 이 유저가 앞에서 몇 번째인지 0부터 시작하는 숫자(Index)로 줍니다.
        // 예: 0이면 1등, 99면 100등
        return redisTemplate.opsForZSet().rank(WAITING_KEY, email);
    }

    // 기능 4 대기열 -> 입장열로 이동 (Batch Process)
    // - count: 한 번에 입장시킬 인원 수 
    public void allowUser(long count) {
        Set<String> users = redisTemplate.opsForZSet().range(WAITING_KEY, 0, count);
    
        // 대기하는 사람이 없으면 종료
        if (users == null || users.isEmpty()) return;

        for (String user : users) {
            // 입장열에 추가 (구매 자격)
            redisTemplate.opsForSet().add(ACTIVE_KEY, user);

            // 대기열에서는 삭제
            redisTemplate.opsForZSet().remove(WAITING_KEY, user);
        }
    }


    // 매진 간판 걸기
    public void setSoldOut(Long performanceId) {
        redisTemplate.opsForValue().set(SOLD_OUT_KEY + performanceId, "true");
    }

    // 매진 간판 내리기 (취소표 발생 시)
    public void removeSoldOut(Long performanceId) {
        redisTemplate.delete(SOLD_OUT_KEY + performanceId);
    }

    // 매진인지 확인
    public boolean isSoldOut(Long performanceId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(SOLD_OUT_KEY + performanceId));
    }
}
