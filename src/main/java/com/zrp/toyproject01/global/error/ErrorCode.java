package com.zrp.toyproject01.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),

    // Account
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "A001", "사용자를 찾을 수 없습니다."),
    
    // Seat
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "좌석을 찾을 수 없습니다."),
    SEAT_ALREADY_RESERVED(HttpStatus.CONFLICT, "S002", "이미 예약된 좌석입니다."),
    
    // Coupon
    COUPON_SOLD_OUT(HttpStatus.CONFLICT, "P001", "쿠폰이 모두 소진되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getCode() { return code; }
    public String getMessage() { return message; }
    
}