package com.sqisoft.reservation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoomTest {

    @Test
    @DisplayName("Room.of로 생성한 인스턴스의 접근자는 생성 인자를 그대로 반환한다")
    void of로_생성한_인스턴스의_접근자는_생성_인자를_그대로_반환한다() {
        Room room = Room.of(1L, "대회의실", 20, "3층");

        assertThat(room.id()).isEqualTo(1L);
        assertThat(room.name()).isEqualTo("대회의실");
        assertThat(room.capacity()).isEqualTo(20);
        assertThat(room.location()).isEqualTo("3층");
    }

    @Test
    @DisplayName("ReservationStatus는 정확히 2종이다")
    void 예약_상태는_정확히_2종이다() {
        assertThat(ReservationStatus.values()).hasSize(2);
    }

    @Test
    @DisplayName("ReservationStatus는 ACTIVE와 CANCELLED를 포함한다")
    void 예약_상태는_ACTIVE와_CANCELLED를_포함한다() {
        assertThat(ReservationStatus.values())
                .containsExactlyInAnyOrder(ReservationStatus.ACTIVE, ReservationStatus.CANCELLED);
    }
}
