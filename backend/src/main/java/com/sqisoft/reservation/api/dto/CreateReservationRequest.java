package com.sqisoft.reservation.api.dto;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

import com.sqisoft.reservation.application.CreateReservationCommand;
import com.sqisoft.reservation.domain.ReservationErrorCode;
import com.sqisoft.reservation.domain.ReservationException;

public record CreateReservationRequest(Long roomId, String reserverName, String purpose,
        String startAt, String endAt) {

    public CreateReservationCommand toCommand() {
        if (startAt == null || endAt == null) {
            throw new ReservationException(ReservationErrorCode.INVALID_DATE_FORMAT);
        }
        try {
            LocalDateTime parsedStartAt = LocalDateTime.parse(startAt);
            LocalDateTime parsedEndAt = LocalDateTime.parse(endAt);
            return new CreateReservationCommand(roomId, reserverName, purpose, parsedStartAt, parsedEndAt);
        } catch (DateTimeParseException e) {
            throw new ReservationException(ReservationErrorCode.INVALID_DATE_FORMAT);
        }
    }
}
