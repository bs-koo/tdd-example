package com.sqisoft.reservation.application;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import com.sqisoft.reservation.domain.Reservation;

public interface ReservationRepository {

    Reservation save(Reservation reservation);

    Optional<Reservation> findById(Long id);

    List<Reservation> findActiveByRoomIdAndDate(Long roomId, LocalDate date);
}
