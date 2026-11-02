package com.sqisoft.reservation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemberPolicyTest {

    private static final String VALID_EMAIL = "bonseung@sqisoft.com";
    private static final String VALID_PASSWORD = "password123";
    private static final String VALID_NAME = "김본승";

    private final MemberPolicy policy = new MemberPolicy();

    @Test
    @DisplayName("이메일·비밀번호·이름이 모두 규칙에 맞으면 예외를 던지지 않는다")
    void 모두_규칙에_맞으면_통과한다() {
        assertThatCode(() -> policy.validate(VALID_EMAIL, VALID_PASSWORD, VALID_NAME))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("이메일이 null이면 INVALID_EMAIL_FORMAT을 던진다")
    void 이메일이_null이면_형식_오류다() {
        assertThatThrownBy(() -> policy.validate(null, VALID_PASSWORD, VALID_NAME))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.INVALID_EMAIL_FORMAT);
    }

    @Test
    @DisplayName("@가 없는 이메일은 INVALID_EMAIL_FORMAT을 던진다")
    void 골뱅이가_없으면_형식_오류다() {
        assertThatThrownBy(() -> policy.validate("bonseung.sqisoft.com", VALID_PASSWORD, VALID_NAME))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.INVALID_EMAIL_FORMAT);
    }

    @Test
    @DisplayName("도메인에 점이 없는 이메일은 INVALID_EMAIL_FORMAT을 던진다")
    void 도메인에_점이_없으면_형식_오류다() {
        assertThatThrownBy(() -> policy.validate("bonseung@sqisoft", VALID_PASSWORD, VALID_NAME))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.INVALID_EMAIL_FORMAT);
    }

    @Test
    @DisplayName("공백이 섞인 이메일은 INVALID_EMAIL_FORMAT을 던진다")
    void 공백이_섞이면_형식_오류다() {
        assertThatThrownBy(() -> policy.validate("bon seung@sqisoft.com", VALID_PASSWORD, VALID_NAME))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.INVALID_EMAIL_FORMAT);
    }

    @Test
    @DisplayName("이메일 앞뒤 공백은 형식 검사 전에 제거되므로 통과한다")
    void 이메일_앞뒤_공백은_허용된다() {
        assertThatCode(() -> policy.validate("  " + VALID_EMAIL + "  ", VALID_PASSWORD, VALID_NAME))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("비밀번호가 7자이면 INVALID_PASSWORD_LENGTH를 던진다")
    void 비밀번호가_7자이면_길이_오류다() {
        assertThatThrownBy(() -> policy.validate(VALID_EMAIL, "1234567", VALID_NAME))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.INVALID_PASSWORD_LENGTH);
    }

    @Test
    @DisplayName("비밀번호가 경계값 8자이면 통과한다")
    void 비밀번호가_8자이면_통과한다() {
        assertThatCode(() -> policy.validate(VALID_EMAIL, "12345678", VALID_NAME))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("비밀번호가 경계값 64자이면 통과한다")
    void 비밀번호가_64자이면_통과한다() {
        assertThatCode(() -> policy.validate(VALID_EMAIL, "a".repeat(64), VALID_NAME))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("비밀번호가 65자이면 INVALID_PASSWORD_LENGTH를 던진다")
    void 비밀번호가_65자이면_길이_오류다() {
        assertThatThrownBy(() -> policy.validate(VALID_EMAIL, "a".repeat(65), VALID_NAME))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.INVALID_PASSWORD_LENGTH);
    }

    @Test
    @DisplayName("비밀번호가 null이면 INVALID_PASSWORD_LENGTH를 던진다")
    void 비밀번호가_null이면_길이_오류다() {
        assertThatThrownBy(() -> policy.validate(VALID_EMAIL, null, VALID_NAME))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.INVALID_PASSWORD_LENGTH);
    }

    @Test
    @DisplayName("비밀번호는 앞뒤 공백을 의미 있는 문자로 취급하므로 공백만 8자여도 통과한다")
    void 비밀번호는_공백을_다듬지_않는다() {
        assertThatCode(() -> policy.validate(VALID_EMAIL, "        ", VALID_NAME))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("이름이 빈 문자열이면 INVALID_MEMBER_NAME을 던진다")
    void 이름이_빈_문자열이면_이름_오류다() {
        assertThatThrownBy(() -> policy.validate(VALID_EMAIL, VALID_PASSWORD, ""))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.INVALID_MEMBER_NAME);
    }

    @Test
    @DisplayName("이름이 공백뿐이면 INVALID_MEMBER_NAME을 던진다")
    void 이름이_공백뿐이면_이름_오류다() {
        assertThatThrownBy(() -> policy.validate(VALID_EMAIL, VALID_PASSWORD, "   "))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.INVALID_MEMBER_NAME);
    }

    @Test
    @DisplayName("이름이 21자이면 INVALID_MEMBER_NAME을 던진다")
    void 이름이_21자이면_이름_오류다() {
        assertThatThrownBy(() -> policy.validate(VALID_EMAIL, VALID_PASSWORD, "가".repeat(21)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.INVALID_MEMBER_NAME);
    }

    @Test
    @DisplayName("이름이 경계값 20자이면 통과한다")
    void 이름이_20자이면_통과한다() {
        assertThatCode(() -> policy.validate(VALID_EMAIL, VALID_PASSWORD, "가".repeat(20)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("이메일과 비밀번호가 동시에 잘못되면 이메일 오류를 먼저 던진다")
    void 이메일_오류가_비밀번호_오류보다_먼저다() {
        assertThatThrownBy(() -> policy.validate("bad-email", "short", VALID_NAME))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.INVALID_EMAIL_FORMAT);
    }

    @Test
    @DisplayName("비밀번호와 이름이 동시에 잘못되면 비밀번호 오류를 먼저 던진다")
    void 비밀번호_오류가_이름_오류보다_먼저다() {
        assertThatThrownBy(() -> policy.validate(VALID_EMAIL, "short", ""))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.INVALID_PASSWORD_LENGTH);
    }
}
