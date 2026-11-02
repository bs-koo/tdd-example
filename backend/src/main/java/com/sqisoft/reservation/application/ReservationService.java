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
    private final RoomService roomService;
    private final ReservationPolicy policy;
    private final Clock clock;

    public ReservationService(ReservationRepository reservationRepository,
            RoomService roomService,
            ReservationPolicy policy,
            Clock clock) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
        this.policy = policy;
        this.clock = clock;
    }

    public Reservation reserve(CreateReservationCommand command) {
        TimeSlot slot = new TimeSlot(command.startAt(), command.endAt());

        policy.validate(command.purpose(), slot);
        roomService.requireAccessibleRoom(command.roomId(), command.memberId());

        boolean overlaps = reservationRepository
                .findActiveByRoomIdAndDate(command.roomId(), slot.date())
                .stream().anyMatch(r -> r.overlapsWith(slot));
        if (overlaps) {
            throw new ReservationException(ReservationErrorCode.OVERLAPPING_RESERVATION);
        }

        Reservation reservation = Reservation.create(
                command.roomId(), command.memberId(), command.reserverName(), command.purpose(), slot,
                LocalDateTime.now(clock));
        return reservationRepository.save(reservation);
    }

    public void cancel(Long reservationId, Long requesterId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.RESERVATION_NOT_FOUND));
        reservation.cancel(requesterId);
        reservationRepository.save(reservation);
    }

    public List<Reservation> findActiveByRoomAndDate(Long roomId, Long memberId, LocalDate date) {
        roomService.requireAccessibleRoom(roomId, memberId);
        return reservationRepository.findActiveByRoomIdAndDate(roomId, date);
    }
}
