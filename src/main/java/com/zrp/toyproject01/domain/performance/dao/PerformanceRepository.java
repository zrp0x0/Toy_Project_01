package com.zrp.toyproject01.domain.performance.dao;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.zrp.toyproject01.domain.performance.domain.Performance;
import jakarta.persistence.LockModeType;

public interface PerformanceRepository extends JpaRepository<Performance, Long> {
    // 필요시 추가
    // 🔒 [핵심] 비관적 락(쓰기 잠금)을 건 조회 메서드
    // PESSIMISTIC_WRITE: "내가 수정할 거니까, 다른 사람은 읽지도 말고 쓰지도 마!" (가장 강력한 락)
    // 동작 방식: SELECT ... FOR UPDATE 쿼리가 나갑니다.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Performance p where p.id = :id")
    Optional<Performance> findByIdWithLock(@Param("id") Long id);
}
