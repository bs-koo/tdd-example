package com.sqisoft.reservation.application;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.sqisoft.reservation.application.fake.InMemoryGroupMembershipRepository;
import com.sqisoft.reservation.application.fake.InMemoryGroupRepository;
import com.sqisoft.reservation.application.fake.InMemoryMemberRepository;
import com.sqisoft.reservation.domain.AppException;
import com.sqisoft.reservation.domain.AuthErrorCode;
import com.sqisoft.reservation.domain.GroupErrorCode;
import com.sqisoft.reservation.domain.GroupRole;
import com.sqisoft.reservation.domain.Member;
import com.sqisoft.reservation.domain.MembershipStatus;
import com.sqisoft.reservation.support.ReservationTestFixtures;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GroupServiceTest {

    private InMemoryGroupRepository groupRepository;
    private InMemoryGroupMembershipRepository membershipRepository;
    private InMemoryMemberRepository memberRepository;
    private GroupService service;

    private Long ownerId;
    private Long inviteeId;
    private Long strangerId;

    @BeforeEach
    void setUp() {
        groupRepository = new InMemoryGroupRepository();
        membershipRepository = new InMemoryGroupMembershipRepository();
        memberRepository = new InMemoryMemberRepository();
        service = new GroupService(groupRepository, membershipRepository, memberRepository,
                new GroupAccess(groupRepository, membershipRepository),
                ReservationTestFixtures.FIXED_CLOCK);

        ownerId = register("owner@sqisoft.com", "김본승");
        inviteeId = register("invitee@sqisoft.com", "박초대");
        strangerId = register("stranger@sqisoft.com", "최무관");
    }

    private Long register(String email, String name) {
        return memberRepository.save(Member.register(email, name, "hashed",
                ReservationTestFixtures.at(ReservationTestFixtures.TODAY, "09:00"))).id();
    }

    private Long createGroupOwnedByOwner() {
        return service.createGroup(ownerId, "개발팀").group().id();
    }

    private GroupErrorCode groupCodeOf(Throwable e) {
        return (GroupErrorCode) ((AppException) e).code();
    }

    // ── 그룹 생성 ──

    @Test
    @DisplayName("그룹을 만들면 생성자가 OWNER이자 APPROVED 멤버가 된다")
    void 생성자는_승인된_그룹장이_된다() {
        GroupView view = service.createGroup(ownerId, "개발팀");

        assertThat(view.group().name()).isEqualTo("개발팀");
        assertThat(view.group().ownerId()).isEqualTo(ownerId);
        assertThat(view.role()).isEqualTo(GroupRole.OWNER);
        assertThat(view.status()).isEqualTo(MembershipStatus.APPROVED);
    }

    @Test
    @DisplayName("그룹 이름이 잘못되면 그룹도 멤버십도 만들어지지 않는다")
    void 이름이_잘못되면_아무것도_만들어지지_않는다() {
        assertThatThrownBy(() -> service.createGroup(ownerId, "  "))
                .isInstanceOf(AppException.class)
                .extracting(this::groupCodeOf)
                .isEqualTo(GroupErrorCode.INVALID_GROUP_NAME);

        assertThat(membershipRepository.findAllByMemberId(ownerId)).isEmpty();
    }

    // ── 내 그룹 조회 ──

    @Test
    @DisplayName("내가 승인된 그룹만 id 오름차순으로 반환한다")
    void 승인된_그룹만_오름차순으로_반환한다() {
        Long first = service.createGroup(ownerId, "개발팀").group().id();
        Long second = service.createGroup(ownerId, "기획팀").group().id();

        List<GroupView> groups = service.findMyGroups(ownerId);

        assertThat(groups).extracting(view -> view.group().id()).containsExactly(first, second);
    }

    @Test
    @DisplayName("아직 수락하지 않은 초대는 내 그룹 목록에 나타나지 않는다")
    void 대기_중인_초대는_그룹_목록에_없다() {
        Long groupId = createGroupOwnedByOwner();
        service.invite(groupId, ownerId, "invitee@sqisoft.com");

        assertThat(service.findMyGroups(inviteeId)).isEmpty();
    }

    @Test
    @DisplayName("거절한 초대는 내 그룹 목록에 나타나지 않는다")
    void 거절한_초대는_그룹_목록에_없다() {
        Long groupId = createGroupOwnedByOwner();
        Long membershipId = service.invite(groupId, ownerId, "invitee@sqisoft.com").membership().id();
        service.reject(membershipId, inviteeId);

        assertThat(service.findMyGroups(inviteeId)).isEmpty();
    }

    @Test
    @DisplayName("어느 그룹에도 속하지 않으면 빈 목록을 반환한다")
    void 소속이_없으면_빈_목록이다() {
        assertThat(service.findMyGroups(strangerId)).isEmpty();
    }

    // ── 초대 ──

    @Test
    @DisplayName("그룹장이 초대하면 대기 상태의 MEMBER 멤버십이 생긴다")
    void 그룹장의_초대는_대기_멤버십을_만든다() {
        Long groupId = createGroupOwnedByOwner();

        GroupMemberView invited = service.invite(groupId, ownerId, "invitee@sqisoft.com");

        assertThat(invited.member().id()).isEqualTo(inviteeId);
        assertThat(invited.membership().role()).isEqualTo(GroupRole.MEMBER);
        assertThat(invited.membership().status()).isEqualTo(MembershipStatus.PENDING);
    }

    @Test
    @DisplayName("초대 이메일은 대소문자를 구분하지 않는다")
    void 초대_이메일은_대소문자를_구분하지_않는다() {
        Long groupId = createGroupOwnedByOwner();

        GroupMemberView invited = service.invite(groupId, ownerId, "INVITEE@SQISOFT.COM");

        assertThat(invited.member().id()).isEqualTo(inviteeId);
    }

    @Test
    @DisplayName("그룹장이 아닌 사람은 초대할 수 없다")
    void 그룹장이_아니면_초대할_수_없다() {
        Long groupId = createGroupOwnedByOwner();

        assertThatThrownBy(() -> service.invite(groupId, strangerId, "invitee@sqisoft.com"))
                .isInstanceOf(AppException.class)
                .extracting(this::groupCodeOf)
                .isEqualTo(GroupErrorCode.NOT_GROUP_OWNER);
    }

    @Test
    @DisplayName("승인된 일반 그룹원도 다른 사람을 초대할 수 없다 — 초대 권한은 그룹장 전용이다")
    void 일반_그룹원은_초대할_수_없다() {
        Long groupId = createGroupOwnedByOwner();
        Long membershipId = service.invite(groupId, ownerId, "invitee@sqisoft.com").membership().id();
        service.accept(membershipId, inviteeId);

        assertThatThrownBy(() -> service.invite(groupId, inviteeId, "stranger@sqisoft.com"))
                .isInstanceOf(AppException.class)
                .extracting(this::groupCodeOf)
                .isEqualTo(GroupErrorCode.NOT_GROUP_OWNER);
    }

    @Test
    @DisplayName("존재하지 않는 그룹에 초대하면 GROUP_NOT_FOUND를 던진다")
    void 없는_그룹에는_초대할_수_없다() {
        assertThatThrownBy(() -> service.invite(999L, ownerId, "invitee@sqisoft.com"))
                .isInstanceOf(AppException.class)
                .extracting(this::groupCodeOf)
                .isEqualTo(GroupErrorCode.GROUP_NOT_FOUND);
    }

    @Test
    @DisplayName("가입하지 않은 이메일로 초대하면 MEMBER_NOT_FOUND를 던진다")
    void 가입하지_않은_이메일은_초대할_수_없다() {
        Long groupId = createGroupOwnedByOwner();

        assertThatThrownBy(() -> service.invite(groupId, ownerId, "nobody@sqisoft.com"))
                .isInstanceOf(AppException.class)
                .extracting(e -> ((AppException) e).code())
                .isEqualTo(AuthErrorCode.MEMBER_NOT_FOUND);
    }

    @Test
    @DisplayName("자기 자신은 초대할 수 없다")
    void 자기_자신은_초대할_수_없다() {
        Long groupId = createGroupOwnedByOwner();

        assertThatThrownBy(() -> service.invite(groupId, ownerId, "owner@sqisoft.com"))
                .isInstanceOf(AppException.class)
                .extracting(this::groupCodeOf)
                .isEqualTo(GroupErrorCode.SELF_INVITATION);
    }

    @Test
    @DisplayName("이미 초대한 사람을 다시 초대하면 ALREADY_INVITED를 던진다")
    void 대기_중인_사람은_다시_초대할_수_없다() {
        Long groupId = createGroupOwnedByOwner();
        service.invite(groupId, ownerId, "invitee@sqisoft.com");

        assertThatThrownBy(() -> service.invite(groupId, ownerId, "invitee@sqisoft.com"))
                .isInstanceOf(AppException.class)
                .extracting(this::groupCodeOf)
                .isEqualTo(GroupErrorCode.ALREADY_INVITED);
    }

    @Test
    @DisplayName("이미 참여 중인 사람을 초대하면 ALREADY_INVITED를 던진다")
    void 참여_중인_사람은_다시_초대할_수_없다() {
        Long groupId = createGroupOwnedByOwner();
        Long membershipId = service.invite(groupId, ownerId, "invitee@sqisoft.com").membership().id();
        service.accept(membershipId, inviteeId);

        assertThatThrownBy(() -> service.invite(groupId, ownerId, "invitee@sqisoft.com"))
                .isInstanceOf(AppException.class)
                .extracting(this::groupCodeOf)
                .isEqualTo(GroupErrorCode.ALREADY_INVITED);
    }

    @Test
    @DisplayName("거절했던 사람은 다시 초대할 수 있고, 새 멤버십을 만들지 않고 기존 것을 되살린다")
    void 거절한_사람은_다시_초대할_수_있다() {
        Long groupId = createGroupOwnedByOwner();
        Long membershipId = service.invite(groupId, ownerId, "invitee@sqisoft.com").membership().id();
        service.reject(membershipId, inviteeId);

        GroupMemberView reinvited = service.invite(groupId, ownerId, "invitee@sqisoft.com");

        assertThat(reinvited.membership().id()).isEqualTo(membershipId);
        assertThat(reinvited.membership().status()).isEqualTo(MembershipStatus.PENDING);
        assertThat(membershipRepository.findAllByGroupId(groupId)).hasSize(2);
    }

    // ── 그룹원 목록 ──

    @Test
    @DisplayName("승인된 그룹원은 대기 중인 사람까지 포함한 전체 명단을 membershipId 오름차순으로 본다")
    void 승인된_그룹원은_전체_명단을_본다() {
        Long groupId = createGroupOwnedByOwner();
        service.invite(groupId, ownerId, "invitee@sqisoft.com");

        List<GroupMemberView> members = service.findMembers(groupId, ownerId);

        assertThat(members).extracting(view -> view.member().id()).containsExactly(ownerId, inviteeId);
        assertThat(members).extracting(view -> view.membership().status())
                .containsExactly(MembershipStatus.APPROVED, MembershipStatus.PENDING);
        assertThat(members).extracting(view -> view.member().name()).containsExactly("김본승", "박초대");
    }

    @Test
    @DisplayName("아직 수락하지 않은 사람은 그룹원 명단을 볼 수 없다")
    void 대기_중인_사람은_명단을_볼_수_없다() {
        Long groupId = createGroupOwnedByOwner();
        service.invite(groupId, ownerId, "invitee@sqisoft.com");

        assertThatThrownBy(() -> service.findMembers(groupId, inviteeId))
                .isInstanceOf(AppException.class)
                .extracting(this::groupCodeOf)
                .isEqualTo(GroupErrorCode.NOT_GROUP_MEMBER);
    }

    @Test
    @DisplayName("그룹과 무관한 사람은 그룹원 명단을 볼 수 없다")
    void 무관한_사람은_명단을_볼_수_없다() {
        Long groupId = createGroupOwnedByOwner();

        assertThatThrownBy(() -> service.findMembers(groupId, strangerId))
                .isInstanceOf(AppException.class)
                .extracting(this::groupCodeOf)
                .isEqualTo(GroupErrorCode.NOT_GROUP_MEMBER);
    }

    @Test
    @DisplayName("존재하지 않는 그룹의 명단을 조회하면 GROUP_NOT_FOUND를 던진다")
    void 없는_그룹의_명단은_조회할_수_없다() {
        assertThatThrownBy(() -> service.findMembers(999L, ownerId))
                .isInstanceOf(AppException.class)
                .extracting(this::groupCodeOf)
                .isEqualTo(GroupErrorCode.GROUP_NOT_FOUND);
    }

    // ── 받은 초대 ──

    @Test
    @DisplayName("내가 받은 대기 중 초대만 그룹 이름·초대자 이름과 함께 반환한다")
    void 받은_대기_초대만_반환한다() {
        Long groupId = createGroupOwnedByOwner();
        service.invite(groupId, ownerId, "invitee@sqisoft.com");

        List<InvitationView> invitations = service.findMyInvitations(inviteeId);

        assertThat(invitations).hasSize(1);
        assertThat(invitations.get(0).group().name()).isEqualTo("개발팀");
        assertThat(invitations.get(0).inviterName()).isEqualTo("김본승");
        assertThat(invitations.get(0).membership().status()).isEqualTo(MembershipStatus.PENDING);
    }

    @Test
    @DisplayName("내가 만든 그룹은 받은 초대 목록에 나타나지 않는다")
    void 내가_만든_그룹은_초대_목록에_없다() {
        createGroupOwnedByOwner();

        assertThat(service.findMyInvitations(ownerId)).isEmpty();
    }

    @Test
    @DisplayName("수락한 초대는 더 이상 받은 초대 목록에 나타나지 않는다")
    void 수락한_초대는_목록에서_사라진다() {
        Long groupId = createGroupOwnedByOwner();
        Long membershipId = service.invite(groupId, ownerId, "invitee@sqisoft.com").membership().id();

        service.accept(membershipId, inviteeId);

        assertThat(service.findMyInvitations(inviteeId)).isEmpty();
    }

    // ── 초대 응답 ──

    @Test
    @DisplayName("초대를 수락하면 그 그룹이 내 그룹 목록에 나타난다")
    void 수락하면_내_그룹이_된다() {
        Long groupId = createGroupOwnedByOwner();
        Long membershipId = service.invite(groupId, ownerId, "invitee@sqisoft.com").membership().id();

        InvitationView accepted = service.accept(membershipId, inviteeId);

        assertThat(accepted.membership().status()).isEqualTo(MembershipStatus.APPROVED);
        assertThat(service.findMyGroups(inviteeId))
                .extracting(view -> view.group().id()).containsExactly(groupId);
        assertThat(service.findMyGroups(inviteeId))
                .extracting(GroupView::role).containsExactly(GroupRole.MEMBER);
    }

    @Test
    @DisplayName("남이 받은 초대는 수락할 수 없다")
    void 남의_초대는_수락할_수_없다() {
        Long groupId = createGroupOwnedByOwner();
        Long membershipId = service.invite(groupId, ownerId, "invitee@sqisoft.com").membership().id();

        assertThatThrownBy(() -> service.accept(membershipId, strangerId))
                .isInstanceOf(AppException.class)
                .extracting(this::groupCodeOf)
                .isEqualTo(GroupErrorCode.NOT_INVITEE);
    }

    @Test
    @DisplayName("존재하지 않는 초대에 응답하면 INVITATION_NOT_FOUND를 던진다")
    void 없는_초대에는_응답할_수_없다() {
        assertThatThrownBy(() -> service.accept(999L, inviteeId))
                .isInstanceOf(AppException.class)
                .extracting(this::groupCodeOf)
                .isEqualTo(GroupErrorCode.INVITATION_NOT_FOUND);
    }

    @Test
    @DisplayName("이미 수락한 초대를 다시 수락하면 INVITATION_ALREADY_RESPONDED를 던진다")
    void 이미_응답한_초대는_다시_응답할_수_없다() {
        Long groupId = createGroupOwnedByOwner();
        Long membershipId = service.invite(groupId, ownerId, "invitee@sqisoft.com").membership().id();
        service.accept(membershipId, inviteeId);

        assertThatThrownBy(() -> service.accept(membershipId, inviteeId))
                .isInstanceOf(AppException.class)
                .extracting(this::groupCodeOf)
                .isEqualTo(GroupErrorCode.INVITATION_ALREADY_RESPONDED);
    }

    @Test
    @DisplayName("초대를 거절하면 그룹원 명단에는 REJECTED로 남는다")
    void 거절하면_명단에_거절로_남는다() {
        Long groupId = createGroupOwnedByOwner();
        Long membershipId = service.invite(groupId, ownerId, "invitee@sqisoft.com").membership().id();

        service.reject(membershipId, inviteeId);

        assertThat(service.findMembers(groupId, ownerId))
                .extracting(view -> view.membership().status())
                .containsExactly(MembershipStatus.APPROVED, MembershipStatus.REJECTED);
    }
}
