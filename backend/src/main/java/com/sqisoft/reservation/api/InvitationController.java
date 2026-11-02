package com.sqisoft.reservation.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sqisoft.reservation.api.dto.InvitationResponse;
import com.sqisoft.reservation.application.GroupService;
import com.sqisoft.reservation.domain.Member;

@RestController
@RequestMapping("/api/invitations")
public class InvitationController {

    private final GroupService groupService;

    public InvitationController(GroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public List<InvitationResponse> findMine(@LoginMember Member member) {
        return groupService.findMyInvitations(member.id()).stream()
                .map(InvitationResponse::from)
                .toList();
    }

    @PostMapping("/{membershipId}/accept")
    public InvitationResponse accept(@LoginMember Member member, @PathVariable Long membershipId) {
        return InvitationResponse.from(groupService.accept(membershipId, member.id()));
    }

    @PostMapping("/{membershipId}/reject")
    public InvitationResponse reject(@LoginMember Member member, @PathVariable Long membershipId) {
        return InvitationResponse.from(groupService.reject(membershipId, member.id()));
    }
}
