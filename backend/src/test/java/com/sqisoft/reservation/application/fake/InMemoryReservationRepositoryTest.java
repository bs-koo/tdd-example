package com.sqisoft.reservation.application.fake;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sqisoft.reservation.domain.Reservation;
import com.sqisoft.reservation.domain.ReservationStatus;
import com.sqisoft.reservation.domain.Room;
import com.sqisoft.reservation.support.ReservationTestFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryReservationRepositoryTest {

    private static final LocalDateTime CREATED_AT =
            ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00");

    @Test
    @DisplayName("id가 null인 예약을 save하면 1부터 증가하는 id가 부여된다")
    void id가_null인_예약을_save하면_1부터_증가하는_id가_부여된다() {
        InMemoryReservationRepository repository = new InMemoryReservationRepository();

        Reservation first = repository.save(newReservation("09:00", "10:00"));
        Reservation second = repository.save(newReservation("10:00", "11:00"));

        assertThat(first.id()).isEqualTo(1L);
        assertThat(second.id()).isEqualTo(2L);
    }

    @Test
    @DisplayName("id가 있는 예약을 save하면 그 id가 유지된 채 저장된다")
    void id가_있는_예약을_save하면_그_id가_유지된_채_저장된다() {
        InMemoryReservationRepository repository = new InMemoryReservationRepository();
        Reservation existing = restoredReservation(10L, "09:00", "10:00", ReservationStatus.ACTIVE);

        Reservation saved = repository.save(existing);

        assertThat(saved.id()).isEqualTo(10L);
    }

    @Test
    @DisplayName("save가 반환한 예약과 같은 내용의 예약을 findById로 다시 조회할 수 있다")
    void save가_반환한_예약과_같은_내용의_예약을_findById로_다시_조회할_수_있다() {
        InMemoryReservationRepository repository = new InMemoryReservationRepository();

        Reservation saved = repository.save(newReservation("09:00", "10:00"));
        Reservation found = repository.findById(saved.id()).orElseThrow();

        assertThat(found.id()).isEqualTo(saved.id());
        assertThat(found.reserverName()).isEqualTo(saved.reserverName());
        assertThat(found.status()).isEqualTo(saved.status());
        assertThat(found.timeSlot()).isEqualTo(saved.timeSlot());
    }

    @Test
    @DisplayName("findById는 저장소 내부 인스턴스가 아닌 준영속 복사본을 반환한다")
    void findById는_저장소_내부_인스턴스가_아닌_준영속_복사본을_반환한다() {
        InMemoryReservationRepository repository = new InMemoryReservationRepository();

        Reservation saved = repository.save(newReservation("09:00", "10:00"));
        Reservation found = repository.findById(saved.id()).orElseThrow();

        assertThat(found).isNotSameAs(saved);
    }

    @Test
    @DisplayName("seed로 id가 5인 예약을 넣은 후 save하면 새 id는 6이다")
    void seed로_id가_5인_예약을_넣은_후_save하면_새_id는_6이다() {
        InMemoryReservationRepository repository = new InMemoryReservationRepository();
        repository.seed(restoredReservation(5L, "09:00", "10:00", ReservationStatus.ACTIVE));

        Reservation saved = repository.save(newReservation("10:00", "11:00"));

        assertThat(saved.id()).isEqualTo(6L);
    }

    @Test
    @DisplayName("seed에 id가 null인 예약을 주면 IllegalArgumentException이 발생한다")
    void seed에_id가_null인_예약을_주면_IllegalArgumentException이_발생한다() {
        InMemoryReservationRepository repository = new InMemoryReservationRepository();
        Reservation withoutId = newReservation("09:00", "10:00");

        assertThatThrownBy(() -> repository.seed(withoutId))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("같은 방이라도 다른 날짜의 예약은 조회 결과에서 제외된다")
    void 같은_방이라도_다른_날짜의_예약은_조회_결과에서_제외된다() {
        InMemoryReservationRepository repository = new InMemoryReservationRepository();
        LocalDate nextDate = ReservationTestFixtures.DEFAULT_DATE.plusDays(1);
        repository.seed(
                restoredReservation(1L, "09:00", "10:00", ReservationStatus.ACTIVE),
                Reservation.restore(
                        2L,
                        ReservationTestFixtures.ROOM_ID,
                        ReservationTestFixtures.RESERVER,
                        "주간 회의",
                        ReservationTestFixtures.slot(nextDate, "09:00", "10:00"),
                        ReservationStatus.ACTIVE,
                        CREATED_AT));

        List<Reservation> result = repository.findActiveByRoomIdAndDate(
                ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.DEFAULT_DATE);

        assertThat(result).extracting(Reservation::id).containsExactly(1L);
    }

    @Test
    @DisplayName("같은 날짜라도 다른 방의 예약은 조회 결과에서 제외된다")
    void 같은_날짜라도_다른_방의_예약은_조회_결과에서_제외된다() {
        InMemoryReservationRepository repository = new InMemoryReservationRepository();
        repository.seed(
                Reservation.restore(1L, 1L, ReservationTestFixtures.RESERVER, "주간 회의",
                        ReservationTestFixtures.defaultSlot("09:00", "10:00"), ReservationStatus.ACTIVE, CREATED_AT),
                Reservation.restore(2L, 2L, ReservationTestFixtures.RESERVER, "주간 회의",
                        ReservationTestFixtures.defaultSlot("09:00", "10:00"), ReservationStatus.ACTIVE, CREATED_AT));

        List<Reservation> result = repository.findActiveByRoomIdAndDate(1L, ReservationTestFixtures.DEFAULT_DATE);

        assertThat(result).extracting(Reservation::id).containsExactly(1L);
    }

    @Test
    @DisplayName("CANCELLED 상태의 예약은 조회 결과에서 제외된다")
    void CANCELLED_상태의_예약은_조회_결과에서_제외된다() {
        InMemoryReservationRepository repository = new InMemoryReservationRepository();
        repository.seed(
                restoredReservation(1L, "09:00", "10:00", ReservationStatus.ACTIVE),
                restoredReservation(2L, "11:00", "12:00", ReservationStatus.CANCELLED));

        List<Reservation> result = repository.findActiveByRoomIdAndDate(
                ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.DEFAULT_DATE);

        assertThat(result).extracting(Reservation::id).containsExactly(1L);
    }

    @Test
    @DisplayName("조회 결과는 startAt 오름차순으로 정렬된다")
    void 조회_결과는_startAt_오름차순으로_정렬된다() {
        InMemoryReservationRepository repository = new InMemoryReservationRepository();
        repository.seed(
                restoredReservation(1L, "11:00", "12:00", ReservationStatus.ACTIVE),
                restoredReservation(2L, "09:00", "10:00", ReservationStatus.ACTIVE),
                restoredReservation(3L, "10:00", "11:00", ReservationStatus.ACTIVE));

        List<Reservation> result = repository.findActiveByRoomIdAndDate(
                ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.DEFAULT_DATE);

        assertThat(result).extracting(Reservation::id).containsExactly(2L, 3L, 1L);
    }

    @Test
    @DisplayName("findActiveByRoomIdAndDate도 저장소 내부 인스턴스가 아닌 복사본을 반환한다")
    void findActiveByRoomIdAndDate도_저장소_내부_인스턴스가_아닌_복사본을_반환한다() {
        InMemoryReservationRepository repository = new InMemoryReservationRepository();

        Reservation saved = repository.save(newReservation("09:00", "10:00"));
        Reservation found = repository.findActiveByRoomIdAndDate(
                ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.DEFAULT_DATE).get(0);

        assertThat(found).isNotSameAs(saved);
        assertThat(found.id()).isEqualTo(saved.id());
        assertThat(found.roomId()).isEqualTo(saved.roomId());
        assertThat(found.reserverName()).isEqualTo(saved.reserverName());
        assertThat(found.purpose()).isEqualTo(saved.purpose());
        assertThat(found.timeSlot()).isEqualTo(saved.timeSlot());
        assertThat(found.status()).isEqualTo(saved.status());
        assertThat(found.createdAt()).isEqualTo(saved.createdAt());
    }

    @Test
    @DisplayName("findAll은 생성자에 전달한 순서를 그대로 유지한다")
    void findAll은_생성자에_전달한_순서를_그대로_유지한다() {
        Room second = Room.of(2L, "소회의실", 4, "2층");
        Room first = Room.of(1L, "대회의실", 20, "3층");
        InMemoryRoomRepository repository = new InMemoryRoomRepository(second, first);

        assertThat(repository.findAll()).extracting(Room::id).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("existsById는 존재하는 id에 true를 반환한다")
    void existsById는_존재하는_id에_true를_반환한다() {
        InMemoryRoomRepository repository = new InMemoryRoomRepository(Room.of(1L, "대회의실", 20, "3층"));

        assertThat(repository.existsById(1L)).isTrue();
    }

    @Test
    @DisplayName("existsById는 존재하지 않는 id에 false를 반환한다")
    void existsById는_존재하지_않는_id에_false를_반환한다() {
        InMemoryRoomRepository repository = new InMemoryRoomRepository(Room.of(1L, "대회의실", 20, "3층"));

        assertThat(repository.existsById(999L)).isFalse();
    }

    private static Reservation newReservation(String startHHmm, String endHHmm) {
        return Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.defaultSlot(startHHmm, endHHmm),
                CREATED_AT);
    }

    private static Reservation restoredReservation(Long id, String startHHmm, String endHHmm, ReservationStatus status) {
        return Reservation.restore(
                id,
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.defaultSlot(startHHmm, endHHmm),
                status,
                CREATED_AT);
    }
}
