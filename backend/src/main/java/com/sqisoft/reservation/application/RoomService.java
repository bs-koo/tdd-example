package com.sqisoft.reservation.application;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import com.sqisoft.reservation.domain.ReservationErrorCode;
import com.sqisoft.reservation.domain.ReservationException;
import com.sqisoft.reservation.domain.Room;

public class RoomService {

    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final GroupAccess groupAccess;
    private final Clock clock;

    public RoomService(RoomRepository roomRepository, ReservationRepository reservationRepository,
            GroupAccess groupAccess, Clock clock) {
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.groupAccess = groupAccess;
        this.clock = clock;
    }

    /** 권한을 입력 검증보다 먼저 본다 — 남의 그룹 사정을 입력 오류 메시지로 흘리지 않기 위해서다. */
    public Room createRoom(Long groupId, Long memberId, CreateRoomCommand command) {
        groupAccess.requireApprovedMember(groupId, memberId);
        return roomRepository.save(
                Room.create(groupId, command.name(), command.capacity(), command.location()));
    }

    /** 방 존재 → 승인된 그룹원 → 입력 검증 순이다. 권한이 입력 검증보다 앞선다. */
    public Room updateRoom(Long roomId, Long memberId, UpdateRoomCommand command) {
        Room room = requireAccessibleRoom(roomId, memberId);
        room.update(command.name(), command.capacity(), command.location());
        return roomRepository.save(room);
    }

    /**
     * 방 존재 → 그룹장 → 앞으로의 예약 확인 순이다. 권한이 예약 상태 확인보다 앞서므로
     * 외부인은 예약이 있는지 알 수 없다. 예약 이력은 지우지 않는다.
     */
    public void deleteRoom(Long roomId, Long memberId) {
        Room room = requireRoom(roomId);
        groupAccess.requireOwner(room.groupId(), memberId);
        if (reservationRepository.existsActiveByRoomIdAndEndAtAfter(roomId, LocalDateTime.now(clock))) {
            throw new ReservationException(ReservationErrorCode.ROOM_HAS_ACTIVE_RESERVATIONS);
        }
        roomRepository.deleteById(roomId);
    }

    public List<Room> findByGroup(Long groupId, Long memberId) {
        groupAccess.requireApprovedMember(groupId, memberId);
        return roomRepository.findAllByGroupId(groupId);
    }

    /** 회의실 부재를 그룹 권한보다 먼저 알린다 — 없는 회의실에 대해 그룹 소속을 물을 수는 없다. */
    public Room requireAccessibleRoom(Long roomId, Long memberId) {
        Room room = requireRoom(roomId);
        groupAccess.requireApprovedMember(room.groupId(), memberId);
        return room;
    }

    private Room requireRoom(Long roomId) {
        return roomRepository.findById(roomId)
                .orElseThrow(() -> new ReservationException(ReservationErrorCode.ROOM_NOT_FOUND));
    }
}
