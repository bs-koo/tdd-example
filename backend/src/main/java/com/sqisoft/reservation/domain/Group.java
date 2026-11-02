package com.sqisoft.reservation.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 회의실과 예약이 귀속되는 울타리. 승인된 그룹원만 그 안의 회의실을 보고 예약할 수 있다.
 * 테이블명이 {@code user_group} 인 것은 {@code GROUP} 이 SQL 예약어이기 때문이다.
 */
@Entity
@Table(name = "user_group")
public class Group {

    static final int NAME_MAX = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private Long ownerId;

    private LocalDateTime createdAt;

    protected Group() {
    }

    private Group(Long id, String name, Long ownerId, LocalDateTime createdAt) {
        this.id = id;
        this.name = name;
        this.ownerId = ownerId;
        this.createdAt = createdAt;
    }

    public static Group create(String name, Long ownerId, LocalDateTime createdAt) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > NAME_MAX) {
            throw new AppException(GroupErrorCode.INVALID_GROUP_NAME);
        }
        return new Group(null, trimmed, ownerId, createdAt);
    }

    public static Group restore(Long id, String name, Long ownerId, LocalDateTime createdAt) {
        return new Group(id, name, ownerId, createdAt);
    }

    public Long id() {
        return id;
    }

    public String name() {
        return name;
    }

    public Long ownerId() {
        return ownerId;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }
}
