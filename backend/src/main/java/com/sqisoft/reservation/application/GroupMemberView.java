package com.sqisoft.reservation.application;

import com.sqisoft.reservation.domain.GroupMembership;
import com.sqisoft.reservation.domain.Member;

public record GroupMemberView(GroupMembership membership, Member member) {
}
