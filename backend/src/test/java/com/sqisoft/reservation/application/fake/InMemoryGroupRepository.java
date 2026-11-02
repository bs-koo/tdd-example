package com.sqisoft.reservation.application.fake;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import com.sqisoft.reservation.application.GroupRepository;
import com.sqisoft.reservation.domain.Group;

/**
 * 테스트 전용 인메모리 {@link GroupRepository} Fake 구현. 프로덕션 코드가 아니다.
 */
public class InMemoryGroupRepository implements GroupRepository {

    private final List<Group> groups = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public Group save(Group group) {
        Group stored = group.id() == null
                ? Group.restore(sequence.incrementAndGet(), group.name(), group.ownerId(), group.createdAt())
                : group;
        groups.removeIf(saved -> saved.id().equals(stored.id()));
        groups.add(stored);
        return stored;
    }

    @Override
    public Optional<Group> findById(Long id) {
        return groups.stream().filter(group -> group.id().equals(id)).findFirst();
    }

    @Override
    public List<Group> findAllByIdIn(List<Long> ids) {
        return groups.stream()
                .filter(group -> ids.contains(group.id()))
                .sorted((a, b) -> Long.compare(a.id(), b.id()))
                .toList();
    }
}
