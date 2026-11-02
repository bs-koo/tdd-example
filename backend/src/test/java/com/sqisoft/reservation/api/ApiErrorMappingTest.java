package com.sqisoft.reservation.api;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.MediaType;

import com.sqisoft.reservation.api.support.ApiTestSupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 예약 생성 경로의 검증 순서가 실제 HTTP 요청으로 재현 가능한지 확인한다.
 * 코드→HTTP 상태 매핑 자체는 {@link ErrorCodeHttpStatusTest}가 전수 검증하므로 반복하지 않는다.
 */
class ApiErrorMappingTest extends ApiTestSupport {

    private Fixture fixture;

    @BeforeEach
    void setUp() throws Exception {
        fixture = prepareGroupWithRoom();
    }

    private String reservationBody(long roomId, String startAt, String endAt) {
        return """
                {"roomId":%d,"purpose":"주간 회의","startAt":"%s","endAt":"%s"}
                """.formatted(roomId, startAt, endAt);
    }

    @ParameterizedTest(name = "[{0}] {1} 코드는 상태 {2}로 유발된다")
    @DisplayName("POST /api/reservations 는 검증 순서 각 단계에서 대상 코드만 유발한다")
    @CsvSource({
            "3,  INVALID_TIME_UNIT,      400, false, 2026-08-26T09:15:00, 2026-08-26T10:00:00",
            "4,  PAST_DATETIME,          400, false, 2026-08-24T09:00:00, 2026-08-24T10:00:00",
            "5,  TOO_FAR_IN_FUTURE,      400, false, 2026-09-09T09:00:00, 2026-09-09T10:00:00",
            "6,  OUTSIDE_BUSINESS_HOURS, 400, false, 2026-08-26T08:00:00, 2026-08-26T09:00:00",
            "7,  INVALID_DURATION,       400, false, 2026-08-26T09:00:00, 2026-08-26T09:00:00",
            "8,  ROOM_NOT_FOUND,         404, true,  2026-08-26T09:00:00, 2026-08-26T10:00:00",
            "13, INVALID_DATE_FORMAT,    400, false, 2026-13-99T09:00:00, 2026-08-26T10:00:00",
    })
    void POST_예약_요청은_검증_순서에_따라_대상_오류_코드를_반환한다(
            String caseId, String expectedCode, int expectedStatus, boolean unknownRoom,
            String startAt, String endAt) throws Exception {
        long roomId = unknownRoom ? 99999L : fixture.roomId();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reservationBody(roomId, startAt, endAt)))
                .andExpect(status().is(expectedStatus))
                .andExpect(jsonPath("$.code").value(expectedCode));
    }

    @Test
    @DisplayName("purpose가 51자를 초과하면 400과 INVALID_PURPOSE_LENGTH 코드를 반환한다")
    void purpose가_51자를_초과하면_INVALID_PURPOSE_LENGTH를_반환한다() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"purpose":"%s",
                                 "startAt":"2026-08-26T09:00:00","endAt":"2026-08-26T10:00:00"}
                                """.formatted(fixture.roomId(), "x".repeat(51))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PURPOSE_LENGTH"));
    }

    // ── 요청 필드 누락 ──

    @Test
    @DisplayName("startAt 필드가 빠지면 500이 아니라 400과 INVALID_DATE_FORMAT 코드를 반환한다")
    void startAt_필드가_누락되면_INVALID_DATE_FORMAT을_반환한다() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"purpose":"주간 회의","endAt":"2026-08-28T10:00:00"}
                                """.formatted(fixture.roomId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"));
    }

    @Test
    @DisplayName("endAt 필드가 빠지면 500이 아니라 400과 INVALID_DATE_FORMAT 코드를 반환한다")
    void endAt_필드가_누락되면_INVALID_DATE_FORMAT을_반환한다() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"purpose":"주간 회의","startAt":"2026-08-28T09:00:00"}
                                """.formatted(fixture.roomId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"));
    }

    @Test
    @DisplayName("roomId 필드가 빠지면 500이 아니라 404와 ROOM_NOT_FOUND 코드를 반환한다")
    void roomId_필드가_누락되면_ROOM_NOT_FOUND를_반환한다() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"purpose":"주간 회의",
                                 "startAt":"2026-08-26T09:00:00","endAt":"2026-08-26T10:00:00"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    @DisplayName("빈 바디({})는 startAt도 없으므로 날짜 파싱 단계에서 400과 INVALID_DATE_FORMAT이 먼저 걸린다")
    void 빈_바디로_요청하면_INVALID_DATE_FORMAT을_반환한다() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"));
    }

    // ── 요청 바디 파싱 실패 ──

    @Test
    @DisplayName("JSON 문법 오류가 있으면 Spring 기본 오류 바디가 아니라 400과 INVALID_DATE_FORMAT 코드를 반환한다")
    void JSON_문법_오류가_있으면_INVALID_DATE_FORMAT을_반환한다() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":1,
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"));
    }

    @Test
    @DisplayName("roomId가 문자열이면(타입 불일치) 400과 INVALID_DATE_FORMAT 코드를 반환한다")
    void roomId가_문자열이면_INVALID_DATE_FORMAT을_반환한다() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":"abc","purpose":"주간 회의",
                                 "startAt":"2026-08-26T09:00:00","endAt":"2026-08-26T10:00:00"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"));
    }

    @Test
    @DisplayName("startAt이 숫자이면(타입 불일치) 400과 INVALID_DATE_FORMAT 코드를 반환한다")
    void startAt이_숫자이면_INVALID_DATE_FORMAT을_반환한다() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"purpose":"주간 회의",
                                 "startAt":123,"endAt":"2026-08-26T10:00:00"}
                                """.formatted(fixture.roomId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"));
    }

    @Test
    @DisplayName("회귀 가드: 경로 변수 타입 불일치는 Spring 기본 400으로 흐르고 code 필드가 없다")
    void 경로_변수_타입_불일치는_code_필드_없이_기본_400을_반환한다() throws Exception {
        mockMvc.perform(get("/api/rooms/{roomId}/reservations", "abc")
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .param("date", "2026-08-26"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").doesNotExist());
    }

    @Test
    @DisplayName("인증되지 않은 요청은 바디를 파싱하기도 전에 401과 UNAUTHENTICATED로 거절된다")
    void 인증_실패가_바디_파싱보다_먼저다() throws Exception {
        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":1,
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
