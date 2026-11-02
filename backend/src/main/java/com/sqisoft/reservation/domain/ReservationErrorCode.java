package com.sqisoft.reservation.domain;

public enum ReservationErrorCode implements ErrorCode {

    OVERLAPPING_RESERVATION("해당 시간대에 이미 예약이 있습니다."),
    INVALID_TIME_UNIT("예약 시각은 30분 단위여야 합니다."),
    OUTSIDE_BUSINESS_HOURS("09:00~18:00 사이만 예약할 수 있습니다."),
    INVALID_DURATION("예약은 30분 이상 4시간 이하여야 합니다."),
    PAST_DATETIME("지난 시간은 예약할 수 없습니다."),
    TOO_FAR_IN_FUTURE("14일 이내만 예약할 수 있습니다."),
    NOT_RESERVER("예약자 본인만 취소할 수 있습니다."),
    ALREADY_CANCELLED("이미 취소된 예약입니다."),
    RESERVATION_NOT_FOUND("예약을 찾을 수 없습니다."),
    ROOM_NOT_FOUND("회의실을 찾을 수 없습니다."),
    INVALID_PURPOSE_LENGTH("회의 목적은 50자 이내로 입력해주세요."),
    INVALID_DATE_FORMAT("날짜 형식이 올바르지 않습니다."),
    INVALID_ROOM_NAME("회의실 이름은 1~30자로 입력해주세요."),
    INVALID_ROOM_CAPACITY("수용 인원은 1명 이상 999명 이하여야 합니다."),
    INVALID_ROOM_LOCATION("위치는 1~30자로 입력해주세요."),
    ROOM_HAS_ACTIVE_RESERVATIONS("예정된 예약이 있어 삭제할 수 없습니다. 예약을 먼저 취소해주세요.");

    private final String message;

    ReservationErrorCode(String message) {
        this.message = message;
    }

    @Override
    public String message() {
        return message;
    }
}
