package com.sqisoft.reservation.domain;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroupTest {

    private static final LocalDateTime CREATED_AT = LocalDateTime.of(2026, 8, 25, 9, 0);
    private static final Long OWNER_ID = 1L;

    @Test
    @DisplayName("그룹을 만들면 이름·소유자·생성 시각을 그대로 보관한다")
    void 생성_인자를_그대로_보관한다() {
        Group group = Group.create("개발팀", OWNER_ID, CREATED_AT);

        assertThat(group.name()).isEqualTo("개발팀");
        assertThat(group.ownerId()).isEqualTo(OWNER_ID);
        assertThat(group.createdAt()).isEqualTo(CREATED_AT);
    }

    @Test
    @DisplayName("그룹 이름은 앞뒤 공백이 제거된다")
    void 이름_앞뒤_공백이_제거된다() {
        Group group = Group.create("  개발팀  ", OWNER_ID, CREATED_AT);

        assertThat(group.name()).isEqualTo("개발팀");
    }

    @Test
    @DisplayName("저장 전 그룹의 id는 null이다")
    void 저장_전_id는_null이다() {
        assertThat(Group.create("개발팀", OWNER_ID, CREATED_AT).id()).isNull();
    }

    @Test
    @DisplayName("그룹 이름이 null이면 INVALID_GROUP_NAME을 던진다")
    void 이름이_null이면_거절한다() {
        assertThatThrownBy(() -> Group.create(null, OWNER_ID, CREATED_AT))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.INVALID_GROUP_NAME);
    }

    @Test
    @DisplayName("그룹 이름이 공백뿐이면 INVALID_GROUP_NAME을 던진다")
    void 이름이_공백뿐이면_거절한다() {
        assertThatThrownBy(() -> Group.create("   ", OWNER_ID, CREATED_AT))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.INVALID_GROUP_NAME);
    }

    @Test
    @DisplayName("그룹 이름이 경계값 30자이면 통과한다")
    void 이름이_30자이면_통과한다() {
        assertThatCode(() -> Group.create("가".repeat(30), OWNER_ID, CREATED_AT))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("그룹 이름이 31자이면 INVALID_GROUP_NAME을 던진다")
    void 이름이_31자이면_거절한다() {
        assertThatThrownBy(() -> Group.create("가".repeat(31), OWNER_ID, CREATED_AT))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.INVALID_GROUP_NAME);
    }

    @Test
    @DisplayName("이름 길이는 공백을 제거한 뒤 잰다 — 공백으로 31자를 채워도 통과한다")
    void 길이는_공백_제거_후_잰다() {
        assertThatCode(() -> Group.create("  " + "가".repeat(29) + "  ", OWNER_ID, CREATED_AT))
                .doesNotThrowAnyException();
    }
}
