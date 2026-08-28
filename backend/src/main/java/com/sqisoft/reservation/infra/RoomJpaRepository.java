package com.sqisoft.reservation.infra;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sqisoft.reservation.domain.Room;

interface RoomJpaRepository extends JpaRepository<Room, Long> {

    List<Room> findAllByOrderByIdAsc();
}
