package com.sqisoft.reservation.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 컨트롤러 파라미터에 붙이면 Authorization 헤더의 Bearer 토큰이 가리키는 회원이 주입된다.
 * 토큰이 없거나 무효하면 주입 대신 401(UNAUTHENTICATED)로 요청이 끝난다.
 * 반드시 메서드의 <b>첫 번째</b> 파라미터로 선언한다 — 인증이 바디 파싱보다 먼저 판정되어야 한다.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface LoginMember {
}
