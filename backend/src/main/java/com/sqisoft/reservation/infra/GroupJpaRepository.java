package com.sqisoft.reservation.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sqisoft.reservation.domain.Group;

interface GroupJpaRepository extends JpaRepository<Group, Long> {

    List<Group> findAllByIdInOrderByIdAsc(List<Long> ids);
}
