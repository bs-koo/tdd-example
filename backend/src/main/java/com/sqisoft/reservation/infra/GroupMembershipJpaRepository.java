package com.sqisoft.reservation.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sqisoft.reservation.domain.GroupMembership;

interface GroupMembershipJpaRepository extends JpaRepository<GroupMembership, Long> {

    Optional<GroupMembership> findByGroupIdAndMemberId(Long groupId, Long memberId);

    List<GroupMembership> findAllByGroupIdOrderByIdAsc(Long groupId);

    List<GroupMembership> findAllByMemberIdOrderByIdAsc(Long memberId);
}
