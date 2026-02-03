package com.zrp.toyproject01.domain.account.dto;

public record LoginResponse(
    String token,
    String nickname,
    String email
) {
    public static LoginResponse of(String token, String nickname, String email) {
        return new LoginResponse(token, nickname, email);
    }
}
