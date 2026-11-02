package com.sqisoft.reservation.domain;

/**
 * 애플리케이션이 클라이언트에게 노출하는 오류 코드의 공통 계약.
 * 구현은 enum 이므로 {@code name()} 은 enum 상수 이름을 그대로 돌려준다 — 이것이 API의 code 문자열이다.
 */
public interface ErrorCode {

    String name();

    String message();
}
