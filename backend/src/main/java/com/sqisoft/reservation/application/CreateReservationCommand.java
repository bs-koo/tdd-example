package com.sqisoft.reservation.application;

import java.time.LocalDateTime;

public record CreateReservationCommand(Long roomId, Long memberId, String reserverName, String purpose,
        LocalDateTime startAt, LocalDateTime endAt) {
}
