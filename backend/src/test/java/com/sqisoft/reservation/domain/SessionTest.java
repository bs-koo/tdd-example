package com.sqisoft.reservation.domain;

import java.time.Duration;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTest {

    private static final LocalDateTime ISSUED_AT = LocalDateTime.of(2026, 8, 25, 9, 0);
    private static final Duration TTL = Duration.ofHours(12);

    @Test
    @DisplayName("발급한 세션의 만료 시각은 발급 시각에 TTL을 더한 값이다")
    void 만료_시각은_발급_시각에_TTL을_더한_값이다() {
        Session session = Session.issue("token-1", 1L, ISSUED_AT, TTL);

        assertThat(session.expiresAt()).isEqualTo(LocalDateTime.of(2026, 8, 25, 21, 0));
    }

    @Test
    @DisplayName("만료 시각 1분 전에는 세션이 유효하다")
    void 만료_직전에는_유효하다() {
        Session session = Session.issue("token-1", 1L, ISSUED_AT, TTL);

        assertThat(session.isValidAt(LocalDateTime.of(2026, 8, 25, 20, 59))).isTrue();
    }

    @Test
    @DisplayName("만료 시각 정각에는 세션이 무효하다")
    void 만료_정각에는_무효하다() {
        Session session = Session.issue("token-1", 1L, ISSUED_AT, TTL);

        assertThat(session.isValidAt(LocalDateTime.of(2026, 8, 25, 21, 0))).isFalse();
    }

    @Test
    @DisplayName("만료 시각을 지나면 세션이 무효하다")
    void 만료_이후에는_무효하다() {
        Session session = Session.issue("token-1", 1L, ISSUED_AT, TTL);

        assertThat(session.isValidAt(LocalDateTime.of(2026, 8, 25, 21, 1))).isFalse();
    }

    @Test
    @DisplayName("발급한 세션은 토큰과 회원 id를 그대로 보관한다")
    void 토큰과_회원_id를_보관한다() {
        Session session = Session.issue("token-1", 42L, ISSUED_AT, TTL);

        assertThat(session.token()).isEqualTo("token-1");
        assertThat(session.memberId()).isEqualTo(42L);
    }
}
