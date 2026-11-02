package com.sqisoft.reservation.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * "누가 어느 그룹에 어떤 자격으로 속해 있는가". 초대와 승인의 상태를 함께 들고 있으므로
 * 초대장과 멤버십이 따로 있지 않다 — 초대는 PENDING 상태의 멤버십이다.
 */
@Entity
@Table(name = "group_membership")
public class GroupMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long groupId;

    @Column(nullable = false)
    private Long memberId;

    @Enumerated(EnumType.STRING)
    private GroupRole role;

    @Enumerated(EnumType.STRING)
    private MembershipStatus status;

    private LocalDateTime invitedAt;

    private LocalDateTime respondedAt;

    protected GroupMembership() {
    }

    private GroupMembership(Long id, Long groupId, Long memberId, GroupRole role,
            MembershipStatus status, LocalDateTime invitedAt, LocalDateTime respondedAt) {
        this.id = id;
        this.groupId = groupId;
        this.memberId = memberId;
        this.role = role;
        this.status = status;
        this.invitedAt = invitedAt;
        this.respondedAt = respondedAt;
    }

    /** 그룹을 만든 사람. 승인 절차 없이 곧바로 참여 상태다. */
    public static GroupMembership owner(Long groupId, Long memberId, LocalDateTime at) {
        return new GroupMembership(null, groupId, memberId, GroupRole.OWNER,
                MembershipStatus.APPROVED, at, at);
    }

    public static GroupMembership invite(Long groupId, Long memberId, LocalDateTime at) {
        return new GroupMembership(null, groupId, memberId, GroupRole.MEMBER,
                MembershipStatus.PENDING, at, null);
    }

    public static GroupMembership restore(Long id, Long groupId, Long memberId, GroupRole role,
            MembershipStatus status, LocalDateTime invitedAt, LocalDateTime respondedAt) {
        return new GroupMembership(id, groupId, memberId, role, status, invitedAt, respondedAt);
    }

    public void accept(Long requesterId, LocalDateTime at) {
        requireInvitee(requesterId);
        requirePending();
        this.status = MembershipStatus.APPROVED;
        this.respondedAt = at;
    }

    public void reject(Long requesterId, LocalDateTime at) {
        requireInvitee(requesterId);
        requirePending();
        this.status = MembershipStatus.REJECTED;
        this.respondedAt = at;
    }

    /** 거절했던 사람을 다시 부른다. 멤버십을 새로 만들지 않으므로 이력이 한 줄로 유지된다. */
    public void reinvite(LocalDateTime at) {
        this.status = MembershipStatus.PENDING;
        this.invitedAt = at;
        this.respondedAt = null;
    }

    private void requireInvitee(Long requesterId) {
        if (!this.memberId.equals(requesterId)) {
            throw new AppException(GroupErrorCode.NOT_INVITEE);
        }
    }

    private void requirePending() {
        if (this.status != MembershipStatus.PENDING) {
            throw new AppException(GroupErrorCode.INVITATION_ALREADY_RESPONDED);
        }
    }

    public Long id() {
        return id;
    }

    public Long groupId() {
        return groupId;
    }

    public Long memberId() {
        return memberId;
    }

    public GroupRole role() {
        return role;
    }

    public MembershipStatus status() {
        return status;
    }

    public LocalDateTime invitedAt() {
        return invitedAt;
    }

    public LocalDateTime respondedAt() {
        return respondedAt;
    }

    public boolean isApproved() {
        return status == MembershipStatus.APPROVED;
    }

    public boolean isOwner() {
        return role == GroupRole.OWNER;
    }

    public boolean isPending() {
        return status == MembershipStatus.PENDING;
    }

    public boolean isRejected() {
        return status == MembershipStatus.REJECTED;
    }
}
