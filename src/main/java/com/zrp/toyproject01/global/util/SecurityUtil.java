package com.zrp.toyproject01.global.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.zrp.toyproject01.global.error.BusinessException;
import com.zrp.toyproject01.global.error.ErrorCode;

import lombok.NoArgsConstructor;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE) // 인스턴스 생성 방지
public class SecurityUtil {
    
    // 현재 로그인한 사용자의 이메일(username) 조회
    public static String getCurrentUserEmail() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || authentication.getName() == null) {
            throw new BusinessException(ErrorCode.LOGIN_REQUIRED);
        }

        return authentication.getName();
    }

}
