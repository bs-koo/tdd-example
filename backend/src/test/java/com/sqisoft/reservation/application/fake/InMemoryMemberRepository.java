package com.sqisoft.reservation.application.fake;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import com.sqisoft.reservation.application.MemberRepository;
import com.sqisoft.reservation.domain.Member;

/**
 * 테스트 전용 인메모리 {@link MemberRepository} Fake 구현. 프로덕션 코드가 아니다.
 * 저장 시 id를 채운 새 인스턴스를 돌려주어 JPA IDENTITY 전략과 같은 관찰 가능한 동작을 흉내 낸다.
 */
public class InMemoryMemberRepository implements MemberRepository {

    private final List<Member> members = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong();

    @Override
    public Member save(Member member) {
        Member stored = member.id() == null
                ? Member.restore(sequence.incrementAndGet(), member.email(), member.name(),
                        member.passwordHash(), member.createdAt())
                : member;
        members.removeIf(saved -> saved.id().equals(stored.id()));
        members.add(stored);
        return stored;
    }

    @Override
    public Optional<Member> findByEmail(String email) {
        String normalized = Member.normalizeEmail(email);
        return members.stream()
                .filter(member -> member.email().equals(normalized))
                .findFirst();
    }

    @Override
    public Optional<Member> findById(Long id) {
        return members.stream()
                .filter(member -> member.id().equals(id))
                .findFirst();
    }

    @Override
    public boolean existsByEmail(String email) {
        return findByEmail(email).isPresent();
    }

    @Override
    public List<Member> findAllByIdIn(List<Long> ids) {
        return members.stream()
                .filter(member -> ids.contains(member.id()))
                .toList();
    }
}
