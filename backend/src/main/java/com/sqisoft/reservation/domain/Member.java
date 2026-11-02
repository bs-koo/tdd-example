package com.sqisoft.reservation.domain;

import java.time.LocalDateTime;
import java.util.Locale;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "app_member")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String passwordHash;

    private LocalDateTime createdAt;

    protected Member() {
    }

    private Member(Long id, String email, String name, String passwordHash, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.passwordHash = passwordHash;
        this.createdAt = createdAt;
    }

    public static Member register(String email, String name, String passwordHash, LocalDateTime createdAt) {
        return new Member(null, normalizeEmail(email), name == null ? null : name.trim(),
                passwordHash, createdAt);
    }

    public static Member restore(Long id, String email, String name, String passwordHash,
            LocalDateTime createdAt) {
        return new Member(id, email, name, passwordHash, createdAt);
    }

    /**
     * 이메일을 조회 키 하나로 접는다. 가입·로그인·초대가 모두 이 값을 쓰므로
     * "대소문자만 다른 같은 사람"이 두 계정으로 갈라지지 않는다.
     */
    public static String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
    }

    public Long id() {
        return id;
    }

    public String email() {
        return email;
    }

    public String name() {
        return name;
    }

    public String passwordHash() {
        return passwordHash;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }
}
