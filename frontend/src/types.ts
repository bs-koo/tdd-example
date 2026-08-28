export type ErrorCode =
  | 'OVERLAPPING_RESERVATION' | 'INVALID_TIME_UNIT' | 'OUTSIDE_BUSINESS_HOURS'
  | 'INVALID_DURATION' | 'PAST_DATETIME' | 'TOO_FAR_IN_FUTURE' | 'NOT_RESERVER'
  | 'ALREADY_CANCELLED' | 'RESERVATION_NOT_FOUND' | 'ROOM_NOT_FOUND'
  | 'INVALID_RESERVER_NAME' | 'INVALID_PURPOSE_LENGTH' | 'INVALID_DATE_FORMAT';

export type SlotTime = string;   // "HH:mm"

export type RoomDto = { id: number; name: string; capacity: number; location: string };
export type ReservationDto = {
  id: number; roomId: number; reserverName: string; purpose: string;
  startAt: string;   // "2026-08-26T09:00:00"
  endAt: string;     // "2026-08-26T10:00:00"
  status: 'ACTIVE' | 'CANCELLED';
};

export type CreateReservationRequest = {
  roomId: number; reserverName: string; purpose: string;
  startAt: string;   // "2026-08-26T09:00:00" — 초 포함
  endAt: string;
};
