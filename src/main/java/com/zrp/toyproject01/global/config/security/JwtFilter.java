package com.zrp.toyproject01.global.config.security;

import java.io.IOException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter{
    
    public static final String AUTHORIZATION_HEADER = "Authorization";
    public static final String BEARER_PREFIX = "Bearer ";

    private final TokenProvider tokenProvider;

    // Controller로 가는 모든 Request는 이곳을 거쳐야함
    @Override
    protected void doFilterInternal(
        HttpServletRequest request, 
        HttpServletResponse response,
        FilterChain filterChain
    ) throws IOException, ServletException {

        // 1. 요청 헤더에서 토큰을 꺼내기
        String jwt = resolveToken(request);

        // 2. 토큰이 있고, 유효하다면?
        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {

            // 3. 토큰에서 인증 정보를 받아오기
            Authentication authentication = tokenProvider.getAuthentication(jwt);

            // 4. Spring Security의 저장소 SecurityContext에 넣어주기
            // 이렇게 하면 이후 모든 로직에서 이 사람은 로그인한 사용자다라고 인식함
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        // 5. 다음 단계(다른 필터 혹은 Controller)로 넘어갑니다.
        filterChain.doFilter(request, response);
    }

    // 요청에서 토큰 파싱하기 (공통 규약을 따름)
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(7);
        }
        return null;
    }
    // - 근데 이거는 클라이언트가 이렇게 보내줘야함
    // fetch('/api/account/profile', {
    //     method: 'GET',
    //     headers: {
    //         'Authorization': 'Bearer ' + myToken,
    //         'Content-Type': 'application/json'
    //     }
    // })

}
