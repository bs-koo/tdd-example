package com.sqisoft.reservation.application;

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
import com.sqisoft.reservation.domain.ReservationException;
import com.sqisoft.reservation.domain.ReservationPolicy;
import com.sqisoft.reservation.domain.ReservationStatus;
import com.sqisoft.reservation.domain.Room;
import com.sqisoft.reservation.domain.TimeSlot;
import com.sqisoft.reservation.support.ReservationTestFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReservationServiceTest {

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
    @DisplayName("AC-17: 모든 규칙을 통과하는 요청은 id가 채워진 예약을 반환한다")
    void 정상_요청은_id가_채워진_예약을_반환한다() {
        Reservation result = service.reserve(command(1L, MEMBER_ID, "주간 회의", "09:00", "10:00"));

        assertThat(result.id()).isNotNull();
    }

    @Test
    @DisplayName("AC-17: 정상 요청으로 생성된 예약의 상태와 필드는 커맨드 값과 일치한다")
    void 정상_요청으로_생성된_예약의_필드는_커맨드_값과_일치한다() {
        Reservation result = service.reserve(command(1L, MEMBER_ID, "주간 회의", "09:00", "10:00"));

        assertThat(result.status()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(result.roomId()).isEqualTo(1L);
        assertThat(result.memberId()).isEqualTo(MEMBER_ID);
        assertThat(result.reserverName()).isEqualTo(ReservationTestFixtures.RESERVER);
        assertThat(result.purpose()).isEqualTo("주간 회의");
        assertThat(result.timeSlot()).isEqualTo(new TimeSlot(
                ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "09:00"),
                ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "10:00")));
    }

    @Test
    @DisplayName("AC-17: 정상 요청으로 생성된 예약은 저장소에서 findById로 다시 조회할 수 있다")
    void 정상_요청으로_생성된_예약은_저장소에서_다시_조회할_수_있다() {
        Reservation result = service.reserve(command(1L, MEMBER_ID, "주간 회의", "09:00", "10:00"));

        assertThat(reservationRepository.findById(result.id())).isPresent();
    }

    @Test
    @DisplayName("AC-2: 다른 회의실이면 동일 시간대여도 예약이 성공한다")
    void 다른_회의실이면_동일_시간대여도_예약이_성공한다() {
        seedActive(1L, MEMBER_ID, "09:00", "10:00");

        Reservation result = service.reserve(command(2L, MEMBER_ID, "다른 회의", "09:00", "10:00"));

        assertThat(result.roomId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("AC-3: 취소된 예약과 동일한 회의실·시간대를 요청해도 예약이 성공한다")
    void 취소된_예약과_동일한_회의실_시간대_요청은_성공한다() {
        LocalDateTime seededCreatedAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00");
        reservationRepository.seed(Reservation.restore(
                1L, 1L, MEMBER_ID, ReservationTestFixtures.RESERVER, "취소된 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                ReservationStatus.CANCELLED, seededCreatedAt));

        Reservation result = service.reserve(command(1L, MEMBER_ID, "새 회의", "09:00", "10:00"));

        assertThat(result.status()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("AC-1: 기존 ACTIVE 예약과 겹치는 시간대를 요청하면 OVERLAPPING_RESERVATION 예외를 던진다")
    void 겹치는_시간대_요청은_OVERLAPPING_RESERVATION_예외를_던진다() {
        seedActive(1L, MEMBER_ID, "09:00", "10:00");

        assertThatThrownBy(() -> service.reserve(command(1L, MEMBER_ID, "새 회의", "09:30", "10:30")))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.OVERLAPPING_RESERVATION);
    }

    @Test
    @DisplayName("AC-1(경계): 기존 예약 직후에 접하는 시간대 요청은 성공한다")
    void 직후_접하는_시간대_요청은_성공한다() {
        seedActive(1L, MEMBER_ID, "09:00", "10:00");

        Reservation result = service.reserve(command(1L, MEMBER_ID, "새 회의", "10:00", "11:00"));

        assertThat(result.status()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    @DisplayName("AC-19: 존재하지 않는 회의실로 요청하면 ROOM_NOT_FOUND 예외를 던진다")
    void 존재하지_않는_회의실로_요청하면_ROOM_NOT_FOUND_예외를_던진다() {
        assertThatThrownBy(() -> service.reserve(command(999L, MEMBER_ID, null, "09:00", "10:00")))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("승인된 그룹원이 아니면 예약할 수 없다")
    void 승인된_그룹원이_아니면_예약할_수_없다() {
        assertThatThrownBy(() -> service.reserve(command(1L, PENDING_MEMBER_ID, "주간 회의", "09:00", "10:00")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.NOT_GROUP_MEMBER);
    }

    @Test
    @DisplayName("남의 그룹 회의실은 예약할 수 없다")
    void 남의_그룹_회의실은_예약할_수_없다() {
        assertThatThrownBy(() -> service.reserve(
                command(OTHER_GROUP_ROOM_ID, MEMBER_ID, "주간 회의", "09:00", "10:00")))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(GroupErrorCode.NOT_GROUP_MEMBER);
    }

    @Test
    @DisplayName("그룹 권한이 없는 요청은 예약을 저장하지 않는다")
    void 그룹_권한이_없는_요청은_예약을_저장하지_않는다() {
        try {
            service.reserve(command(1L, PENDING_MEMBER_ID, "주간 회의", "09:00", "10:00"));
        } catch (AppException ignored) {
            // 거절 확인은 저장소 상태 검증으로 대체
        }

        assertThat(reservationRepository.findActiveByRoomIdAndDate(1L, ReservationTestFixtures.DEFAULT_DATE))
                .isEmpty();
    }

    @Test
    @DisplayName("검증 순서: 시간 규칙 위반과 그룹 권한 없음이 동시일 때 시간 규칙 오류가 먼저다")
    void 시간_규칙_위반이_그룹_권한보다_먼저_판정된다() {
        assertThatThrownBy(() -> service.reserve(command(1L, PENDING_MEMBER_ID, null, "09:15", "09:45")))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_TIME_UNIT);
    }

    @Test
    @DisplayName("검증 순서: 시간 규칙 위반과 회의실 부재가 동시일 때 시간 규칙 오류가 먼저다")
    void 시간_규칙_위반이_회의실_부재보다_먼저_판정된다() {
        assertThatThrownBy(() -> service.reserve(command(999L, MEMBER_ID, null, "09:15", "09:45")))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_TIME_UNIT);
    }

    @Test
    @DisplayName("정책 위임: 목적이 51자면 정책 위반이 그대로 전파되어 INVALID_PURPOSE_LENGTH 예외를 던진다")
    void 목적이_51자면_정책_위반이_그대로_전파된다() {
        assertThatThrownBy(() -> service.reserve(
                command(1L, MEMBER_ID, "가".repeat(51), "09:00", "10:00")))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.INVALID_PURPOSE_LENGTH);
    }

    @Test
    @DisplayName("예약자 본인이 아닌 사람이 취소하면 NOT_RESERVER 예외를 던진다")
    void 예약자_본인이_아닌_사람이_취소하면_NOT_RESERVER_예외를_던진다() {
        Reservation reserved = service.reserve(command(1L, MEMBER_ID, "주간 회의", "09:00", "10:00"));

        assertThatThrownBy(() -> service.cancel(reserved.id(), OTHER_MEMBER_ID))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.NOT_RESERVER);
    }

    @Test
    @DisplayName("같은 이름이지만 memberId가 다른 사람이 취소하면 NOT_RESERVER 예외를 던진다 — 취소 권한은 이름이 아니라 신원으로 판정한다")
    void 같은_이름이지만_memberId가_다르면_NOT_RESERVER_예외를_던진다() {
        LocalDateTime seededCreatedAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00");
        // 예약자명은 동명이인이라 완전히 같지만, 신원(memberId)은 MEMBER_ID 한 사람뿐이다.
        reservationRepository.seed(Reservation.restore(
                1L, 1L, MEMBER_ID, ReservationTestFixtures.RESERVER, "주간 회의",
                ReservationTestFixtures.defaultSlot("09:00", "10:00"),
                ReservationStatus.ACTIVE, seededCreatedAt));

        assertThat(reservationRepository.findById(1L).orElseThrow().reserverName())
                .isEqualTo(ReservationTestFixtures.RESERVER);
        assertThatThrownBy(() -> service.cancel(1L, OTHER_MEMBER_ID))
                .isInstanceOf(ReservationException.class)
                .extracting(e -> ((ReservationException) e).code())
                .isEqualTo(ReservationErrorCode.NOT_RESERVER);
    }

    private CreateReservationCommand command(Long roomId, Long memberId, String purpose,
            String startHHmm, String endHHmm) {
        return new CreateReservationCommand(
                roomId,
                memberId,
                ReservationTestFixtures.RESERVER,
                purpose,
                ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, startHHmm),
                ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, endHHmm));
    }

    private void seedActive(Long roomId, Long memberId, String startHHmm, String endHHmm) {
        LocalDateTime seededCreatedAt = ReservationTestFixtures.at(ReservationTestFixtures.DEFAULT_DATE, "08:00");
        reservationRepository.seed(Reservation.restore(
                1L, roomId, memberId, ReservationTestFixtures.RESERVER, "기존 회의",
                ReservationTestFixtures.defaultSlot(startHHmm, endHHmm),
                ReservationStatus.ACTIVE, seededCreatedAt));
    }
}
