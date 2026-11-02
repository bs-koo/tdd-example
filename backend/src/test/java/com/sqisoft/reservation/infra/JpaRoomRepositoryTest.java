package com.sqisoft.reservation.infra;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import com.sqisoft.reservation.application.ReservationRepository;
import com.sqisoft.reservation.application.RoomRepository;
import com.sqisoft.reservation.domain.Reservation;
import com.sqisoft.reservation.domain.Room;
import com.sqisoft.reservation.support.ReservationTestFixtures;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/** RoomRepository.deleteById 계약을 실제 JPA 어댑터에 대해 검증한다 (InMemoryRoomRepositoryTest와 동일 계약 + BR-6). */
@DataJpaTest
@AutoConfigureTestDatabase
class JpaRoomRepositoryTest {

    private static final Long GROUP_ID = 1L;

    @Autowired
    private RoomJpaRepository roomJpaRepository;

    @Autowired
    private ReservationJpaRepository reservationJpaRepository;

    @PersistenceContext
    private EntityManager em;

    private RoomRepository repository;
    private ReservationRepository reservationRepository;
    private Room target;
    private Room other;

    @BeforeEach
    void setUp() {
        repository = new JpaRoomRepository(roomJpaRepository);
        reservationRepository = new JpaReservationRepository(reservationJpaRepository);
        target = repository.save(Room.create(GROUP_ID, "삭제대상실", 10, "3층"));
        other = repository.save(Room.create(GROUP_ID, "남는실", 4, "2층"));
        em.flush();
        em.clear();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    @Test
    @DisplayName("deleteById 후 findById는 empty다")
    void deleteById_후_findById는_empty다() {
        repository.deleteById(target.id());
        flushAndClear();

        assertThat(repository.findById(target.id())).isEmpty();
    }

    @Test
    @DisplayName("deleteById 후 findAllByGroupId에서 그 방만 빠진다")
    void deleteById_후_그룹_목록에서_그_방만_빠진다() {
        repository.deleteById(target.id());
        flushAndClear();

        assertThat(repository.findAllByGroupId(GROUP_ID)).extracting(Room::id).contains(other.id())
                .doesNotContain(target.id());
    }

    @Test
    @DisplayName("deleteById 후 existsById는 false이고 다른 방은 true다")
    void deleteById_후_existsById는_false다() {
        repository.deleteById(target.id());
        flushAndClear();

        assertThat(repository.existsById(target.id())).isFalse();
        assertThat(repository.existsById(other.id())).isTrue();
    }

    @Test
    @DisplayName("없는 id를 deleteById해도 예외가 없다")
    void 없는_id를_deleteById해도_예외가_없다() {
        assertThatNoException().isThrownBy(() -> repository.deleteById(999999L));
        assertThat(repository.existsById(other.id())).isTrue();
    }

    @Test
    @DisplayName("null id를 deleteById해도 예외 없이 무시된다")
    void null_id를_deleteById해도_예외가_없다() {
        assertThatNoException().isThrownBy(() -> repository.deleteById(null));
        assertThat(repository.existsById(target.id())).isTrue();
    }

    @Test
    @DisplayName("BR-6: 방을 삭제해도 그 roomId를 가진 예약 행은 그대로 남는다")
    void 방을_삭제해도_예약_행은_남는다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00");
        Reservation saved = reservationRepository.save(Reservation.create(
                target.id(), ReservationTestFixtures.MEMBER_ID, ReservationTestFixtures.RESERVER, "주간 회의",
                ReservationTestFixtures.slot(ReservationTestFixtures.DEFAULT_DATE, "09:00", "10:00"), createdAt));
        flushAndClear();

        repository.deleteById(target.id());
        flushAndClear();

        Reservation kept = reservationRepository.findById(saved.id()).orElseThrow();
        assertThat(kept.roomId()).isEqualTo(target.id());
    }
}
