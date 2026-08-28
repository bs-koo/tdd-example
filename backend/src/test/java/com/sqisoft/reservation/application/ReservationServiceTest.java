package com.sqisoft.reservation.application;

import java.time.LocalDateTime;

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
import com.sqisoft.reservation.domain.TimeSlot;
import com.sqisoft.reservation.support.ReservationTestFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationServiceTest {

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
    @DisplayName("AC-17: 모든 규칙을 통과하는 요청은 id가 채워진 예약을 반환한다")
    void 정상_요청은_id가_채워진_예약을_반환한다() {
        LocalDateTime start = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");
        LocalDateTime end = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "10:00");
        CreateReservationCommand command = new CreateReservationCommand(
                1L, ReservationTestFixtures.RESERVER, "주간 회의", start, end);

        Reservation result = service.reserve(command);

        assertThat(result.id()).isNotNull();
    }

    @Test
    @DisplayName("AC-17: 정상 요청으로 생성된 예약의 상태와 필드는 커맨드 값과 일치한다")
    void 정상_요청으로_생성된_예약의_필드는_커맨드_값과_일치한다() {
        LocalDateTime start = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");
        LocalDateTime end = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "10:00");
        CreateReservationCommand command = new CreateReservationCommand(
                1L, ReservationTestFixtures.RESERVER, "주간 회의", start, end);

        Reservation result = service.reserve(command);

        assertThat(result.status()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(result.roomId()).isEqualTo(1L);
        assertThat(result.reserverName()).isEqualTo(ReservationTestFixtures.RESERVER);
        assertThat(result.purpose()).isEqualTo("주간 회의");
        assertThat(result.timeSlot()).isEqualTo(new TimeSlot(start, end));
    }

    @Test
    @DisplayName("AC-17: 정상 요청으로 생성된 예약은 저장소에서 findById로 다시 조회할 수 있다")
    void 정상_요청으로_생성된_예약은_저장소에서_다시_조회할_수_있다() {
        LocalDateTime start = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");
        LocalDateTime end = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "10:00");
        CreateReservationCommand command = new CreateReservationCommand(
                1L, ReservationTestFixtures.RESERVER, "주간 회의", start, end);

        Reservation result = service.reserve(command);

        assertThat(reservationRepository.findById(result.id())).isPresent();
    }

    @Test
    @DisplayName("AC-2: 다른 회의실이면 동일 시간대여도 예약이 성공한다")
    void 다른_회의실이면_동일_시간대여도_예약이_성공한다() {
        LocalDateTime seededCreatedAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00");
        reservationRepository.seed(Reservation.restore(
                1L, 1L, ReservationTestFixtures.RESERVER, "기존 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                ReservationStatus.ACTIVE, seededCreatedAt));

        LocalDateTime start = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");
        LocalDateTime end = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "10:00");
        CreateReservationCommand command = new CreateReservationCommand(
                2L, ReservationTestFixtures.RESERVER, "다른 회의", start, end);

        Reservation result = service.reserve(command);

        assertThat(result.roomId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("AC-3: 취소된 예약과 동일한 회의실·시간대를 요청해도 예약이 성공한다")
    void 취소된_예약과_동일한_회의실_시간대_요청은_성공한다() {
        LocalDateTime seededCreatedAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00");
        reservationRepository.seed(Reservation.restore(
                1L, 1L, ReservationTestFixtures.RESERVER, "취소된 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                ReservationStatus.CANCELLED, seededCreatedAt));

        LocalDateTime start = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");
        LocalDateTime end = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "10:00");
        CreateReservationCommand command = new CreateReservationCommand(
                1L, ReservationTestFixtures.RESERVER, "새 회의", start, end);

        Reservation result = service.reserve(command);

        assertThat(result.status()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("AC-1: 기존 ACTIVE 예약과 겹치는 시간대를 요청하면 OVERLAPPING_RESERVATION 예외를 던진다")
    void 겹치는_시간대_요청은_OVERLAPPING_RESERVATION_예외를_던진다() {
        LocalDateTime seededCreatedAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00");
        reservationRepository.seed(Reservation.restore(
                1L, 1L, ReservationTestFixtures.RESERVER, "기존 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                ReservationStatus.ACTIVE, seededCreatedAt));

        LocalDateTime start = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:30");
        LocalDateTime end = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "10:30");
        CreateReservationCommand command = new CreateReservationCommand(
                1L, ReservationTestFixtures.RESERVER, "새 회의", start, end);

        assertThatThrownBy(() -> service.reserve(command))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.OVERLAPPING_RESERVATION);
    }

    @Test
    @DisplayName("AC-1(경계): 기존 예약 직후에 접하는 시간대 요청은 성공한다")
    void 직후_접하는_시간대_요청은_성공한다() {
        LocalDateTime seededCreatedAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00");
        reservationRepository.seed(Reservation.restore(
                1L, 1L, ReservationTestFixtures.RESERVER, "기존 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                ReservationStatus.ACTIVE, seededCreatedAt));

        LocalDateTime start = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "10:00");
        LocalDateTime end = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "11:00");
        CreateReservationCommand command = new CreateReservationCommand(
                1L, ReservationTestFixtures.RESERVER, "새 회의", start, end);

        Reservation result = service.reserve(command);

        assertThat(result.status()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("AC-19: 존재하지 않는 회의실로 요청하면 ROOM_NOT_FOUND 예외를 던진다")
    void 존재하지_않는_회의실로_요청하면_ROOM_NOT_FOUND_예외를_던진다() {
        LocalDateTime start = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");
        LocalDateTime end = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "10:00");
        CreateReservationCommand command = new CreateReservationCommand(
                999L, "김본승", null, start, end);

        assertThatThrownBy(() -> service.reserve(command))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("정책 위임: 예약자명이 빈 문자열이면 정책 위반이 그대로 전파되어 INVALID_RESERVER_NAME 예외를 던진다")
    void 예약자명이_빈_문자열이면_정책_위반이_그대로_전파된다() {
        LocalDateTime start = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00");
        LocalDateTime end = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "10:00");
        CreateReservationCommand command = new CreateReservationCommand(
                1L, "", null, start, end);

        assertThatThrownBy(() -> service.reserve(command))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_RESERVER_NAME);
    }
}
