package com.zrp.toyproject01.domain.account.domain;

import com.zrp.toyproject01.global.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class RefreshToken extends BaseTimeEntity {
    
    @Id
    @Column(name = "rt_key")
    private String key; // 사용자 Email (PK)

    @Column(name = "rt_value")
    private String value; // Refresh Token 문자열

    @Builder
    public RefreshToken(String key, String value) {
        this.key = key;
        this.value = value;
    }

    public RefreshToken updateValue(String token) {
        this.value = token;
        return this;
    }
}
