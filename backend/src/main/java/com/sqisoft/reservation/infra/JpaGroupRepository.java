package com.sqisoft.reservation.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.sqisoft.reservation.application.GroupRepository;
import com.sqisoft.reservation.domain.Group;

@Repository
public class JpaGroupRepository implements GroupRepository {

    private final GroupJpaRepository jpa;

    public JpaGroupRepository(GroupJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Group save(Group group) {
        return jpa.save(group);
    }

    @Override
    public Optional<Group> findById(Long id) {
        return id == null ? Optional.empty() : jpa.findById(id);
    }

    @Override
    public List<Group> findAllByIdIn(List<Long> ids) {
        return ids.isEmpty() ? List.of() : jpa.findAllByIdInOrderByIdAsc(ids);
    }
}
