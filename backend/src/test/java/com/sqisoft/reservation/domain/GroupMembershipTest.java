package com.sqisoft.reservation.domain;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroupMembershipTest {

    private static final Long GROUP_ID = 1L;
    private static final Long OWNER_ID = 10L;
    private static final Long INVITEE_ID = 20L;
    private static final Long STRANGER_ID = 30L;
    private static final LocalDateTime INVITED_AT = LocalDateTime.of(2026, 8, 25, 9, 0);
    private static final LocalDateTime RESPONDED_AT = LocalDateTime.of(2026, 8, 25, 10, 0);

    @Test
    @DisplayName("그룹장 멤버십은 OWNER 역할로 즉시 APPROVED 상태다")
    void 그룹장은_즉시_승인_상태다() {
        GroupMembership membership = GroupMembership.owner(GROUP_ID, OWNER_ID, INVITED_AT);

        assertThat(membership.role()).isEqualTo(GroupRole.OWNER);
        assertThat(membership.status()).isEqualTo(MembershipStatus.APPROVED);
        assertThat(membership.isApproved()).isTrue();
        assertThat(membership.isOwner()).isTrue();
    }

    @Test
    @DisplayName("초대한 멤버십은 MEMBER 역할로 PENDING 상태에서 시작한다")
    void 초대는_대기_상태로_시작한다() {
        GroupMembership membership = GroupMembership.invite(GROUP_ID, INVITEE_ID, INVITED_AT);

        assertThat(membership.role()).isEqualTo(GroupRole.MEMBER);
        assertThat(membership.status()).isEqualTo(MembershipStatus.PENDING);
        assertThat(membership.isApproved()).isFalse();
        assertThat(membership.isOwner()).isFalse();
    }

    @Test
    @DisplayName("초대를 수락하면 APPROVED가 되고 응답 시각이 기록된다")
    void 수락하면_승인된다() {
        GroupMembership membership = GroupMembership.invite(GROUP_ID, INVITEE_ID, INVITED_AT);

        membership.accept(INVITEE_ID, RESPONDED_AT);

        assertThat(membership.status()).isEqualTo(MembershipStatus.APPROVED);
        assertThat(membership.isApproved()).isTrue();
        assertThat(membership.respondedAt()).isEqualTo(RESPONDED_AT);
    }

    @Test
    @DisplayName("초대를 거절하면 REJECTED가 되고 승인 상태가 아니다")
    void 거절하면_거절_상태가_된다() {
        GroupMembership membership = GroupMembership.invite(GROUP_ID, INVITEE_ID, INVITED_AT);

        membership.reject(INVITEE_ID, RESPONDED_AT);

        assertThat(membership.status()).isEqualTo(MembershipStatus.REJECTED);
        assertThat(membership.isApproved()).isFalse();
    }

    @Test
    @DisplayName("초대받은 본인이 아니면 수락할 수 없다")
    void 남의_초대는_수락할_수_없다() {
        GroupMembership membership = GroupMembership.invite(GROUP_ID, INVITEE_ID, INVITED_AT);

        assertThatThrownBy(() -> membership.accept(STRANGER_ID, RESPONDED_AT))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.NOT_INVITEE);
    }

    @Test
    @DisplayName("초대받은 본인이 아니면 거절할 수 없다")
    void 남의_초대는_거절할_수_없다() {
        GroupMembership membership = GroupMembership.invite(GROUP_ID, INVITEE_ID, INVITED_AT);

        assertThatThrownBy(() -> membership.reject(STRANGER_ID, RESPONDED_AT))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.NOT_INVITEE);
    }

    @Test
    @DisplayName("거절 시도가 실패해도 상태는 PENDING 그대로다")
    void 실패한_거절은_상태를_바꾸지_않는다() {
        GroupMembership membership = GroupMembership.invite(GROUP_ID, INVITEE_ID, INVITED_AT);

        assertThatThrownBy(() -> membership.reject(STRANGER_ID, RESPONDED_AT))
                .isInstanceOf(AppException.class);

        assertThat(membership.status()).isEqualTo(MembershipStatus.PENDING);
    }

    @Test
    @DisplayName("이미 수락한 초대를 다시 수락하면 INVITATION_ALREADY_RESPONDED를 던진다")
    void 이미_수락한_초대는_다시_수락할_수_없다() {
        GroupMembership membership = GroupMembership.invite(GROUP_ID, INVITEE_ID, INVITED_AT);
        membership.accept(INVITEE_ID, RESPONDED_AT);

        assertThatThrownBy(() -> membership.accept(INVITEE_ID, RESPONDED_AT))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.INVITATION_ALREADY_RESPONDED);
    }

    @Test
    @DisplayName("이미 거절한 초대를 수락하면 INVITATION_ALREADY_RESPONDED를 던진다")
    void 이미_거절한_초대는_수락할_수_없다() {
        GroupMembership membership = GroupMembership.invite(GROUP_ID, INVITEE_ID, INVITED_AT);
        membership.reject(INVITEE_ID, RESPONDED_AT);

        assertThatThrownBy(() -> membership.accept(INVITEE_ID, RESPONDED_AT))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.INVITATION_ALREADY_RESPONDED);
    }

    @Test
    @DisplayName("그룹장 멤버십은 이미 APPROVED이므로 수락 대상이 아니다")
    void 그룹장_멤버십은_수락_대상이_아니다() {
        GroupMembership membership = GroupMembership.owner(GROUP_ID, OWNER_ID, INVITED_AT);

        assertThatThrownBy(() -> membership.accept(OWNER_ID, RESPONDED_AT))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.INVITATION_ALREADY_RESPONDED);
    }

    @Test
    @DisplayName("거절했던 멤버십을 다시 초대하면 PENDING으로 되돌아간다")
    void 거절한_사람을_다시_초대할_수_있다() {
        GroupMembership membership = GroupMembership.invite(GROUP_ID, INVITEE_ID, INVITED_AT);
        membership.reject(INVITEE_ID, RESPONDED_AT);

        membership.reinvite(LocalDateTime.of(2026, 8, 26, 9, 0));

        assertThat(membership.status()).isEqualTo(MembershipStatus.PENDING);
        assertThat(membership.invitedAt()).isEqualTo(LocalDateTime.of(2026, 8, 26, 9, 0));
        assertThat(membership.respondedAt()).isNull();
    }

    @Test
    @DisplayName("재초대한 멤버십은 다시 수락할 수 있다")
    void 재초대한_멤버십은_수락할_수_있다() {
        GroupMembership membership = GroupMembership.invite(GROUP_ID, INVITEE_ID, INVITED_AT);
        membership.reject(INVITEE_ID, RESPONDED_AT);
        membership.reinvite(LocalDateTime.of(2026, 8, 26, 9, 0));

        membership.accept(INVITEE_ID, LocalDateTime.of(2026, 8, 26, 10, 0));

        assertThat(membership.isApproved()).isTrue();
    }

    @Test
    @DisplayName("MembershipStatus는 PENDING·APPROVED·REJECTED 3종이다")
    void 멤버십_상태는_3종이다() {
        assertThat(MembershipStatus.values()).containsExactlyInAnyOrder(
                MembershipStatus.PENDING, MembershipStatus.APPROVED, MembershipStatus.REJECTED);
    }

    @Test
    @DisplayName("GroupRole은 OWNER·MEMBER 2종이다")
    void 그룹_역할은_2종이다() {
        assertThat(GroupRole.values()).containsExactlyInAnyOrder(GroupRole.OWNER, GroupRole.MEMBER);
    }
}
