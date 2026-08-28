package com.sqisoft.reservation.application;

import java.util.List;

import com.sqisoft.reservation.domain.Room;

public interface RoomRepository {

    List<Room> findAll();

    boolean existsById(Long roomId);
}
