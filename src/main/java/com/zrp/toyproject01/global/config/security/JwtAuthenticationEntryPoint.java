package com.zrp.toyproject01.global.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // 유효한 자격증명(토큰)을 제공하지 않고 접근하려 할 때 401 에러를 리턴
        // (기본 동작은 로그인 페이지 리다이렉트거나 403임)
        response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
    }
}


