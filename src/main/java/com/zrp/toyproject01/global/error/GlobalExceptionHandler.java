package com.zrp.toyproject01.global.error;

import com.zrp.toyproject01.global.common.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // 1. 우리가 의도한 비즈니스 예외 처리
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[Business Exception] Code: {}, Message: {}", errorCode.getCode(), e.getMessage());
        
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMessage()));
    }

    // 2. 그 외 알 수 없는 시스템 예외 처리 (최후의 방어선)
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("[Internal Server Error]", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getCode(), errorCode.getMessage()));
    }

}

// - 나중에 로그를 따로 뽑아 볼 수 있거나 AOP를 적용해보는 것?도 좋을 듯