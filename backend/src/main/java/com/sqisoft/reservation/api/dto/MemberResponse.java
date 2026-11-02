package com.sqisoft.reservation.api.dto;

import com.sqisoft.reservation.domain.Member;

/** 비밀번호 해시는 담지 않는다 — 이 레코드가 회원 정보의 외부 노출 경계다. */
public record MemberResponse(Long id, String email, String name) {

    public static MemberResponse from(Member member) {
        return new MemberResponse(member.id(), member.email(), member.name());
    }
}
