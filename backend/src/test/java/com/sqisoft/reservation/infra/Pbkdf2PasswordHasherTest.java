package com.sqisoft.reservation.infra;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sqisoft.reservation.application.PasswordHasher;

import static org.assertj.core.api.Assertions.assertThat;

class Pbkdf2PasswordHasherTest {

    private final PasswordHasher hasher = new Pbkdf2PasswordHasher();

    @Test
    @DisplayName("해시한 비밀번호는 같은 원문으로 검증하면 일치한다")
    void 같은_원문은_일치한다() {
        String hash = hasher.hash("password123");

        assertThat(hasher.matches("password123", hash)).isTrue();
    }

    @Test
    @DisplayName("해시한 비밀번호는 다른 원문으로 검증하면 불일치한다")
    void 다른_원문은_불일치한다() {
        String hash = hasher.hash("password123");

        assertThat(hasher.matches("password124", hash)).isFalse();
    }

    @Test
    @DisplayName("같은 비밀번호라도 해시할 때마다 다른 문자열이 나온다 — 솔트가 매번 새로 생성된다")
    void 같은_비밀번호도_해시는_매번_다르다() {
        String first = hasher.hash("password123");
        String second = hasher.hash("password123");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("솔트가 달라도 각 해시는 자기 원문과 일치한다")
    void 서로_다른_해시도_각자_원문과_일치한다() {
        String first = hasher.hash("password123");
        String second = hasher.hash("password123");

        assertThat(hasher.matches("password123", first)).isTrue();
        assertThat(hasher.matches("password123", second)).isTrue();
    }

    @Test
    @DisplayName("해시 문자열에는 원문 비밀번호가 포함되지 않는다")
    void 해시에는_원문이_들어있지_않다() {
        String hash = hasher.hash("password123");

        assertThat(hash).doesNotContain("password123");
    }

    @Test
    @DisplayName("형식이 깨진 해시로 검증하면 예외 대신 false를 돌려준다")
    void 형식이_깨진_해시는_예외_없이_false다() {
        assertThat(hasher.matches("password123", "깨진-해시")).isFalse();
    }

    @Test
    @DisplayName("null 해시로 검증하면 예외 대신 false를 돌려준다")
    void null_해시는_예외_없이_false다() {
        assertThat(hasher.matches("password123", null)).isFalse();
    }

    @Test
    @DisplayName("빈 문자열 비밀번호도 해시하고 검증할 수 있다")
    void 빈_문자열도_해시하고_검증한다() {
        String hash = hasher.hash("");

        assertThat(hasher.matches("", hash)).isTrue();
        assertThat(hasher.matches("x", hash)).isFalse();
    }
}
