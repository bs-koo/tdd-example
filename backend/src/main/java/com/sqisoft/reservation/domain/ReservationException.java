package com.sqisoft.reservation.domain;

public class ReservationException extends AppException {

    public ReservationException(ReservationErrorCode code) {
        super(code);
    }

    @Override
    public ReservationErrorCode code() {
        return (ReservationErrorCode) super.code();
    }
}
