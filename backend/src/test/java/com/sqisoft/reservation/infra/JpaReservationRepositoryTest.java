package com.sqisoft.reservation.infra;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.sqisoft.reservation.application.ReservationRepository;
import com.sqisoft.reservation.domain.Reservation;
import com.sqisoft.reservation.domain.ReservationStatus;
import com.sqisoft.reservation.support.ReservationTestFixtures;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link InMemoryReservationRepositoryTest}가 검증한 것과 동일한 쿼리 의미론을
 * 실제 JPA 어댑터({@link JpaReservationRepository})에 대해 검증한다 (설계서 C-11).
 */
@DataJpaTest
@AutoConfigureTestDatabase
class JpaReservationRepositoryTest {

    private static final LocalDateTime CREATED_AT =
            ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00");

    @Autowired
    private ReservationJpaRepository jpaRepository;

    @PersistenceContext
    private EntityManager em;

    private ReservationRepository repository;

    @BeforeEach
    void setUp() {
        repository = new JpaReservationRepository(jpaRepository);
    }

    @Test
    @DisplayName("save로 저장하면 id가 부여된다")
    void save로_저장하면_id가_부여된다() {
        Reservation saved = repository.save(newReservation(ReservationTestFixtures.DEFAULT_DATE, "09:00", "10:00"));

        assertThat(saved.id()).isNotNull();
    }

    @Test
    @DisplayName("findById로 저장한 예약을 다시 찾을 수 있다")
    void findById로_저장한_예약을_다시_찾을_수_있다() {
        Reservation saved = repository.save(newReservation(ReservationTestFixtures.DEFAULT_DATE, "09:00", "10:00"));

        Optional<Reservation> found = repository.findById(saved.id());

        assertThat(found).isPresent();
        assertThat(found.get().id()).isEqualTo(saved.id());
    }

    @Test
    @DisplayName("없는 id로 findById하면 Optional_empty를 반환한다")
    void 없는_id로_findById하면_Optional_empty를_반환한다() {
        Optional<Reservation> found = repository.findById(999L);

        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("같은 방이라도 다른 날짜의 예약은 조회 결과에서 제외된다")
    void 같은_방이라도_다른_날짜의_예약은_조회_결과에서_제외된다() {
        LocalDate nextDate = ReservationTestFixtures.DEFAULT_DATE.plusDays(1);
        repository.save(newReservation(ReservationTestFixtures.DEFAULT_DATE, "09:00", "10:00"));
        repository.save(newReservation(nextDate, "09:00", "10:00"));

        List<Reservation> result = repository.findActiveByRoomIdAndDate(
                ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.DEFAULT_DATE);

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("같은 날짜라도 다른 방의 예약은 조회 결과에서 제외된다")
    void 같은_날짜라도_다른_방의_예약은_조회_결과에서_제외된다() {
        repository.save(newReservationForRoom(1L, ReservationTestFixtures.DEFAULT_DATE, "09:00", "10:00"));
        repository.save(newReservationForRoom(2L, ReservationTestFixtures.DEFAULT_DATE, "09:00", "10:00"));

        List<Reservation> result = repository.findActiveByRoomIdAndDate(1L, ReservationTestFixtures.DEFAULT_DATE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).roomId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("CANCELLED 상태의 예약은 조회 결과에서 제외된다")
    void CANCELLED_상태의_예약은_조회_결과에서_제외된다() {
        Reservation active = repository.save(newReservation(ReservationTestFixtures.DEFAULT_DATE, "09:00", "10:00"));
        Reservation toCancel = repository.save(newReservation(ReservationTestFixtures.DEFAULT_DATE, "11:00", "12:00"));
        toCancel.cancel(ReservationTestFixtures.RESERVER);
        repository.save(toCancel);

        List<Reservation> result = repository.findActiveByRoomIdAndDate(
                ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.DEFAULT_DATE);

        assertThat(result).extracting(Reservation::id).containsExactly(active.id());
    }

    @Test
    @DisplayName("조회 결과는 startAt 오름차순으로 정렬된다")
    void 조회_결과는_startAt_오름차순으로_정렬된다() {
        repository.save(newReservation(ReservationTestFixtures.DEFAULT_DATE, "11:00", "12:00"));
        repository.save(newReservation(ReservationTestFixtures.DEFAULT_DATE, "09:00", "10:00"));
        repository.save(newReservation(ReservationTestFixtures.DEFAULT_DATE, "10:00", "11:00"));

        List<Reservation> result = repository.findActiveByRoomIdAndDate(
                ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.DEFAULT_DATE);

        assertThat(result)
                .extracting(reservation -> reservation.timeSlot().start())
                .containsExactly(
                        ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00"),
                        ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "10:00"),
                        ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "11:00"));
    }

    @Test
    @DisplayName("당일 00시 예약은 포함되고 익일 00시 예약은 제외된다")
    void 당일_00시_예약은_포함되고_익일_00시_예약은_제외된다() {
        LocalDate nextDate = ReservationTestFixtures.DEFAULT_DATE.plusDays(1);
        Reservation startOfDay = repository.save(newReservation(ReservationTestFixtures.DEFAULT_DATE, "00:00", "01:00"));
        repository.save(newReservation(nextDate, "00:00", "01:00"));

        List<Reservation> result = repository.findActiveByRoomIdAndDate(
                ReservationTestFixtures.ROOM_ID, ReservationTestFixtures.DEFAULT_DATE);

        assertThat(result).extracting(Reservation::id).containsExactly(startOfDay.id());
    }

    @Test
    @DisplayName("AC-39: 준영속 상태의 예약을 취소한 뒤 save 하면 재조회 시 CANCELLED로 영속화되어 있다")
    void 준영속_상태에서_취소_후_save하면_영속화된다() {
        Reservation saved = repository.save(newReservation(ReservationTestFixtures.DEFAULT_DATE, "09:00", "10:00"));
        Long id = saved.id();
        em.flush();
        em.clear();

        Reservation reloaded = repository.findById(id).orElseThrow();
        // findById 직후에는 reloaded가 같은 영속성 컨텍스트(em)에 다시 관리(managed) 상태로 편입된다.
        // 이 상태에서 곧바로 cancel() 하면 save() 호출 없이도 더티체킹이 flush 시 UPDATE를 발행해
        // save()의 필요성을 검증하지 못한다 (기존 8건이 눈이 먼 것과 동일한 함정).
        // em.detach로 reloaded 하나만 명시적으로 준영속화해 cancel()이 순수 POJO 변이가 되도록 만든다.
        em.detach(reloaded);

        reloaded.cancel(ReservationTestFixtures.RESERVER);
        repository.save(reloaded);
        em.flush();
        em.clear();

        Reservation refetched = repository.findById(id).orElseThrow();
        assertThat(refetched.status()).isEqualTo(ReservationStatus.CANCELLED);
    }

    private static Reservation newReservation(LocalDate date, String startHHmm, String endHHmm) {
        return newReservationForRoom(ReservationTestFixtures.ROOM_ID, date, startHHmm, endHHmm);
    }

    private static Reservation newReservationForRoom(Long roomId, LocalDate date, String startHHmm, String endHHmm) {
        return Reservation.create(
                roomId,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.slot(date, startHHmm, endHHmm),
                CREATED_AT);
    }
}
