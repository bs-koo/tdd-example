package com.sqisoft.reservation.application;

import java.util.List;
import java.util.Optional;

import com.sqisoft.reservation.domain.GroupMembership;

public interface GroupMembershipRepository {

    GroupMembership save(GroupMembership membership);

    Optional<GroupMembership> findById(Long id);

    Optional<GroupMembership> findByGroupIdAndMemberId(Long groupId, Long memberId);

    /** membershipId 오름차순. */
    List<GroupMembership> findAllByGroupId(Long groupId);

    /** membershipId 오름차순. */
    List<GroupMembership> findAllByMemberId(Long memberId);
}
