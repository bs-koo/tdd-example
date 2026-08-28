package com.sqisoft.reservation.application;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sqisoft.reservation.application.fake.InMemoryReservationRepository;
import com.sqisoft.reservation.application.fake.InMemoryRoomRepository;
import com.sqisoft.reservation.domain.Reservation;
import com.sqisoft.reservation.domain.ReservationErrorCode;
import com.sqisoft.reservation.domain.ReservationException;
import com.sqisoft.reservation.domain.ReservationPolicy;
import com.sqisoft.reservation.domain.ReservationStatus;
import com.sqisoft.reservation.domain.Room;
import com.sqisoft.reservation.support.ReservationTestFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationServiceCancelTest {

    private InMemoryReservationRepository reservationRepository;
    private InMemoryRoomRepository roomRepository;
    private ReservationService service;

    @BeforeEach
    void setUp() {
        reservationRepository = new InMemoryReservationRepository();
        roomRepository = new InMemoryRoomRepository(
                Room.of(1L, "대회의실", 20, "3층"),
                Room.of(2L, "중회의실", 10, "3층"));
        service = new ReservationService(
                reservationRepository,
                roomRepository,
                new ReservationPolicy(ReservationTestFixtures.FIXED_CLOCK),
                ReservationTestFixtures.FIXED_CLOCK);
    }

    @Test
    @DisplayName("AC-23: 존재하지 않는 예약을 취소하면 RESERVATION_NOT_FOUND 예외를 던진다")
    void 존재하지_않는_예약을_취소하면_RESERVATION_NOT_FOUND_예외를_던진다() {
        assertThatThrownBy(() -> service.cancel(999L, ReservationTestFixtures.RESERVER))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND);
    }

    @Test
    @DisplayName("도메인 위임: 타인 이름으로 취소하면 NOT_RESERVER 예외가 그대로 전파된다")
    void 타인_이름으로_취소하면_NOT_RESERVER_예외가_그대로_전파된다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00");
        reservationRepository.seed(Reservation.restore(
                1L, ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.RESERVER, "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                ReservationStatus.ACTIVE, createdAt));

        assertThatThrownBy(() -> service.cancel(1L, "박철수"))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.NOT_RESERVER);
    }

    @Test
    @DisplayName("도메인 위임: 이미 취소된 예약을 본인 이름으로 취소하면 ALREADY_CANCELLED 예외가 그대로 전파된다")
    void 이미_취소된_예약을_본인_이름으로_취소하면_ALREADY_CANCELLED_예외가_그대로_전파된다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00");
        reservationRepository.seed(Reservation.restore(
                1L, ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.RESERVER, "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                ReservationStatus.CANCELLED, createdAt));

        assertThatThrownBy(() -> service.cancel(1L, ReservationTestFixtures.RESERVER))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.ALREADY_CANCELLED);
    }

    @Test
    @DisplayName("본인 이름으로 취소하면 예외 없이 저장소에 CANCELLED 상태로 영속화된다")
    void 본인_이름으로_취소하면_저장소에_CANCELLED_상태로_영속화된다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00");
        reservationRepository.seed(Reservation.restore(
                1L, ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.RESERVER, "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                ReservationStatus.ACTIVE, createdAt));

        assertThatNoException().isThrownBy(() -> service.cancel(1L, ReservationTestFixtures.RESERVER));

        assertThat(reservationRepository.findById(1L))
                .get()
                .extracting(Reservation::status)
                .isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("AC-24: 취소 후 동일 회의실·동일 시간대로 재예약하면 성공한다")
    void 취소_후_동일_시간대로_재예약하면_성공한다() {
        LocalDateTime start = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");
        LocalDateTime end = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "10:00");
        CreateReservationCommand firstCommand = new CreateReservationCommand(
                ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.RESERVER, "기존 회의", start, end);
        Reservation first = service.reserve(firstCommand);

        service.cancel(first.id(), ReservationTestFixtures.RESERVER);

        CreateReservationCommand secondCommand = new CreateReservationCommand(
                ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.RESERVER, "새 회의", start, end);
        Reservation second = service.reserve(secondCommand);

        assertThat(second.status()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("AC-26b: 존재하지 않는 회의실로 조회하면 ROOM_NOT_FOUND 예외를 던진다")
    void 존재하지_않는_회의실로_조회하면_ROOM_NOT_FOUND_예외를_던진다() {
        assertThatThrownBy(() -> service.findActiveByRoomAndDate(999L, ReservationTestFixtures.DEFAULT_DATE))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("회의실과 날짜로 조회하면 ACTIVE 예약만 반환하고 CANCELLED 예약은 제외한다")
    void 회의실과_날짜로_조회하면_ACTIVE_예약만_반환한다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "07:00");
        reservationRepository.seed(
                Reservation.restore(1L, ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.RESERVER, "회의1",
                        ReservationTestFixtures.defaultSlot("09:00", "10:00"), ReservationStatus.ACTIVE, createdAt),
                Reservation.restore(2L, ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.RESERVER, "회의2",
                        ReservationTestFixtures.defaultSlot("11:00", "12:00"), ReservationStatus.ACTIVE, createdAt),
                Reservation.restore(3L, ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.RESERVER, "취소된 회의",
                        ReservationTestFixtures.defaultSlot("13:00", "14:00"), ReservationStatus.CANCELLED, createdAt));

        List<Reservation> result = service.findActiveByRoomAndDate(
                ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.DEFAULT_DATE);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("AC-28: 예약이 없는 날짜로 조회하면 빈 리스트를 반환한다")
    void 예약이_없는_날짜로_조회하면_빈_리스트를_반환한다() {
        List<Reservation> result = service.findActiveByRoomAndDate(
                ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.DEFAULT_DATE);

        assertThat(result).isEmpty();
    }
}
