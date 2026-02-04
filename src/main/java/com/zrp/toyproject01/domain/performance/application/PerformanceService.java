package com.zrp.toyproject01.domain.performance.application;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zrp.toyproject01.domain.account.dao.UserRepository;
import com.zrp.toyproject01.domain.account.domain.User;
import com.zrp.toyproject01.domain.performance.dao.PerformanceRepository;
import com.zrp.toyproject01.domain.performance.domain.Performance;
import com.zrp.toyproject01.domain.performance.dto.PerformanceRegisterRequest;
import com.zrp.toyproject01.domain.performance.dto.PerformanceResponse;
import com.zrp.toyproject01.domain.reservation.dao.ReservationRepository;
import com.zrp.toyproject01.domain.reservation.domain.Reservation;
import com.zrp.toyproject01.domain.reservation.domain.ReservationStatus;
import com.zrp.toyproject01.global.error.BusinessException;
import com.zrp.toyproject01.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PerformanceService {
    
    private final PerformanceRepository performanceRepository;
    private final UserRepository userRepository;
    private final ReservationRepository reservationRepository;

    // 공연 등록 (관리자용)
    @Transactional
    @CacheEvict(value = "performances", key = "'all'")
    public Long register(PerformanceRegisterRequest request) {
        Performance performance = Performance.create(
            request.name(),
            request.price(),
            request.maxStock()
        );
        return performanceRepository.save(performance).getId();
    }

    // 공연 목록 조회
    @Cacheable(value = "performances", key = "'all'", cacheManager = "cacheManager")
    public List<PerformanceResponse> findAll() {
        // 이 로그가 찍히면 -> DB까지 갔다는 뜻
        // 이 로그가 안 찍히면 -> Redis에서 가져왔다는 뜻 (캐시 Hit)
        System.out.println("## ## ## DB 조회!! ## ## ##");

        return performanceRepository.findAll().stream() 
            .map(PerformanceResponse::from)
            .collect(Collectors.toList());
    }

    // 핵심: 예매 (재고 감소)
    // - 동시성 이슈 발생할 핵심 메소드
    @Transactional
    public void purchase(Long id, int quantity, String email) {
        // 1. 공연 조회 (이 시점의 stock은 1일 수 있음) 4. 낙관적 락 사용
        Performance performance = performanceRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND));

        // 3. 비관적 락 걸고 조회
        // Performance performance = performanceRepository.findByIdWithLock(id)
        //     .orElseThrow(() -> new BusinessException(ErrorCode.PERFORMANCE_NOT_FOUND));

        // 2. 유저 조회
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 3. 재고 감소 (Entity에게 위임)
        // - 여기서 동시에 접근하면, 두 스레드 모두 통과할 수 있음
        performance.decreaseStock(quantity);

        // 4. 예약 생성 및 저장 (영주증 발행)
        Reservation reservation = Reservation.create(user, performance, quantity);
        reservationRepository.save(reservation);
    }

    // 예약 취소
    @Transactional
    @CacheEvict(value = "performances", allEntries = true) // 만약 공연 상세보기를 했을 때, 거기에 적힌 수량도 바뀌어야하니깐 추가해보긴 했어
    public void cancel(Long reservationId) {
        // 1. 예약 내역 조회
        Reservation reservation = reservationRepository.findById(reservationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND));

        // 2. 이미 취소된 예약인지 체크
        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            throw new BusinessException(ErrorCode.ALREADY_CANCELLED);
        }

        // 3. 재고 복구 (해당 공연을 찾아와서 취소 수량만큼 더함)
        Performance performance = reservation.getPerformance();
        performance.increaseStock((reservation.getCount()));
        // 여기서 N + 1?

        // 4. 예약 상태 변경
        reservation.cancel();

        // 5. 캐시 비우기 (재고 수량이 바뀌었으니 목록 캐시도 날려야 정확함)

    }

}
