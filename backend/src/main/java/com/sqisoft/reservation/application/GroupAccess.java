package com.sqisoft.reservation.application;

import com.sqisoft.reservation.domain.AppException;
import com.sqisoft.reservation.domain.Group;
import com.sqisoft.reservation.domain.GroupErrorCode;
import com.sqisoft.reservation.domain.GroupMembership;

/**
 * 그룹 권한 확인 한 곳. 회의실 추가와 예약도 같은 규칙을 써야 하므로
 * {@link GroupService} 안에 두지 않고 따로 뽑아 세 서비스가 공유한다.
 */
public class GroupAccess {

    private final GroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;

    public GroupAccess(GroupRepository groupRepository, GroupMembershipRepository membershipRepository) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
    }

    public Group requireGroup(Long groupId) {
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new AppException(GroupErrorCode.GROUP_NOT_FOUND));
    }

    /** 그룹 부재를 먼저 알린다 — 초대 대기 중인 사람에게 "그룹이 없다"고 답하면 혼란스럽다. */
    public GroupMembership requireApprovedMember(Long groupId, Long memberId) {
        requireGroup(groupId);
        return membershipRepository.findByGroupIdAndMemberId(groupId, memberId)
                .filter(GroupMembership::isApproved)
                .orElseThrow(() -> new AppException(GroupErrorCode.NOT_GROUP_MEMBER));
    }

    public GroupMembership requireOwner(Long groupId, Long memberId) {
        requireGroup(groupId);
        return membershipRepository.findByGroupIdAndMemberId(groupId, memberId)
                .filter(membership -> membership.isApproved() && membership.isOwner())
                .orElseThrow(() -> new AppException(GroupErrorCode.NOT_GROUP_OWNER));
    }
}
