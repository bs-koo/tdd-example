package com.sqisoft.reservation.api;

import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.sqisoft.reservation.domain.ReservationErrorCode;
import com.sqisoft.reservation.domain.ReservationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ReservationException.class)
    public ResponseEntity<ErrorResponse> handle(ReservationException e) {
        return ResponseEntity.status(ErrorCodeHttpStatus.of(e.code()))
                .body(ErrorResponse.from(e.code()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handle(HttpMessageNotReadableException e) {
        // AC-40 / 설계 C-7 — 요청 바디를 읽지 못하면 정의된 코드로 거절한다.
        // 코드가 INVALID_DATE_FORMAT 인 이유는 C-7 명시 + 13종 계약 유지 (PRD AC-40 참조).
        return ResponseEntity.status(ErrorCodeHttpStatus.of(ReservationErrorCode.INVALID_DATE_FORMAT))
                .body(ErrorResponse.from(ReservationErrorCode.INVALID_DATE_FORMAT));
    }
}
