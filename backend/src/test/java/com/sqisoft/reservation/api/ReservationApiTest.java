package com.sqisoft.reservation.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.sqisoft.reservation.api.support.ApiTestSupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReservationApiTest extends ApiTestSupport {

    // ── 예약 생성 ──

    @Test
    @DisplayName("승인된 그룹원이 예약하면 201과 예약 정보를 반환한다")
    void 승인된_그룹원은_예약할_수_있다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(fixture.memberToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"purpose":"주간 회의",
                                 "startAt":"2026-08-26T09:00:00","endAt":"2026-08-26T10:00:00"}
                                """.formatted(fixture.roomId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.roomId").value((int) fixture.roomId()))
                .andExpect(jsonPath("$.purpose").value("주간 회의"))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    @DisplayName("예약자명은 요청이 아니라 로그인한 사용자에게서 온다")
    void 예약자명은_로그인_사용자에게서_온다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(fixture.memberToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"purpose":"주간 회의",
                                 "startAt":"2026-08-26T09:00:00","endAt":"2026-08-26T10:00:00"}
                                """.formatted(fixture.roomId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reserverName").value("박그룹"));
    }

    @Test
    @DisplayName("요청 바디에 reserverName을 실어 보내도 무시하고 토큰의 이름을 쓴다 — 이름을 위조할 수 없다")
    void 요청의_예약자명은_무시된다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(fixture.memberToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"reserverName":"김본승","purpose":"주간 회의",
                                 "startAt":"2026-08-26T09:00:00","endAt":"2026-08-26T10:00:00"}
                                """.formatted(fixture.roomId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reserverName").value("박그룹"));
    }

    @Test
    @DisplayName("로그인하지 않으면 예약할 수 없다 — 401과 UNAUTHENTICATED")
    void 비로그인_예약은_401이다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();

        mockMvc.perform(post("/api/reservations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"purpose":"주간 회의",
                                 "startAt":"2026-08-26T09:00:00","endAt":"2026-08-26T10:00:00"}
                                """.formatted(fixture.roomId())))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("초대를 수락하지 않은 사람은 예약할 수 없다 — 403과 NOT_GROUP_MEMBER")
    void 승인_대기자는_예약할_수_없다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();
        String pendingToken = signupAndLogin("pending@sqisoft.com", "정대기");
        invite(fixture.ownerToken(), fixture.groupId(), "pending@sqisoft.com");

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(pendingToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"purpose":"주간 회의",
                                 "startAt":"2026-08-26T09:00:00","endAt":"2026-08-26T10:00:00"}
                                """.formatted(fixture.roomId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));
    }

    @Test
    @DisplayName("초대를 거절한 사람은 예약할 수 없다 — 403과 NOT_GROUP_MEMBER")
    void 초대를_거절한_사람은_예약할_수_없다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();
        String rejectedToken = signupAndLogin("rejected@sqisoft.com", "한거절");
        long membershipId = invite(fixture.ownerToken(), fixture.groupId(), "rejected@sqisoft.com");
        mockMvc.perform(post("/api/invitations/{id}/reject", membershipId)
                .header("Authorization", bearer(rejectedToken)));

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(rejectedToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"purpose":"주간 회의",
                                 "startAt":"2026-08-26T09:00:00","endAt":"2026-08-26T10:00:00"}
                                """.formatted(fixture.roomId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));
    }

    @Test
    @DisplayName("자기 그룹이 아닌 회의실은 예약할 수 없다 — 403과 NOT_GROUP_MEMBER")
    void 남의_그룹_회의실은_예약할_수_없다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();
        String strangerToken = signupAndLogin("stranger@sqisoft.com", "최무관");
        createGroup(strangerToken, "다른팀");

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(strangerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"purpose":"주간 회의",
                                 "startAt":"2026-08-26T09:00:00","endAt":"2026-08-26T10:00:00"}
                                """.formatted(fixture.roomId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));
    }

    @Test
    @DisplayName("같은 시간대를 두 번 예약하면 409와 OVERLAPPING_RESERVATION을 반환한다")
    void 시간이_겹치면_409다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();
        createReservation(fixture.ownerToken(), fixture.roomId(), "선점 회의",
                "2026-08-26T09:00:00", "2026-08-26T10:00:00");

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(fixture.memberToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"purpose":"겹치는 회의",
                                 "startAt":"2026-08-26T09:30:00","endAt":"2026-08-26T10:30:00"}
                                """.formatted(fixture.roomId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("OVERLAPPING_RESERVATION"));
    }

    @Test
    @DisplayName("존재하지 않는 회의실을 예약하면 404와 ROOM_NOT_FOUND를 반환한다")
    void 없는_회의실_예약은_404다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":99999,"purpose":"주간 회의",
                                 "startAt":"2026-08-26T09:00:00","endAt":"2026-08-26T10:00:00"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    @DisplayName("시간 규칙 위반은 그룹 권한 검사보다 먼저 판정된다")
    void 시간_규칙이_그룹_권한보다_먼저다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();
        String strangerToken = signupAndLogin("stranger@sqisoft.com", "최무관");

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(strangerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"purpose":"주간 회의",
                                 "startAt":"2026-08-26T09:15:00","endAt":"2026-08-26T10:00:00"}
                                """.formatted(fixture.roomId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TIME_UNIT"));
    }

    // ── 예약 취소 ──

    @Test
    @DisplayName("예약자 본인이 취소하면 204를 반환한다")
    void 본인_취소는_204다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();
        long reservationId = createReservation(fixture.memberToken(), fixture.roomId(), "주간 회의",
                "2026-08-26T09:00:00", "2026-08-26T10:00:00");

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                        .header("Authorization", bearer(fixture.memberToken())))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("같은 그룹의 다른 사람이 취소하면 403과 NOT_RESERVER를 반환한다")
    void 같은_그룹의_남이_취소하면_403이다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();
        long reservationId = createReservation(fixture.memberToken(), fixture.roomId(), "주간 회의",
                "2026-08-26T09:00:00", "2026-08-26T10:00:00");

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                        .header("Authorization", bearer(fixture.ownerToken())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_RESERVER"));
    }

    @Test
    @DisplayName("예약자와 이름이 같아도 다른 계정이면 취소할 수 없다 — 이름이 아니라 신원으로 판정한다")
    void 동명이인은_취소할_수_없다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();
        long reservationId = createReservation(fixture.memberToken(), fixture.roomId(), "주간 회의",
                "2026-08-26T09:00:00", "2026-08-26T10:00:00");

        String sameNameToken = signupAndLogin("twin@sqisoft.com", "박그룹");
        accept(sameNameToken, invite(fixture.ownerToken(), fixture.groupId(), "twin@sqisoft.com"));

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                        .header("Authorization", bearer(sameNameToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_RESERVER"));
    }

    @Test
    @DisplayName("이미 취소한 예약을 다시 취소하면 409와 ALREADY_CANCELLED를 반환한다")
    void 중복_취소는_409다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();
        long reservationId = createReservation(fixture.memberToken(), fixture.roomId(), "주간 회의",
                "2026-08-26T09:00:00", "2026-08-26T10:00:00");
        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                .header("Authorization", bearer(fixture.memberToken())));

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                        .header("Authorization", bearer(fixture.memberToken())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_CANCELLED"));
    }

    @Test
    @DisplayName("존재하지 않는 예약을 취소하면 404와 RESERVATION_NOT_FOUND를 반환한다")
    void 없는_예약_취소는_404다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();

        mockMvc.perform(post("/api/reservations/{id}/cancel", 99999L)
                        .header("Authorization", bearer(fixture.ownerToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESERVATION_NOT_FOUND"));
    }

    @Test
    @DisplayName("로그인하지 않으면 취소할 수 없다 — 401과 UNAUTHENTICATED")
    void 비로그인_취소는_401이다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();
        long reservationId = createReservation(fixture.memberToken(), fixture.roomId(), "주간 회의",
                "2026-08-26T09:00:00", "2026-08-26T10:00:00");

        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("취소 후 같은 시간대로 다시 예약할 수 있다")
    void 취소_후_재예약할_수_있다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();
        long reservationId = createReservation(fixture.memberToken(), fixture.roomId(), "기존 회의",
                "2026-08-26T09:00:00", "2026-08-26T10:00:00");
        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                .header("Authorization", bearer(fixture.memberToken())));

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"purpose":"새 회의",
                                 "startAt":"2026-08-26T09:00:00","endAt":"2026-08-26T10:00:00"}
                                """.formatted(fixture.roomId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }
}
