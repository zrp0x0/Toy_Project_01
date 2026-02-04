package com.zrp.toyproject01.global.error;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Common
    INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),

    // Account
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "A001", "사용자를 찾을 수 없습니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "A002", "이미 가입된 이메일입니다."),
    INFO_NOT_TRUE(HttpStatus.BAD_REQUEST, "A003", "정보가 일치하지 않습니다."),
    LOGIN_REQUIRED(HttpStatus.UNAUTHORIZED, "A004", "로그인이 필요한 서비스입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND, "A005", "토큰을 찾을 수 없습니다."),
    INVALID_TOKEN(HttpStatus.BAD_REQUEST, "A006", "유효하지 않은 토큰입니다."),

    // Post
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "해당 게시글을 찾을 수 없습니다."),
    NO_PERMISSION(HttpStatus.FORBIDDEN, "P002", "수정/삭제 권한이 없습니다."),

    // performance
    SEAT_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "좌석을 찾을 수 없습니다."),
    SEAT_ALREADY_RESERVED(HttpStatus.CONFLICT, "S002", "이미 예약된 좌석입니다."),
    SEAT_SOLD_OUT(HttpStatus.CONFLICT, "S003", "좌석이 모두 매진되었습니다."),


    // Coupon
    COUPON_SOLD_OUT(HttpStatus.CONFLICT, "P001", "쿠폰이 모두 소진되었습니다."),
    PERFORMANCE_NOT_FOUND(HttpStatus.NOT_FOUND, "P003", "해당 공연을 찾을 수 없습니다.");

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