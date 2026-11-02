package com.sqisoft.reservation.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.sqisoft.reservation.application.MemberRepository;
import com.sqisoft.reservation.domain.Member;

@Repository
public class JpaMemberRepository implements MemberRepository {

    private final MemberJpaRepository jpa;

    public JpaMemberRepository(MemberJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Member save(Member member) {
        return jpa.save(member);
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        return email == null ? Optional.empty() : jpa.findByEmail(email);
    }

    @Override
    public Optional<Member> findById(Long id) {
        return id == null ? Optional.empty() : jpa.findById(id);
    }

    @Override
    public boolean existsByEmail(String email) {
        return email != null && jpa.existsByEmail(email);
    }

    @Override
    public List<Member> findAllByIdIn(List<Long> ids) {
        return ids.isEmpty() ? List.of() : jpa.findAllByIdIn(ids);
    }
}
