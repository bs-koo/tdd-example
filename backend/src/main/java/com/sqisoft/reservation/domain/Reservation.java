package com.sqisoft.reservation.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long roomId;

    private Long memberId;

    private String reserverName;

    private String purpose;

    private LocalDateTime startAt;

    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    private ReservationStatus status;

    private LocalDateTime createdAt;

    protected Reservation() {
    }

    private Reservation(Long id, Long roomId, Long memberId, String reserverName, String purpose,
            TimeSlot slot, ReservationStatus status, LocalDateTime createdAt) {
        this.id = id;
        this.roomId = roomId;
        this.memberId = memberId;
        this.reserverName = reserverName;
        this.purpose = purpose;
        this.startAt = slot.start();
        this.endAt = slot.end();
        this.status = status;
        this.createdAt = createdAt;
    }

    public static Reservation create(Long roomId, Long memberId, String reserverName, String purpose,
            TimeSlot slot, LocalDateTime createdAt) {
        String normalizedName = reserverName == null ? "" : reserverName.trim();
        String normalizedPurpose = purpose == null ? "" : purpose;
        return new Reservation(null, roomId, memberId, normalizedName, normalizedPurpose, slot,
                ReservationStatus.ACTIVE, createdAt);
    }

    public static Reservation restore(Long id, Long roomId, Long memberId, String reserverName, String purpose,
            TimeSlot slot, ReservationStatus status, LocalDateTime createdAt) {
        return new Reservation(id, roomId, memberId, reserverName, purpose, slot, status, createdAt);
    }

    public Long id() {
        return id;
    }

    public Long roomId() {
        return roomId;
    }

    public Long memberId() {
        return memberId;
    }

    public String reserverName() {
        return reserverName;
    }

    public String purpose() {
        return purpose;
    }

    public TimeSlot timeSlot() {
        return new TimeSlot(startAt, endAt);
    }

    public ReservationStatus status() {
        return status;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public boolean isActive() {
        return status == ReservationStatus.ACTIVE;
    }

    /** 종료 시각이 기준 시각과 같으면 이미 끝난 예약으로 본다(반개구간). */
    public boolean isOngoingOrUpcomingAt(LocalDateTime now) {
        return isActive() && endAt.isAfter(now);
    }

    public boolean overlapsWith(TimeSlot other) {
        return isActive() && timeSlot().overlaps(other);
    }

    /** 취소 권한은 표시용 예약자명이 아니라 예약을 만든 신원으로 판정한다. */
    public void cancel(Long requesterId) {
        if (requesterId == null || !requesterId.equals(this.memberId)) {
            throw new ReservationException(ReservationErrorCode.NOT_RESERVER);
        }
        if (this.status == ReservationStatus.CANCELLED) {
            throw new ReservationException(ReservationErrorCode.ALREADY_CANCELLED);
        }
        this.status = ReservationStatus.CANCELLED;
    }
}
