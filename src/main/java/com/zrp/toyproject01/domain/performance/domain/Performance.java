package com.zrp.toyproject01.domain.performance.domain;

import com.zrp.toyproject01.global.common.BaseTimeEntity;
import com.zrp.toyproject01.global.error.BusinessException;
import com.zrp.toyproject01.global.error.ErrorCode;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "performance")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Performance extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name; // 공연명

    @Column(nullable = false)
    private int price; // 가격

    @Column(nullable = false)
    private int maxStock; // 최대 좌석 수

    @Column(nullable = false)
    private int stock; // 남은 좌석 수 (동시성 제어의 핵심이 될 것)

    @Version
    private Long version;

    private Performance(String name, int price, int maxStock) {
        this.name = name;
        this.price = price;
        this.maxStock = maxStock;
        this.stock = maxStock; // 처음에 꽉 차있음
    }

    public static Performance create(String name, int price, int maxStock) {
        return new Performance(name, price, maxStock);
    }

    /**
     * 핵심 비즈니스 로직
     */
    // 재고 감소
    public void decreaseStock(int quantity) {
        int restStock = this.stock - quantity;

        // 1. 검사 (Check)
        if (restStock < 0) {
            // 재고 부족 시 예외 발생
            throw new BusinessException(ErrorCode.PERFORMANCE_SOLD_OUT);
        }

        // 2. 반영 (Act)
        this.stock = restStock;
    }

    // 옵션 재고 증가 (취소 시)
    public void increaseStock(int quantity) {
        if (this.stock + quantity > this.maxStock) {
            // 최대 재고보다 많아질 순 없음 (설계에 따라 달라질 듯)
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        this.stock += quantity;
    }

}
