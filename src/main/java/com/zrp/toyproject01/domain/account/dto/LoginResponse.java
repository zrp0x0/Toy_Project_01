package com.zrp.toyproject01.domain.account.dto;

import lombok.Builder;

@Builder
public record LoginResponse(
    String grantType,
    String accessToken,
    String refreshToken, // 추가됨
    Long accessTokenExpiresIn,
    String nickname,
    String email
) {
    public static LoginResponse of(TokenDto tokenDto, String nickname, String email) {
        return LoginResponse.builder()
                .grantType(tokenDto.grantType())
                .accessToken(tokenDto.accessToken())
                .refreshToken(tokenDto.refreshToken())
                .accessTokenExpiresIn(tokenDto.accessTokenExpiresIn())
                .nickname(nickname)
                .email(email)
                .build();
    }
}
