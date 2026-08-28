package com.sqisoft.reservation.api;

import com.sqisoft.reservation.domain.ReservationErrorCode;
import org.springframework.http.HttpStatus;

import java.util.EnumMap;
import java.util.Map;

final class ErrorCodeHttpStatus {

    private static final Map<ReservationErrorCode, HttpStatus> MAPPING = new EnumMap<>(ReservationErrorCode.class);

    static {
        MAPPING.put(ReservationErrorCode.OVERLAPPING_RESERVATION, HttpStatus.CONFLICT);
        MAPPING.put(ReservationErrorCode.INVALID_TIME_UNIT, HttpStatus.BAD_REQUEST);
        MAPPING.put(ReservationErrorCode.OUTSIDE_BUSINESS_HOURS, HttpStatus.BAD_REQUEST);
        MAPPING.put(ReservationErrorCode.INVALID_DURATION, HttpStatus.BAD_REQUEST);
        MAPPING.put(ReservationErrorCode.PAST_DATETIME, HttpStatus.BAD_REQUEST);
        MAPPING.put(ReservationErrorCode.TOO_FAR_IN_FUTURE, HttpStatus.BAD_REQUEST);
        MAPPING.put(ReservationErrorCode.NOT_RESERVER, HttpStatus.FORBIDDEN);
        MAPPING.put(ReservationErrorCode.ALREADY_CANCELLED, HttpStatus.CONFLICT);
        MAPPING.put(ReservationErrorCode.RESERVATION_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAPPING.put(ReservationErrorCode.ROOM_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAPPING.put(ReservationErrorCode.INVALID_RESERVER_NAME, HttpStatus.BAD_REQUEST);
        MAPPING.put(ReservationErrorCode.INVALID_PURPOSE_LENGTH, HttpStatus.BAD_REQUEST);
        MAPPING.put(ReservationErrorCode.INVALID_DATE_FORMAT, HttpStatus.BAD_REQUEST);
    }

    private ErrorCodeHttpStatus() {
    }

    static HttpStatus of(ReservationErrorCode code) {
        HttpStatus status = MAPPING.get(code);
        if (status == null) {
            throw new IllegalStateException("매핑되지 않은 오류 코드: " + code);
        }
        return status;
    }
}
