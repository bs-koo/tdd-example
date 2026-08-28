package com.sqisoft.reservation.support;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.sqisoft.reservation.domain.TimeSlot;

public final class ReservationTestFixtures {

    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    public static final Instant FIXED_INSTANT = Instant.parse("2026-08-25T00:00:00Z"); // = KST 09:00
    public static final Clock FIXED_CLOCK = Clock.fixed(FIXED_INSTANT, ZONE);
    public static final LocalDate TODAY = LocalDate.of(2026, 8, 25);
    public static final LocalDate DEFAULT_DATE = LocalDate.of(2026, 8, 26); // 날짜 미명시 AC 전용
    public static final LocalDate MAX_DATE = LocalDate.of(2026, 9, 8);
    public static final LocalDate OVER_MAX_DATE = LocalDate.of(2026, 9, 9);
    public static final Long ROOM_ID = 1L;
    public static final String RESERVER = "김본승";

    private ReservationTestFixtures() {
    }

    public static LocalDateTime at(LocalDate date, String hhmm) {
        String[] parts = hhmm.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        return date.atTime(hour, minute);
    }

    public static TimeSlot slot(LocalDate date, String startHHmm, String endHHmm) {
        return new TimeSlot(at(date, startHHmm), at(date, endHHmm));
    }

    public static TimeSlot defaultSlot(String startHHmm, String endHHmm) {
        return slot(DEFAULT_DATE, startHHmm, endHHmm);
    }
}
