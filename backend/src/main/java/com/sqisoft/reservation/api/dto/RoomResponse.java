package com.sqisoft.reservation.api.dto;

import com.sqisoft.reservation.domain.Room;

public record RoomResponse(Long id, String name, int capacity, String location) {

    public static RoomResponse from(Room room) {
        return new RoomResponse(room.id(), room.name(), room.capacity(), room.location());
    }
}
