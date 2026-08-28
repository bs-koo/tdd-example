package com.sqisoft.reservation.application.fake;

import java.util.Arrays;
import java.util.List;

import com.sqisoft.reservation.application.RoomRepository;
import com.sqisoft.reservation.domain.Room;

/**
 * 테스트 전용 인메모리 {@link RoomRepository} Fake 구현. 프로덕션 코드가 아니다.
 */
public class InMemoryRoomRepository implements RoomRepository {

    private final List<Room> rooms;

    public InMemoryRoomRepository(Room... rooms) {
        this.rooms = Arrays.asList(rooms);
    }

    @Override
    public List<Room> findAll() {
        return rooms;
    }

    @Override
    public boolean existsById(Long roomId) {
        return rooms.stream().anyMatch(room -> room.id().equals(roomId));
    }
}
