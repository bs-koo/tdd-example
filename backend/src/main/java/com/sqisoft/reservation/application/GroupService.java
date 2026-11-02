package com.sqisoft.reservation.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.sqisoft.reservation.domain.AppException;
import com.sqisoft.reservation.domain.AuthErrorCode;
import com.sqisoft.reservation.domain.Group;
import com.sqisoft.reservation.domain.GroupErrorCode;
import com.sqisoft.reservation.domain.GroupMembership;
import com.sqisoft.reservation.domain.Member;

public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMembershipRepository membershipRepository;
    private final MemberRepository memberRepository;
    private final GroupAccess groupAccess;
    private final Clock clock;

    public GroupService(GroupRepository groupRepository,
            GroupMembershipRepository membershipRepository,
            MemberRepository memberRepository,
            GroupAccess groupAccess,
            Clock clock) {
        this.groupRepository = groupRepository;
        this.membershipRepository = membershipRepository;
        this.memberRepository = memberRepository;
        this.groupAccess = groupAccess;
        this.clock = clock;
    }

    public GroupView createGroup(Long ownerId, String name) {
        LocalDateTime now = LocalDateTime.now(clock);
        Group group = groupRepository.save(Group.create(name, ownerId, now));
        GroupMembership membership = membershipRepository.save(
                GroupMembership.owner(group.id(), ownerId, now));
        return new GroupView(group, membership.role(), membership.status());
    }

    public List<GroupView> findMyGroups(Long memberId) {
        List<GroupMembership> approved = membershipRepository.findAllByMemberId(memberId).stream()
                .filter(GroupMembership::isApproved)
                .toList();
        Map<Long, Group> groups = groupsById(approved.stream().map(GroupMembership::groupId).toList());

        return approved.stream()
                .filter(membership -> groups.containsKey(membership.groupId()))
                .map(membership -> new GroupView(groups.get(membership.groupId()),
                        membership.role(), membership.status()))
                .sorted(Comparator.comparing(view -> view.group().id()))
                .toList();
    }

    public List<GroupMemberView> findMembers(Long groupId, Long requesterId) {
        groupAccess.requireApprovedMember(groupId, requesterId);

        List<GroupMembership> memberships = membershipRepository.findAllByGroupId(groupId);
        Map<Long, Member> members = membersById(memberships.stream()
                .map(GroupMembership::memberId).toList());

        return memberships.stream()
                .filter(membership -> members.containsKey(membership.memberId()))
                .map(membership -> new GroupMemberView(membership, members.get(membership.memberId())))
                .toList();
    }

    public GroupMemberView invite(Long groupId, Long ownerId, String email) {
        groupAccess.requireOwner(groupId, ownerId);

        Member invitee = memberRepository.findByEmail(Member.normalizeEmail(email))
                .orElseThrow(() -> new AppException(AuthErrorCode.MEMBER_NOT_FOUND));
        if (invitee.id().equals(ownerId)) {
            throw new AppException(GroupErrorCode.SELF_INVITATION);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        GroupMembership membership = membershipRepository
                .findByGroupIdAndMemberId(groupId, invitee.id())
                .map(existing -> reinvite(existing, now))
                .orElseGet(() -> GroupMembership.invite(groupId, invitee.id(), now));

        return new GroupMemberView(membershipRepository.save(membership), invitee);
    }

    private GroupMembership reinvite(GroupMembership existing, LocalDateTime now) {
        if (!existing.isRejected()) {
            throw new AppException(GroupErrorCode.ALREADY_INVITED);
        }
        existing.reinvite(now);
        return existing;
    }

    public List<InvitationView> findMyInvitations(Long memberId) {
        List<GroupMembership> pending = membershipRepository.findAllByMemberId(memberId).stream()
                .filter(GroupMembership::isPending)
                .toList();
        Map<Long, Group> groups = groupsById(pending.stream().map(GroupMembership::groupId).toList());
        Map<Long, Member> inviters = membersById(groups.values().stream().map(Group::ownerId).toList());

        return pending.stream()
                .filter(membership -> groups.containsKey(membership.groupId()))
                .map(membership -> toInvitationView(membership, groups, inviters))
                .toList();
    }

    public InvitationView accept(Long membershipId, Long memberId) {
        return respond(membershipId, membership -> membership.accept(memberId, LocalDateTime.now(clock)));
    }

    public InvitationView reject(Long membershipId, Long memberId) {
        return respond(membershipId, membership -> membership.reject(memberId, LocalDateTime.now(clock)));
    }

    private InvitationView respond(Long membershipId, java.util.function.Consumer<GroupMembership> response) {
        GroupMembership membership = membershipRepository.findById(membershipId)
                .orElseThrow(() -> new AppException(GroupErrorCode.INVITATION_NOT_FOUND));
        response.accept(membership);
        GroupMembership saved = membershipRepository.save(membership);

        Map<Long, Group> groups = groupsById(List.of(saved.groupId()));
        Map<Long, Member> inviters = membersById(groups.values().stream().map(Group::ownerId).toList());
        return toInvitationView(saved, groups, inviters);
    }

    private InvitationView toInvitationView(GroupMembership membership, Map<Long, Group> groups,
            Map<Long, Member> inviters) {
        Group group = groups.get(membership.groupId());
        Member inviter = inviters.get(group.ownerId());
        return new InvitationView(membership, group, inviter == null ? "" : inviter.name());
    }

    private Map<Long, Group> groupsById(List<Long> ids) {
        return groupRepository.findAllByIdIn(ids).stream()
                .collect(Collectors.toMap(Group::id, Function.identity()));
    }

    private Map<Long, Member> membersById(List<Long> ids) {
        return memberRepository.findAllByIdIn(ids).stream()
                .collect(Collectors.toMap(Member::id, Function.identity()));
    }
}
