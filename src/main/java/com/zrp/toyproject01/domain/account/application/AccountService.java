package com.zrp.toyproject01.domain.account.application;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zrp.toyproject01.domain.account.dao.RefreshTokenRepository;
import com.zrp.toyproject01.domain.account.dao.UserRepository;
import com.zrp.toyproject01.domain.account.domain.RefreshToken;
import com.zrp.toyproject01.domain.account.domain.User;
import com.zrp.toyproject01.domain.account.dto.LoginRequest;
import com.zrp.toyproject01.domain.account.dto.LoginResponse;
import com.zrp.toyproject01.domain.account.dto.SignupRequest;
import com.zrp.toyproject01.domain.account.dto.TokenDto;
import com.zrp.toyproject01.global.config.security.TokenProvider;
import com.zrp.toyproject01.global.error.BusinessException;
import com.zrp.toyproject01.global.error.ErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본적으로 읽기 전용으로 설정해서 성능 최적화
public class AccountService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    // 1. 회원가입
    @Transactional // 쓰기 작업이므로 readOnly = false
    public void signup(SignupRequest request) {
        // 이메일 중복 검사
        if (userRepository.existsByEmail(request.email())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        // **비밀번호 암호화**
        String encodedPassword = passwordEncoder.encode(request.password());

        // 유저 엔티티 생성 및 저장
        User user = User.create(request.email(), encodedPassword, request.nickname());
        userRepository.save(user);
    }

    // 2. 로그인
    @Transactional
    public LoginResponse login(LoginRequest request) {
        // 사용자 조회
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INFO_NOT_TRUE);
        }

        // 토큰 발급
        String accessToken = tokenProvider.createToken(user.getEmail(), user.getRoles(), user.getNickname());
        String refreshToken = tokenProvider.createRefreshToken(user.getEmail());

        // Refresh Token DB 저장
        RefreshToken rt = refreshTokenRepository.findByKey(user.getEmail())
            .map(item -> item.updateValue(refreshToken)) // 있으면 새 것으로 갈아끼우기
            .orElse(RefreshToken.builder() // 없으면 발급해주기
                    .key(user.getEmail())
                    .value(refreshToken)
                    .build()
            );

        refreshTokenRepository.save(rt);

        // TokenDto 포장 (두 개의 토큰을 담음)
        TokenDto tokenDto = TokenDto.builder()
                .grantType("Bearer")
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .accessTokenExpiresIn(tokenProvider.getExpiration(accessToken))
                .build();

        return LoginResponse.of(tokenDto, user.getNickname(), user.getEmail());
    }

    // 3. 토큰 재발급
    @Transactional
    public TokenDto reissue(String refreshToken) {
        // 1. Refresh Token 검증 (만료 여부, 위조 여부 확인)
        if (!tokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }

        // 2. 토큰에서 이메일 추출
        // (Refresh Token이 유효하다면 이메일을 꺼낼 수 있음)
        String email = tokenProvider.getEmail(refreshToken);

        // 3. DB에서 저장된 Refresh Token 가져오기
        RefreshToken rt = refreshTokenRepository.findByKey(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.REFRESH_TOKEN_NOT_FOUND));

        // 4. 토큰 일치 여부 검사
        if (!rt.getValue().equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 5. 새 토큰 생성 (Access + Refresh 둘 다 새로 발급)
        User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = tokenProvider.createToken(user.getEmail(), user.getRoles(), user.getNickname());
        String newRefreshToken = tokenProvider.createRefreshToken(user.getEmail());

        // 6. DB 업데이트 (Refresh Token 교체)
        rt.updateValue(newRefreshToken);
        refreshTokenRepository.save(rt);

        // 7. 새 토큰 반환
        return TokenDto.builder()
                .grantType("Bearer")
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .accessTokenExpiresIn(tokenProvider.getExpiration(newAccessToken))
                .build();
    }

}

// 정석적인 Authe..builder? 이거 공부해봐야겠다