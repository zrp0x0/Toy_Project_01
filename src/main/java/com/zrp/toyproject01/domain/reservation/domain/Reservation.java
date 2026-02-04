package com.zrp.toyproject01.domain.reservation.domain;

import com.zrp.toyproject01.domain.account.domain.User;
import com.zrp.toyproject01.domain.performance.domain.Performance;
import com.zrp.toyproject01.global.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reservation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Reservation extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 누가 예약했는가?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 어떤 공연인가?
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "performance_id", nullable = false)
    private Performance performance;

    // 몇 장 샀는가?
    @Column(nullable = false)
    private int count;

    // 얼마에 샀는가? (가격 스냅샷 - 나중에 공연 가격이 바뀌어도 영향 받지 않도록)
    @Column(nullable = false)
    private int price;

    // 예약 상태 (완료/취소)
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;

    // 생성자 (팩토리 메소드용)
    private Reservation(
        User user,
        Performance performance,
        int count,
        int price
    ) {
        this.user = user;
        this.performance = performance;
        this.count = count;
        this.price = price;
        this.status = ReservationStatus.RESERVED; // 기본 상태는 예약 완료
    }

    // 팩토리 메소드 : 예약 생성
    public static Reservation create(
        User user,
        Performance performance,
        int count
    ) {
        return new Reservation(user, performance, count, performance.getPrice());
    }

    // (옵션) 예약 취소 로직
    public void cancel() {
        this.status = ReservationStatus.CANCELLED;
    }

}
