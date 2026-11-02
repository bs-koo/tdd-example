package com.sqisoft.reservation.domain;

import java.time.Duration;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "auth_session")
public class Session {

    @Id
    private String token;

    @Column(nullable = false)
    private Long memberId;

    private LocalDateTime issuedAt;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    protected Session() {
    }

    private Session(String token, Long memberId, LocalDateTime issuedAt, LocalDateTime expiresAt) {
        this.token = token;
        this.memberId = memberId;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
    }

    public static Session issue(String token, Long memberId, LocalDateTime issuedAt, Duration ttl) {
        return new Session(token, memberId, issuedAt, issuedAt.plus(ttl));
    }

    public String token() {
        return token;
    }

    public Long memberId() {
        return memberId;
    }

    public LocalDateTime issuedAt() {
        return issuedAt;
    }

    public LocalDateTime expiresAt() {
        return expiresAt;
    }

    /** 만료 시각 정각은 이미 만료된 것으로 본다. */
    public boolean isValidAt(LocalDateTime now) {
        return now.isBefore(expiresAt);
    }
}
