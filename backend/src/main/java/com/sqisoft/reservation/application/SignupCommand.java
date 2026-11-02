package com.sqisoft.reservation.application;

public record SignupCommand(String email, String password, String name) {
}
