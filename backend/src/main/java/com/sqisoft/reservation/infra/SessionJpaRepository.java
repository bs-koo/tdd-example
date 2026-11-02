package com.sqisoft.reservation.infra;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sqisoft.reservation.domain.Session;

interface SessionJpaRepository extends JpaRepository<Session, String> {
}
