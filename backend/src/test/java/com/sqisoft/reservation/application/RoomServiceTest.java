package com.sqisoft.reservation.application;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sqisoft.reservation.application.fake.InMemoryRoomRepository;
import com.sqisoft.reservation.domain.Room;

import static org.assertj.core.api.Assertions.assertThat;

class RoomServiceTest {

    @Test
    @DisplayName("리포지토리가 반환한 회의실 목록을 순서 그대로 반환한다")
    void 리포지토리가_반환한_순서_그대로_반환한다() {
        InMemoryRoomRepository repository = new InMemoryRoomRepository(
                Room.of(2L, "중회의실", 10, "3층"),
                Room.of(1L, "대회의실", 20, "3층"));
        RoomService service = new RoomService(repository);

        List<Room> result = service.findAll();

        assertThat(result).extracting(Room::id).containsExactly(2L, 1L);
    }

    @Test
    @DisplayName("리포지토리에 회의실이 없으면 빈 목록을 반환한다")
    void 회의실이_없으면_빈_목록을_반환한다() {
        InMemoryRoomRepository repository = new InMemoryRoomRepository();
        RoomService service = new RoomService(repository);

        List<Room> result = service.findAll();

        assertThat(result).isEmpty();
    }
}
