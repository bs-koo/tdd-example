package com.sqisoft.reservation.application;

import com.sqisoft.reservation.domain.Group;
import com.sqisoft.reservation.domain.GroupRole;
import com.sqisoft.reservation.domain.MembershipStatus;

/** 그룹과 "그것을 조회한 사람의" 멤버십을 함께 묶은 조회 결과. */
public record GroupView(Group group, GroupRole role, MembershipStatus status) {
}
