package com.sqisoft.reservation.api;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.sqisoft.reservation.api.support.ApiTestSupport;
import com.sqisoft.reservation.application.ReservationRepository;
import com.sqisoft.reservation.domain.Reservation;
import com.sqisoft.reservation.domain.ReservationStatus;
import com.sqisoft.reservation.domain.TimeSlot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 기준 시각(now) = 2026-08-25 09:00 KST (FixedClockConfig). */
class RoomDeleteApiTest extends ApiTestSupport {

    private static final String ACTIVE_MSG = "예정된 예약이 있어 삭제할 수 없습니다. 예약을 먼저 취소해주세요.";

    @Autowired
    private ReservationRepository reservationRepository;

    private record Fx(String ownerToken, String memberToken, long groupId, long roomId) {
    }

    // ── 헬퍼 ──

    private ResultActions deleteRoom(long roomId, String token) throws Exception {
        var request = delete("/api/rooms/{roomId}", roomId);
        if (token != null) {
            request.header("Authorization", bearer(token));
        }
        return mockMvc.perform(request);
    }

    /** 그룹장 owner + 승인 그룹원 member + 방 "대회의실"/10/"3층" 1개. */
    private Fx prepare() throws Exception {
        String ownerToken = signupAndLogin("owner@sqisoft.com", "김본승");
        String memberToken = signupAndLogin("member@sqisoft.com", "박그룹");
        long groupId = createGroup(ownerToken, "개발팀");
        accept(memberToken, invite(ownerToken, groupId, "member@sqisoft.com"));
        long roomId = createRoom(ownerToken, groupId, "대회의실", 10, "3층");
        return new Fx(ownerToken, memberToken, groupId, roomId);
    }

    /** 예약 API 로 2026-08-26 10:00~11:00 ACTIVE 예약을 만든다 (예약자: member). */
    private long futureReservation(Fx f) throws Exception {
        return createReservation(f.memberToken(), f.roomId(), "앞으로의 회의",
                "2026-08-26T10:00:00", "2026-08-26T11:00:00");
    }

    /** 지난/경계 슬롯은 예약 API 로 만들 수 없어 저장소로 직접 심는다. */
    private void seedActive(long roomId, LocalDateTime start, LocalDateTime end) {
        reservationRepository.save(Reservation.create(
                roomId, 10L, "박그룹", "심어둔 회의",
                new TimeSlot(start, end), LocalDateTime.of(2026, 8, 20, 9, 0)));
        em.flush();
    }

    private void cancelByApi(String token, long reservationId) throws Exception {
        mockMvc.perform(post("/api/reservations/{id}/cancel", reservationId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());
    }

    private void assertRoomListed(String token, long groupId, long roomId) throws Exception {
        em.flush();
        em.clear();
        mockMvc.perform(get("/api/groups/{groupId}/rooms", groupId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + roomId + ")]").isNotEmpty());
    }

    private void assertRoomNotListed(String token, long groupId, long roomId) throws Exception {
        em.flush();
        em.clear();
        mockMvc.perform(get("/api/groups/{groupId}/rooms", groupId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id==" + roomId + ")]").isEmpty());
    }

    private long reservationCount() {
        em.flush();
        em.clear();
        return (Long) em.createQuery("select count(r) from Reservation r").getSingleResult();
    }

    // ── AC-3: 인증 ──

    @Test
    @DisplayName("Authorization 헤더가 없으면 삭제할 수 없다 — 401 UNAUTHENTICATED, 방은 그대로")
    void 헤더_없는_삭제는_401이다() throws Exception {
        Fx f = prepare();

        deleteRoom(f.roomId(), null)
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        assertRoomListed(f.ownerToken(), f.groupId(), f.roomId());
    }

    @Test
    @DisplayName("잘못된 토큰으로는 삭제할 수 없다 — 401 UNAUTHENTICATED, 방은 그대로")
    void 잘못된_토큰_삭제는_401이다() throws Exception {
        Fx f = prepare();

        deleteRoom(f.roomId(), "not-a-valid-token")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

        assertRoomListed(f.ownerToken(), f.groupId(), f.roomId());
    }

    // ── AC-5: 방 없음 ──

    @Test
    @DisplayName("없는 roomId 는 외부인이 삭제해도 404 ROOM_NOT_FOUND 이다 (403 아님)")
    void 없는_방_삭제는_404다() throws Exception {
        prepare();
        String stranger = signupAndLogin("stranger@sqisoft.com", "최무관");

        deleteRoom(999999L, stranger)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    // ── AC-21: 성공 ──

    @Test
    @DisplayName("그룹장이 예약 없는 방을 삭제하면 204 이고 방 목록에는 다른 방 1건만 남는다")
    void 그룹장은_예약_없는_방을_삭제할_수_있다() throws Exception {
        Fx f = prepare();
        long r2 = createRoom(f.ownerToken(), f.groupId(), "소회의실", 6, "4층");

        deleteRoom(f.roomId(), f.ownerToken())
                .andExpect(status().isNoContent());

        em.flush();
        em.clear();
        mockMvc.perform(get("/api/groups/{groupId}/rooms", f.groupId())
                        .header("Authorization", bearer(f.ownerToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value((int) r2))
                .andExpect(jsonPath("$[0].name").value("소회의실"));
    }

    // ── AC-22, AC-23: 권한 ──

    @Test
    @DisplayName("일반 그룹원은 삭제할 수 없다 — 403 NOT_GROUP_OWNER, 방은 그대로")
    void 일반_그룹원은_삭제할_수_없다() throws Exception {
        Fx f = prepare();

        deleteRoom(f.roomId(), f.memberToken())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_OWNER"));

        assertRoomListed(f.ownerToken(), f.groupId(), f.roomId());
    }

    @Test
    @DisplayName("승인 대기자(PENDING)는 삭제할 수 없다 — 403 NOT_GROUP_OWNER, 방은 그대로")
    void 대기자는_삭제할_수_없다() throws Exception {
        Fx f = prepare();
        String pending = signupAndLogin("pending@sqisoft.com", "정대기");
        invite(f.ownerToken(), f.groupId(), "pending@sqisoft.com");

        deleteRoom(f.roomId(), pending)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_OWNER"));

        assertRoomListed(f.ownerToken(), f.groupId(), f.roomId());
    }

    @Test
    @DisplayName("초대를 거절한 사람(REJECTED)은 삭제할 수 없다 — 403 NOT_GROUP_OWNER, 방은 그대로")
    void 거절자는_삭제할_수_없다() throws Exception {
        Fx f = prepare();
        String rejected = signupAndLogin("rejected@sqisoft.com", "한거절");
        long membershipId = invite(f.ownerToken(), f.groupId(), "rejected@sqisoft.com");
        mockMvc.perform(post("/api/invitations/{id}/reject", membershipId)
                .header("Authorization", bearer(rejected)));

        deleteRoom(f.roomId(), rejected)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_OWNER"));

        assertRoomListed(f.ownerToken(), f.groupId(), f.roomId());
    }

    @Test
    @DisplayName("그룹 외부인은 삭제할 수 없다 — 403 NOT_GROUP_OWNER, 방은 그대로")
    void 외부인은_삭제할_수_없다() throws Exception {
        Fx f = prepare();
        String stranger = signupAndLogin("stranger@sqisoft.com", "최무관");

        deleteRoom(f.roomId(), stranger)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_OWNER"));

        assertRoomListed(f.ownerToken(), f.groupId(), f.roomId());
    }

    // ── AC-24, AC-31: 앞으로의 ACTIVE 예약 ──

    @Test
    @DisplayName("앞으로의 ACTIVE 예약이 있으면 그룹장도 삭제할 수 없다 — 409, 방과 예약은 그대로")
    void 앞으로의_예약이_있으면_삭제는_409다() throws Exception {
        Fx f = prepare();
        futureReservation(f);

        deleteRoom(f.roomId(), f.ownerToken())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_HAS_ACTIVE_RESERVATIONS"));

        assertRoomListed(f.ownerToken(), f.groupId(), f.roomId());
        mockMvc.perform(get("/api/rooms/{roomId}/reservations", f.roomId())
                        .header("Authorization", bearer(f.ownerToken()))
                        .param("date", "2026-08-26"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    @DisplayName("삭제 거절 응답은 정확한 code 와 안내 문구를 담는다")
    void 삭제_거절_응답은_정해진_문구를_담는다() throws Exception {
        Fx f = prepare();
        futureReservation(f);

        deleteRoom(f.roomId(), f.ownerToken())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_HAS_ACTIVE_RESERVATIONS"))
                .andExpect(jsonPath("$.message").value(ACTIVE_MSG));
    }

    // ── AC-25: 경계 ──

    @Test
    @DisplayName("경계: 종료 시각이 기준 시각과 같은 ACTIVE 예약(08:00~09:00)만 있으면 삭제된다 — 204")
    void 종료가_기준시각과_같은_예약만_있으면_삭제된다() throws Exception {
        Fx f = prepare();
        seedActive(f.roomId(), LocalDateTime.of(2026, 8, 25, 8, 0), LocalDateTime.of(2026, 8, 25, 9, 0));

        deleteRoom(f.roomId(), f.ownerToken()).andExpect(status().isNoContent());

        assertRoomNotListed(f.ownerToken(), f.groupId(), f.roomId());
    }

    @Test
    @DisplayName("경계: 진행 중인 ACTIVE 예약(08:30~09:30)이 있으면 삭제할 수 없다 — 409")
    void 진행_중인_예약이_있으면_삭제는_409다() throws Exception {
        Fx f = prepare();
        seedActive(f.roomId(), LocalDateTime.of(2026, 8, 25, 8, 30), LocalDateTime.of(2026, 8, 25, 9, 30));

        deleteRoom(f.roomId(), f.ownerToken())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ROOM_HAS_ACTIVE_RESERVATIONS"));

        assertRoomListed(f.ownerToken(), f.groupId(), f.roomId());
    }

    @Test
    @DisplayName("경계: 앞으로의 날짜에 CANCELLED 예약만 있으면 삭제된다 — 204")
    void 앞으로의_취소된_예약만_있으면_삭제된다() throws Exception {
        Fx f = prepare();
        cancelByApi(f.memberToken(), futureReservation(f));

        deleteRoom(f.roomId(), f.ownerToken()).andExpect(status().isNoContent());

        assertRoomNotListed(f.ownerToken(), f.groupId(), f.roomId());
    }

    @Test
    @DisplayName("경계: 어제(2026-08-24)의 ACTIVE 예약만 있으면 삭제된다 — 204")
    void 어제의_예약만_있으면_삭제된다() throws Exception {
        Fx f = prepare();
        seedActive(f.roomId(), LocalDateTime.of(2026, 8, 24, 10, 0), LocalDateTime.of(2026, 8, 24, 11, 0));

        deleteRoom(f.roomId(), f.ownerToken()).andExpect(status().isNoContent());

        assertRoomNotListed(f.ownerToken(), f.groupId(), f.roomId());
    }

    // ── AC-26: 취소 후 삭제 ──

    @Test
    @DisplayName("예약자가 앞으로의 예약을 취소한 뒤에는 그룹장이 삭제할 수 있다 — 204")
    void 예약을_취소한_뒤에는_삭제된다() throws Exception {
        Fx f = prepare();
        long reservationId = futureReservation(f);
        deleteRoom(f.roomId(), f.ownerToken()).andExpect(status().isConflict());

        cancelByApi(f.memberToken(), reservationId);

        deleteRoom(f.roomId(), f.ownerToken()).andExpect(status().isNoContent());
        assertRoomNotListed(f.ownerToken(), f.groupId(), f.roomId());
    }

    // ── AC-27: 권한이 예약 상태보다 먼저 ──

    @Test
    @DisplayName("앞으로의 예약이 있는 방도 외부인은 409 가 아니라 403 NOT_GROUP_OWNER 이다")
    void 외부인의_삭제는_예약이_있어도_403이다() throws Exception {
        Fx f = prepare();
        futureReservation(f);
        String stranger = signupAndLogin("stranger@sqisoft.com", "최무관");

        deleteRoom(f.roomId(), stranger)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_OWNER"));

        assertRoomListed(f.ownerToken(), f.groupId(), f.roomId());
        assertThat(reservationCount()).isEqualTo(1L);
    }

    @Test
    @DisplayName("앞으로의 예약이 있는 방도 일반 그룹원은 409 가 아니라 403 NOT_GROUP_OWNER 이다")
    void 일반_그룹원의_삭제는_예약이_있어도_403이다() throws Exception {
        Fx f = prepare();
        futureReservation(f);

        deleteRoom(f.roomId(), f.memberToken())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_OWNER"));

        assertRoomListed(f.ownerToken(), f.groupId(), f.roomId());
        assertThat(reservationCount()).isEqualTo(1L);
    }

    // ── AC-28: 예약 행 보존 ──

    @Test
    @DisplayName("방을 삭제해도 지난 ACTIVE·CANCELLED 예약 행은 상태와 roomId 그대로 남고 재취소는 409 ALREADY_CANCELLED 이다")
    void 삭제해도_예약_행은_남는다() throws Exception {
        Fx f = prepare();
        seedActive(f.roomId(), LocalDateTime.of(2026, 8, 24, 10, 0), LocalDateTime.of(2026, 8, 24, 11, 0));
        long cancelledId = futureReservation(f);
        cancelByApi(f.memberToken(), cancelledId);

        deleteRoom(f.roomId(), f.ownerToken()).andExpect(status().isNoContent());

        em.flush();
        em.clear();
        List<Reservation> rows = em.createQuery(
                "select r from Reservation r order by r.id", Reservation.class).getResultList();
        assertThat(rows).hasSize(2);
        assertThat(rows).extracting(Reservation::status)
                .containsExactly(ReservationStatus.ACTIVE, ReservationStatus.CANCELLED);
        assertThat(rows).extracting(Reservation::roomId)
                .containsExactly(f.roomId(), f.roomId());

        mockMvc.perform(post("/api/reservations/{id}/cancel", cancelledId)
                        .header("Authorization", bearer(f.memberToken())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_CANCELLED"));
    }

    // ── AC-29: 삭제 후 접근 ──

    @Test
    @DisplayName("삭제된 방에 예약 생성을 시도하면 404 ROOM_NOT_FOUND 이고 예약 건수는 그대로다")
    void 삭제된_방에는_예약할_수_없다() throws Exception {
        Fx f = prepare();
        deleteRoom(f.roomId(), f.ownerToken()).andExpect(status().isNoContent());
        long before = reservationCount();

        mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(f.memberToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"purpose":"유령 회의","startAt":"2026-08-26T10:00:00","endAt":"2026-08-26T11:00:00"}
                                """.formatted(f.roomId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));

        assertThat(reservationCount()).isEqualTo(before);
    }

    @Test
    @DisplayName("삭제된 방의 날짜별 예약 조회는 404 ROOM_NOT_FOUND 이다")
    void 삭제된_방의_예약_조회는_404다() throws Exception {
        Fx f = prepare();
        deleteRoom(f.roomId(), f.ownerToken()).andExpect(status().isNoContent());

        mockMvc.perform(get("/api/rooms/{roomId}/reservations", f.roomId())
                        .header("Authorization", bearer(f.memberToken()))
                        .param("date", "2026-08-26"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    @DisplayName("이미 삭제된 방을 다시 삭제하면 404 ROOM_NOT_FOUND 이다")
    void 삭제된_방_재삭제는_404다() throws Exception {
        Fx f = prepare();
        deleteRoom(f.roomId(), f.ownerToken()).andExpect(status().isNoContent());

        deleteRoom(f.roomId(), f.ownerToken())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    @Test
    @DisplayName("삭제된 방을 수정하면 404 ROOM_NOT_FOUND 이다")
    void 삭제된_방_수정은_404다() throws Exception {
        Fx f = prepare();
        deleteRoom(f.roomId(), f.ownerToken()).andExpect(status().isNoContent());

        mockMvc.perform(put("/api/rooms/{roomId}", f.roomId())
                        .header("Authorization", bearer(f.memberToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"되살리기","capacity":5,"location":"1층"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROOM_NOT_FOUND"));
    }

    // ── AC-30: 유일한 방 삭제 후 ──

    @Test
    @DisplayName("유일한 방을 삭제하면 목록은 빈 배열이고 이어서 새 방을 추가할 수 있다")
    void 유일한_방_삭제_후_새_방을_추가할_수_있다() throws Exception {
        Fx f = prepare();

        deleteRoom(f.roomId(), f.ownerToken()).andExpect(status().isNoContent());

        em.flush();
        em.clear();
        String list = mockMvc.perform(get("/api/groups/{groupId}/rooms", f.groupId())
                        .header("Authorization", bearer(f.ownerToken())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(list).isEqualTo("[]");

        mockMvc.perform(post("/api/groups/{groupId}/rooms", f.groupId())
                        .header("Authorization", bearer(f.ownerToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"신규","capacity":4,"location":"1층"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("신규"));
    }
}
