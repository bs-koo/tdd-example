package com.sqisoft.reservation.application;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sqisoft.reservation.application.fake.InMemoryGroupMembershipRepository;
import com.sqisoft.reservation.application.fake.InMemoryGroupRepository;
import com.sqisoft.reservation.application.fake.InMemoryRoomRepository;
import com.sqisoft.reservation.domain.AppException;
import com.sqisoft.reservation.domain.Group;
import com.sqisoft.reservation.domain.GroupErrorCode;
import com.sqisoft.reservation.domain.GroupMembership;
import com.sqisoft.reservation.domain.ReservationErrorCode;
import com.sqisoft.reservation.domain.Room;
import com.sqisoft.reservation.support.ReservationTestFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomServiceUpdateTest {

    private static final LocalDateTime NOW =
            ReservationTestFixtures.at(ReservationTestFixtures.TODAY, "09:00");
    private static final Long OWNER_ID = ReservationTestFixtures.MEMBER_ID;
    private static final Long OTHER_GROUP_OWNER_ID = ReservationTestFixtures.OTHER_MEMBER_ID;
    private static final Long PENDING_ID = 30L;
    private static final Long MEMBER_ID = 40L;
    private static final Long REJECTED_ID = 50L;
    private static final Long OUTSIDER_ID = 60L;
    private static final Long ABSENT_ROOM_ID = 999999L;

    private InMemoryRoomRepository roomRepository;
    private RoomService service;
    private Long groupId;
    private Long otherGroupId;
    private Room r1;
    private Room r2;
    private Room r3;

    @BeforeEach
    void setUp() {
        roomRepository = new InMemoryRoomRepository();
        InMemoryGroupRepository groupRepository = new InMemoryGroupRepository();
        InMemoryGroupMembershipRepository membershipRepository = new InMemoryGroupMembershipRepository();

        Group group = groupRepository.save(Group.create("우리 팀", OWNER_ID, NOW));
        groupId = group.id();
        membershipRepository.save(GroupMembership.owner(groupId, OWNER_ID, NOW));
        membershipRepository.save(GroupMembership.invite(groupId, PENDING_ID, NOW));
        membershipRepository.save(GroupMembership.invite(groupId, MEMBER_ID, NOW))
                .accept(MEMBER_ID, NOW);
        membershipRepository.save(GroupMembership.invite(groupId, REJECTED_ID, NOW))
                .reject(REJECTED_ID, NOW);

        Group otherGroup = groupRepository.save(Group.create("다른 팀", OTHER_GROUP_OWNER_ID, NOW));
        otherGroupId = otherGroup.id();
        membershipRepository.save(GroupMembership.owner(otherGroupId, OTHER_GROUP_OWNER_ID, NOW));

        service = new RoomService(
                roomRepository,
                new com.sqisoft.reservation.application.fake.InMemoryReservationRepository(),
                new GroupAccess(groupRepository, membershipRepository),
                ReservationTestFixtures.FIXED_CLOCK);

        r1 = service.createRoom(groupId, OWNER_ID, new CreateRoomCommand("대회의실", 10, "3층"));
        r2 = service.createRoom(groupId, OWNER_ID, new CreateRoomCommand("소회의실", 4, "2층"));
        r3 = service.createRoom(otherGroupId, OTHER_GROUP_OWNER_ID, new CreateRoomCommand("타그룹실", 6, "7층"));
    }

    private void assertRoomUnchanged(Long roomId, String name, int capacity, String location) {
        Room found = roomRepository.findById(roomId).orElseThrow();
        assertThat(found.name()).isEqualTo(name);
        assertThat(found.capacity()).isEqualTo(capacity);
        assertThat(found.location()).isEqualTo(location);
    }

    private void assertR1Untouched() {
        assertRoomUnchanged(r1.id(), "대회의실", 10, "3층");
    }

    @Test
    @DisplayName("AC-4: 없는 회의실이면 외부인이 요청해도 ROOM_NOT_FOUND다")
    void 없는_회의실은_외부인이_요청해도_ROOM_NOT_FOUND다() {
        assertThatThrownBy(() -> service.updateRoom(
                ABSENT_ROOM_ID, OUTSIDER_ID, new UpdateRoomCommand("대회의실 B", 12, "5층")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(ReservationErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("AC-6: 그룹장이 수정하면 반환값과 저장소에서 다시 읽은 값이 새 값이고 id·groupId·방 개수는 그대로다")
    void 그룹장이_수정하면_저장소에도_새_값이_반영된다() {
        Room updated = service.updateRoom(r1.id(), OWNER_ID, new UpdateRoomCommand("대회의실 B", 12, "5층"));

        assertThat(updated.name()).isEqualTo("대회의실 B");
        assertThat(updated.capacity()).isEqualTo(12);
        assertThat(updated.location()).isEqualTo("5층");
        Room reread = roomRepository.findById(r1.id()).orElseThrow();
        assertThat(reread.name()).isEqualTo("대회의실 B");
        assertThat(reread.capacity()).isEqualTo(12);
        assertThat(reread.location()).isEqualTo("5층");
        assertThat(reread.id()).isEqualTo(r1.id());
        assertThat(reread.groupId()).isEqualTo(groupId);
        assertThat(roomRepository.findAllByGroupId(groupId)).hasSize(2);
    }

    @Test
    @DisplayName("AC-7: 일반 그룹원(MEMBER)도 회의실을 수정할 수 있다")
    void 일반_그룹원도_회의실을_수정할_수_있다() {
        service.updateRoom(r1.id(), MEMBER_ID, new UpdateRoomCommand("회의실 B", 8, "2층"));

        assertRoomUnchanged(r1.id(), "회의실 B", 8, "2층");
    }

    @Test
    @DisplayName("AC-18: 수정은 같은 그룹의 다른 방과 다른 그룹의 방에 영향을 주지 않는다")
    void 수정은_다른_방에_영향을_주지_않는다() {
        service.updateRoom(r1.id(), OWNER_ID, new UpdateRoomCommand("대회의실 B", 12, "5층"));

        assertRoomUnchanged(r2.id(), "소회의실", 4, "2층");
        assertRoomUnchanged(r3.id(), "타그룹실", 6, "7층");
    }

    @Test
    @DisplayName("AC-19: 초대 대기(PENDING) 사용자는 수정할 수 없고 방은 그대로다")
    void 초대_대기_사용자는_수정할_수_없다() {
        assertThatThrownBy(() -> service.updateRoom(
                r1.id(), PENDING_ID, new UpdateRoomCommand("대회의실 B", 12, "5층")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.NOT_GROUP_MEMBER);
        assertR1Untouched();
    }

    @Test
    @DisplayName("AC-19: 초대 거절(REJECTED) 사용자는 수정할 수 없고 방은 그대로다")
    void 초대_거절_사용자는_수정할_수_없다() {
        assertThatThrownBy(() -> service.updateRoom(
                r1.id(), REJECTED_ID, new UpdateRoomCommand("대회의실 B", 12, "5층")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.NOT_GROUP_MEMBER);
        assertR1Untouched();
    }

    @Test
    @DisplayName("AC-19: 외부인은 수정할 수 없고 방은 그대로다")
    void 외부인은_수정할_수_없다() {
        assertThatThrownBy(() -> service.updateRoom(
                r1.id(), OUTSIDER_ID, new UpdateRoomCommand("대회의실 B", 12, "5층")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.NOT_GROUP_MEMBER);
        assertR1Untouched();
    }

    @Test
    @DisplayName("AC-20: 외부인이 잘못된 값으로 요청해도 NOT_GROUP_MEMBER가 먼저고 방은 그대로다")
    void 외부인의_잘못된_값_요청은_권한_오류가_먼저다() {
        assertThatThrownBy(() -> service.updateRoom(
                r1.id(), OUTSIDER_ID, new UpdateRoomCommand("가".repeat(31), 0, "나".repeat(31))))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.NOT_GROUP_MEMBER);
        assertR1Untouched();
    }

    @Test
    @DisplayName("권한 있는 사용자의 이름 31자 요청은 INVALID_ROOM_NAME이고 방은 그대로다")
    void 권한_있는_사용자의_이름_31자_요청은_INVALID_ROOM_NAME이다() {
        assertThatThrownBy(() -> service.updateRoom(
                r1.id(), OWNER_ID, new UpdateRoomCommand("가".repeat(31), 12, "5층")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_ROOM_NAME);
        assertR1Untouched();
    }
}
