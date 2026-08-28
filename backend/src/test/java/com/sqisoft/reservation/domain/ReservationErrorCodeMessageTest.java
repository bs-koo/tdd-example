package com.sqisoft.reservation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class ReservationErrorCodeMessageTest {

    @ParameterizedTest(name = "{0} 코드는 \"{1}\" 메시지를 반환한다")
    @DisplayName("오류 코드는 정해진 한국어 문구로 변환된다")
    @CsvSource({
            "OVERLAPPING_RESERVATION, 해당 시간대에 이미 예약이 있습니다.",
            "INVALID_TIME_UNIT, 예약 시각은 30분 단위여야 합니다.",
            "OUTSIDE_BUSINESS_HOURS, 09:00~18:00 사이만 예약할 수 있습니다.",
            "INVALID_DURATION, 예약은 30분 이상 4시간 이하여야 합니다.",
            "PAST_DATETIME, 지난 시간은 예약할 수 없습니다.",
            "TOO_FAR_IN_FUTURE, 14일 이내만 예약할 수 있습니다.",
            "NOT_RESERVER, 예약자 본인만 취소할 수 있습니다.",
            "ALREADY_CANCELLED, 이미 취소된 예약입니다.",
            "RESERVATION_NOT_FOUND, 예약을 찾을 수 없습니다.",
            "ROOM_NOT_FOUND, 회의실을 찾을 수 없습니다.",
            "INVALID_RESERVER_NAME, 예약자명은 1~20자로 입력해주세요.",
            "INVALID_PURPOSE_LENGTH, 회의 목적은 50자 이내로 입력해주세요.",
            "INVALID_DATE_FORMAT, 날짜 형식이 올바르지 않습니다.",
    })
    void 오류_코드는_정해진_한국어_문구로_변환된다(ReservationErrorCode code, String expectedMessage) {
        assertThat(code.message()).isEqualTo(expectedMessage);
    }

    @Test
    @DisplayName("오류 코드는 정확히 13종이다")
    void 오류_코드는_정확히_13종이다() {
        assertThat(ReservationErrorCode.values()).hasSize(13);
    }

    @Test
    @DisplayName("ReservationException은 생성자로 받은 코드를 code()로 그대로 반환한다")
    void 예외는_생성자로_받은_코드를_그대로_반환한다() {
        ReservationException exception = new ReservationException(ReservationErrorCode.ROOM_NOT_FOUND);

        assertThat(exception.code()).isEqualTo(ReservationErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("ReservationException의 메시지는 코드의 한국어 메시지와 같다")
    void 예외의_메시지는_코드의_한국어_메시지와_같다() {
        ReservationException exception = new ReservationException(ReservationErrorCode.NOT_RESERVER);

        assertThat(exception.getMessage()).isEqualTo(ReservationErrorCode.NOT_RESERVER.message());
    }
}
