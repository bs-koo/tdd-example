package com.sqisoft.reservation.application;

import java.util.List;

import com.sqisoft.reservation.domain.Room;

public class RoomService {

    private final RoomRepository roomRepository;

    public RoomService(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
    }

    public List<Room> findAll() {
        return roomRepository.findAll();
    }
}
