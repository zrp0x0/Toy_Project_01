package com.zrp.toyproject01.domain.account.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.zrp.toyproject01.domain.account.application.AccountService;
import com.zrp.toyproject01.domain.account.dto.LoginRequest;
import com.zrp.toyproject01.domain.account.dto.LoginResponse;
import com.zrp.toyproject01.domain.account.dto.SignupRequest;
import com.zrp.toyproject01.domain.account.dto.TokenDto;
import com.zrp.toyproject01.global.common.ApiResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {
    
    private final AccountService accountService;

    // 회원가입
    @PostMapping("/signup")
    public ApiResponse<Void> signup(
        @RequestBody @Valid SignupRequest request
    ) {
        accountService.signup(request);
        return ApiResponse.ok();
    }

    // 로그인
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(
        @RequestBody @Valid LoginRequest request
    ) {
        LoginResponse response = accountService.login(request);
        return ApiResponse.ok(response);
    }

    // 토큰 재발급
    @PostMapping("/reissue")
    public ApiResponse<TokenDto> reissue(
        @RequestBody TokenDto tokenDto
    ) {
        // 클라이언트가 보낸 Refresh Token으로 재발급 요청
        TokenDto newToken = accountService.reissue(tokenDto.refreshToken());
        return ApiResponse.ok(newToken);
    }

}
