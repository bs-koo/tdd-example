package com.sqisoft.reservation.api;

import com.sqisoft.reservation.domain.ReservationErrorCode;

public record ErrorResponse(String code, String message) {

    public static ErrorResponse from(ReservationErrorCode code) {
        return new ErrorResponse(code.name(), code.message());
    }
}
