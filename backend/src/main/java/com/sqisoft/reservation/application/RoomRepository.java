package com.sqisoft.reservation.application;

import java.util.List;
import java.util.Optional;

import com.sqisoft.reservation.domain.Room;

public interface RoomRepository {

    Room save(Room room);

    Optional<Room> findById(Long roomId);

    List<Room> findAllByGroupId(Long groupId);

    boolean existsById(Long roomId);

    void deleteById(Long roomId);
}
