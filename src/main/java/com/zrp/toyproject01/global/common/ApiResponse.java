package com.zrp.toyproject01.global.common;

public record ApiResponse<T>(
    boolean success,
    T data,
    ErrorResponse error
) {
    // 1. 성공 응답 (데이터 있음)
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    // 2. 성공 응답 (데이터 없음 - 예: 삭제 성공)
    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, null, null);
    }

    // 3. 실패 응답
    public static <T> ApiResponse<T> fail(String code, String message) {
        return new ApiResponse<>(false, null, new ErrorResponse(code, message));
    }

    // 내부용 에러 객체
    public record ErrorResponse(String code, String message) {}
}

// 결과 JSON:
// {
//   "success": true,
//   "data": { "name": "철수", "age": 25 },
//   "error": null
// }

// 결과 JSON:
// {
//   "success": false,
//   "data": null,
//   "error": { "code": "404", "message": "사용자를 찾을 수 없습니다." }
// }