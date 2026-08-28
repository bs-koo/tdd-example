import type { ReservationDto, SlotTime } from '../types';

export function findReservationAt(
  reservations: ReservationDto[],
  roomId: number,
  slot: SlotTime,
): ReservationDto | undefined {
  return reservations.find(
    (r) =>
      r.roomId === roomId &&
      r.status === 'ACTIVE' &&
      r.startAt.slice(11, 16) <= slot &&
      slot < r.endAt.slice(11, 16),
  );
}
