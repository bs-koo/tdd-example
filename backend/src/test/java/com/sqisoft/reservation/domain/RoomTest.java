package com.sqisoft.reservation.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sqisoft.reservation.support.ReservationTestFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomTest {

    @Test
    @DisplayName("create로 생성한 회의실은 id가 null이고 나머지 접근자는 생성 인자를 그대로 반환한다")
    void create로_생성한_회의실은_id가_null이고_나머지는_생성_인자를_그대로_반환한다() {
        Room room = Room.create(ReservationTestFixtures.GROUP_ID, "대회의실", 20, "3층");

        assertThat(room.id()).isNull();
        assertThat(room.groupId()).isEqualTo(ReservationTestFixtures.GROUP_ID);
        assertThat(room.name()).isEqualTo("대회의실");
        assertThat(room.capacity()).isEqualTo(20);
        assertThat(room.location()).isEqualTo("3층");
    }

    @Test
    @DisplayName("create는 이름과 위치를 trim해서 저장한다")
    void create는_이름과_위치를_trim해서_저장한다() {
        Room room = Room.create(ReservationTestFixtures.GROUP_ID, "  대회의실  ", 20, "  3층  ");

        assertThat(room.name()).isEqualTo("대회의실");
        assertThat(room.location()).isEqualTo("3층");
    }

    @Test
    @DisplayName("restore로 생성한 회의실은 주어진 id와 groupId를 그대로 반환한다")
    void restore로_생성한_회의실은_주어진_id와_groupId를_그대로_반환한다() {
        Room room = Room.restore(7L, 3L, "대회의실", 20, "3층");

        assertThat(room.id()).isEqualTo(7L);
        assertThat(room.groupId()).isEqualTo(3L);
        assertThat(room.name()).isEqualTo("대회의실");
        assertThat(room.capacity()).isEqualTo(20);
        assertThat(room.location()).isEqualTo("3층");
    }

    @Test
    @DisplayName("restore는 검증하지 않으므로 규칙을 어긴 값도 그대로 복원한다")
    void restore는_검증하지_않으므로_규칙을_어긴_값도_그대로_복원한다() {
        assertThatNoException()
                .isThrownBy(() -> Room.restore(1L, ReservationTestFixtures.GROUP_ID, "", 0, ""));
    }

    @ParameterizedTest(name = "이름이 {0}자면 통과한다")
    @DisplayName("경계: 이름이 1자 또는 30자면 통과한다")
    @ValueSource(ints = {1, 30})
    void 이름이_1자_또는_30자면_통과한다(int length) {
        assertThatNoException().isThrownBy(
                () -> Room.create(ReservationTestFixtures.GROUP_ID, "가".repeat(length), 20, "3층"));
    }

    @Test
    @DisplayName("경계: 이름이 31자면 INVALID_ROOM_NAME 예외를 던진다")
    void 이름이_31자면_INVALID_ROOM_NAME_예외를_던진다() {
        assertThatThrownBy(() -> Room.create(ReservationTestFixtures.GROUP_ID, "가".repeat(31), 20, "3층"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_ROOM_NAME);
    }

    @Test
    @DisplayName("이름이 빈 문자열이면 INVALID_ROOM_NAME 예외를 던진다")
    void 이름이_빈_문자열이면_INVALID_ROOM_NAME_예외를_던진다() {
        assertThatThrownBy(() -> Room.create(ReservationTestFixtures.GROUP_ID, "", 20, "3층"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_ROOM_NAME);
    }

    @Test
    @DisplayName("이름이 공백만으로 이루어지면 trim 후 빈 문자열이므로 INVALID_ROOM_NAME 예외를 던진다")
    void 이름이_공백만이면_INVALID_ROOM_NAME_예외를_던진다() {
        assertThatThrownBy(() -> Room.create(ReservationTestFixtures.GROUP_ID, "   ", 20, "3층"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_ROOM_NAME);
    }

    @Test
    @DisplayName("이름이 null이면 INVALID_ROOM_NAME 예외를 던진다")
    void 이름이_null이면_INVALID_ROOM_NAME_예외를_던진다() {
        assertThatThrownBy(() -> Room.create(ReservationTestFixtures.GROUP_ID, null, 20, "3층"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_ROOM_NAME);
    }

    @ParameterizedTest(name = "수용 인원이 {0}명이면 통과한다")
    @DisplayName("경계: 수용 인원이 1명 또는 999명이면 통과한다")
    @ValueSource(ints = {1, 999})
    void 수용_인원이_1명_또는_999명이면_통과한다(int capacity) {
        assertThatNoException().isThrownBy(
                () -> Room.create(ReservationTestFixtures.GROUP_ID, "대회의실", capacity, "3층"));
    }

    @ParameterizedTest(name = "수용 인원이 {0}명이면 INVALID_ROOM_CAPACITY 예외를 던진다")
    @DisplayName("경계: 수용 인원이 0명 이하이거나 1000명 이상이면 INVALID_ROOM_CAPACITY 예외를 던진다")
    @ValueSource(ints = {-1, 0, 1000})
    void 수용_인원이_범위를_벗어나면_INVALID_ROOM_CAPACITY_예외를_던진다(int capacity) {
        assertThatThrownBy(() -> Room.create(ReservationTestFixtures.GROUP_ID, "대회의실", capacity, "3층"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_ROOM_CAPACITY);
    }

    @ParameterizedTest(name = "위치가 {0}자면 통과한다")
    @DisplayName("경계: 위치가 1자 또는 30자면 통과한다")
    @ValueSource(ints = {1, 30})
    void 위치가_1자_또는_30자면_통과한다(int length) {
        assertThatNoException().isThrownBy(
                () -> Room.create(ReservationTestFixtures.GROUP_ID, "대회의실", 20, "가".repeat(length)));
    }

    @Test
    @DisplayName("경계: 위치가 31자면 INVALID_ROOM_LOCATION 예외를 던진다")
    void 위치가_31자면_INVALID_ROOM_LOCATION_예외를_던진다() {
        assertThatThrownBy(() -> Room.create(ReservationTestFixtures.GROUP_ID, "대회의실", 20, "가".repeat(31)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_ROOM_LOCATION);
    }

    @Test
    @DisplayName("위치가 빈 문자열이면 INVALID_ROOM_LOCATION 예외를 던진다")
    void 위치가_빈_문자열이면_INVALID_ROOM_LOCATION_예외를_던진다() {
        assertThatThrownBy(() -> Room.create(ReservationTestFixtures.GROUP_ID, "대회의실", 20, ""))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_ROOM_LOCATION);
    }

    @Test
    @DisplayName("위치가 공백만으로 이루어지면 trim 후 빈 문자열이므로 INVALID_ROOM_LOCATION 예외를 던진다")
    void 위치가_공백만이면_INVALID_ROOM_LOCATION_예외를_던진다() {
        assertThatThrownBy(() -> Room.create(ReservationTestFixtures.GROUP_ID, "대회의실", 20, "   "))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_ROOM_LOCATION);
    }

    @Test
    @DisplayName("위치가 null이면 INVALID_ROOM_LOCATION 예외를 던진다")
    void 위치가_null이면_INVALID_ROOM_LOCATION_예외를_던진다() {
        assertThatThrownBy(() -> Room.create(ReservationTestFixtures.GROUP_ID, "대회의실", 20, null))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_ROOM_LOCATION);
    }

    @ParameterizedTest(name = "이름 \"{0}\", 수용 인원 {1}, 위치 \"{2}\" 이면 {3} 예외를 던진다")
    @DisplayName("검증 순서는 이름 → 수용 인원 → 위치로 고정된다")
    @CsvSource({
            "'', 0, '', INVALID_ROOM_NAME",
            "대회의실, 0, '', INVALID_ROOM_CAPACITY",
            "대회의실, 20, '', INVALID_ROOM_LOCATION",
    })
    void 검증_순서는_이름_수용인원_위치로_고정된다(String name, int capacity, String location,
            ReservationErrorCode expected) {
        assertThatThrownBy(() -> Room.create(ReservationTestFixtures.GROUP_ID, name, capacity, location))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(expected);
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

    // ---- update ----

    private static Room 원본_회의실() {
        return Room.restore(7L, 3L, "대회의실", 10, "3층");
    }

    private static void assertUpdateFails(String name, int capacity, String location,
            ReservationErrorCode expected) {
        Room room = 원본_회의실();
        assertThatThrownBy(() -> room.update(name, capacity, location))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("update는 이름·위치·인원을 바꾸고 id와 groupId는 유지한다")
    void update는_이름_위치_인원을_바꾸고_id와_groupId는_유지한다() {
        Room room = 원본_회의실();

        room.update("대회의실 B", 12, "5층");

        assertThat(room.name()).isEqualTo("대회의실 B");
        assertThat(room.capacity()).isEqualTo(12);
        assertThat(room.location()).isEqualTo("5층");
        assertThat(room.id()).isEqualTo(7L);
        assertThat(room.groupId()).isEqualTo(3L);
    }

    @Test
    @DisplayName("update는 이름과 위치를 trim해서 저장한다")
    void update는_이름과_위치를_trim해서_저장한다() {
        Room room = 원본_회의실();

        room.update("  소회의실  ", 4, "  2층  ");

        assertThat(room.name()).isEqualTo("소회의실");
        assertThat(room.location()).isEqualTo("2층");
    }

    @ParameterizedTest(name = "수정 이름이 {0}자(trim 후 {1}자)면 허용된다")
    @DisplayName("update 경계: 이름이 30자, 공백 포함 33자(trim 후 30자)면 허용된다")
    @CsvSource({"30, 0", "30, 3"})
    void update_이름_경계_허용(int trimmedLength, int padding) {
        Room room = 원본_회의실();
        String name = " ".repeat(padding) + "가".repeat(trimmedLength);

        room.update(name, 10, "3층");

        assertThat(room.name()).hasSize(30);
    }

    @Test
    @DisplayName("update 경계: 이름이 1자 'A'면 그대로 저장된다")
    void update_이름이_A_한글자면_그대로_저장된다() {
        Room room = 원본_회의실();

        room.update("A", 10, "3층");

        assertThat(room.name()).isEqualTo("A");
    }

    @ParameterizedTest(name = "수정 이름이 [{0}]이면 INVALID_ROOM_NAME")
    @DisplayName("update 경계: 이름이 공백만, 빈 문자열이면 INVALID_ROOM_NAME")
    @ValueSource(strings = {"   ", ""})
    void update_이름_위반은_INVALID_ROOM_NAME(String name) {
        assertUpdateFails(name, 10, "3층", ReservationErrorCode.INVALID_ROOM_NAME);
    }

    @Test
    @DisplayName("update 경계: 이름이 31자면 INVALID_ROOM_NAME")
    void update_이름이_31자면_INVALID_ROOM_NAME() {
        assertUpdateFails("가".repeat(31), 10, "3층", ReservationErrorCode.INVALID_ROOM_NAME);
    }

    @Test
    @DisplayName("update에서 이름이 null이면 INVALID_ROOM_NAME")
    void update_이름이_null이면_INVALID_ROOM_NAME() {
        assertUpdateFails(null, 10, "3층", ReservationErrorCode.INVALID_ROOM_NAME);
    }

    @ParameterizedTest(name = "수정 위치 길이 {0}자면 허용된다")
    @DisplayName("update 경계: 위치가 1자 또는 30자면 허용된다")
    @ValueSource(ints = {1, 30})
    void update_위치_경계_허용(int length) {
        Room room = 원본_회의실();
        String location = "나".repeat(length);

        room.update("대회의실", 10, location);

        assertThat(room.location()).isEqualTo(location);
    }

    @ParameterizedTest(name = "수정 위치가 [{0}]이면 INVALID_ROOM_LOCATION")
    @DisplayName("update 경계: 위치가 공백만, 빈 문자열이면 INVALID_ROOM_LOCATION")
    @ValueSource(strings = {"   ", ""})
    void update_위치_위반은_INVALID_ROOM_LOCATION(String location) {
        assertUpdateFails("대회의실", 10, location, ReservationErrorCode.INVALID_ROOM_LOCATION);
    }

    @Test
    @DisplayName("update 경계: 위치가 31자면 INVALID_ROOM_LOCATION")
    void update_위치가_31자면_INVALID_ROOM_LOCATION() {
        assertUpdateFails("대회의실", 10, "나".repeat(31), ReservationErrorCode.INVALID_ROOM_LOCATION);
    }

    @Test
    @DisplayName("update에서 위치가 null이면 INVALID_ROOM_LOCATION")
    void update_위치가_null이면_INVALID_ROOM_LOCATION() {
        assertUpdateFails("대회의실", 10, null, ReservationErrorCode.INVALID_ROOM_LOCATION);
    }

    @ParameterizedTest(name = "수정 인원 {0}명이면 허용된다")
    @DisplayName("update 경계: 인원이 1명 또는 999명이면 허용된다")
    @ValueSource(ints = {1, 999})
    void update_인원_경계_허용(int capacity) {
        Room room = 원본_회의실();

        room.update("대회의실", capacity, "3층");

        assertThat(room.capacity()).isEqualTo(capacity);
    }

    @ParameterizedTest(name = "수정 인원 {0}명이면 INVALID_ROOM_CAPACITY")
    @DisplayName("update 경계: 인원이 -1, 0, 1000이면 INVALID_ROOM_CAPACITY")
    @ValueSource(ints = {-1, 0, 1000})
    void update_인원_위반은_INVALID_ROOM_CAPACITY(int capacity) {
        assertUpdateFails("대회의실", capacity, "3층", ReservationErrorCode.INVALID_ROOM_CAPACITY);
    }

    @ParameterizedTest(name = "이름 {0}자, 인원 {1}, 위치 {2}자 이면 {3}")
    @DisplayName("update 검증 순서는 이름 → 인원 → 위치로 첫 위반 하나만 던진다")
    @CsvSource({
            "31, 0, 31, INVALID_ROOM_NAME",
            "4, 0, 31, INVALID_ROOM_CAPACITY",
    })
    void update_검증_순서는_이름_인원_위치(int nameLength, int capacity, int locationLength,
            ReservationErrorCode expected) {
        assertUpdateFails("가".repeat(nameLength), capacity, "나".repeat(locationLength), expected);
    }

    @Test
    @DisplayName("update를 같은 값으로 호출해도 예외 없이 값이 그대로다")
    void update를_같은_값으로_호출해도_값이_그대로다() {
        Room room = 원본_회의실();

        assertThatNoException().isThrownBy(() -> room.update("대회의실", 10, "3층"));

        assertThat(room.name()).isEqualTo("대회의실");
        assertThat(room.capacity()).isEqualTo(10);
        assertThat(room.location()).isEqualTo("3층");
    }

    @Test
    @DisplayName("원자성: 이름이 유효하고 인원이 위반이면 예외 후 이름·인원·위치가 모두 원래 값이다")
    void 원자성_이름_유효_인원_위반이면_모든_값이_원래대로다() {
        Room room = 원본_회의실();

        assertThatThrownBy(() -> room.update("새이름", 0, "새위치"))
                .isInstanceOf(AppException.class);

        assertThat(room.name()).isEqualTo("대회의실");
        assertThat(room.capacity()).isEqualTo(10);
        assertThat(room.location()).isEqualTo("3층");
    }

    @Test
    @DisplayName("원자성: 이름·인원이 유효하고 위치가 위반이면 예외 후 이름·인원·위치가 모두 원래 값이다")
    void 원자성_이름_인원_유효_위치_위반이면_모든_값이_원래대로다() {
        Room room = 원본_회의실();

        assertThatThrownBy(() -> room.update("새이름", 5, "나".repeat(31)))
                .isInstanceOf(AppException.class);

        assertThat(room.name()).isEqualTo("대회의실");
        assertThat(room.capacity()).isEqualTo(10);
        assertThat(room.location()).isEqualTo("3층");
    }
}
