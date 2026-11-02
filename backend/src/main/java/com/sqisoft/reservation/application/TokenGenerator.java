package com.sqisoft.reservation.application;

/**
 * 세션 토큰 발급기. 추측 불가능한 값을 돌려줘야 한다.
 */
public interface TokenGenerator {

    String generate();
}
