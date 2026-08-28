package com.sqisoft.reservation.domain;

public class ReservationException extends RuntimeException {

    private final ReservationErrorCode code;

    public ReservationException(ReservationErrorCode code) {
        super(code.message());
        this.code = code;
    }

    public ReservationErrorCode code() {
        return code;
    }
}
