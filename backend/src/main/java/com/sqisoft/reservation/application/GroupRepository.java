package com.sqisoft.reservation.application;

import java.util.List;
import java.util.Optional;

import com.sqisoft.reservation.domain.Group;

public interface GroupRepository {

    Group save(Group group);

    Optional<Group> findById(Long id);

    List<Group> findAllByIdIn(List<Long> ids);
}
