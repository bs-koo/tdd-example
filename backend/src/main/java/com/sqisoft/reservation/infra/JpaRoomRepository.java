package com.sqisoft.reservation.infra;

import java.util.List;

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
    public List<Room> findAll() {
        return jpa.findAllByOrderByIdAsc();
    }

    @Override
    public boolean existsById(Long roomId) {
        return jpa.existsById(roomId);
    }
}
