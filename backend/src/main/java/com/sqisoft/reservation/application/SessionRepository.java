package com.sqisoft.reservation.application;

import java.util.Optional;

import com.sqisoft.reservation.domain.Session;

public interface SessionRepository {

    Session save(Session session);

    Optional<Session> findByToken(String token);

    void deleteByToken(String token);
}
