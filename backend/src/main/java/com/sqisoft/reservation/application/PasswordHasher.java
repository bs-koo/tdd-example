package com.sqisoft.reservation.application;

/**
 * 비밀번호 단방향 해시. 도메인·애플리케이션은 알고리즘을 알지 않는다.
 */
public interface PasswordHasher {

    String hash(String rawPassword);

    /** 형식이 깨진 해시나 null을 받아도 예외 없이 false를 돌려준다. */
    boolean matches(String rawPassword, String storedHash);
}
