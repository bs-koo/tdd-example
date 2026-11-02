package com.sqisoft.reservation.api.dto;

import com.sqisoft.reservation.application.GroupView;

public record GroupResponse(Long id, String name, Long ownerId, String role, String status) {

    public static GroupResponse from(GroupView view) {
        return new GroupResponse(
                view.group().id(),
                view.group().name(),
                view.group().ownerId(),
                view.role().name(),
                view.status().name());
    }
}
