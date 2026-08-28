package com.sqisoft.reservation.infra;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sqisoft.reservation.domain.Reservation;
import com.sqisoft.reservation.domain.ReservationStatus;

interface ReservationJpaRepository extends JpaRepository<Reservation, Long> {

    List<Reservation> findByRoomIdAndStatusAndStartAtGreaterThanEqualAndStartAtLessThanOrderByStartAtAsc(
            Long roomId, ReservationStatus status, LocalDateTime from, LocalDateTime to);
}
