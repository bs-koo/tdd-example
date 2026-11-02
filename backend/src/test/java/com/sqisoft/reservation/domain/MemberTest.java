package com.sqisoft.reservation.domain;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemberTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 25, 9, 0);

    @Test
    @DisplayName("등록한 회원의 이메일은 소문자로 정규화된다")
    void 이메일은_소문자로_정규화된다() {
        Member member = Member.register("BonSeung@SQIsoft.COM", "김본승", "hashed", CREATED_AT);

        assertThat(member.email()).isEqualTo("bonseung@sqisoft.com");
    }

    @Test
    @DisplayName("등록한 회원의 이메일은 앞뒤 공백이 제거된다")
    void 이메일_앞뒤_공백이_제거된다() {
        Member member = Member.register("  bonseung@sqisoft.com  ", "김본승", "hashed", CREATED_AT);

        assertThat(member.email()).isEqualTo("bonseung@sqisoft.com");
    }

    @Test
    @DisplayName("등록한 회원의 이름은 앞뒤 공백이 제거된다")
    void 이름_앞뒤_공백이_제거된다() {
        Member member = Member.register("bonseung@sqisoft.com", "  김본승  ", "hashed", CREATED_AT);

        assertThat(member.name()).isEqualTo("김본승");
    }

    @Test
    @DisplayName("등록한 회원은 넘겨받은 비밀번호 해시를 그대로 보관한다")
    void 비밀번호_해시를_그대로_보관한다() {
        Member member = Member.register("bonseung@sqisoft.com", "김본승", "hashed-value", CREATED_AT);

        assertThat(member.passwordHash()).isEqualTo("hashed-value");
    }

    @Test
    @DisplayName("등록한 회원은 넘겨받은 생성 시각을 보관한다")
    void 생성_시각을_보관한다() {
        Member member = Member.register("bonseung@sqisoft.com", "김본승", "hashed", CREATED_AT);

        assertThat(member.createdAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("저장 전 회원의 id는 null이다")
    void 저장_전_id는_null이다() {
        Member member = Member.register("bonseung@sqisoft.com", "김본승", "hashed", CREATED_AT);

        assertThat(member.id()).isNull();
    }

    @Test
    @DisplayName("normalizeEmail은 대소문자와 앞뒤 공백을 없애 조회 키를 하나로 만든다")
    void normalizeEmail은_조회_키를_하나로_만든다() {
        assertThat(Member.normalizeEmail("  BonSeung@SQIsoft.COM ")).isEqualTo("bonseung@sqisoft.com");
    }

    @Test
    @DisplayName("normalizeEmail은 null을 null로 돌려준다")
    void normalizeEmail은_null을_그대로_돌려준다() {
        assertThat(Member.normalizeEmail(null)).isNull();
    }
}
