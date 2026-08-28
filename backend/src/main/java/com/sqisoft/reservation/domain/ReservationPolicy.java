package com.sqisoft.reservation.domain;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class ReservationPolicy {

    static final LocalTime BUSINESS_OPEN = LocalTime.of(9, 0);
    static final LocalTime BUSINESS_CLOSE = LocalTime.of(18, 0);
    static final int SLOT_UNIT_MINUTES = 30;
    static final Duration MIN_DURATION = Duration.ofMinutes(30);
    static final Duration MAX_DURATION = Duration.ofHours(4);
    static final int MAX_ADVANCE_DAYS = 14;
    static final int RESERVER_NAME_MAX = 20;
    static final int PURPOSE_MAX = 50;

    private final Clock clock;

    public ReservationPolicy(Clock clock) {
        this.clock = clock;
    }

    public void validate(String reserverName, String purpose, TimeSlot slot) {
        if (reserverName == null) {
            throw new ReservationException(ReservationErrorCode.INVALID_RESERVER_NAME);
        }
        String trimmedReserverName = reserverName.trim();
        if (trimmedReserverName.isEmpty() || trimmedReserverName.length() > RESERVER_NAME_MAX) {
            throw new ReservationException(ReservationErrorCode.INVALID_RESERVER_NAME);
        }
        if (purpose != null && purpose.length() > PURPOSE_MAX) {
            throw new ReservationException(ReservationErrorCode.INVALID_PURPOSE_LENGTH);
        }

        LocalDateTime start = slot.start();
        LocalDateTime end = slot.end();

        if (!isOnUnit(start) || !isOnUnit(end)) {
            throw new ReservationException(ReservationErrorCode.INVALID_TIME_UNIT);
        }
        if (start.isBefore(LocalDateTime.now(clock))) {
            throw new ReservationException(ReservationErrorCode.PAST_DATETIME);
        }
        if (start.toLocalDate().isAfter(LocalDate.now(clock).plusDays(MAX_ADVANCE_DAYS))) {
            throw new ReservationException(ReservationErrorCode.TOO_FAR_IN_FUTURE);
        }
        if (start.toLocalTime().isBefore(BUSINESS_OPEN)
                || end.toLocalTime().isAfter(BUSINESS_CLOSE)
                || !start.toLocalDate().equals(end.toLocalDate())) {
            throw new ReservationException(ReservationErrorCode.OUTSIDE_BUSINESS_HOURS);
        }

        Duration duration = slot.duration();
        if (duration.compareTo(MIN_DURATION) < 0 || duration.compareTo(MAX_DURATION) > 0) {
            throw new ReservationException(ReservationErrorCode.INVALID_DURATION);
        }
    }

    private boolean isOnUnit(LocalDateTime dateTime) {
        return dateTime.getMinute() % SLOT_UNIT_MINUTES == 0
                && dateTime.getSecond() == 0
                && dateTime.getNano() == 0;
    }
}
