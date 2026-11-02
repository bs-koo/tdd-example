package com.sqisoft.reservation.application;

public record CreateRoomCommand(String name, int capacity, String location) {
}
