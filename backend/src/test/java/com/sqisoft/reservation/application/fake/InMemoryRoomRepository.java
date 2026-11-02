package com.sqisoft.reservation.application.fake;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import com.sqisoft.reservation.application.RoomRepository;
import com.sqisoft.reservation.domain.Room;

/**
 * 테스트 전용 인메모리 {@link RoomRepository} Fake 구현. 프로덕션 코드가 아니다.
 * 저장 시 id를 채운 새 인스턴스를 돌려주어 JPA IDENTITY 전략과 같은 관찰 가능한 동작을 흉내 낸다.
 */
public class InMemoryRoomRepository implements RoomRepository {

    private final List<Room> rooms = new ArrayList<>();
    private final AtomicLong sequence = new AtomicLong();

    public InMemoryRoomRepository(Room... rooms) {
        for (Room room : rooms) {
            save(room);
        }
    }

    @Override
    public Room save(Room room) {
        Long id = room.id() == null ? sequence.incrementAndGet() : room.id();
        Room stored = copyOf(id, room);
        rooms.removeIf(saved -> saved.id().equals(stored.id()));
        rooms.add(stored);
        sequence.updateAndGet(current -> Math.max(current, stored.id()));
        return copyOf(stored.id(), stored);
    }

    @Override
    public Optional<Room> findById(Long roomId) {
        if (roomId == null) {
            return Optional.empty();
        }
        return rooms.stream().filter(room -> room.id().equals(roomId)).findFirst()
                .map(room -> copyOf(room.id(), room));
    }

    @Override
    public List<Room> findAllByGroupId(Long groupId) {
        return rooms.stream()
                .filter(room -> room.groupId().equals(groupId))
                .sorted(Comparator.comparing(Room::id))
                .map(room -> copyOf(room.id(), room))
                .toList();
    }

    // 저장·조회 모두 복사본을 주고받아, 영속화(save) 없이 객체만 바꾼 변경이 저장소에 새어 들어가지 않게 한다.
    private static Room copyOf(Long id, Room room) {
        return Room.restore(id, room.groupId(), room.name(), room.capacity(), room.location());
    }

    @Override
    public void deleteById(Long roomId) {
        if (roomId == null) {
            return;
        }
        rooms.removeIf(room -> room.id().equals(roomId));
    }

    @Override
    public boolean existsById(Long roomId) {
        return findById(roomId).isPresent();
    }
}
