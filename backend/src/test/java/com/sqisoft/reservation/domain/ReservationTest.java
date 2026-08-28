package com.sqisoft.reservation.domain;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.sqisoft.reservation.support.ReservationTestFixtures;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationTest {

    @Test
    @DisplayName("create로 생성한 예약은 id가 null이다")
    void create로_생성한_예약은_id가_null이다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");

        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                createdAt);

        assertThat(reservation.id()).isNull();
    }

    @Test
    @DisplayName("create로 생성한 예약의 상태는 ACTIVE다")
    void create로_생성한_예약의_상태는_ACTIVE다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");

        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                createdAt);

        assertThat(reservation.status()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("create로 생성한 예약은 isActive가 true다")
    void create로_생성한_예약은_isActive가_true다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");

        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                createdAt);

        assertThat(reservation.isActive()).isTrue();
    }

    @Test
    @DisplayName("create로 생성한 예약의 접근자는 생성 인자를 그대로 반환한다")
    void create로_생성한_예약의_접근자는_생성_인자를_그대로_반환한다() {
        TimeSlot slot = ReservationTestFixtures.defaultSlot("09:00", "10:00");
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");

        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                slot,
                createdAt);

        assertThat(reservation.roomId()).isEqualTo(ReservationTestFixtures.ROOM_ID);
        assertThat(reservation.reserverName()).isEqualTo(ReservationTestFixtures.RESERVER);
        assertThat(reservation.purpose()).isEqualTo("주간 회의");
        assertThat(reservation.timeSlot()).isEqualTo(slot);
        assertThat(reservation.createdAt()).isEqualTo(createdAt);
    }

    @Test
    @DisplayName("reserverName은 trim되어 저장된다")
    void reserverName은_trim되어_저장된다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");

        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                "  김본승  ",
                "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                createdAt);

        assertThat(reservation.reserverName()).isEqualTo("김본승");
    }

    @Test
    @DisplayName("purpose가 null이면 빈 문자열로 저장된다")
    void purpose가_null이면_빈_문자열로_저장된다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");

        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                null,
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                createdAt);

        assertThat(reservation.purpose()).isEqualTo("");
    }

    @Test
    @DisplayName("purpose는 trim하지 않고 원문 그대로 저장된다")
    void purpose는_trim하지_않고_원문_그대로_저장된다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");

        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "  주간 회의  ",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                createdAt);

        assertThat(reservation.purpose()).isEqualTo("  주간 회의  ");
    }

    @Test
    @DisplayName("restore로 생성한 예약은 주어진 id를 그대로 반환한다")
    void restore로_생성한_예약은_주어진_id를_그대로_반환한다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");

        Reservation reservation = Reservation.restore(
                42L,
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                ReservationStatus.ACTIVE,
                createdAt);

        assertThat(reservation.id()).isEqualTo(42L);
    }

    @Test
    @DisplayName("restore로 CANCELLED 상태로 만든 예약은 isActive가 false다")
    void restore로_CANCELLED_상태로_만든_예약은_isActive가_false다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");

        Reservation reservation = Reservation.restore(
                1L,
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                ReservationStatus.CANCELLED,
                createdAt);

        assertThat(reservation.isActive()).isFalse();
    }

    @ParameterizedTest(name = "{0}~{1} 구간과의 overlapsWith 결과는 {2} 이다")
    @DisplayName("ACTIVE 예약은 09:00~10:00 기준으로 겹치는 구간에 true, 접하기만 하는 구간에 false를 반환한다")
    @CsvSource({
            "09:30, 10:30, true",  // 겹침
            "10:00, 11:00, false", // 직후 접함 -> 겹치지 않음
            "08:00, 09:00, false", // 직전 접함 -> 겹치지 않음
    })
    void ACTIVE_예약의_overlapsWith는_시간대_겹침_여부를_반환한다(String otherStart, String otherEnd, boolean expected) {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00");
        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                createdAt);
        TimeSlot other = ReservationTestFixtures.defaultSlot(otherStart, otherEnd);

        assertThat(reservation.overlapsWith(other)).isEqualTo(expected);
    }

    @Test
    @DisplayName("CANCELLED 예약은 완전히 동일한 시간대와도 겹치지 않는다")
    void CANCELLED_예약은_완전히_동일한_시간대와도_겹치지_않는다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00");
        TimeSlot slot = ReservationTestFixtures.defaultSlot("09:00", "10:00");
        Reservation reservation = Reservation.restore(
                1L,
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                slot,
                ReservationStatus.CANCELLED,
                createdAt);

        assertThat(reservation.overlapsWith(slot)).isFalse();
    }
}
