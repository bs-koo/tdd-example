package com.sqisoft.reservation.application.fake;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import com.sqisoft.reservation.application.GroupMembershipRepository;
import com.sqisoft.reservation.domain.GroupMembership;

/**
 * 테스트 전용 인메모리 {@link GroupMembershipRepository} Fake 구현. 프로덕션 코드가 아니다.
 */
public class InMemoryGroupMembershipRepository implements GroupMembershipRepository {

    private final List<GroupMembership> memberships = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public GroupMembership save(GroupMembership membership) {
        GroupMembership stored = membership.id() == null
                ? GroupMembership.restore(sequence.incrementAndGet(), membership.groupId(),
                        membership.memberId(), membership.role(), membership.status(),
                        membership.invitedAt(), membership.respondedAt())
                : membership;
        memberships.removeIf(saved -> saved.id().equals(stored.id()));
        memberships.add(stored);
        return stored;
    }

    @Override
    public Optional<GroupMembership> findById(Long id) {
        return memberships.stream().filter(m -> m.id().equals(id)).findFirst();
    }

    @Override
    public Optional<GroupMembership> findByGroupIdAndMemberId(Long groupId, Long memberId) {
        return memberships.stream()
                .filter(m -> m.groupId().equals(groupId) && m.memberId().equals(memberId))
                .findFirst();
    }

    @Override
    public List<GroupMembership> findAllByGroupId(Long groupId) {
        return memberships.stream()
                .filter(m -> m.groupId().equals(groupId))
                .sorted(Comparator.comparing(GroupMembership::id))
                .toList();
    }

    @Override
    public List<GroupMembership> findAllByMemberId(Long memberId) {
        return memberships.stream()
                .filter(m -> m.memberId().equals(memberId))
                .sorted(Comparator.comparing(GroupMembership::id))
                .toList();
    }
}
