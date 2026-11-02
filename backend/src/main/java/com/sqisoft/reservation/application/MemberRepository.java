package com.sqisoft.reservation.application;

import java.util.List;
import java.util.Optional;

import com.sqisoft.reservation.domain.Member;

public interface MemberRepository {

    Member save(Member member);

    Optional<Member> findByEmail(String email);

    Optional<Member> findById(Long id);

    boolean existsByEmail(String email);

    List<Member> findAllByIdIn(List<Long> ids);
}
