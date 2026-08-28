package com.sqisoft.reservation.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.sqisoft.reservation.domain.Reservation;
import com.sqisoft.reservation.domain.ReservationErrorCode;
import com.sqisoft.reservation.domain.ReservationException;
import com.sqisoft.reservation.domain.ReservationPolicy;
import com.sqisoft.reservation.domain.TimeSlot;

public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final ReservationPolicy policy;
    private final Clock clock;

    public ReservationService(ReservationRepository reservationRepository,
            RoomRepository roomRepository,
            ReservationPolicy policy,
            Clock clock) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.policy = policy;
        this.clock = clock;
    }

    public Reservation reserve(CreateReservationCommand command) {
        TimeSlot slot = new TimeSlot(command.startAt(), command.endAt());

        policy.validate(command.reserverName(), command.purpose(), slot);

        if (command.roomId() == null || !roomRepository.existsById(command.roomId())) {
            throw new ReservationException(ReservationErrorCode.ROOM_NOT_FOUND);
        }

        boolean overlaps = reservationRepository
                .findActiveByRoomIdAndDate(command.roomId(), slot.date())
                .stream().anyMatch(r -> r.overlapsWith(slot));
        if (overlaps) {
            throw new ReservationException(ReservationErrorCode.OVERLAPPING_RESERVATION);
        }

        Reservation reservation = Reservation.create(
                command.roomId(), command.reserverName(), command.purpose(), slot,
                LocalDateTime.now(clock));
        return reservationRepository.save(reservation);
    }

    public void cancel(Long reservationId, String requesterName) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.RESERVATION_NOT_FOUND));
        reservation.cancel(requesterName);
        reservationRepository.save(reservation);
    }

    public List<Reservation> findActiveByRoomAndDate(Long roomId, LocalDate date) {
        if (!roomRepository.existsById(roomId)) {
            throw new ReservationException(ReservationErrorCode.ROOM_NOT_FOUND);
        }
        return reservationRepository.findActiveByRoomIdAndDate(roomId, date);
    }
}
