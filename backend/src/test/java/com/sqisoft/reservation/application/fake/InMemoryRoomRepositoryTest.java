package com.sqisoft.reservation.application.fake;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sqisoft.reservation.domain.Room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/** RoomRepository.deleteById 계약을 인메모리 Fake에 대해 검증한다 (JpaRoomRepositoryTest와 동일 계약). */
class InMemoryRoomRepositoryTest {

    private static final Long GROUP_ID = 1L;

    private InMemoryRoomRepository repository;
    private Room target;
    private Room other;

    @BeforeEach
    void setUp() {
        repository = new InMemoryRoomRepository();
        target = repository.save(Room.create(GROUP_ID, "대회의실", 10, "3층"));
        other = repository.save(Room.create(GROUP_ID, "소회의실", 4, "2층"));
    }

    @Test
    @DisplayName("deleteById 후 findById는 empty다")
    void deleteById_후_findById는_empty다() {
        repository.deleteById(target.id());

        assertThat(repository.findById(target.id())).isEmpty();
    }

    @Test
    @DisplayName("deleteById 후 findAllByGroupId에서 그 방만 빠진다")
    void deleteById_후_그룹_목록에서_그_방만_빠진다() {
        repository.deleteById(target.id());

        assertThat(repository.findAllByGroupId(GROUP_ID)).extracting(Room::id).containsExactly(other.id());
    }

    @Test
    @DisplayName("deleteById 후 existsById는 false이고 다른 방은 true다")
    void deleteById_후_existsById는_false다() {
        repository.deleteById(target.id());

        assertThat(repository.existsById(target.id())).isFalse();
        assertThat(repository.existsById(other.id())).isTrue();
    }

    @Test
    @DisplayName("없는 id를 deleteById해도 예외가 없고 기존 방은 그대로다")
    void 없는_id를_deleteById해도_예외가_없다() {
        assertThatNoException().isThrownBy(() -> repository.deleteById(999999L));

        assertThat(repository.findAllByGroupId(GROUP_ID)).hasSize(2);
    }
}
