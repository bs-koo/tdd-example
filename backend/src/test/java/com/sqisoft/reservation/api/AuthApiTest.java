package com.sqisoft.reservation.api;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.sqisoft.reservation.api.support.ApiTestSupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthApiTest extends ApiTestSupport {

    // ── 가입 ──

    @Test
    @DisplayName("가입에 성공하면 201과 회원 정보를 반환하고 비밀번호는 응답에 포함하지 않는다")
    void 가입에_성공하면_201과_회원_정보를_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bonseung@sqisoft.com","password":"password123","name":"김본승"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("bonseung@sqisoft.com"))
                .andExpect(jsonPath("$.name").value("김본승"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());
    }

    @Test
    @DisplayName("이메일 형식이 잘못되면 400과 INVALID_EMAIL_FORMAT을 반환한다")
    void 이메일_형식이_잘못되면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"password123","name":"김본승"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_EMAIL_FORMAT"));
    }

    @Test
    @DisplayName("비밀번호가 8자 미만이면 400과 INVALID_PASSWORD_LENGTH를 반환한다")
    void 비밀번호가_짧으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bonseung@sqisoft.com","password":"1234567","name":"김본승"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PASSWORD_LENGTH"));
    }

    @Test
    @DisplayName("이름이 비어 있으면 400과 INVALID_MEMBER_NAME을 반환한다")
    void 이름이_비어_있으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bonseung@sqisoft.com","password":"password123","name":"  "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_MEMBER_NAME"));
    }

    @Test
    @DisplayName("이미 가입된 이메일로 가입하면 409와 EMAIL_ALREADY_REGISTERED를 반환한다")
    void 중복_이메일_가입은_409를_반환한다() throws Exception {
        signupAndLogin("bonseung@sqisoft.com", "김본승");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bonseung@sqisoft.com","password":"password123","name":"다른사람"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("EMAIL_ALREADY_REGISTERED"));
    }

    // ── 로그인 ──

    @Test
    @DisplayName("로그인에 성공하면 200과 토큰·회원 정보를 반환한다")
    void 로그인에_성공하면_토큰을_반환한다() throws Exception {
        signupAndLogin("bonseung@sqisoft.com", "김본승");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bonseung@sqisoft.com","password":"password123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.member.email").value("bonseung@sqisoft.com"))
                .andExpect(jsonPath("$.member.name").value("김본승"));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 401과 INVALID_CREDENTIALS를 반환한다")
    void 비밀번호가_틀리면_401을_반환한다() throws Exception {
        signupAndLogin("bonseung@sqisoft.com", "김본승");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bonseung@sqisoft.com","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @DisplayName("가입하지 않은 이메일로 로그인해도 401과 INVALID_CREDENTIALS를 반환한다")
    void 미가입_이메일_로그인은_같은_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nobody@sqisoft.com","password":"password123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_CREDENTIALS"));
    }

    // ── 내 정보 ──

    @Test
    @DisplayName("Bearer 토큰으로 내 정보를 조회하면 200과 회원 정보를 반환한다")
    void 토큰으로_내_정보를_조회한다() throws Exception {
        String token = signupAndLogin("bonseung@sqisoft.com", "김본승");

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("bonseung@sqisoft.com"))
                .andExpect(jsonPath("$.name").value("김본승"));
    }

    @Test
    @DisplayName("Authorization 헤더가 없으면 401과 UNAUTHENTICATED를 반환한다")
    void 헤더가_없으면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("Bearer 접두어가 없는 헤더는 401과 UNAUTHENTICATED를 반환한다")
    void Bearer_접두어가_없으면_401을_반환한다() throws Exception {
        String token = signupAndLogin("bonseung@sqisoft.com", "김본승");

        mockMvc.perform(get("/api/auth/me").header("Authorization", token))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("발급된 적 없는 토큰은 401과 UNAUTHENTICATED를 반환한다")
    void 없는_토큰은_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer("made-up-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    // ── 로그아웃 ──

    @Test
    @DisplayName("로그아웃하면 204를 반환하고 그 토큰은 더 이상 쓸 수 없다")
    void 로그아웃한_토큰은_쓸_수_없다() throws Exception {
        String token = signupAndLogin("bonseung@sqisoft.com", "김본승");

        mockMvc.perform(post("/api/auth/logout").header("Authorization", bearer(token)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    @DisplayName("토큰 없이 로그아웃하면 401과 UNAUTHENTICATED를 반환한다")
    void 토큰_없는_로그아웃은_401이다() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }
}
