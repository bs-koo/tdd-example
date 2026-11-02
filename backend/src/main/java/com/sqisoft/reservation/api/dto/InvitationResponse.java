package com.sqisoft.reservation.api.dto;

import com.sqisoft.reservation.application.InvitationView;

/** id는 멤버십 id다 — 초대는 PENDING 상태의 멤버십이므로 별도 식별자가 없다. */
public record InvitationResponse(Long id, Long groupId, String groupName, String invitedBy, String status) {

    public static InvitationResponse from(InvitationView view) {
        return new InvitationResponse(
                view.membership().id(),
                view.group().id(),
                view.group().name(),
                view.inviterName(),
                view.membership().status().name());
    }
}
