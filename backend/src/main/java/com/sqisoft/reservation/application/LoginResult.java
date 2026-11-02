package com.sqisoft.reservation.application;

import com.sqisoft.reservation.domain.Member;

public record LoginResult(String token, Member member) {
}
