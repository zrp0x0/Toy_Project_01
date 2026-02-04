package com.zrp.toyproject01.domain.performance.application;

import java.util.List;
import java.util.stream.Collectors;
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
    public Long register(PerformanceRegisterRequest request) {
        Performance performance = Performance.create(
            request.name(),
            request.price(),
            request.maxStock()
        );
        return performanceRepository.save(performance).getId();
    }

    // 공연 목록 조회
    public List<PerformanceResponse> findAll() {
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

}
