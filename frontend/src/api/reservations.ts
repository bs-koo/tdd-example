import type {
  CreateReservationRequest,
  CreateRoomRequest,
  ReservationDto,
  RoomDto,
} from '../types';
import type { ReservationApi } from './types';
import { authorizedFetch, throwApiError } from './client';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export const reservationApi: ReservationApi = {
  // 회의실은 언제나 그룹 문맥에서 조회한다 — 전체 목록 엔드포인트는 없다.
  async fetchRooms(groupId: number): Promise<RoomDto[]> {
    const response = await authorizedFetch(`/api/groups/${groupId}/rooms`);
    if (!response.ok) {
      await throwApiError(response);
    }
    return (await response.json()) as RoomDto[];
  },

  async createRoom(groupId: number, request: CreateRoomRequest): Promise<RoomDto> {
    const response = await authorizedFetch(`/api/groups/${groupId}/rooms`, {
      method: 'POST',
      headers: JSON_HEADERS,
      body: JSON.stringify(request),
    });
    if (!response.ok) {
      await throwApiError(response);
    }
    return (await response.json()) as RoomDto;
  },

  async fetchReservations(roomId: number, date: string): Promise<ReservationDto[]> {
    const response = await authorizedFetch(`/api/rooms/${roomId}/reservations?date=${date}`);
    if (!response.ok) {
      await throwApiError(response);
    }
    return (await response.json()) as ReservationDto[];
  },

  async createReservation(request: CreateReservationRequest): Promise<ReservationDto> {
    const response = await authorizedFetch('/api/reservations', {
      method: 'POST',
      headers: JSON_HEADERS,
      body: JSON.stringify(request),
    });
    if (!response.ok) {
      await throwApiError(response);
    }
    return (await response.json()) as ReservationDto;
  },

  // 본인 확인은 토큰으로 끝난다 — 취소 요청에는 바디가 없다.
  async cancelReservation(id: number): Promise<void> {
    const response = await authorizedFetch(`/api/reservations/${id}/cancel`, { method: 'POST' });
    if (!response.ok) {
      await throwApiError(response);
    }
  },
};
