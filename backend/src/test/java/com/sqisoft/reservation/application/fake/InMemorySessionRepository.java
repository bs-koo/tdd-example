package com.sqisoft.reservation.application.fake;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.sqisoft.reservation.application.SessionRepository;
import com.sqisoft.reservation.domain.Session;

/**
 * 테스트 전용 인메모리 {@link SessionRepository} Fake 구현. 프로덕션 코드가 아니다.
 */
public class InMemorySessionRepository implements SessionRepository {

    private final List<Session> sessions = new ArrayList<>();

    @Override
    public Session save(Session session) {
        sessions.removeIf(saved -> saved.token().equals(session.token()));
        sessions.add(session);
        return session;
    }

    @Override
    public Optional<Session> findByToken(String token) {
        return sessions.stream()
                .filter(session -> session.token().equals(token))
                .findFirst();
    }

    @Override
    public void deleteByToken(String token) {
        sessions.removeIf(session -> session.token().equals(token));
    }
}
