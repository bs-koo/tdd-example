package com.sqisoft.reservation.api;

import java.util.Arrays;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpStatus;

import com.sqisoft.reservation.domain.AuthErrorCode;
import com.sqisoft.reservation.domain.ErrorCode;
import com.sqisoft.reservation.domain.GroupErrorCode;
import com.sqisoft.reservation.domain.ReservationErrorCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class ErrorCodeHttpStatusTest {

    static Stream<Arguments> codeToStatus() {
        return Stream.of(
                Arguments.of(ReservationErrorCode.OVERLAPPING_RESERVATION, HttpStatus.CONFLICT),
                Arguments.of(ReservationErrorCode.INVALID_TIME_UNIT, HttpStatus.BAD_REQUEST),
                Arguments.of(ReservationErrorCode.OUTSIDE_BUSINESS_HOURS, HttpStatus.BAD_REQUEST),
                Arguments.of(ReservationErrorCode.INVALID_DURATION, HttpStatus.BAD_REQUEST),
                Arguments.of(ReservationErrorCode.PAST_DATETIME, HttpStatus.BAD_REQUEST),
                Arguments.of(ReservationErrorCode.TOO_FAR_IN_FUTURE, HttpStatus.BAD_REQUEST),
                Arguments.of(ReservationErrorCode.NOT_RESERVER, HttpStatus.FORBIDDEN),
                Arguments.of(ReservationErrorCode.ALREADY_CANCELLED, HttpStatus.CONFLICT),
                Arguments.of(ReservationErrorCode.RESERVATION_NOT_FOUND, HttpStatus.NOT_FOUND),
                Arguments.of(ReservationErrorCode.ROOM_NOT_FOUND, HttpStatus.NOT_FOUND),
                Arguments.of(ReservationErrorCode.INVALID_PURPOSE_LENGTH, HttpStatus.BAD_REQUEST),
                Arguments.of(ReservationErrorCode.INVALID_DATE_FORMAT, HttpStatus.BAD_REQUEST),
                Arguments.of(ReservationErrorCode.INVALID_ROOM_NAME, HttpStatus.BAD_REQUEST),
                Arguments.of(ReservationErrorCode.INVALID_ROOM_CAPACITY, HttpStatus.BAD_REQUEST),
                Arguments.of(ReservationErrorCode.INVALID_ROOM_LOCATION, HttpStatus.BAD_REQUEST),
                Arguments.of(ReservationErrorCode.ROOM_HAS_ACTIVE_RESERVATIONS, HttpStatus.CONFLICT),

                Arguments.of(AuthErrorCode.UNAUTHENTICATED, HttpStatus.UNAUTHORIZED),
                Arguments.of(AuthErrorCode.INVALID_CREDENTIALS, HttpStatus.UNAUTHORIZED),
                Arguments.of(AuthErrorCode.EMAIL_ALREADY_REGISTERED, HttpStatus.CONFLICT),
                Arguments.of(AuthErrorCode.INVALID_EMAIL_FORMAT, HttpStatus.BAD_REQUEST),
                Arguments.of(AuthErrorCode.INVALID_PASSWORD_LENGTH, HttpStatus.BAD_REQUEST),
                Arguments.of(AuthErrorCode.INVALID_MEMBER_NAME, HttpStatus.BAD_REQUEST),
                Arguments.of(AuthErrorCode.MEMBER_NOT_FOUND, HttpStatus.NOT_FOUND),

                Arguments.of(GroupErrorCode.GROUP_NOT_FOUND, HttpStatus.NOT_FOUND),
                Arguments.of(GroupErrorCode.INVALID_GROUP_NAME, HttpStatus.BAD_REQUEST),
                Arguments.of(GroupErrorCode.NOT_GROUP_OWNER, HttpStatus.FORBIDDEN),
                Arguments.of(GroupErrorCode.NOT_GROUP_MEMBER, HttpStatus.FORBIDDEN),
                Arguments.of(GroupErrorCode.ALREADY_INVITED, HttpStatus.CONFLICT),
                Arguments.of(GroupErrorCode.SELF_INVITATION, HttpStatus.BAD_REQUEST),
                Arguments.of(GroupErrorCode.INVITATION_NOT_FOUND, HttpStatus.NOT_FOUND),
                Arguments.of(GroupErrorCode.NOT_INVITEE, HttpStatus.FORBIDDEN),
                Arguments.of(GroupErrorCode.INVITATION_ALREADY_RESPONDED, HttpStatus.CONFLICT));
    }

    static Stream<ErrorCode> allCodes() {
        return Stream.of(
                        Arrays.stream(ReservationErrorCode.values()),
                        Arrays.stream(AuthErrorCode.values()),
                        Arrays.stream(GroupErrorCode.values()))
                .flatMap(codes -> codes.map(ErrorCode.class::cast));
    }

    @ParameterizedTest(name = "{0} 코드는 {1} 상태로 매핑된다")
    @DisplayName("오류 코드는 정해진 HTTP 상태로 매핑된다")
    @MethodSource("codeToStatus")
    void 오류_코드는_정해진_HTTP_상태로_매핑된다(ErrorCode code, HttpStatus expectedStatus) {
        assertThat(ErrorCodeHttpStatus.of(code)).isEqualTo(expectedStatus);
    }

    @Test
    @DisplayName("세 오류 코드 열거형의 모든 값은 예외 없이 HTTP 상태로 매핑된다")
    void 모든_오류_코드는_예외_없이_HTTP_상태로_매핑된다() {
        assertThat(allCodes()).allSatisfy(code -> assertThatNoException()
                .isThrownBy(() -> ErrorCodeHttpStatus.of(code)));
    }

    @Test
    @DisplayName("매핑 표에 빠진 코드가 없도록, 위 표는 전체 코드 수와 개수가 일치한다")
    void 매핑_표는_전체_코드를_빠짐없이_다룬다() {
        assertThat(codeToStatus().count()).isEqualTo(allCodes().count());
    }

    @Test
    @DisplayName("오류 코드는 총 32종이며 모두 매핑 표에 있다")
    void 오류_코드는_총_32종이다() {
        assertThat(allCodes().count()).isEqualTo(32);
        assertThat(codeToStatus().count()).isEqualTo(32);
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
        ErrorResponse response = ErrorResponse.from(AuthErrorCode.UNAUTHENTICATED);

        assertThat(response.message()).isEqualTo(AuthErrorCode.UNAUTHENTICATED.message());
    }
}
