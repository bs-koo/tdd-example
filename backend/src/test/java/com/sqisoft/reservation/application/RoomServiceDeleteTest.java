package com.sqisoft.reservation.application;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sqisoft.reservation.application.fake.InMemoryGroupMembershipRepository;
import com.sqisoft.reservation.application.fake.InMemoryGroupRepository;
import com.sqisoft.reservation.application.fake.InMemoryReservationRepository;
import com.sqisoft.reservation.application.fake.InMemoryRoomRepository;
import com.sqisoft.reservation.domain.AppException;
import com.sqisoft.reservation.domain.Group;
import com.sqisoft.reservation.domain.GroupErrorCode;
import com.sqisoft.reservation.domain.GroupMembership;
import com.sqisoft.reservation.domain.Reservation;
import com.sqisoft.reservation.domain.ReservationErrorCode;
import com.sqisoft.reservation.domain.ReservationStatus;
import com.sqisoft.reservation.domain.Room;
import com.sqisoft.reservation.support.ReservationTestFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoomServiceDeleteTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 10, 10, 0);
    private static final Clock CLOCK = Clock.fixed(
            NOW.atZone(ReservationTestFixtures.ZONE).toInstant(), ReservationTestFixtures.ZONE);
    private static final LocalDate TODAY = NOW.toLocalDate();
    private static final LocalDate TOMORROW = TODAY.plusDays(1);
    private static final LocalDate YESTERDAY = TODAY.minusDays(1);

    private static final Long OWNER_ID = ReservationTestFixtures.MEMBER_ID;
    private static final Long OTHER_GROUP_OWNER_ID = ReservationTestFixtures.OTHER_MEMBER_ID;
    private static final Long PENDING_ID = 30L;
    private static final Long MEMBER_ID = 40L;
    private static final Long REJECTED_ID = 50L;
    private static final Long OUTSIDER_ID = 60L;
    private static final Long ABSENT_ROOM_ID = 999999L;

    private InMemoryRoomRepository roomRepository;
    private InMemoryReservationRepository reservationRepository;
    private RoomService service;
    private Long groupId;
    private long nextReservationId = 1L;

    @BeforeEach
    void setUp() {
        roomRepository = new InMemoryRoomRepository();
        reservationRepository = new InMemoryReservationRepository();
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
        membershipRepository.save(GroupMembership.owner(otherGroup.id(), OTHER_GROUP_OWNER_ID, NOW));

        service = new RoomService(
                roomRepository, reservationRepository,
                new GroupAccess(groupRepository, membershipRepository), CLOCK);
    }

    private Room newRoom(String name) {
        return service.createRoom(groupId, OWNER_ID, new CreateRoomCommand(name, 10, "3층"));
    }

    private Reservation reserve(Long roomId, LocalDate date, String start, String end, ReservationStatus status) {
        Reservation reservation = Reservation.restore(
                nextReservationId++, roomId, OWNER_ID, ReservationTestFixtures.RESERVER, "회의",
                ReservationTestFixtures.slot(date, start, end), status, NOW.minusDays(5));
        reservationRepository.seed(reservation);
        return reservation;
    }

    private Reservation reserveActive(Long roomId, LocalDate date, String start, String end) {
        return reserve(roomId, date, start, end, ReservationStatus.ACTIVE);
    }

    private Object deleteErrorCode(Long roomId, Long memberId) {
        try {
            service.deleteRoom(roomId, memberId);
        } catch (AppException e) {
            return e.code();
        }
        return null;
    }

    @Test
    @DisplayName("AC-5: 없는 회의실은 외부인이 삭제 요청해도 ROOM_NOT_FOUND다")
    void 없는_회의실은_외부인이_삭제_요청해도_ROOM_NOT_FOUND다() {
        assertThatThrownBy(() -> service.deleteRoom(ABSENT_ROOM_ID, OUTSIDER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(ReservationErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("AC-21: 그룹장이 예약 없는 방을 삭제하면 그 방만 목록에서 빠진다")
    void 그룹장이_예약_없는_방을_삭제하면_그_방만_목록에서_빠진다() {
        Room r1 = newRoom("대회의실");
        Room r2 = newRoom("소회의실");

        assertThatNoException().isThrownBy(() -> service.deleteRoom(r1.id(), OWNER_ID));

        assertThat(service.findByGroup(groupId, OWNER_ID)).extracting(Room::id).containsExactly(r2.id());
    }

    @Test
    @DisplayName("AC-22: 일반 그룹원(MEMBER)은 삭제할 수 없고 NOT_GROUP_OWNER이며 방은 남는다")
    void 일반_그룹원은_삭제할_수_없다() {
        Room r1 = newRoom("대회의실");

        assertThatThrownBy(() -> service.deleteRoom(r1.id(), MEMBER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.NOT_GROUP_OWNER);
        assertThat(roomRepository.findById(r1.id())).isPresent();
    }

    @Test
    @DisplayName("AC-23: 초대 대기(PENDING) 사용자는 삭제할 수 없고 NOT_GROUP_OWNER이며 방은 남는다")
    void 초대_대기_사용자는_삭제할_수_없다() {
        Room r1 = newRoom("대회의실");

        assertThatThrownBy(() -> service.deleteRoom(r1.id(), PENDING_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.NOT_GROUP_OWNER);
        assertThat(roomRepository.findById(r1.id())).isPresent();
    }

    @Test
    @DisplayName("AC-23: 초대 거절(REJECTED) 사용자는 삭제할 수 없고 NOT_GROUP_OWNER이며 방은 남는다")
    void 초대_거절_사용자는_삭제할_수_없다() {
        Room r1 = newRoom("대회의실");

        assertThatThrownBy(() -> service.deleteRoom(r1.id(), REJECTED_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.NOT_GROUP_OWNER);
        assertThat(roomRepository.findById(r1.id())).isPresent();
    }

    @Test
    @DisplayName("AC-23: 외부인은 삭제할 수 없고 NOT_GROUP_OWNER이며 방은 남는다")
    void 외부인은_삭제할_수_없다() {
        Room r1 = newRoom("대회의실");

        assertThatThrownBy(() -> service.deleteRoom(r1.id(), OUTSIDER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.NOT_GROUP_OWNER);
        assertThat(roomRepository.findById(r1.id())).isPresent();
    }

    @Test
    @DisplayName("AC-24: 앞으로의 ACTIVE 예약이 있는 방은 그룹장도 삭제할 수 없고 방과 예약은 그대로다")
    void 앞으로의_ACTIVE_예약이_있는_방은_삭제할_수_없다() {
        Room r = newRoom("대회의실");
        Reservation reservation = reserveActive(r.id(), TOMORROW, "09:00", "10:00");

        assertThatThrownBy(() -> service.deleteRoom(r.id(), OWNER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(ReservationErrorCode.ROOM_HAS_ACTIVE_RESERVATIONS);
        assertThat(service.findByGroup(groupId, OWNER_ID)).extracting(Room::id).containsExactly(r.id());
        assertThat(reservationRepository.findById(reservation.id()).orElseThrow().status())
                .isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("AC-25 경계: 종료 시각이 기준 시각과 같은 ACTIVE 예약만 있는 방은 삭제된다")
    void 종료가_기준_시각과_같은_예약만_있는_방은_삭제된다() {
        Room ra = newRoom("Ra");
        reserveActive(ra.id(), TODAY, "09:00", "10:00");

        assertThatNoException().isThrownBy(() -> service.deleteRoom(ra.id(), OWNER_ID));

        assertThat(roomRepository.findById(ra.id())).isEmpty();
    }

    @Test
    @DisplayName("AC-25 경계: 진행 중인 ACTIVE 예약이 있는 방은 삭제할 수 없다")
    void 진행_중인_예약이_있는_방은_삭제할_수_없다() {
        Room rb = newRoom("Rb");
        reserveActive(rb.id(), TODAY, "09:30", "10:30");

        assertThatThrownBy(() -> service.deleteRoom(rb.id(), OWNER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(ReservationErrorCode.ROOM_HAS_ACTIVE_RESERVATIONS);
        assertThat(roomRepository.findById(rb.id())).isPresent();
    }

    @Test
    @DisplayName("AC-25: 미래의 CANCELLED 예약만 있는 방은 삭제된다")
    void 미래의_CANCELLED_예약만_있는_방은_삭제된다() {
        Room rc = newRoom("Rc");
        reserve(rc.id(), TOMORROW, "09:00", "10:00", ReservationStatus.CANCELLED);

        assertThatNoException().isThrownBy(() -> service.deleteRoom(rc.id(), OWNER_ID));

        assertThat(roomRepository.findById(rc.id())).isEmpty();
    }

    @Test
    @DisplayName("AC-25: 어제의 ACTIVE 예약만 있는 방은 삭제된다")
    void 어제의_ACTIVE_예약만_있는_방은_삭제된다() {
        Room rd = newRoom("Rd");
        reserveActive(rd.id(), YESTERDAY, "09:00", "10:00");

        assertThatNoException().isThrownBy(() -> service.deleteRoom(rd.id(), OWNER_ID));

        assertThat(roomRepository.findById(rd.id())).isEmpty();
    }

    @Test
    @DisplayName("AC-25: 한 방의 앞으로의 예약은 다른 방의 삭제를 막지 않는다")
    void 다른_방의_앞으로의_예약은_삭제를_막지_않는다() {
        Room busy = newRoom("바쁜방");
        Room free = newRoom("빈방");
        reserveActive(busy.id(), TOMORROW, "09:00", "10:00");

        assertThatNoException().isThrownBy(() -> service.deleteRoom(free.id(), OWNER_ID));

        assertThat(roomRepository.findById(free.id())).isEmpty();
        assertThat(roomRepository.findById(busy.id())).isPresent();
    }

    @Test
    @DisplayName("AC-26: 예약을 취소한 뒤에는 그룹장이 방을 삭제할 수 있다")
    void 예약을_취소한_뒤에는_방을_삭제할_수_있다() {
        Room r = newRoom("대회의실");
        Reservation reservation = reserveActive(r.id(), TOMORROW, "09:00", "10:00");
        reservation.cancel(OWNER_ID);
        reservationRepository.save(reservation);

        assertThatNoException().isThrownBy(() -> service.deleteRoom(r.id(), OWNER_ID));

        assertThat(service.findByGroup(groupId, OWNER_ID)).extracting(Room::id).doesNotContain(r.id());
    }

    @Test
    @DisplayName("AC-27: 외부인은 앞으로의 예약이 있어도 NOT_GROUP_OWNER를 받고 방과 예약은 그대로다")
    void 외부인은_앞으로의_예약이_있어도_NOT_GROUP_OWNER를_받는다() {
        Room r = newRoom("대회의실");
        Reservation reservation = reserveActive(r.id(), TOMORROW, "09:00", "10:00");

        assertThat(deleteErrorCode(r.id(), OUTSIDER_ID)).isEqualTo(GroupErrorCode.NOT_GROUP_OWNER);
        assertThat(roomRepository.findById(r.id())).isPresent();
        assertThat(reservationRepository.findById(reservation.id()).orElseThrow().status())
                .isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("AC-27: 일반 그룹원은 앞으로의 예약이 있어도 NOT_GROUP_OWNER를 받고 방과 예약은 그대로다")
    void 일반_그룹원은_앞으로의_예약이_있어도_NOT_GROUP_OWNER를_받는다() {
        Room r = newRoom("대회의실");
        Reservation reservation = reserveActive(r.id(), TOMORROW, "09:00", "10:00");

        assertThat(deleteErrorCode(r.id(), MEMBER_ID)).isEqualTo(GroupErrorCode.NOT_GROUP_OWNER);
        assertThat(roomRepository.findById(r.id())).isPresent();
        assertThat(reservationRepository.findById(reservation.id()).orElseThrow().status())
                .isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("AC-28: 방을 삭제해도 지난 ACTIVE 예약과 CANCELLED 예약 행은 상태·roomId 그대로 남는다")
    void 방을_삭제해도_예약_행은_그대로_남는다() {
        Room r = newRoom("대회의실");
        Reservation past = reserveActive(r.id(), YESTERDAY, "09:00", "10:00");
        Reservation cancelled = reserve(r.id(), TOMORROW, "09:00", "10:00", ReservationStatus.CANCELLED);

        service.deleteRoom(r.id(), OWNER_ID);

        Reservation keptPast = reservationRepository.findById(past.id()).orElseThrow();
        Reservation keptCancelled = reservationRepository.findById(cancelled.id()).orElseThrow();
        assertThat(keptPast.status()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(keptPast.roomId()).isEqualTo(r.id());
        assertThat(keptCancelled.status()).isEqualTo(ReservationStatus.CANCELLED);
        assertThat(keptCancelled.roomId()).isEqualTo(r.id());
    }

    @Test
    @DisplayName("AC-29: 삭제된 방을 수정하면 ROOM_NOT_FOUND다")
    void 삭제된_방을_수정하면_ROOM_NOT_FOUND다() {
        Room r = newRoom("대회의실");
        service.deleteRoom(r.id(), OWNER_ID);

        assertThatThrownBy(() -> service.updateRoom(r.id(), OWNER_ID, new UpdateRoomCommand("새 이름", 5, "1층")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(ReservationErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("AC-29: 삭제된 방을 requireAccessibleRoom으로 조회하면 ROOM_NOT_FOUND다")
    void 삭제된_방을_조회하면_ROOM_NOT_FOUND다() {
        Room r = newRoom("대회의실");
        service.deleteRoom(r.id(), OWNER_ID);

        assertThatThrownBy(() -> service.requireAccessibleRoom(r.id(), MEMBER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(ReservationErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("AC-29: 이미 삭제된 방을 다시 삭제하면 ROOM_NOT_FOUND다")
    void 이미_삭제된_방을_다시_삭제하면_ROOM_NOT_FOUND다() {
        Room r = newRoom("대회의실");
        service.deleteRoom(r.id(), OWNER_ID);

        assertThatThrownBy(() -> service.deleteRoom(r.id(), OWNER_ID))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(ReservationErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("AC-30: 마지막 방을 삭제하면 목록이 비고 이어서 새 방을 만들 수 있다")
    void 마지막_방을_삭제하면_목록이_비고_새_방을_만들_수_있다() {
        Room only = newRoom("대회의실");

        service.deleteRoom(only.id(), OWNER_ID);

        assertThat(service.findByGroup(groupId, OWNER_ID)).isEmpty();
        Room created = service.createRoom(groupId, OWNER_ID, new CreateRoomCommand("신규", 4, "1층"));
        assertThat(created.id()).isNotNull();
        assertThat(service.findByGroup(groupId, OWNER_ID)).extracting(Room::name).containsExactly("신규");
    }
}
