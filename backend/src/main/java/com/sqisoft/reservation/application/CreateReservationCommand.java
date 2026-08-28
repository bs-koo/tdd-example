package com.sqisoft.reservation.application;

import java.time.LocalDateTime;

public record CreateReservationCommand(Long roomId, String reserverName, String purpose,
        LocalDateTime startAt, LocalDateTime endAt) {
}
