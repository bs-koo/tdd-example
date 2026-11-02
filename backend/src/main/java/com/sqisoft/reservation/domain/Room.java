package com.sqisoft.reservation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "room")
public class Room {

    static final int NAME_MAX = 30;
    static final int LOCATION_MAX = 30;
    static final int CAPACITY_MIN = 1;
    static final int CAPACITY_MAX = 999;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long groupId;

    private String name;

    private int capacity;

    private String location;

    protected Room() {
    }

    private Room(Long id, Long groupId, String name, int capacity, String location) {
        this.id = id;
        this.groupId = groupId;
        this.name = name;
        this.capacity = capacity;
        this.location = location;
    }

    public static Room create(Long groupId, String name, int capacity, String location) {
        String validName = validName(name);
        int validCapacity = validCapacity(capacity);
        String validLocation = validLocation(location);
        return new Room(null, groupId, validName, validCapacity, validLocation);
    }

    /** 세 값을 모두 검증한 뒤에 대입한다 — 하나라도 위반하면 어떤 필드도 바뀌지 않는다. */
    public void update(String name, int capacity, String location) {
        String validName = validName(name);
        int validCapacity = validCapacity(capacity);
        String validLocation = validLocation(location);
        this.name = validName;
        this.capacity = validCapacity;
        this.location = validLocation;
    }

    private static String validName(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty() || trimmed.length() > NAME_MAX) {
            throw new AppException(ReservationErrorCode.INVALID_ROOM_NAME);
        }
        return trimmed;
    }

    private static int validCapacity(int capacity) {
        if (capacity < CAPACITY_MIN || capacity > CAPACITY_MAX) {
            throw new AppException(ReservationErrorCode.INVALID_ROOM_CAPACITY);
        }
        return capacity;
    }

    private static String validLocation(String location) {
        String trimmed = location == null ? "" : location.trim();
        if (trimmed.isEmpty() || trimmed.length() > LOCATION_MAX) {
            throw new AppException(ReservationErrorCode.INVALID_ROOM_LOCATION);
        }
        return trimmed;
    }

    public static Room restore(Long id, Long groupId, String name, int capacity, String location) {
        return new Room(id, groupId, name, capacity, location);
    }

    public Long id() {
        return id;
    }

    public Long groupId() {
        return groupId;
    }

    public String name() {
        return name;
    }

    public int capacity() {
        return capacity;
    }

    public String location() {
        return location;
    }
}
