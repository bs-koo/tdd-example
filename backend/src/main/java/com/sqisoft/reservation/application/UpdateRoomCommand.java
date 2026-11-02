package com.sqisoft.reservation.application;

public record UpdateRoomCommand(String name, int capacity, String location) {
}
