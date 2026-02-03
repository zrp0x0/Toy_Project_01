package com.zrp.toyproject01.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.zrp.toyproject01.global.config.security.JwtFilter;
import com.zrp.toyproject01.global.config.security.TokenProvider;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // @PreAuthorize 어노테이션 사용을 위해 필요함
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final TokenProvider tokenProvider;

    // 비밀번호 암호화 기기 등록 (BCrypt)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 보안 필터 체인 설정 (핵심)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF 보안 비활성화 (서버에 인증 정보를 저장하지 않으므로 불피요함)
            .csrf(AbstractHttpConfigurer::disable)
            
            // 세션 설정: Stateless (세션을 만들지도, 쓰지도 않음)
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            // URL 별 권한 설정
            .authorizeHttpRequests(auth -> auth
                // 회워 가입, 로그인은 인증 없이 접근 허용
                .requestMatchers("/api/account/signup", "/api/account/login")
                .permitAll()
                .requestMatchers("/", "/login", "/signup", "/favicon.ico", "/error")
                .permitAll()
                // 그 외 모든 요청은 인증 필요
                .anyRequest().authenticated()
            )

            // 이제 만든 JwtFilter를 아이디/비번 인증 필터보다 앞에 끼워 넣음
            .addFilterBefore(
                new JwtFilter(tokenProvider),
                UsernamePasswordAuthenticationFilter.class
            );

        return http.build();
    }

}
