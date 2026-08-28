package com.sqisoft.reservation.infra;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.sqisoft.reservation.application.ReservationRepository;
import com.sqisoft.reservation.domain.Reservation;
import com.sqisoft.reservation.domain.ReservationStatus;

@Repository
public class JpaReservationRepository implements ReservationRepository {

    private final ReservationJpaRepository jpa;

    public JpaReservationRepository(ReservationJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Reservation save(Reservation reservation) {
        return jpa.save(reservation);
    }

    @Override
    public Optional<Reservation> findById(Long id) {
        return jpa.findById(id);
    }

    @Override
    public List<Reservation> findActiveByRoomIdAndDate(Long roomId, LocalDate date) {
        return jpa.findByRoomIdAndStatusAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
                roomId, ReservationStatus.ACTIVE, date.atStartOfDay(), date.plusDays(1).atStartOfDay());
    }
}
