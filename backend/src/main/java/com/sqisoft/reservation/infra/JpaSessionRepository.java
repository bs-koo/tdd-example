package com.sqisoft.reservation.infra;

import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.sqisoft.reservation.application.SessionRepository;
import com.sqisoft.reservation.domain.Session;

@Repository
public class JpaSessionRepository implements SessionRepository {

    private final SessionJpaRepository jpa;

    public JpaSessionRepository(SessionJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Session save(Session session) {
        return jpa.save(session);
    }

    @Override
    public Optional<Session> findByToken(String token) {
        return token == null ? Optional.empty() : jpa.findById(token);
    }

    @Override
    public void deleteByToken(String token) {
        if (token != null) {
            jpa.deleteById(token);
        }
    }
}
