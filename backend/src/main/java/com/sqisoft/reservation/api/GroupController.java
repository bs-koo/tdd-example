package com.sqisoft.reservation.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.sqisoft.reservation.api.dto.CreateGroupRequest;
import com.sqisoft.reservation.api.dto.CreateRoomRequest;
import com.sqisoft.reservation.api.dto.GroupMemberResponse;
import com.sqisoft.reservation.api.dto.GroupResponse;
import com.sqisoft.reservation.api.dto.InviteRequest;
import com.sqisoft.reservation.api.dto.RoomResponse;
import com.sqisoft.reservation.application.GroupService;
import com.sqisoft.reservation.application.RoomService;
import com.sqisoft.reservation.domain.Member;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    private final GroupService groupService;
    private final RoomService roomService;

    public GroupController(GroupService groupService, RoomService roomService) {
        this.groupService = groupService;
        this.roomService = roomService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public GroupResponse create(@LoginMember Member member, @RequestBody CreateGroupRequest request) {
        return GroupResponse.from(groupService.createGroup(member.id(), request.name()));
    }

    @GetMapping
    public List<GroupResponse> findMine(@LoginMember Member member) {
        return groupService.findMyGroups(member.id()).stream()
                .map(GroupResponse::from)
                .toList();
    }

    @GetMapping("/{groupId}/members")
    public List<GroupMemberResponse> findMembers(@LoginMember Member member, @PathVariable Long groupId) {
        return groupService.findMembers(groupId, member.id()).stream()
                .map(GroupMemberResponse::from)
                .toList();
    }

    @PostMapping("/{groupId}/invitations")
    @ResponseStatus(HttpStatus.CREATED)
    public GroupMemberResponse invite(@LoginMember Member member, @PathVariable Long groupId,
            @RequestBody InviteRequest request) {
        return GroupMemberResponse.from(groupService.invite(groupId, member.id(), request.email()));
    }

    @PostMapping("/{groupId}/rooms")
    @ResponseStatus(HttpStatus.CREATED)
    public RoomResponse createRoom(@LoginMember Member member, @PathVariable Long groupId,
            @RequestBody CreateRoomRequest request) {
        return RoomResponse.from(roomService.createRoom(groupId, member.id(), request.toCommand()));
    }

    @GetMapping("/{groupId}/rooms")
    public List<RoomResponse> findRooms(@LoginMember Member member, @PathVariable Long groupId) {
        return roomService.findByGroup(groupId, member.id()).stream()
                .map(RoomResponse::from)
                .toList();
    }
}
