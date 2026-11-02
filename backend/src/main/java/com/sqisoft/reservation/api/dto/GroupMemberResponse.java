package com.sqisoft.reservation.api.dto;

import com.sqisoft.reservation.application.GroupMemberView;

public record GroupMemberResponse(Long membershipId, Long memberId, String name, String email,
        String role, String status) {

    public static GroupMemberResponse from(GroupMemberView view) {
        return new GroupMemberResponse(
                view.membership().id(),
                view.member().id(),
                view.member().name(),
                view.member().email(),
                view.membership().role().name(),
                view.membership().status().name());
    }
}
