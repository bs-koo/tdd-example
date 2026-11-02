package com.sqisoft.reservation.api.support;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;
import com.sqisoft.reservation.support.FixedClockConfig;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

/**
 * 모든 MockMvc 통합 테스트의 부모.
 * 개별 클래스에 @SpringBootTest 를 직접 붙이지 않는다 — 컨텍스트 캐시를 1개로 유지한다 (C-3).
 * ObjectMapper 는 주입하지 않는다 — 요청 바디는 텍스트 블록으로 작성한다 (§0.7, Jackson 3 회피).
 * 응답에서 값 하나를 꺼낼 때는 JsonPath 를 직접 쓴다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(FixedClockConfig.class)
@Transactional
public abstract class ApiTestSupport {

    protected static final String PASSWORD = "password123";

    @Autowired
    protected MockMvc mockMvc;

    @PersistenceContext
    protected EntityManager em;

    /** 가입 후 곧바로 로그인해 Bearer 토큰을 돌려준다. */
    protected String signupAndLogin(String email, String name) throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s","name":"%s"}
                        """.formatted(email, PASSWORD, name)));
        return login(email);
    }

    protected String login(String email) throws Exception {
        String body = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"%s"}
                                """.formatted(email, PASSWORD)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(body, "$.token");
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    /** 그룹을 만들고 groupId 를 돌려준다. */
    protected long createGroup(String token, String name) throws Exception {
        String body = mockMvc.perform(post("/api/groups")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s"}
                                """.formatted(name)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /** 그룹에 회의실을 추가하고 roomId 를 돌려준다. */
    protected long createRoom(String token, long groupId, String name, int capacity, String location)
            throws Exception {
        String body = mockMvc.perform(post("/api/groups/{groupId}/rooms", groupId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","capacity":%d,"location":"%s"}
                                """.formatted(name, capacity, location)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /** 초대를 보내고 membershipId 를 돌려준다. */
    protected long invite(String ownerToken, long groupId, String email) throws Exception {
        String body = mockMvc.perform(post("/api/groups/{groupId}/invitations", groupId)
                        .header("Authorization", bearer(ownerToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.membershipId")).longValue();
    }

    protected void accept(String inviteeToken, long membershipId) throws Exception {
        mockMvc.perform(post("/api/invitations/{id}/accept", membershipId)
                .header("Authorization", bearer(inviteeToken)));
    }

    /** 예약을 만들고 reservationId 를 돌려준다. */
    protected long createReservation(String token, long roomId, String purpose,
            String startAt, String endAt) throws Exception {
        String body = mockMvc.perform(post("/api/reservations")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"roomId":%d,"purpose":"%s","startAt":"%s","endAt":"%s"}
                                """.formatted(roomId, purpose, startAt, endAt)))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    /**
     * 그룹장 1명 + 승인된 그룹원 1명 + 회의실 1개가 준비된 상태를 만든다.
     * 예약 관련 테스트 대부분이 이 상태에서 시작한다.
     */
    protected Fixture prepareGroupWithRoom() throws Exception {
        String ownerToken = signupAndLogin("owner@sqisoft.com", "김본승");
        String memberToken = signupAndLogin("member@sqisoft.com", "박그룹");
        long groupId = createGroup(ownerToken, "개발팀");
        long membershipId = invite(ownerToken, groupId, "member@sqisoft.com");
        accept(memberToken, membershipId);
        long roomId = createRoom(ownerToken, groupId, "대회의실", 20, "3층");
        return new Fixture(ownerToken, memberToken, groupId, roomId);
    }

    protected record Fixture(String ownerToken, String memberToken, long groupId, long roomId) {
    }
}
