package com.zrp.toyproject01.domain.account.application;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.zrp.toyproject01.domain.account.dao.UserRepository;
import com.zrp.toyproject01.domain.account.domain.User;
import com.zrp.toyproject01.domain.account.dto.LoginRequest;
import com.zrp.toyproject01.domain.account.dto.LoginResponse;
import com.zrp.toyproject01.domain.account.dto.SignupRequest;
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
    public LoginResponse login(LoginRequest request) {
        // 사용자 조회
        User user = userRepository.findByEmail(request.email())
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 비밀번호 검증
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BusinessException(ErrorCode.INFO_NOT_TRUE);
        }

        // 토큰 발급
        String token = tokenProvider.createToken(user.getEmail(), user.getRoles(), user.getNickname());

        return LoginResponse.of(token, user.getNickname(), user.getEmail());
    }

}
