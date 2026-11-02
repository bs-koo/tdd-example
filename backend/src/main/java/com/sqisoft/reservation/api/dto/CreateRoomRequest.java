package com.sqisoft.reservation.api.dto;

import com.sqisoft.reservation.application.CreateRoomCommand;

public record CreateRoomRequest(String name, Integer capacity, String location) {

    /** capacity 필드가 빠지면 0으로 내려 도메인의 인원 검증에 걸리게 한다. */
    public CreateRoomCommand toCommand() {
        return new CreateRoomCommand(name, capacity == null ? 0 : capacity, location);
    }
}
