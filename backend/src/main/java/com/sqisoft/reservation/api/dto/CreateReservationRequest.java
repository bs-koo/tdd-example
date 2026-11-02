package com.sqisoft.reservation.api.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import com.sqisoft.reservation.application.CreateReservationCommand;
import com.sqisoft.reservation.domain.ReservationErrorCode;
import com.sqisoft.reservation.domain.ReservationException;

/**
 * 예약자명은 이 요청에 없다 — 로그인한 사용자에게서만 온다.
 * 클라이언트가 reserverName 을 실어 보내도 알 수 없는 필드로 무시된다.
 */
public record CreateReservationRequest(Long roomId, String purpose, String startAt, String endAt) {

    public CreateReservationCommand toCommand(Long memberId, String reserverName) {
        if (startAt == null || endAt == null) {
            throw new ReservationException(ReservationErrorCode.INVALID_DATE_FORMAT);
        }
        try {
            return new CreateReservationCommand(roomId, memberId, reserverName, purpose,
                    LocalDateTime.parse(startAt), LocalDateTime.parse(endAt));
        } catch (DateTimeParseException e) {
            throw new ReservationException(ReservationErrorCode.INVALID_DATE_FORMAT);
        }
    }
}
