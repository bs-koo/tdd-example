package com.sqisoft.reservation.application;

import com.sqisoft.reservation.domain.Group;
import com.sqisoft.reservation.domain.GroupMembership;

public record InvitationView(GroupMembership membership, Group group, String inviterName) {
}
