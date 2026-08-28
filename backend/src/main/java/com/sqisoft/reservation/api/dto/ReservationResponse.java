package com.sqisoft.reservation.api.dto;

import java.time.format.DateTimeFormatter;

import com.sqisoft.reservation.domain.Reservation;

public record ReservationResponse(Long id, Long roomId, String reserverName, String purpose,
        String startAt, String endAt, String status) {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static ReservationResponse from(Reservation r) {
        return new ReservationResponse(
                r.id(),
                r.roomId(),
                r.reserverName(),
                r.purpose(),
                r.timeSlot().start().format(FORMATTER),
                r.timeSlot().end().format(FORMATTER),
                r.status().name());
    }
}
