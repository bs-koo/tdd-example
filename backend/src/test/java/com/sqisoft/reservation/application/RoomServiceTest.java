package com.sqisoft.reservation.application;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.sqisoft.reservation.application.fake.InMemoryGroupMembershipRepository;
import com.sqisoft.reservation.application.fake.InMemoryGroupRepository;
import com.sqisoft.reservation.application.fake.InMemoryRoomRepository;
import com.sqisoft.reservation.domain.AppException;
import com.sqisoft.reservation.domain.Group;
import com.sqisoft.reservation.domain.GroupErrorCode;
import com.sqisoft.reservation.domain.GroupMembership;
import com.sqisoft.reservation.domain.ReservationErrorCode;
import com.sqisoft.reservation.domain.ReservationException;
import com.sqisoft.reservation.domain.Room;
import com.sqisoft.reservation.support.ReservationTestFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomServiceTest {

    private static final LocalDateTime NOW =
            ReservationTestFixtures.at(ReservationTestFixtures.TODAY, "09:00");
    private static final Long GROUP_ID = ReservationTestFixtures.GROUP_ID;
    private static final Long OWNER_ID = ReservationTestFixtures.MEMBER_ID;
    private static final Long OTHER_MEMBER_ID = ReservationTestFixtures.OTHER_MEMBER_ID;
    private static final Long PENDING_MEMBER_ID = 30L;
    private static final Long ABSENT_GROUP_ID = 999L;

    private InMemoryRoomRepository roomRepository;
    private InMemoryGroupRepository groupRepository;
    private InMemoryGroupMembershipRepository membershipRepository;
    private RoomService service;
    private Long otherGroupId;

    @BeforeEach
    void setUp() {
        roomRepository = new InMemoryRoomRepository();
        groupRepository = new InMemoryGroupRepository();
        membershipRepository = new InMemoryGroupMembershipRepository();

        // GROUP_ID(1) = OWNER_ID의 그룹. PENDING_MEMBER_ID는 초대만 받고 아직 수락하지 않았다.
        Group group = groupRepository.save(Group.create("우리 팀", OWNER_ID, NOW));
        membershipRepository.save(GroupMembership.owner(group.id(), OWNER_ID, NOW));
        membershipRepository.save(GroupMembership.invite(group.id(), PENDING_MEMBER_ID, NOW));

        // OTHER_MEMBER_ID만 속한 별개의 그룹.
        Group otherGroup = groupRepository.save(Group.create("다른 팀", OTHER_MEMBER_ID, NOW));
        membershipRepository.save(GroupMembership.owner(otherGroup.id(), OTHER_MEMBER_ID, NOW));
        otherGroupId = otherGroup.id();

        service = new RoomService(
                roomRepository,
                new com.sqisoft.reservation.application.fake.InMemoryReservationRepository(),
                new GroupAccess(groupRepository, membershipRepository),
                ReservationTestFixtures.FIXED_CLOCK);
    }

    @Test
    @DisplayName("승인된 그룹원이 회의실을 추가하면 그 그룹 소속으로 저장된다")
    void 승인된_그룹원이_회의실을_추가하면_그_그룹_소속으로_저장된다() {
        Room created = service.createRoom(GROUP_ID, OWNER_ID, new CreateRoomCommand("대회의실", 20, "3층"));

        assertThat(created.id()).isNotNull();
        assertThat(created.groupId()).isEqualTo(GROUP_ID);
        assertThat(created.name()).isEqualTo("대회의실");
        assertThat(created.capacity()).isEqualTo(20);
        assertThat(created.location()).isEqualTo("3층");
        assertThat(roomRepository.findById(created.id())).isPresent();
    }

    @Test
    @DisplayName("대기 중(PENDING)인 사람은 회의실을 추가할 수 없다")
    void 대기_중인_사람은_회의실을_추가할_수_없다() {
        assertThatThrownBy(() -> service.createRoom(
                GROUP_ID, PENDING_MEMBER_ID, new CreateRoomCommand("대회의실", 20, "3층")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.NOT_GROUP_MEMBER);
    }

    @Test
    @DisplayName("그룹과 무관한 사람은 회의실을 추가할 수 없다")
    void 그룹과_무관한_사람은_회의실을_추가할_수_없다() {
        assertThatThrownBy(() -> service.createRoom(
                GROUP_ID, OTHER_MEMBER_ID, new CreateRoomCommand("대회의실", 20, "3층")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.NOT_GROUP_MEMBER);
    }

    @Test
    @DisplayName("없는 그룹에 회의실을 추가하면 GROUP_NOT_FOUND 예외를 던진다")
    void 없는_그룹에_회의실을_추가하면_GROUP_NOT_FOUND_예외를_던진다() {
        assertThatThrownBy(() -> service.createRoom(
                ABSENT_GROUP_ID, OWNER_ID, new CreateRoomCommand("대회의실", 20, "3층")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.GROUP_NOT_FOUND);
    }

    @Test
    @DisplayName("권한이 없으면 이름이 잘못됐더라도 NOT_GROUP_MEMBER가 먼저다 — 권한 검사가 입력 검증보다 앞선다")
    void 권한이_없으면_이름이_잘못됐더라도_NOT_GROUP_MEMBER가_먼저다() {
        assertThatThrownBy(() -> service.createRoom(
                GROUP_ID, OTHER_MEMBER_ID, new CreateRoomCommand("", 0, "")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.NOT_GROUP_MEMBER);
    }

    @Test
    @DisplayName("권한 검사에 걸린 요청은 회의실을 저장하지 않는다")
    void 권한_검사에_걸린_요청은_회의실을_저장하지_않는다() {
        try {
            service.createRoom(GROUP_ID, OTHER_MEMBER_ID, new CreateRoomCommand("대회의실", 20, "3층"));
        } catch (AppException ignored) {
            // 거절 확인은 저장소 상태 검증으로 대체
        }

        assertThat(roomRepository.findAllByGroupId(GROUP_ID)).isEmpty();
    }

    @ParameterizedTest(name = "이름이 {0}자면 회의실을 추가할 수 있다")
    @DisplayName("경계: 이름이 1자 또는 30자면 회의실을 추가할 수 있다")
    @ValueSource(ints = {1, 30})
    void 이름이_1자_또는_30자면_회의실을_추가할_수_있다(int length) {
        assertThatNoException().isThrownBy(() -> service.createRoom(
                GROUP_ID, OWNER_ID, new CreateRoomCommand("가".repeat(length), 20, "3층")));
    }

    @Test
    @DisplayName("경계: 이름이 31자면 INVALID_ROOM_NAME 예외를 던진다")
    void 이름이_31자면_INVALID_ROOM_NAME_예외를_던진다() {
        assertThatThrownBy(() -> service.createRoom(
                GROUP_ID, OWNER_ID, new CreateRoomCommand("가".repeat(31), 20, "3층")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_ROOM_NAME);
    }

    @ParameterizedTest(name = "수용 인원이 {0}명이면 회의실을 추가할 수 있다")
    @DisplayName("경계: 수용 인원이 1명 또는 999명이면 회의실을 추가할 수 있다")
    @ValueSource(ints = {1, 999})
    void 수용_인원이_1명_또는_999명이면_회의실을_추가할_수_있다(int capacity) {
        assertThatNoException().isThrownBy(() -> service.createRoom(
                GROUP_ID, OWNER_ID, new CreateRoomCommand("대회의실", capacity, "3층")));
    }

    @ParameterizedTest(name = "수용 인원이 {0}명이면 INVALID_ROOM_CAPACITY 예외를 던진다")
    @DisplayName("경계: 수용 인원이 0명이거나 1000명이면 INVALID_ROOM_CAPACITY 예외를 던진다")
    @ValueSource(ints = {0, 1000})
    void 수용_인원이_범위를_벗어나면_INVALID_ROOM_CAPACITY_예외를_던진다(int capacity) {
        assertThatThrownBy(() -> service.createRoom(
                GROUP_ID, OWNER_ID, new CreateRoomCommand("대회의실", capacity, "3층")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_ROOM_CAPACITY);
    }

    @Test
    @DisplayName("위치가 빈 문자열이면 INVALID_ROOM_LOCATION 예외를 던진다")
    void 위치가_빈_문자열이면_INVALID_ROOM_LOCATION_예외를_던진다() {
        assertThatThrownBy(() -> service.createRoom(
                GROUP_ID, OWNER_ID, new CreateRoomCommand("대회의실", 20, "")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_ROOM_LOCATION);
    }

    @ParameterizedTest(name = "이름 \"{0}\", 수용 인원 {1}, 위치 \"{2}\" 이면 {3} 예외를 던진다")
    @DisplayName("입력 검증 순서는 이름 → 수용 인원 → 위치로 고정된다")
    @CsvSource({
            "'', 0, '', INVALID_ROOM_NAME",
            "대회의실, 0, '', INVALID_ROOM_CAPACITY",
            "대회의실, 20, '', INVALID_ROOM_LOCATION",
    })
    void 입력_검증_순서는_이름_수용인원_위치로_고정된다(String name, int capacity, String location,
            ReservationErrorCode expected) {
        assertThatThrownBy(() -> service.createRoom(
                GROUP_ID, OWNER_ID, new CreateRoomCommand(name, capacity, location)))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(expected);
    }

    @Test
    @DisplayName("그룹 회의실 목록은 그 그룹 것만 id 오름차순으로 반환한다")
    void 그룹_회의실_목록은_그_그룹_것만_id_오름차순으로_반환한다() {
        Room addedFirst = service.createRoom(GROUP_ID, OWNER_ID, new CreateRoomCommand("중회의실", 10, "3층"));
        Room addedSecond = service.createRoom(GROUP_ID, OWNER_ID, new CreateRoomCommand("대회의실", 20, "3층"));
        service.createRoom(otherGroupId, OTHER_MEMBER_ID, new CreateRoomCommand("타그룹 회의실", 8, "5층"));

        List<Room> result = service.findByGroup(GROUP_ID, OWNER_ID);

        assertThat(result).extracting(Room::id).containsExactly(addedFirst.id(), addedSecond.id());
        assertThat(result).extracting(Room::groupId).containsOnly(GROUP_ID);
    }

    @Test
    @DisplayName("회의실이 하나도 없는 그룹의 목록은 빈 리스트다")
    void 회의실이_하나도_없는_그룹의_목록은_빈_리스트다() {
        assertThat(service.findByGroup(GROUP_ID, OWNER_ID)).isEmpty();
    }

    @Test
    @DisplayName("승인되지 않은 사람은 그룹 회의실 목록을 볼 수 없다")
    void 승인되지_않은_사람은_그룹_회의실_목록을_볼_수_없다() {
        service.createRoom(GROUP_ID, OWNER_ID, new CreateRoomCommand("대회의실", 20, "3층"));

        assertThatThrownBy(() -> service.findByGroup(GROUP_ID, PENDING_MEMBER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.NOT_GROUP_MEMBER);
    }

    @Test
    @DisplayName("없는 그룹의 회의실 목록을 조회하면 GROUP_NOT_FOUND 예외를 던진다")
    void 없는_그룹의_회의실_목록을_조회하면_GROUP_NOT_FOUND_예외를_던진다() {
        assertThatThrownBy(() -> service.findByGroup(ABSENT_GROUP_ID, OWNER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.GROUP_NOT_FOUND);
    }

    @Test
    @DisplayName("requireAccessibleRoom: 승인된 그룹원에게는 회의실을 그대로 돌려준다")
    void requireAccessibleRoom은_승인된_그룹원에게_회의실을_돌려준다() {
        Room created = service.createRoom(GROUP_ID, OWNER_ID, new CreateRoomCommand("대회의실", 20, "3층"));

        Room found = service.requireAccessibleRoom(created.id(), OWNER_ID);

        assertThat(found.id()).isEqualTo(created.id());
        assertThat(found.groupId()).isEqualTo(GROUP_ID);
    }

    @Test
    @DisplayName("requireAccessibleRoom: 없는 회의실이면 ROOM_NOT_FOUND 예외를 던진다")
    void requireAccessibleRoom은_없는_회의실이면_ROOM_NOT_FOUND_예외를_던진다() {
        assertThatThrownBy(() -> service.requireAccessibleRoom(999L, OWNER_ID))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("requireAccessibleRoom: roomId가 null이면 ROOM_NOT_FOUND 예외를 던진다")
    void requireAccessibleRoom은_roomId가_null이면_ROOM_NOT_FOUND_예외를_던진다() {
        assertThatThrownBy(() -> service.requireAccessibleRoom(null, OWNER_ID))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("requireAccessibleRoom: 남의 그룹 회의실이면 NOT_GROUP_MEMBER 예외를 던진다")
    void requireAccessibleRoom은_남의_그룹_회의실이면_NOT_GROUP_MEMBER_예외를_던진다() {
        Room otherRoom = service.createRoom(
                otherGroupId, OTHER_MEMBER_ID, new CreateRoomCommand("타그룹 회의실", 8, "5층"));

        assertThatThrownBy(() -> service.requireAccessibleRoom(otherRoom.id(), OWNER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.NOT_GROUP_MEMBER);
    }

    @Test
    @DisplayName("requireAccessibleRoom: 회의실 부재가 그룹 권한보다 먼저 판정된다")
    void requireAccessibleRoom은_회의실_부재를_그룹_권한보다_먼저_판정한다() {
        // 어느 그룹에도 속하지 않은 사람이 없는 회의실을 요청해도 ROOM_NOT_FOUND가 먼저다.
        assertThatThrownBy(() -> service.requireAccessibleRoom(999L, PENDING_MEMBER_ID))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.ROOM_NOT_FOUND);
    }
}
