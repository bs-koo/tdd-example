package com.sqisoft.reservation.domain;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

public record TimeSlot(LocalDateTime start, LocalDateTime end) {

    public TimeSlot {
        Objects.requireNonNull(start);
        Objects.requireNonNull(end);
    }

    public boolean overlaps(TimeSlot other) {
        return start.isBefore(other.end) && other.start.isBefore(end);
    }

    public Duration duration() {
        return Duration.between(start, end);
    }

    public LocalDate date() {
        return start.toLocalDate();
    }
}
