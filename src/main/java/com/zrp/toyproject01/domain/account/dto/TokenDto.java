package com.zrp.toyproject01.domain.account.dto;

import lombok.Builder;

@Builder
public record TokenDto(
    String grantType,           // Bearer
    String accessToken,         // 액세스 토큰 
    String refreshToken,        // 리프레시 토큰
    Long accessTokenExpiresIn   // 만료 시간
) {
    
}
