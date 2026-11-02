package com.sqisoft.reservation.infra;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.sqisoft.reservation.application.RoomRepository;
import com.sqisoft.reservation.domain.Room;

@Repository
public class JpaRoomRepository implements RoomRepository {

    private final RoomJpaRepository jpa;

    public JpaRoomRepository(RoomJpaRepository jpa) {
        this.jpa = jpa;
    }

    @Override
    public Room save(Room room) {
        return jpa.save(room);
    }

    /** Spring Data의 findById는 null 인자에 InvalidDataAccessApiUsageException을 던지므로 먼저 걸러낸다. */
    @Override
    public Optional<Room> findById(Long roomId) {
        if (roomId == null) {
            return Optional.empty();
        }
        return jpa.findById(roomId);
    }

    @Override
    public List<Room> findAllByGroupId(Long groupId) {
        return jpa.findAllByGroupIdOrderByIdAsc(groupId);
    }

    @Override
    public boolean existsById(Long roomId) {
        return roomId != null && jpa.existsById(roomId);
    }

    /** 없는 id는 무시한다. 예약은 방을 FK 없이 id로만 가리키므로 예약 행에는 영향이 없다. */
    @Override
    public void deleteById(Long roomId) {
        if (roomId == null) {
            return;
        }
        jpa.deleteById(roomId);
    }
}
