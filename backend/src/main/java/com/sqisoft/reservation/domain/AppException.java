package com.sqisoft.reservation.domain;

/**
 * 도메인이 정의한 오류 코드를 실어 나르는 예외의 뿌리.
 * API 계층은 이 타입 하나만 잡아 {@link ErrorCode} → HTTP 상태로 변환한다.
 */
public class AppException extends RuntimeException {

    private final ErrorCode code;

    public AppException(ErrorCode code) {
        super(code.message());
        this.code = code;
    }

    public ErrorCode code() {
        return code;
    }
}
