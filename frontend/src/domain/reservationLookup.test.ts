import { describe, it, expect } from 'vitest';
import type { ReservationDto } from '../types';
import { findReservationAt } from './reservationLookup';

const BASE_RESERVATION: ReservationDto = {
  id: 1,
  roomId: 1,
  reserverName: '김본승',
  purpose: '주간 회의',
  startAt: '2026-08-26T09:00:00',
  endAt: '2026-08-26T10:00:00',
  status: 'ACTIVE',
};

describe('findReservationAt', () => {
  it('시작 경계 슬롯(09:00)에서 예약을 찾는다', () => {
    expect(findReservationAt([BASE_RESERVATION], 1, '09:00')?.id).toBe(1);
  });

  it('구간 포함 매칭 — 시작 이후 슬롯(09:30)에서도 같은 예약을 찾는다', () => {
    expect(findReservationAt([BASE_RESERVATION], 1, '09:30')?.id).toBe(1);
  });

  it('종료 경계 슬롯(10:00)은 반개구간이므로 찾지 못한다', () => {
    expect(findReservationAt([BASE_RESERVATION], 1, '10:00')).toBeUndefined();
  });

  it('시작 이전 슬롯(08:30)에서는 찾지 못한다', () => {
    expect(findReservationAt([BASE_RESERVATION], 1, '08:30')).toBeUndefined();
  });

  it('다른 roomId로 조회하면 찾지 못한다', () => {
    expect(findReservationAt([BASE_RESERVATION], 2, '09:00')).toBeUndefined();
  });

  it('같은 시간대라도 CANCELLED 상태면 찾지 못한다', () => {
    const cancelled: ReservationDto = { ...BASE_RESERVATION, status: 'CANCELLED' };
    expect(findReservationAt([cancelled], 1, '09:00')).toBeUndefined();
  });

  it('예약 목록이 비어 있으면 undefined를 반환한다', () => {
    expect(findReservationAt([], 1, '09:00')).toBeUndefined();
  });

  it('여러 예약 중 조회 슬롯을 덮는 예약을 정확히 골라낸다', () => {
    const second: ReservationDto = {
      id: 2,
      roomId: 1,
      reserverName: '이몽룡',
      purpose: '면접',
      startAt: '2026-08-26T11:00:00',
      endAt: '2026-08-26T12:00:00',
      status: 'ACTIVE',
    };
    expect(findReservationAt([BASE_RESERVATION, second], 1, '11:30')?.purpose).toBe('면접');
  });
});
