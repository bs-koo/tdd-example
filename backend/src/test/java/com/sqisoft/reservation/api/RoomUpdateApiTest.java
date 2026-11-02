package com.sqisoft.reservation.api;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.sqisoft.reservation.api.support.ApiTestSupport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RoomUpdateApiTest extends ApiTestSupport {

    private static final String NEW_BODY = """
            {"name":"대회의실 B","capacity":12,"location":"5층"}
            """;

    private record Fx(String ownerToken, String memberToken, long groupId, long roomId) {
    }

    // ── 헬퍼 ──

    private ResultActions putRoom(long roomId, String token, String body) throws Exception {
        var request = put("/api/rooms/{roomId}", roomId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
        if (token != null) {
            request.header("Authorization", bearer(token));
        }
        return mockMvc.perform(request);
    }

    private String body(String name, Integer capacity, String location) {
        String cap = capacity == null ? "" : "\"capacity\":" + capacity + ",";
        return "{\"name\":\"" + name + "\"," + cap + "\"location\":\"" + location + "\"}";
    }

    private String roomList(String token, long groupId) throws Exception {
        return mockMvc.perform(get("/api/groups/{groupId}/rooms", groupId)
                        .header("Authorization", bearer(token)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    /** 방 목록(그룹 단위)에서 첫 방이 기대 값 그대로인지 확인한다. */
    private void assertOriginalRoom(String token, long groupId) throws Exception {
        em.flush();
        em.clear();
        mockMvc.perform(get("/api/groups/{groupId}/rooms", groupId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("대회의실"))
                .andExpect(jsonPath("$[0].capacity").value(10))
                .andExpect(jsonPath("$[0].location").value("3층"));
    }

    /** 그룹장 + 승인 그룹원 + 방 "대회의실"/10/"3층" 1개. */
    private Fx prepare() throws Exception {
        String ownerToken = signupAndLogin("owner@sqisoft.com", "김본승");
        String memberToken = signupAndLogin("member@sqisoft.com", "박그룹");
        long groupId = createGroup(ownerToken, "개발팀");
        accept(memberToken, invite(ownerToken, groupId, "member@sqisoft.com"));
        long roomId = createRoom(ownerToken, groupId, "대회의실", 10, "3층");
        return new Fx(ownerToken, memberToken, groupId, roomId);
    }

    // ── AC-1, AC-2: 인증 ──

    @Test
    @DisplayName("Authorization 헤더가 없으면 수정할 수 없다 — 401 UNAUTHENTICATED, 방은 그대로")
    void 헤더_없는_수정은_401이다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), null, NEW_BODY)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        assertOriginalRoom(f.ownerToken(), f.groupId());
    }

    @Test
    @DisplayName("잘못된 토큰으로는 수정할 수 없다 — 401 UNAUTHENTICATED, 방은 그대로")
    void 잘못된_토큰_수정은_401이다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), "not-a-valid-token", NEW_BODY)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        assertOriginalRoom(f.ownerToken(), f.groupId());
    }

    @Test
    @DisplayName("로그아웃해 무효가 된 토큰으로는 수정할 수 없다 — 401 UNAUTHENTICATED, 방은 그대로")
    void 로그아웃된_토큰_수정은_401이다() throws Exception {
        Fx f = prepare();
        mockMvc.perform(post("/api/auth/logout").header("Authorization", bearer(f.ownerToken())));

        putRoom(f.roomId(), f.ownerToken(), NEW_BODY)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        String viewer = login("member@sqisoft.com");
        assertOriginalRoom(viewer, f.groupId());
    }

    @Test
    @DisplayName("헤더가 없으면 본문이 깨져 있어도 401 UNAUTHENTICATED 이다 (400 아님)")
    void 헤더_없고_본문_깨져도_401이다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), null, "{bad")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("특성화: 토큰이 있고 본문 JSON 이 깨지면 400 INVALID_DATE_FORMAT 이다 (기존 파싱 우선 계약)")
    void 토큰_있고_본문_깨지면_400이다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), f.ownerToken(), "{bad")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_DATE_FORMAT"));

        assertOriginalRoom(f.ownerToken(), f.groupId());
    }

    // ── AC-4: 방 없음 ──

    @Test
    @DisplayName("없는 roomId 는 외부인이 유효한 값으로 수정해도 404 ROOM_NOT_FOUND 이다")
    void 없는_방_수정은_404다() throws Exception {
        prepare();
        String stranger = signupAndLogin("stranger@sqisoft.com", "최무관");

        putRoom(999999L, stranger, NEW_BODY)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    // ── AC-6, AC-7: 성공 ──

    @Test
    @DisplayName("그룹장이 수정하면 200 과 새 값을 반환하고 id·groupId 는 그대로이며 방 개수는 불변이다")
    void 그룹장은_방을_수정할_수_있다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), f.ownerToken(), NEW_BODY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value((int) f.roomId()))
                .andExpect(jsonPath("$.groupId").value((int) f.groupId()))
                .andExpect(jsonPath("$.name").value("대회의실 B"))
                .andExpect(jsonPath("$.capacity").value(12))
                .andExpect(jsonPath("$.location").value("5층"));

        em.flush();
        em.clear();
        mockMvc.perform(get("/api/groups/{groupId}/rooms", f.groupId())
                        .header("Authorization", bearer(f.ownerToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value((int) f.roomId()))
                .andExpect(jsonPath("$[0].name").value("대회의실 B"))
                .andExpect(jsonPath("$[0].capacity").value(12))
                .andExpect(jsonPath("$[0].location").value("5층"));
    }

    @Test
    @DisplayName("승인된 일반 그룹원도 수정할 수 있다 — 200")
    void 일반_그룹원도_방을_수정할_수_있다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), f.memberToken(), NEW_BODY)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("대회의실 B"));
    }

    // ── AC-19, AC-20: 권한 ──

    @Test
    @DisplayName("승인 대기자(PENDING)는 수정할 수 없다 — 403 NOT_GROUP_MEMBER, 방은 그대로")
    void 대기자는_수정할_수_없다() throws Exception {
        Fx f = prepare();
        String pending = signupAndLogin("pending@sqisoft.com", "정대기");
        invite(f.ownerToken(), f.groupId(), "pending@sqisoft.com");

        putRoom(f.roomId(), pending, NEW_BODY)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));

        assertOriginalRoom(f.ownerToken(), f.groupId());
    }

    @Test
    @DisplayName("초대를 거절한 사람(REJECTED)은 수정할 수 없다 — 403 NOT_GROUP_MEMBER, 방은 그대로")
    void 거절자는_수정할_수_없다() throws Exception {
        Fx f = prepare();
        String rejected = signupAndLogin("rejected@sqisoft.com", "한거절");
        long membershipId = invite(f.ownerToken(), f.groupId(), "rejected@sqisoft.com");
        mockMvc.perform(post("/api/invitations/{id}/reject", membershipId)
                .header("Authorization", bearer(rejected)));

        putRoom(f.roomId(), rejected, NEW_BODY)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));

        assertOriginalRoom(f.ownerToken(), f.groupId());
    }

    @Test
    @DisplayName("그룹 외부인은 수정할 수 없다 — 403 NOT_GROUP_MEMBER, 방은 그대로")
    void 외부인은_수정할_수_없다() throws Exception {
        Fx f = prepare();
        String stranger = signupAndLogin("stranger@sqisoft.com", "최무관");

        putRoom(f.roomId(), stranger, NEW_BODY)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));

        assertOriginalRoom(f.ownerToken(), f.groupId());
    }

    @Test
    @DisplayName("외부인이 입력도 모두 위반하면 400 이 아니라 403 NOT_GROUP_MEMBER 이다 (권한이 검증보다 먼저)")
    void 외부인의_위반_입력은_403이다() throws Exception {
        Fx f = prepare();
        String stranger = signupAndLogin("stranger@sqisoft.com", "최무관");

        putRoom(f.roomId(), stranger, body("가".repeat(31), 0, "나".repeat(31)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));

        assertOriginalRoom(f.ownerToken(), f.groupId());
    }

    // ── AC-8~16: 입력 검증 ──

    @Test
    @DisplayName("이름 앞뒤 공백은 trim 되어 저장된다")
    void 이름은_trim_된다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), f.ownerToken(), body("  소회의실  ", 6, "3층"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("소회의실"));
    }

    @Test
    @DisplayName("이름 1자는 허용된다 — 200")
    void 이름_1자는_허용된다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), f.ownerToken(), body("가", 10, "3층"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("가"));
    }

    @Test
    @DisplayName("이름 30자는 허용된다 — 200")
    void 이름_30자는_허용된다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), f.ownerToken(), body("가".repeat(30), 10, "3층"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("가".repeat(30)));
    }

    @Test
    @DisplayName("이름 31자는 400 INVALID_ROOM_NAME 이다")
    void 이름_31자는_400이다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), f.ownerToken(), body("가".repeat(31), 10, "3층"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ROOM_NAME"));
    }

    @Test
    @DisplayName("공백뿐인 이름은 400 INVALID_ROOM_NAME 이다")
    void 공백뿐인_이름은_400이다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), f.ownerToken(), body("   ", 10, "3층"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ROOM_NAME"));
    }

    @Test
    @DisplayName("위치 31자는 400 INVALID_ROOM_LOCATION 이다")
    void 위치_31자는_400이다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), f.ownerToken(), body("새이름", 10, "나".repeat(31)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ROOM_LOCATION"));
    }

    @Test
    @DisplayName("인원 1 은 허용된다 — 200")
    void 인원_1은_허용된다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), f.ownerToken(), body("대회의실", 1, "3층"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(1));
    }

    @Test
    @DisplayName("인원 999 는 허용된다 — 200")
    void 인원_999는_허용된다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), f.ownerToken(), body("대회의실", 999, "3층"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.capacity").value(999));
    }

    @Test
    @DisplayName("인원 0 은 400 INVALID_ROOM_CAPACITY 이다")
    void 인원_0은_400이다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), f.ownerToken(), body("대회의실", 0, "3층"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ROOM_CAPACITY"));
    }

    @Test
    @DisplayName("인원 1000 은 400 INVALID_ROOM_CAPACITY 이다")
    void 인원_1000은_400이다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), f.ownerToken(), body("대회의실", 1000, "3층"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ROOM_CAPACITY"));
    }

    @Test
    @DisplayName("인원 -1 은 400 INVALID_ROOM_CAPACITY 이다")
    void 인원_음수는_400이다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), f.ownerToken(), body("대회의실", -1, "3층"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ROOM_CAPACITY"));
    }

    @Test
    @DisplayName("인원 필드를 생략하면 0 으로 간주되어 400 INVALID_ROOM_CAPACITY 이다")
    void 인원_생략은_400이다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), f.ownerToken(), body("대회의실", null, "3층"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ROOM_CAPACITY"));
    }

    @Test
    @DisplayName("위반이 여럿이면 이름이 가장 먼저 보고된다 — 이름31+인원0+위치31 → INVALID_ROOM_NAME")
    void 위반_여럿이면_이름이_먼저다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), f.ownerToken(), body("가".repeat(31), 0, "나".repeat(31)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ROOM_NAME"));
    }

    @Test
    @DisplayName("이름이 정상이고 인원0+위치31 이면 인원이 위치보다 먼저 보고된다 — INVALID_ROOM_CAPACITY")
    void 위반_여럿이면_인원이_위치보다_먼저다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), f.ownerToken(), body("새이름", 0, "나".repeat(31)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ROOM_CAPACITY"));
    }

    @Test
    @DisplayName("같은 값으로 수정해도 200 이다")
    void 같은_값으로_수정해도_200이다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), f.ownerToken(), body("대회의실", 10, "3층"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("대회의실"))
                .andExpect(jsonPath("$.capacity").value(10))
                .andExpect(jsonPath("$.location").value("3층"));
    }

    // ── 원자성 ──

    @Test
    @DisplayName("원자성: 새 이름 + 인원 0 으로 거부되면 이름·인원·위치 모두 원래 값이다")
    void 인원_위반_거부시_이름도_바뀌지_않는다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), f.ownerToken(), body("새이름", 0, "3층"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ROOM_CAPACITY"));

        assertOriginalRoom(f.ownerToken(), f.groupId());
    }

    @Test
    @DisplayName("원자성: 새 이름 + 새 인원 + 위치 31자로 거부되면 이름·인원·위치 모두 원래 값이다")
    void 위치_위반_거부시_이름과_인원도_바뀌지_않는다() throws Exception {
        Fx f = prepare();

        putRoom(f.roomId(), f.ownerToken(), body("새이름", 5, "나".repeat(31)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_ROOM_LOCATION"));

        assertOriginalRoom(f.ownerToken(), f.groupId());
    }

    // ── AC-17: 예약 불변 ──

    @Test
    @DisplayName("예약이 있는 방의 이름과 인원(10→1)을 바꿔도 200 이고 예약은 그대로다")
    void 예약이_있어도_수정되고_예약은_불변이다() throws Exception {
        Fx f = prepare();
        createReservation(f.memberToken(), f.roomId(), "정상 회의",
                "2026-08-26T09:00:00", "2026-08-26T10:00:00");
        long cancelled = createReservation(f.memberToken(), f.roomId(), "취소될 회의",
                "2026-08-26T11:00:00", "2026-08-26T12:00:00");
        mockMvc.perform(post("/api/reservations/{id}/cancel", cancelled)
                        .header("Authorization", bearer(f.memberToken())))
                .andExpect(status().isNoContent());

        putRoom(f.roomId(), f.ownerToken(), body("바뀐이름", 1, "3층"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("바뀐이름"))
                .andExpect(jsonPath("$.capacity").value(1));

        em.flush();
        em.clear();
        mockMvc.perform(get("/api/rooms/{roomId}/reservations", f.roomId())
                        .header("Authorization", bearer(f.ownerToken()))
                        .param("date", "2026-08-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].purpose").value("정상 회의"))
                .andExpect(jsonPath("$[0].reserverName").value("박그룹"))
                .andExpect(jsonPath("$[0].startAt").value("2026-08-26T09:00:00"))
                .andExpect(jsonPath("$[0].endAt").value("2026-08-26T10:00:00"))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    // ── AC-18: 다른 방 불변 ──

    @Test
    @DisplayName("한 방을 수정해도 같은 그룹의 다른 방과 다른 그룹의 방은 그대로다")
    void 다른_방은_바뀌지_않는다() throws Exception {
        Fx f = prepare();
        long sibling = createRoom(f.ownerToken(), f.groupId(), "소회의실", 6, "4층");
        long otherGroup = createGroup(f.ownerToken(), "기획팀");
        createRoom(f.ownerToken(), otherGroup, "대회의실", 10, "3층");
        String siblingBefore = roomList(f.ownerToken(), otherGroup);

        putRoom(f.roomId(), f.ownerToken(), NEW_BODY).andExpect(status().isOk());

        em.flush();
        em.clear();
        mockMvc.perform(get("/api/groups/{groupId}/rooms", f.groupId())
                        .header("Authorization", bearer(f.ownerToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("대회의실 B"))
                .andExpect(jsonPath("$[1].id").value((int) sibling))
                .andExpect(jsonPath("$[1].name").value("소회의실"))
                .andExpect(jsonPath("$[1].capacity").value(6))
                .andExpect(jsonPath("$[1].location").value("4층"));
        assertThat(roomList(f.ownerToken(), otherGroup)).isEqualTo(siblingBefore);
        mockMvc.perform(get("/api/groups/{groupId}/rooms", otherGroup)
                        .header("Authorization", bearer(f.ownerToken())))
                .andExpect(jsonPath("$[0].name").value("대회의실"))
                .andExpect(jsonPath("$[0].capacity").value(10))
                .andExpect(jsonPath("$[0].location").value("3층"));
    }
}
