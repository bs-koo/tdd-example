package com.sqisoft.reservation.api.dto;

import com.sqisoft.reservation.application.SignupCommand;

public record SignupRequest(String email, String password, String name) {

    public SignupCommand toCommand() {
        return new SignupCommand(email, password, name);
    }
}
