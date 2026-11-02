package com.sqisoft.reservation.api;

import com.sqisoft.reservation.domain.AuthErrorCode;
import com.sqisoft.reservation.domain.ErrorCode;
import com.sqisoft.reservation.domain.GroupErrorCode;
import com.sqisoft.reservation.domain.ReservationErrorCode;
import org.springframework.http.HttpStatus;

import java.util.HashMap;
import java.util.Map;

final class ErrorCodeHttpStatus {

    private static final Map<ErrorCode, HttpStatus> MAPPING = new HashMap<>();

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
        MAPPING.put(ReservationErrorCode.INVALID_PURPOSE_LENGTH, HttpStatus.BAD_REQUEST);
        MAPPING.put(ReservationErrorCode.INVALID_DATE_FORMAT, HttpStatus.BAD_REQUEST);
        MAPPING.put(ReservationErrorCode.INVALID_ROOM_NAME, HttpStatus.BAD_REQUEST);
        MAPPING.put(ReservationErrorCode.INVALID_ROOM_CAPACITY, HttpStatus.BAD_REQUEST);
        MAPPING.put(ReservationErrorCode.INVALID_ROOM_LOCATION, HttpStatus.BAD_REQUEST);
        MAPPING.put(ReservationErrorCode.ROOM_HAS_ACTIVE_RESERVATIONS, HttpStatus.CONFLICT);

        MAPPING.put(AuthErrorCode.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED);
        MAPPING.put(AuthErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED);
        MAPPING.put(AuthErrorCode.EMAIL_ALREADY_REGISTERED, HttpStatus.CONFLICT);
        MAPPING.put(AuthErrorCode.INVALID_EMAIL_FORMAT, HttpStatus.BAD_REQUEST);
        MAPPING.put(AuthErrorCode.INVALID_PASSWORD_LENGTH, HttpStatus.BAD_REQUEST);
        MAPPING.put(AuthErrorCode.INVALID_MEMBER_NAME, HttpStatus.BAD_REQUEST);
        MAPPING.put(AuthErrorCode.MEMBER_NOT_FOUND, HttpStatus.NOT_FOUND);

        MAPPING.put(GroupErrorCode.GROUP_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAPPING.put(GroupErrorCode.INVALID_GROUP_NAME, HttpStatus.BAD_REQUEST);
        MAPPING.put(GroupErrorCode.NOT_GROUP_OWNER, HttpStatus.FORBIDDEN);
        MAPPING.put(GroupErrorCode.NOT_GROUP_MEMBER, HttpStatus.FORBIDDEN);
        MAPPING.put(GroupErrorCode.ALREADY_INVITED, HttpStatus.CONFLICT);
        MAPPING.put(GroupErrorCode.SELF_INVITATION, HttpStatus.BAD_REQUEST);
        MAPPING.put(GroupErrorCode.INVITATION_NOT_FOUND, HttpStatus.NOT_FOUND);
        MAPPING.put(GroupErrorCode.NOT_INVITEE, HttpStatus.FORBIDDEN);
        MAPPING.put(GroupErrorCode.INVITATION_ALREADY_RESPONDED, HttpStatus.CONFLICT);
    }

    private ErrorCodeHttpStatus() {
    }

    static HttpStatus of(ErrorCode code) {
        HttpStatus status = MAPPING.get(code);
        if (status == null) {
            throw new IllegalStateException("매핑되지 않은 오류 코드: " + code);
        }
        return status;
    }
}
