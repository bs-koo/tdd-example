package com.sqisoft.reservation.api;

import com.sqisoft.reservation.domain.ReservationErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class ErrorCodeHttpStatusTest {

    @ParameterizedTest(name = "{0} 코드는 {1} 상태로 매핑된다")
    @DisplayName("오류 코드는 정해진 HTTP 상태로 매핑된다")
    @CsvSource({
            "OVERLAPPING_RESERVATION, CONFLICT",
            "INVALID_TIME_UNIT, BAD_REQUEST",
            "OUTSIDE_BUSINESS_HOURS, BAD_REQUEST",
            "INVALID_DURATION, BAD_REQUEST",
            "PAST_DATETIME, BAD_REQUEST",
            "TOO_FAR_IN_FUTURE, BAD_REQUEST",
            "NOT_RESERVER, FORBIDDEN",
            "ALREADY_CANCELLED, CONFLICT",
            "RESERVATION_NOT_FOUND, NOT_FOUND",
            "ROOM_NOT_FOUND, NOT_FOUND",
            "INVALID_RESERVER_NAME, BAD_REQUEST",
            "INVALID_PURPOSE_LENGTH, BAD_REQUEST",
            "INVALID_DATE_FORMAT, BAD_REQUEST",
    })
    void 오류_코드는_정해진_HTTP_상태로_매핑된다(ReservationErrorCode code, HttpStatus expectedStatus) {
        assertThat(ErrorCodeHttpStatus.of(code)).isEqualTo(expectedStatus);
    }

    @Test
    @DisplayName("모든 오류 코드는 예외 없이 HTTP 상태로 매핑된다")
    void 모든_오류_코드는_예외_없이_HTTP_상태로_매핑된다() {
        assertThat(ReservationErrorCode.values())
                .allSatisfy(code -> assertThatNoException()
                        .isThrownBy(() -> ErrorCodeHttpStatus.of(code)));
    }

    @Test
    @DisplayName("ErrorResponse.from()은 코드의 enum 이름을 code로 담는다")
    void ErrorResponse_from은_코드의_enum_이름을_code로_담는다() {
        ErrorResponse response = ErrorResponse.from(ReservationErrorCode.NOT_RESERVER);

        assertThat(response.code()).isEqualTo("NOT_RESERVER");
    }

    @Test
    @DisplayName("ErrorResponse.from()은 코드의 한국어 메시지를 message로 담는다")
    void ErrorResponse_from은_코드의_한국어_메시지를_message로_담는다() {
        ErrorResponse response = ErrorResponse.from(ReservationErrorCode.NOT_RESERVER);

        assertThat(response.message()).isEqualTo(ReservationErrorCode.NOT_RESERVER.message());
    }
}
