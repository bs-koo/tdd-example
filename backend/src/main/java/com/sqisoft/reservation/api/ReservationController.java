package com.sqisoft.reservation.api;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sqisoft.reservation.api.dto.CancelReservationRequest;
import com.sqisoft.reservation.api.dto.CreateReservationRequest;
import com.sqisoft.reservation.api.dto.ReservationResponse;
import com.sqisoft.reservation.application.ReservationService;
import com.sqisoft.reservation.domain.Reservation;

@RestController
@RequestMapping("/api/reservations")
public class ReservationController {

    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse create(@RequestBody CreateReservationRequest request) {
        Reservation reservation = reservationService.reserve(request.toCommand());
        return ReservationResponse.from(reservation);
    }

    @PostMapping("/{id}/cancel")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable Long id, @RequestBody CancelReservationRequest request) {
        reservationService.cancel(id, request.reserverName());
    }
}
