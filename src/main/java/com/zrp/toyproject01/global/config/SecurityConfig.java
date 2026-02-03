package com.zrp.toyproject01.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.zrp.toyproject01.global.config.security.JwtAuthenticationEntryPoint;
import com.zrp.toyproject01.global.config.security.JwtFilter;
import com.zrp.toyproject01.global.config.security.TokenProvider;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // @PreAuthorize 어노테이션 사용을 위해 필요함
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final TokenProvider tokenProvider;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    // 비밀번호 암호화 기기 등록 (BCrypt)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    // 보안 필터 체인 설정 (핵심)
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            // .cors(Customizer.withDefaults())
            
            // [핵심 추가] 예외 처리 설정: 인증 실패 시 401을 뱉도록 설정
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint(jwtAuthenticationEntryPoint)
            )

            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            .authorizeHttpRequests(auth -> auth
                // 1. 로그인, 회원가입, 재발급 등은 누구나 접근 가능
                .requestMatchers("/api/account/**", "/login", "/signup", "/", "/favicon.ico", "/error").permitAll()
                .requestMatchers("/post/**").permitAll() // 화면 페이지 조회
                
                // 2. [명시적 허용] 게시글 목록/상세 조회(GET)는 누구나
                .requestMatchers(HttpMethod.GET, "/api/posts/**").permitAll()

                // 3. [명시적 차단] 글쓰기(POST), 수정(PUT), 삭제(DELETE)는 반드시 인증 필요
                // (anyRequest에 걸리긴 하지만, 확실하게 적어주는 게 좋습니다)
                .requestMatchers(HttpMethod.POST, "/api/posts/**").authenticated()
                .requestMatchers(HttpMethod.PUT, "/api/posts/**").authenticated()
                .requestMatchers(HttpMethod.DELETE, "/api/posts/**").authenticated()

                // 그 외 나머지는 인증 필요
                .anyRequest().authenticated()
            )
            
            .addFilterBefore(new JwtFilter(tokenProvider), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

}
