package com.sqisoft.reservation.api;

import com.sqisoft.reservation.domain.ErrorCode;

public record ErrorResponse(String code, String message) {

    public static ErrorResponse from(ErrorCode code) {
        return new ErrorResponse(code.name(), code.message());
    }
}
