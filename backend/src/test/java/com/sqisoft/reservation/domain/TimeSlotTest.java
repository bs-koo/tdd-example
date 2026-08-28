package com.sqisoft.reservation.domain;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.sqisoft.reservation.support.ReservationTestFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimeSlotTest {

    @ParameterizedTest(name = "{0}~{1} 구간은 09:00~10:00 과의 겹침이 {2} 이다")
    @DisplayName("겹침 판정 9케이스가 판정표대로 동작한다 (양방향 대칭 포함)")
    @CsvSource({
            "08:00, 09:00, false", // 직전에 접함 -> 허용(겹치지 않음)
            "08:30, 09:30, true",  // 뒤쪽 부분 겹침 -> 거절
            "09:00, 10:00, true",  // 완전 동일 -> 거절
            "09:15, 09:45, true",  // 기존에 포함됨 -> 거절
            "09:30, 10:00, true",  // 기존에 포함됨 -> 거절
            "08:30, 10:30, true",  // 기존을 포함함 -> 거절
            "09:30, 10:30, true",  // 앞쪽 부분 겹침 -> 거절
            "10:00, 11:00, false", // 직후에 접함 -> 허용(겹치지 않음)
            "11:00, 12:00, false", // 완전히 분리 -> 허용(겹치지 않음)
    })
    void 겹침_판정_9케이스가_판정표대로_동작한다(String reqStart, String reqEnd, boolean expectedOverlap) {
        TimeSlot existing = ReservationTestFixtures.defaultSlot("09:00", "10:00");
        TimeSlot requested = ReservationTestFixtures.defaultSlot(reqStart, reqEnd);

        assertThat(existing.overlaps(requested)).isEqualTo(expectedOverlap);
        assertThat(requested.overlaps(existing)).isEqualTo(expectedOverlap);
    }

    @Test
    @DisplayName("duration은 시작과 종료 사이의 Duration을 반환한다")
    void duration은_시작과_종료_사이의_Duration을_반환한다() {
        TimeSlot slot = ReservationTestFixtures.defaultSlot("09:15", "09:45");

        assertThat(slot.duration()).isEqualTo(Duration.ofMinutes(30));
    }

    @Test
    @DisplayName("date는 시작 시각의 날짜를 반환한다")
    void date는_시작_시각의_날짜를_반환한다() {
        TimeSlot slot = ReservationTestFixtures.defaultSlot("09:00", "10:00");

        assertThat(slot.date()).isEqualTo(ReservationTestFixtures.DEFAULT_DATE);
    }

    @Test
    @DisplayName("시작 시각이 null이면 NullPointerException을 던진다")
    void 시작_시각이_null이면_예외를_던진다() {
        LocalDateTime end = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "10:00");

        assertThatThrownBy(() -> new TimeSlot(null, end))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("종료 시각이 null이면 NullPointerException을 던진다")
    void 종료_시각이_null이면_예외를_던진다() {
        LocalDateTime start = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");

        assertThatThrownBy(() -> new TimeSlot(start, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("시작과 종료가 같아도 생성자는 예외를 던지지 않는다 (순서 검증은 다른 클래스의 책임)")
    void 시작과_종료가_같아도_예외를_던지지_않는다() {
        LocalDateTime same = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");

        assertThatNoException().isThrownBy(() -> new TimeSlot(same, same));
    }

    @Test
    @DisplayName("FIXED_CLOCK은 KST 2026-08-25 09:00을 가리킨다")
    void FIXED_CLOCK은_KST_2026_08_25_09시를_가리킨다() {
        LocalDateTime now = LocalDateTime.now(ReservationTestFixtures.FIXED_CLOCK);

        assertThat(now).isEqualTo(LocalDateTime.of(2026, 8, 25, 9, 0));
    }
}
