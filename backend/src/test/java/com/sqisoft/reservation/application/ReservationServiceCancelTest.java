package com.sqisoft.reservation.application;

import java.time.LocalDateTime;
import java.util.List;

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
import com.sqisoft.reservation.domain.ReservationException;
import com.sqisoft.reservation.domain.ReservationPolicy;
import com.sqisoft.reservation.domain.ReservationStatus;
import com.sqisoft.reservation.domain.Room;
import com.sqisoft.reservation.support.ReservationTestFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationServiceCancelTest {

    private static final LocalDateTime NOW =
            ReservationTestFixtures.at(ReservationTestFixtures.TODAY, "09:00");
    private static final Long GROUP_ID = ReservationTestFixtures.GROUP_ID;
    private static final Long MEMBER_ID = ReservationTestFixtures.MEMBER_ID;
    private static final Long OTHER_MEMBER_ID = ReservationTestFixtures.OTHER_MEMBER_ID;
    private static final Long PENDING_MEMBER_ID = 30L;
    private static final Long OTHER_GROUP_ID = 2L;
    private static final Long OTHER_GROUP_ROOM_ID = 3L;

    private InMemoryReservationRepository reservationRepository;
    private ReservationService service;

    @BeforeEach
    void setUp() {
        reservationRepository = new InMemoryReservationRepository();

        InMemoryGroupRepository groupRepository = new InMemoryGroupRepository();
        InMemoryGroupMembershipRepository membershipRepository = new InMemoryGroupMembershipRepository();

        Group group = groupRepository.save(Group.create("우리 팀", MEMBER_ID, NOW));
        membershipRepository.save(GroupMembership.owner(group.id(), MEMBER_ID, NOW));
        membershipRepository.save(GroupMembership.invite(group.id(), PENDING_MEMBER_ID, NOW));

        Group otherGroup = groupRepository.save(Group.create("다른 팀", OTHER_MEMBER_ID, NOW));
        membershipRepository.save(GroupMembership.owner(otherGroup.id(), OTHER_MEMBER_ID, NOW));

        InMemoryRoomRepository roomRepository = new InMemoryRoomRepository(
                Room.restore(1L, GROUP_ID, "대회의실", 20, "3층"),
                Room.restore(2L, GROUP_ID, "중회의실", 10, "3층"),
                Room.restore(OTHER_GROUP_ROOM_ID, OTHER_GROUP_ID, "타그룹 회의실", 8, "5층"));

        RoomService roomService = new RoomService(
                roomRepository, reservationRepository,
                new GroupAccess(groupRepository, membershipRepository),
                ReservationTestFixtures.FIXED_CLOCK);

        service = new ReservationService(
                reservationRepository,
                roomService,
                new ReservationPolicy(ReservationTestFixtures.FIXED_CLOCK),
                ReservationTestFixtures.FIXED_CLOCK);
    }

    @Test
    @DisplayName("AC-23: 존재하지 않는 예약을 취소하면 RESERVATION_NOT_FOUND 예외를 던진다")
    void 존재하지_않는_예약을_취소하면_RESERVATION_NOT_FOUND_예외를_던진다() {
        assertThatThrownBy(() -> service.cancel(999L, MEMBER_ID))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.RESERVATION_NOT_FOUND);
    }

    @Test
    @DisplayName("도메인 위임: 타인이 취소하면 NOT_RESERVER 예외가 그대로 전파된다")
    void 타인이_취소하면_NOT_RESERVER_예외가_그대로_전파된다() {
        seed(1L, ReservationStatus.ACTIVE);

        assertThatThrownBy(() -> service.cancel(1L, OTHER_MEMBER_ID))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.NOT_RESERVER);
    }

    @Test
    @DisplayName("타인의 취소 시도가 거절되면 저장소의 예약은 ACTIVE로 유지된다")
    void 타인의_취소_시도가_거절되면_저장소의_예약은_ACTIVE로_유지된다() {
        seed(1L, ReservationStatus.ACTIVE);

        try {
            service.cancel(1L, OTHER_MEMBER_ID);
        } catch (ReservationException ignored) {
            // 거절 확인은 저장소 상태 검증으로 대체
        }

        assertThat(reservationRepository.findById(1L))
                .get()
                .extracting(Reservation::status)
                .isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("도메인 위임: 이미 취소된 예약을 본인이 취소하면 ALREADY_CANCELLED 예외가 그대로 전파된다")
    void 이미_취소된_예약을_본인이_취소하면_ALREADY_CANCELLED_예외가_그대로_전파된다() {
        seed(1L, ReservationStatus.CANCELLED);

        assertThatThrownBy(() -> service.cancel(1L, MEMBER_ID))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.ALREADY_CANCELLED);
    }

    @Test
    @DisplayName("본인이 취소하면 예외 없이 저장소에 CANCELLED 상태로 영속화된다")
    void 본인이_취소하면_저장소에_CANCELLED_상태로_영속화된다() {
        seed(1L, ReservationStatus.ACTIVE);

        assertThatNoException().isThrownBy(() -> service.cancel(1L, MEMBER_ID));

        assertThat(reservationRepository.findById(1L))
                .get()
                .extracting(Reservation::status)
                .isEqualTo(ReservationStatus.CANCELLED);
    }

    @Test
    @DisplayName("AC-24: 취소 후 동일 회의실·동일 시간대로 재예약하면 성공한다")
    void 취소_후_동일_시간대로_재예약하면_성공한다() {
        Reservation first = service.reserve(command("기존 회의"));

        service.cancel(first.id(), MEMBER_ID);

        Reservation second = service.reserve(command("새 회의"));

        assertThat(second.status()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("AC-26b: 존재하지 않는 회의실로 조회하면 ROOM_NOT_FOUND 예외를 던진다")
    void 존재하지_않는_회의실로_조회하면_ROOM_NOT_FOUND_예외를_던진다() {
        assertThatThrownBy(() -> service.findActiveByRoomAndDate(
                999L, MEMBER_ID, ReservationTestFixtures.DEFAULT_DATE))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("승인된 그룹원이 아니면 회의실의 예약을 조회할 수 없다")
    void 승인된_그룹원이_아니면_회의실의_예약을_조회할_수_없다() {
        assertThatThrownBy(() -> service.findActiveByRoomAndDate(
                ReservationTestFixtures.ROOM_ID, PENDING_MEMBER_ID, ReservationTestFixtures.DEFAULT_DATE))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.NOT_GROUP_MEMBER);
    }

    @Test
    @DisplayName("남의 그룹 회의실의 예약은 조회할 수 없다")
    void 남의_그룹_회의실의_예약은_조회할_수_없다() {
        assertThatThrownBy(() -> service.findActiveByRoomAndDate(
                OTHER_GROUP_ROOM_ID, MEMBER_ID, ReservationTestFixtures.DEFAULT_DATE))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.NOT_GROUP_MEMBER);
    }

    @Test
    @DisplayName("회의실과 날짜로 조회하면 ACTIVE 예약만 반환하고 CANCELLED 예약은 제외한다")
    void 회의실과_날짜로_조회하면_ACTIVE_예약만_반환한다() {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "07:00");
        reservationRepository.seed(
                Reservation.restore(1L, ReservationTestFixtures.ROOM_ID, MEMBER_ID,
                        ReservationTestFixtures.RESERVER, "회의1",
                        ReservationTestFixtures.defaultSlot("09:00", "10:00"), ReservationStatus.ACTIVE, createdAt),
                Reservation.restore(2L, ReservationTestFixtures.ROOM_ID, MEMBER_ID,
                        ReservationTestFixtures.RESERVER, "회의2",
                        ReservationTestFixtures.defaultSlot("11:00", "12:00"), ReservationStatus.ACTIVE, createdAt),
                Reservation.restore(3L, ReservationTestFixtures.ROOM_ID, MEMBER_ID,
                        ReservationTestFixtures.RESERVER, "취소된 회의",
                        ReservationTestFixtures.defaultSlot("13:00", "14:00"), ReservationStatus.CANCELLED, createdAt));

        List<Reservation> result = service.findActiveByRoomAndDate(
                ReservationTestFixtures.ROOM_ID, MEMBER_ID, ReservationTestFixtures.DEFAULT_DATE);

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("AC-28: 예약이 없는 날짜로 조회하면 빈 리스트를 반환한다")
    void 예약이_없는_날짜로_조회하면_빈_리스트를_반환한다() {
        List<Reservation> result = service.findActiveByRoomAndDate(
                ReservationTestFixtures.ROOM_ID, MEMBER_ID, ReservationTestFixtures.DEFAULT_DATE);

        assertThat(result).isEmpty();
    }

    private CreateReservationCommand command(String purpose) {
        return new CreateReservationCommand(
                ReservationTestFixtures.ROOM_ID,
                MEMBER_ID,
                ReservationTestFixtures.RESERVER,
                purpose,
                ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00"),
                ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "10:00"));
    }

    private void seed(Long id, ReservationStatus status) {
        LocalDateTime createdAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00");
        reservationRepository.seed(Reservation.restore(
                id, ReservationTestFixtures.ROOM_ID, MEMBER_ID, ReservationTestFixtures.RESERVER, "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"), status, createdAt));
    }
}
