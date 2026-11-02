package com.sqisoft.reservation.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.sqisoft.reservation.application.GroupMembershipRepository;
import com.sqisoft.reservation.domain.GroupMembership;

@Repository
public class JpaGroupMembershipRepository implements GroupMembershipRepository {

    private final GroupMembershipJpaRepository jpa;

    public JpaGroupMembershipRepository(GroupMembershipJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public GroupMembership save(GroupMembership membership) {
        return jpa.save(membership);
    }

    @Override
    public Optional<GroupMembership> findById(Long id) {
        return id == null ? Optional.empty() : jpa.findById(id);
    }

    @Override
    public Optional<GroupMembership> findByGroupIdAndMemberId(Long groupId, Long memberId) {
        return jpa.findByGroupIdAndMemberId(groupId, memberId);
    }

    @Override
    public List<GroupMembership> findAllByGroupId(Long groupId) {
        return jpa.findAllByGroupIdOrderByIdAsc(groupId);
    }

    @Override
    public List<GroupMembership> findAllByMemberId(Long memberId) {
        return jpa.findAllByMemberIdOrderByIdAsc(memberId);
    }
}
