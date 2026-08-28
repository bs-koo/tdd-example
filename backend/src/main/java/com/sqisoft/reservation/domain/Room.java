package com.sqisoft.reservation.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "room")
public class Room {

    @Id
    private Long id;

    private String name;

    private int capacity;

    private String location;

    protected Room() {
    }

    private Room(Long id, String name, int capacity, String location) {
        this.id = id;
        this.name = name;
        this.capacity = capacity;
        this.location = location;
    }

    public static Room of(Long id, String name, int capacity, String location) {
        return new Room(id, name, capacity, location);
    }

    public Long id() {
        return id;
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
