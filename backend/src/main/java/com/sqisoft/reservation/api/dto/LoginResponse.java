package com.sqisoft.reservation.api.dto;

import com.sqisoft.reservation.application.LoginResult;

public record LoginResponse(String token, MemberResponse member) {

    public static LoginResponse from(LoginResult result) {
        return new LoginResponse(result.token(), MemberResponse.from(result.member()));
    }
}
