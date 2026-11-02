package com.sqisoft.reservation.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.sqisoft.reservation.api.support.ApiTestSupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomApiTest extends ApiTestSupport {

    private static final String ROOM_BODY = """
            {"name":"대회의실","capacity":20,"location":"3층"}
            """;

    // ── 회의실 추가 ──

    @Test
    @DisplayName("승인된 그룹원이 회의실을 추가하면 201과 그 그룹 소속의 회의실을 반환한다")
    void 승인된_그룹원은_회의실을_추가할_수_있다() throws Exception {
        String ownerToken = signupAndLogin("owner@sqisoft.com", "김본승");
        String memberToken = signupAndLogin("member@sqisoft.com", "박그룹");
        long groupId = createGroup(ownerToken, "개발팀");
        accept(memberToken, invite(ownerToken, groupId, "member@sqisoft.com"));

        mockMvc.perform(post("/api/groups/{groupId}/rooms", groupId)
                        .header("Authorization", bearer(memberToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROOM_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.groupId").value((int) groupId))
                .andExpect(jsonPath("$.name").value("대회의실"))
                .andExpect(jsonPath("$.capacity").value(20))
                .andExpect(jsonPath("$.location").value("3층"));
    }

    @Test
    @DisplayName("초대만 받고 아직 수락하지 않은 사람은 회의실을 추가할 수 없다 — 403과 NOT_GROUP_MEMBER")
    void 승인_대기자는_회의실을_추가할_수_없다() throws Exception {
        String ownerToken = signupAndLogin("owner@sqisoft.com", "김본승");
        String pendingToken = signupAndLogin("pending@sqisoft.com", "정대기");
        long groupId = createGroup(ownerToken, "개발팀");
        invite(ownerToken, groupId, "pending@sqisoft.com");

        mockMvc.perform(post("/api/groups/{groupId}/rooms", groupId)
                        .header("Authorization", bearer(pendingToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROOM_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));
    }

    @Test
    @DisplayName("그룹과 무관한 사람은 회의실을 추가할 수 없다 — 403과 NOT_GROUP_MEMBER")
    void 무관한_사람은_회의실을_추가할_수_없다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();
        String strangerToken = signupAndLogin("stranger@sqisoft.com", "최무관");

        mockMvc.perform(post("/api/groups/{groupId}/rooms", fixture.groupId())
                        .header("Authorization", bearer(strangerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROOM_BODY))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));
    }

    @Test
    @DisplayName("로그인하지 않으면 회의실을 추가할 수 없다 — 401과 UNAUTHENTICATED")
    void 비로그인_회의실_추가는_401이다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();

        mockMvc.perform(post("/api/groups/{groupId}/rooms", fixture.groupId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROOM_BODY))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("회의실 이름이 비어 있으면 400과 INVALID_ROOM_NAME을 반환한다")
    void 빈_회의실_이름은_400이다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();

        mockMvc.perform(post("/api/groups/{groupId}/rooms", fixture.groupId())
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  ","capacity":20,"location":"3층"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ROOM_NAME"));
    }

    @Test
    @DisplayName("수용 인원이 0이면 400과 INVALID_ROOM_CAPACITY를 반환한다")
    void 수용_인원_0은_400이다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();

        mockMvc.perform(post("/api/groups/{groupId}/rooms", fixture.groupId())
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"소회의실","capacity":0,"location":"3층"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ROOM_CAPACITY"));
    }

    @Test
    @DisplayName("위치가 비어 있으면 400과 INVALID_ROOM_LOCATION을 반환한다")
    void 빈_위치는_400이다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();

        mockMvc.perform(post("/api/groups/{groupId}/rooms", fixture.groupId())
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"소회의실","capacity":6,"location":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ROOM_LOCATION"));
    }

    @Test
    @DisplayName("존재하지 않는 그룹에 회의실을 추가하면 404와 GROUP_NOT_FOUND를 반환한다")
    void 없는_그룹에_회의실_추가는_404다() throws Exception {
        String token = signupAndLogin("owner@sqisoft.com", "김본승");

        mockMvc.perform(post("/api/groups/{groupId}/rooms", 99999L)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ROOM_BODY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GROUP_NOT_FOUND"));
    }

    // ── 그룹 회의실 목록 ──

    @Test
    @DisplayName("그룹 회의실 목록은 그 그룹의 회의실만 id 오름차순으로 반환한다")
    void 그룹_회의실_목록은_그_그룹_것만_반환한다() throws Exception {
        String ownerToken = signupAndLogin("owner@sqisoft.com", "김본승");
        long myGroup = createGroup(ownerToken, "개발팀");
        long otherGroup = createGroup(ownerToken, "기획팀");
        createRoom(ownerToken, myGroup, "대회의실", 20, "3층");
        createRoom(ownerToken, otherGroup, "다른팀 회의실", 8, "7층");
        createRoom(ownerToken, myGroup, "소회의실", 6, "5층");

        mockMvc.perform(get("/api/groups/{groupId}/rooms", myGroup)
                        .header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("대회의실"))
                .andExpect(jsonPath("$[1].name").value("소회의실"));
    }

    @Test
    @DisplayName("회의실이 없는 그룹은 404가 아니라 200과 빈 배열을 반환한다")
    void 회의실이_없으면_빈_배열이다() throws Exception {
        String token = signupAndLogin("owner@sqisoft.com", "김본승");
        long groupId = createGroup(token, "개발팀");

        mockMvc.perform(get("/api/groups/{groupId}/rooms", groupId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("승인되지 않은 사람은 그룹 회의실 목록을 볼 수 없다 — 403과 NOT_GROUP_MEMBER")
    void 승인되지_않으면_회의실_목록을_볼_수_없다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();
        String strangerToken = signupAndLogin("stranger@sqisoft.com", "최무관");

        mockMvc.perform(get("/api/groups/{groupId}/rooms", fixture.groupId())
                        .header("Authorization", bearer(strangerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));
    }

    // ── 회의실 예약 현황 조회 ──

    @Test
    @DisplayName("승인된 그룹원은 회의실의 날짜별 예약 현황을 조회할 수 있다")
    void 승인된_그룹원은_예약_현황을_조회한다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();
        createReservation(fixture.memberToken(), fixture.roomId(), "당일 회의",
                "2026-08-25T09:00:00", "2026-08-25T10:00:00");

        mockMvc.perform(get("/api/rooms/{roomId}/reservations", fixture.roomId())
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .param("date", "2026-08-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].purpose").value("당일 회의"))
                .andExpect(jsonPath("$[0].reserverName").value("박그룹"))
                .andExpect(jsonPath("$[0].startAt").value("2026-08-25T09:00:00"))
                .andExpect(jsonPath("$[0].endAt").value("2026-08-25T10:00:00"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("다른 날짜의 예약은 조회 결과에서 제외된다")
    void 다른_날짜의_예약은_제외된다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();
        createReservation(fixture.ownerToken(), fixture.roomId(), "당일 회의",
                "2026-08-25T09:00:00", "2026-08-25T10:00:00");
        createReservation(fixture.ownerToken(), fixture.roomId(), "다음날 회의",
                "2026-08-26T09:00:00", "2026-08-26T10:00:00");

        mockMvc.perform(get("/api/rooms/{roomId}/reservations", fixture.roomId())
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .param("date", "2026-08-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].purpose").value("당일 회의"));
    }

    @Test
    @DisplayName("취소된 예약은 조회 결과에서 제외된다")
    void 취소된_예약은_제외된다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();
        createReservation(fixture.ownerToken(), fixture.roomId(), "정상 회의",
                "2026-08-25T09:00:00", "2026-08-25T10:00:00");
        long cancelled = createReservation(fixture.ownerToken(), fixture.roomId(), "취소될 회의",
                "2026-08-25T11:00:00", "2026-08-25T12:00:00");

        mockMvc.perform(post("/api/reservations/{id}/cancel", cancelled)
                        .header("Authorization", bearer(fixture.ownerToken())))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/rooms/{roomId}/reservations", fixture.roomId())
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .param("date", "2026-08-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].purpose").value("정상 회의"));
    }

    @Test
    @DisplayName("예약이 없는 날짜는 404가 아니라 200과 빈 배열을 반환한다")
    void 예약이_없는_날짜는_빈_배열이다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();

        mockMvc.perform(get("/api/rooms/{roomId}/reservations", fixture.roomId())
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .param("date", "2026-08-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("남의 그룹 회의실 현황은 조회할 수 없다 — 403과 NOT_GROUP_MEMBER")
    void 남의_그룹_회의실_현황은_조회할_수_없다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();
        String strangerToken = signupAndLogin("stranger@sqisoft.com", "최무관");

        mockMvc.perform(get("/api/rooms/{roomId}/reservations", fixture.roomId())
                        .header("Authorization", bearer(strangerToken))
                        .param("date", "2026-08-25"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));
    }

    @Test
    @DisplayName("존재하지 않는 회의실을 조회하면 404와 ROOM_NOT_FOUND를 반환한다")
    void 없는_회의실_조회는_404다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();

        mockMvc.perform(get("/api/rooms/{roomId}/reservations", 99999L)
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .param("date", "2026-08-26"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    @DisplayName("날짜 형식이 잘못되면 400과 INVALID_DATE_FORMAT을 반환한다")
    void 잘못된_날짜_형식은_400이다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();

        mockMvc.perform(get("/api/rooms/{roomId}/reservations", fixture.roomId())
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .param("date", "2026-13-99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"));
    }

    @Test
    @DisplayName("날짜 형식 오류와 회의실 부재가 동시에 발생하면 INVALID_DATE_FORMAT이 우선한다")
    void 날짜_형식_오류가_회의실_부재보다_우선한다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();

        mockMvc.perform(get("/api/rooms/{roomId}/reservations", 99999L)
                        .header("Authorization", bearer(fixture.ownerToken()))
                        .param("date", "2026-13-99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"));
    }

    @Test
    @DisplayName("인증 실패는 날짜 형식 검사보다 먼저 판정된다 — 401과 UNAUTHENTICATED")
    void 인증_실패가_날짜_형식보다_먼저다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();

        mockMvc.perform(get("/api/rooms/{roomId}/reservations", fixture.roomId())
                        .param("date", "2026-13-99"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
