package com.sqisoft.reservation.domain;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sqisoft.reservation.support.ReservationTestFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationCancelTest {

    @Test
    @DisplayName("본인 이름으로 cancel을 호출하면 예외가 발생하지 않는다")
    void 본인_이름으로_cancel을_호출하면_예외가_발생하지_않는다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");
        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                createdAt);

        assertThatNoException().isThrownBy(() -> reservation.cancel(ReservationTestFixtures.RESERVER));
    }

    @Test
    @DisplayName("본인 이름으로 cancel하면 status가 CANCELLED로 바뀐다")
    void 본인_이름으로_cancel하면_status가_CANCELLED로_바뀐다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");
        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                createdAt);

        reservation.cancel(ReservationTestFixtures.RESERVER);

        assertThat(reservation.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("본인 이름으로 cancel하면 isActive가 false가 된다")
    void 본인_이름으로_cancel하면_isActive가_false가_된다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");
        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                createdAt);

        reservation.cancel(ReservationTestFixtures.RESERVER);

        assertThat(reservation.isActive()).isFalse();
    }

    @Test
    @DisplayName("타인 이름으로 cancel하면 NOT_RESERVER 예외가 발생한다")
    void 타인_이름으로_cancel하면_NOT_RESERVER_예외가_발생한다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");
        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                createdAt);

        assertThatThrownBy(() -> reservation.cancel("박철수"))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.NOT_RESERVER);
    }

    @Test
    @DisplayName("타인 이름으로 cancel이 거절되면 status는 ACTIVE로 유지된다")
    void 타인_이름으로_cancel이_거절되면_status는_ACTIVE로_유지된다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");
        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                createdAt);

        try {
            reservation.cancel("박철수");
        } catch (ReservationException ignored) {
            // 거절 확인은 status 검증으로 대체
        }

        assertThat(reservation.status()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("이미 CANCELLED 상태인 예약을 본인 이름으로 cancel하면 ALREADY_CANCELLED 예외가 발생한다")
    void 이미_CANCELLED_상태인_예약을_본인_이름으로_cancel하면_ALREADY_CANCELLED_예외가_발생한다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");
        Reservation reservation = Reservation.restore(
                1L,
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                ReservationStatus.CANCELLED,
                createdAt);

        assertThatThrownBy(() -> reservation.cancel(ReservationTestFixtures.RESERVER))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.ALREADY_CANCELLED);
    }

    @Test
    @DisplayName("이미 CANCELLED 상태인 예약이라도 타인 이름으로 cancel하면, 본인 확인이 상태 검사보다 우선하여 NOT_RESERVER 예외가 발생한다")
    void 이미_CANCELLED_상태인_예약을_타인_이름으로_cancel하면_NOT_RESERVER_예외가_발생한다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");
        Reservation reservation = Reservation.restore(
                1L,
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                ReservationStatus.CANCELLED,
                createdAt);

        assertThatThrownBy(() -> reservation.cancel("박철수"))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.NOT_RESERVER);
    }

    @Test
    @DisplayName("공백이 포함된 본인 이름으로 cancel하면 trim되어 성공한다")
    void 공백이_포함된_본인_이름으로_cancel하면_trim되어_성공한다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");
        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                createdAt);

        assertThatNoException().isThrownBy(() -> reservation.cancel("  김본승  "));
    }

    @Test
    @DisplayName("null 이름으로 cancel하면 NOT_RESERVER 예외가 발생한다")
    void null_이름으로_cancel하면_NOT_RESERVER_예외가_발생한다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");
        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                createdAt);

        assertThatThrownBy(() -> reservation.cancel(null))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.NOT_RESERVER);
    }

    @Test
    @DisplayName("본인이 취소한 예약을 다시 본인 이름으로 cancel하면 ALREADY_CANCELLED 예외가 발생한다")
    void 본인이_취소한_예약을_다시_본인_이름으로_cancel하면_ALREADY_CANCELLED_예외가_발생한다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");
        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                createdAt);
        reservation.cancel(ReservationTestFixtures.RESERVER);

        assertThatThrownBy(() -> reservation.cancel(ReservationTestFixtures.RESERVER))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.ALREADY_CANCELLED);
    }
}
