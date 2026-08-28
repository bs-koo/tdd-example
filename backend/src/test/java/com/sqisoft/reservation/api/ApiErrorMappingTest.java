package com.sqisoft.reservation.api;

import com.sqisoft.reservation.api.support.ApiTestSupport;
import com.sqisoft.reservation.domain.Reservation;
import com.sqisoft.reservation.support.ReservationTestFixtures;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AC-30 (E2E 축): 오류 코드 13종이 각각 실제 HTTP 요청으로 도달 가능하며,
 * 정확한 상태 코드와 code 문자열로 응답되는지를 검증한다.
 * 코드→HTTP 상태 매핑 자체는 {@link ErrorCodeHttpStatusTest}가 이미 전수 검증했으므로 반복하지 않는다.
 */
class ApiErrorMappingTest extends ApiTestSupport {

    @ParameterizedTest(name = "[{0}] {1} 코드는 상태 {2}로 유발된다")
    @DisplayName("POST /api/reservations 는 검증 순서 3·4·5·6·7·8·13단계에서 대상 코드만 유발한다")
    @CsvSource({
            "3,  INVALID_TIME_UNIT,      400, 1,   2026-08-26T09:15:00, 2026-08-26T10:00:00",
            "4,  PAST_DATETIME,          400, 1,   2026-08-24T09:00:00, 2026-08-24T10:00:00",
            "5,  TOO_FAR_IN_FUTURE,      400, 1,   2026-09-09T09:00:00, 2026-09-09T10:00:00",
            "6,  OUTSIDE_BUSINESS_HOURS, 400, 1,   2026-08-26T08:00:00, 2026-08-26T09:00:00",
            "7,  INVALID_DURATION,       400, 1,   2026-08-26T09:00:00, 2026-08-26T09:00:00",
            "8,  ROOM_NOT_FOUND,         404, 999, 2026-08-26T09:00:00, 2026-08-26T10:00:00",
            "13, INVALID_DATE_FORMAT,    400, 1,   2026-13-99T09:00:00, 2026-08-26T10:00:00",
    })
    void POST_예약_요청은_검증_순서에_따라_대상_오류_코드를_반환한다(
            String caseId, String expectedCode, int expectedStatus, long roomId,
            String startAt, String endAt) throws Exception {
        String requestBody = """
                {"roomId":%d,"reserverName":"김본승","purpose":"주간 회의",
                 "startAt":"%s","endAt":"%s"}
                """.formatted(roomId, startAt, endAt);

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    @Test
    @DisplayName("AC-30 (#1): reserverName이 빈 문자열이면 400과 INVALID_RESERVER_NAME 코드를 반환한다")
    void reserverName이_빈_문자열이면_INVALID_RESERVER_NAME을_반환한다() throws Exception {
        String requestBody = """
                {"roomId":1,"reserverName":"","purpose":"주간 회의",
                 "startAt":"2026-08-26T09:00:00","endAt":"2026-08-26T10:00:00"}
                """;

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_RESERVER_NAME"));
    }

    @Test
    @DisplayName("AC-30 (#2): purpose가 51자를 초과하면 400과 INVALID_PURPOSE_LENGTH 코드를 반환한다")
    void purpose가_51자를_초과하면_INVALID_PURPOSE_LENGTH를_반환한다() throws Exception {
        String requestBody = """
                {"roomId":1,"reserverName":"김본승","purpose":"%s",
                 "startAt":"2026-08-26T09:00:00","endAt":"2026-08-26T10:00:00"}
                """.formatted("x".repeat(51));

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PURPOSE_LENGTH"));
    }

    @Test
    @DisplayName("AC-30 (#9): 동일 시간대에 이미 예약이 있으면 409와 OVERLAPPING_RESERVATION 코드를 반환한다")
    void 동일_시간대에_이미_예약이_있으면_OVERLAPPING_RESERVATION을_반환한다() throws Exception {
        Reservation existing = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "선점 회의",
                ReservationTestFixtures.slot(ReservationTestFixtures.DEFAULT_DATE, "09:00", "10:00"),
                ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00"));
        em.persist(existing);
        em.flush();
        em.clear();

        String requestBody = """
                {"roomId":1,"reserverName":"김본승","purpose":"주간 회의",
                 "startAt":"2026-08-26T09:00:00","endAt":"2026-08-26T10:00:00"}
                """;

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OVERLAPPING_RESERVATION"));
    }

    @Test
    @DisplayName("AC-30 (#10): 존재하지 않는 예약을 취소하면 404와 RESERVATION_NOT_FOUND 코드를 반환한다")
    void 존재하지_않는_예약을_취소하면_RESERVATION_NOT_FOUND를_반환한다() throws Exception {
        String requestBody = """
                {"reserverName":"김본승"}
                """;

        mockMvc.perform(post("/api/reservations/{id}/cancel", 99999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("AC-30 (#11): 예약자 이름이 일치하지 않으면 403과 NOT_RESERVER 코드를 반환한다")
    void 예약자_이름이_일치하지_않으면_NOT_RESERVER를_반환한다() throws Exception {
        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.slot(ReservationTestFixtures.DEFAULT_DATE, "09:00", "10:00"),
                ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00"));
        em.persist(reservation);
        em.flush();
        Long id = reservation.id();
        em.clear();

        String requestBody = """
                {"reserverName":"박다른"}
                """;

        mockMvc.perform(post("/api/reservations/{id}/cancel", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_RESERVER"));
    }

    @Test
    @DisplayName("AC-30 (#12): 이미 취소된 예약을 같은 예약자명으로 다시 취소하면 409와 ALREADY_CANCELLED 코드를 반환한다")
    void 이미_취소된_예약을_다시_취소하면_ALREADY_CANCELLED를_반환한다() throws Exception {
        Reservation reservation = Reservation.create(
                ReservationTestFixtures.ROOM_ID,
                ReservationTestFixtures.RESERVER,
                "주간 회의",
                ReservationTestFixtures.slot(ReservationTestFixtures.DEFAULT_DATE, "09:00", "10:00"),
                ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00"));
        em.persist(reservation);
        reservation.cancel(ReservationTestFixtures.RESERVER);
        em.flush();
        Long id = reservation.id();
        em.clear();

        String requestBody = """
                {"reserverName":"김본승"}
                """;

        mockMvc.perform(post("/api/reservations/{id}/cancel", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_CANCELLED"));
    }

    // ── AC-36: 요청 필드 누락 시 정의된 오류 코드로 거절한다 (review 1회차 반영) ──

    @Test
    @DisplayName("AC-36: startAt 필드가 요청 바디에서 아예 빠지면 500이 아니라 400과 INVALID_DATE_FORMAT 코드를 반환한다")
    void startAt_필드가_누락되면_INVALID_DATE_FORMAT을_반환한다() throws Exception {
        String requestBody = """
                {"roomId":1,"reserverName":"김본승","purpose":"주간 회의",
                 "endAt":"2026-08-28T10:00:00"}
                """;

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"));
    }

    @Test
    @DisplayName("AC-36: endAt 필드가 요청 바디에서 아예 빠지면 500이 아니라 400과 INVALID_DATE_FORMAT 코드를 반환한다")
    void endAt_필드가_누락되면_INVALID_DATE_FORMAT을_반환한다() throws Exception {
        String requestBody = """
                {"roomId":1,"reserverName":"김본승","purpose":"주간 회의",
                 "startAt":"2026-08-28T09:00:00"}
                """;

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"));
    }

    @Test
    @DisplayName("AC-36: roomId 필드가 요청 바디에서 아예 빠지면 500이 아니라 404와 ROOM_NOT_FOUND 코드를 반환한다")
    void roomId_필드가_누락되면_ROOM_NOT_FOUND를_반환한다() throws Exception {
        String requestBody = """
                {"reserverName":"김본승","purpose":"주간 회의",
                 "startAt":"2026-08-26T09:00:00","endAt":"2026-08-26T10:00:00"}
                """;

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    @DisplayName("AC-36: 빈 바디({})는 startAt도 없으므로 toCommand()의 날짜 파싱 단계에서 400과 INVALID_DATE_FORMAT이 먼저 걸린다")
    void 빈_바디로_요청하면_INVALID_DATE_FORMAT을_반환한다() throws Exception {
        String requestBody = "{}";

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"));
    }

    // ── AC-40: 요청 바디 파싱 실패 시 정의된 오류 코드로 거절한다 (review 2회차 반영) ──
    // 설계 C-7이 "HttpMessageNotReadableException만 INVALID_DATE_FORMAT/400 유지"로 요구했으나
    // 구현에서 누락되어, JSON 문법 오류·타입 불일치가 code 필드 없는 Spring 기본 오류 바디로 나갔다.

    @Test
    @DisplayName("AC-40: JSON 문법 오류가 있으면 Spring 기본 오류 바디가 아니라 400과 INVALID_DATE_FORMAT 코드를 반환한다")
    void JSON_문법_오류가_있으면_INVALID_DATE_FORMAT을_반환한다() throws Exception {
        String requestBody = """
                {"roomId":1,
                """;

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"));
    }

    @Test
    @DisplayName("AC-40: roomId가 문자열이면(타입 불일치) 400과 INVALID_DATE_FORMAT 코드를 반환한다")
    void roomId가_문자열이면_INVALID_DATE_FORMAT을_반환한다() throws Exception {
        String requestBody = """
                {"roomId":"abc","reserverName":"김본승","purpose":"주간 회의",
                 "startAt":"2026-08-26T09:00:00","endAt":"2026-08-26T10:00:00"}
                """;

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"));
    }

    @Test
    @DisplayName("AC-40: roomId가 배열이면(타입 불일치) 400과 INVALID_DATE_FORMAT 코드를 반환한다")
    void roomId가_배열이면_INVALID_DATE_FORMAT을_반환한다() throws Exception {
        String requestBody = """
                {"roomId":[1,2],"reserverName":"김본승","purpose":"주간 회의",
                 "startAt":"2026-08-26T09:00:00","endAt":"2026-08-26T10:00:00"}
                """;

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"));
    }

    @Test
    @DisplayName("AC-40: startAt이 숫자이면(타입 불일치) 400과 INVALID_DATE_FORMAT 코드를 반환한다")
    void startAt이_숫자이면_INVALID_DATE_FORMAT을_반환한다() throws Exception {
        String requestBody = """
                {"roomId":1,"reserverName":"김본승","purpose":"주간 회의",
                 "startAt":123,"endAt":"2026-08-26T10:00:00"}
                """;

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"));
    }

    @Test
    @DisplayName("AC-40 회귀 가드: GET /api/rooms/abc/reservations 의 경로 변수 타입 불일치는 C-7대로 Spring 기본 400으로 흐르고 code 필드가 없다")
    void 경로_변수_타입_불일치는_code_필드_없이_기본_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/rooms/{roomId}/reservations", "abc")
                        .param("date", "2026-08-26"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").doesNotExist());
    }
}
