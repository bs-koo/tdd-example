package com.sqisoft.reservation.api;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.sqisoft.reservation.api.dto.ReservationResponse;
import com.sqisoft.reservation.api.dto.RoomResponse;
import com.sqisoft.reservation.application.ReservationService;
import com.sqisoft.reservation.application.RoomService;
import com.sqisoft.reservation.domain.ReservationErrorCode;
import com.sqisoft.reservation.domain.ReservationException;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final ReservationService reservationService;

    public RoomController(RoomService roomService, ReservationService reservationService) {
        this.roomService = roomService;
        this.reservationService = reservationService;
    }

    @GetMapping
    public List<RoomResponse> findAll() {
        return roomService.findAll().stream()
                .map(RoomResponse::from)
                .toList();
    }

    @GetMapping("/{roomId}/reservations")
    public List<ReservationResponse> findReservations(@PathVariable Long roomId, @RequestParam String date) {
        LocalDate parsedDate = parseDate(date);
        return reservationService.findActiveByRoomAndDate(roomId, parsedDate).stream()
                .map(ReservationResponse::from)
                .toList();
    }

    private LocalDate parseDate(String date) {
        try {
            return LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new ReservationException(ReservationErrorCode.INVALID_DATE_FORMAT);
        }
    }
}
