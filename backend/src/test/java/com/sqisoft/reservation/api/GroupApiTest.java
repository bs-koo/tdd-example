package com.sqisoft.reservation.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.sqisoft.reservation.api.support.ApiTestSupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GroupApiTest extends ApiTestSupport {

    // ── 그룹 생성·조회 ──

    @Test
    @DisplayName("그룹을 만들면 201과 함께 생성자가 OWNER·APPROVED로 반환된다")
    void 그룹을_만들면_생성자가_그룹장이_된다() throws Exception {
        String token = signupAndLogin("owner@sqisoft.com", "김본승");

        mockMvc.perform(post("/api/groups")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"개발팀"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("개발팀"))
                .andExpect(jsonPath("$.role").value("OWNER"))
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    @DisplayName("로그인하지 않고 그룹을 만들면 401과 UNAUTHENTICATED를 반환한다")
    void 비로그인_그룹_생성은_401이다() throws Exception {
        mockMvc.perform(post("/api/groups")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"개발팀"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("그룹 이름이 비어 있으면 400과 INVALID_GROUP_NAME을 반환한다")
    void 빈_그룹_이름은_400이다() throws Exception {
        String token = signupAndLogin("owner@sqisoft.com", "김본승");

        mockMvc.perform(post("/api/groups")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_GROUP_NAME"));
    }

    @Test
    @DisplayName("내 그룹 목록은 승인된 그룹만 반환한다 — 대기 중인 초대는 빠진다")
    void 내_그룹_목록은_승인된_것만_반환한다() throws Exception {
        String ownerToken = signupAndLogin("owner@sqisoft.com", "김본승");
        String inviteeToken = signupAndLogin("invitee@sqisoft.com", "박초대");
        long groupId = createGroup(ownerToken, "개발팀");
        invite(ownerToken, groupId, "invitee@sqisoft.com");

        mockMvc.perform(get("/api/groups").header("Authorization", bearer(ownerToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("개발팀"));

        mockMvc.perform(get("/api/groups").header("Authorization", bearer(inviteeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── 초대 ──

    @Test
    @DisplayName("그룹장이 초대하면 201과 PENDING 멤버십을 반환한다")
    void 그룹장의_초대는_201이다() throws Exception {
        String ownerToken = signupAndLogin("owner@sqisoft.com", "김본승");
        signupAndLogin("invitee@sqisoft.com", "박초대");
        long groupId = createGroup(ownerToken, "개발팀");

        mockMvc.perform(post("/api/groups/{groupId}/invitations", groupId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"invitee@sqisoft.com"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.membershipId").isNumber())
                .andExpect(jsonPath("$.email").value("invitee@sqisoft.com"))
                .andExpect(jsonPath("$.name").value("박초대"))
                .andExpect(jsonPath("$.role").value("MEMBER"))
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("그룹장이 아니면 초대할 수 없고 403과 NOT_GROUP_OWNER를 반환한다")
    void 그룹장이_아니면_초대는_403이다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();
        signupAndLogin("third@sqisoft.com", "최제삼");

        mockMvc.perform(post("/api/groups/{groupId}/invitations", fixture.groupId())
                        .header("Authorization", bearer(fixture.memberToken()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"third@sqisoft.com"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_OWNER"));
    }

    @Test
    @DisplayName("가입하지 않은 이메일을 초대하면 404와 MEMBER_NOT_FOUND를 반환한다")
    void 미가입_이메일_초대는_404다() throws Exception {
        String ownerToken = signupAndLogin("owner@sqisoft.com", "김본승");
        long groupId = createGroup(ownerToken, "개발팀");

        mockMvc.perform(post("/api/groups/{groupId}/invitations", groupId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@sqisoft.com"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MEMBER_NOT_FOUND"));
    }

    @Test
    @DisplayName("자기 자신을 초대하면 400과 SELF_INVITATION을 반환한다")
    void 자기_자신_초대는_400이다() throws Exception {
        String ownerToken = signupAndLogin("owner@sqisoft.com", "김본승");
        long groupId = createGroup(ownerToken, "개발팀");

        mockMvc.perform(post("/api/groups/{groupId}/invitations", groupId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"owner@sqisoft.com"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SELF_INVITATION"));
    }

    @Test
    @DisplayName("이미 초대한 사람을 또 초대하면 409와 ALREADY_INVITED를 반환한다")
    void 중복_초대는_409다() throws Exception {
        String ownerToken = signupAndLogin("owner@sqisoft.com", "김본승");
        signupAndLogin("invitee@sqisoft.com", "박초대");
        long groupId = createGroup(ownerToken, "개발팀");
        invite(ownerToken, groupId, "invitee@sqisoft.com");

        mockMvc.perform(post("/api/groups/{groupId}/invitations", groupId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"invitee@sqisoft.com"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_INVITED"));
    }

    // ── 초대 응답 ──

    @Test
    @DisplayName("받은 초대 목록은 그룹 이름과 초대자 이름을 함께 반환한다")
    void 받은_초대_목록을_조회한다() throws Exception {
        String ownerToken = signupAndLogin("owner@sqisoft.com", "김본승");
        String inviteeToken = signupAndLogin("invitee@sqisoft.com", "박초대");
        long groupId = createGroup(ownerToken, "개발팀");
        long membershipId = invite(ownerToken, groupId, "invitee@sqisoft.com");

        mockMvc.perform(get("/api/invitations").header("Authorization", bearer(inviteeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value((int) membershipId))
                .andExpect(jsonPath("$[0].groupName").value("개발팀"))
                .andExpect(jsonPath("$[0].invitedBy").value("김본승"))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    @DisplayName("초대를 수락하면 200과 APPROVED를 반환하고 그 그룹이 내 그룹 목록에 나타난다")
    void 초대를_수락하면_내_그룹이_된다() throws Exception {
        String ownerToken = signupAndLogin("owner@sqisoft.com", "김본승");
        String inviteeToken = signupAndLogin("invitee@sqisoft.com", "박초대");
        long groupId = createGroup(ownerToken, "개발팀");
        long membershipId = invite(ownerToken, groupId, "invitee@sqisoft.com");

        mockMvc.perform(post("/api/invitations/{id}/accept", membershipId)
                        .header("Authorization", bearer(inviteeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(get("/api/groups").header("Authorization", bearer(inviteeToken)))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value((int) groupId))
                .andExpect(jsonPath("$[0].role").value("MEMBER"));
    }

    @Test
    @DisplayName("남이 받은 초대를 수락하면 403과 NOT_INVITEE를 반환한다")
    void 남의_초대_수락은_403이다() throws Exception {
        String ownerToken = signupAndLogin("owner@sqisoft.com", "김본승");
        signupAndLogin("invitee@sqisoft.com", "박초대");
        String strangerToken = signupAndLogin("stranger@sqisoft.com", "최무관");
        long groupId = createGroup(ownerToken, "개발팀");
        long membershipId = invite(ownerToken, groupId, "invitee@sqisoft.com");

        mockMvc.perform(post("/api/invitations/{id}/accept", membershipId)
                        .header("Authorization", bearer(strangerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_INVITEE"));
    }

    @Test
    @DisplayName("이미 수락한 초대를 또 수락하면 409와 INVITATION_ALREADY_RESPONDED를 반환한다")
    void 중복_수락은_409다() throws Exception {
        String ownerToken = signupAndLogin("owner@sqisoft.com", "김본승");
        String inviteeToken = signupAndLogin("invitee@sqisoft.com", "박초대");
        long groupId = createGroup(ownerToken, "개발팀");
        long membershipId = invite(ownerToken, groupId, "invitee@sqisoft.com");
        accept(inviteeToken, membershipId);

        mockMvc.perform(post("/api/invitations/{id}/accept", membershipId)
                        .header("Authorization", bearer(inviteeToken)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVITATION_ALREADY_RESPONDED"));
    }

    @Test
    @DisplayName("초대를 거절하면 200과 REJECTED를 반환하고 그룹 목록에 나타나지 않는다")
    void 초대를_거절하면_그룹에_들어가지_않는다() throws Exception {
        String ownerToken = signupAndLogin("owner@sqisoft.com", "김본승");
        String inviteeToken = signupAndLogin("invitee@sqisoft.com", "박초대");
        long groupId = createGroup(ownerToken, "개발팀");
        long membershipId = invite(ownerToken, groupId, "invitee@sqisoft.com");

        mockMvc.perform(post("/api/invitations/{id}/reject", membershipId)
                        .header("Authorization", bearer(inviteeToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/api/groups").header("Authorization", bearer(inviteeToken)))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("존재하지 않는 초대에 응답하면 404와 INVITATION_NOT_FOUND를 반환한다")
    void 없는_초대_응답은_404다() throws Exception {
        String token = signupAndLogin("owner@sqisoft.com", "김본승");

        mockMvc.perform(post("/api/invitations/{id}/accept", 99999L)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVITATION_NOT_FOUND"));
    }

    // ── 그룹원 명단 ──

    @Test
    @DisplayName("승인된 그룹원은 대기 중인 사람까지 포함한 명단을 본다")
    void 승인된_그룹원은_명단을_본다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();
        signupAndLogin("pending@sqisoft.com", "정대기");
        invite(fixture.ownerToken(), fixture.groupId(), "pending@sqisoft.com");

        mockMvc.perform(get("/api/groups/{groupId}/members", fixture.groupId())
                        .header("Authorization", bearer(fixture.memberToken())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].name").value("김본승"))
                .andExpect(jsonPath("$[0].role").value("OWNER"))
                .andExpect(jsonPath("$[0].status").value("APPROVED"))
                .andExpect(jsonPath("$[1].name").value("박그룹"))
                .andExpect(jsonPath("$[1].status").value("APPROVED"))
                .andExpect(jsonPath("$[2].name").value("정대기"))
                .andExpect(jsonPath("$[2].status").value("PENDING"));
    }

    @Test
    @DisplayName("그룹과 무관한 사람은 명단을 볼 수 없고 403과 NOT_GROUP_MEMBER를 받는다")
    void 무관한_사람의_명단_조회는_403이다() throws Exception {
        Fixture fixture = prepareGroupWithRoom();
        String strangerToken = signupAndLogin("stranger@sqisoft.com", "최무관");

        mockMvc.perform(get("/api/groups/{groupId}/members", fixture.groupId())
                        .header("Authorization", bearer(strangerToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("NOT_GROUP_MEMBER"));
    }

    @Test
    @DisplayName("존재하지 않는 그룹의 명단을 조회하면 404와 GROUP_NOT_FOUND를 반환한다")
    void 없는_그룹_명단_조회는_404다() throws Exception {
        String token = signupAndLogin("owner@sqisoft.com", "김본승");

        mockMvc.perform(get("/api/groups/{groupId}/members", 99999L)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GROUP_NOT_FOUND"));
    }
}
