package com.sqisoft.reservation.api;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sqisoft.reservation.api.dto.ReservationResponse;
import com.sqisoft.reservation.api.dto.RoomResponse;
import com.sqisoft.reservation.api.dto.UpdateRoomRequest;
import com.sqisoft.reservation.application.ReservationService;
import com.sqisoft.reservation.application.RoomService;
import com.sqisoft.reservation.domain.Member;
import com.sqisoft.reservation.domain.ReservationErrorCode;
import com.sqisoft.reservation.domain.ReservationException;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final ReservationService reservationService;
    private final RoomService roomService;

    public RoomController(ReservationService reservationService, RoomService roomService) {
        this.reservationService = reservationService;
        this.roomService = roomService;
    }

    /** @LoginMember 를 첫 파라미터로 두어 인증이 본문 파싱보다 먼저 판정되게 한다. */
    @PutMapping("/{roomId}")
    public RoomResponse update(@LoginMember Member member, @PathVariable Long roomId,
            @RequestBody UpdateRoomRequest request) {
        return RoomResponse.from(roomService.updateRoom(roomId, member.id(), request.toCommand()));
    }

    @DeleteMapping("/{roomId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@LoginMember Member member, @PathVariable Long roomId) {
        roomService.deleteRoom(roomId, member.id());
    }

    @GetMapping("/{roomId}/reservations")
    public List<ReservationResponse> findReservations(@LoginMember Member member,
            @PathVariable Long roomId, @RequestParam String date) {
        LocalDate parsedDate = parseDate(date);
        return reservationService.findActiveByRoomAndDate(roomId, member.id(), parsedDate).stream()
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
