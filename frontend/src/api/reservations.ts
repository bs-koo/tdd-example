import type { RoomDto, ReservationDto, CreateReservationRequest } from '../types';
import type { ReservationApi } from './types';
import { throwApiError } from './client';

export const reservationApi: ReservationApi = {
  async fetchRooms(): Promise<RoomDto[]> {
    const response = await fetch('/api/rooms');
    if (!response.ok) {
      await throwApiError(response);
    }
    return (await response.json()) as RoomDto[];
  },

  async fetchReservations(roomId: number, date: string): Promise<ReservationDto[]> {
    const response = await fetch(`/api/rooms/${roomId}/reservations?date=${date}`);
    if (!response.ok) {
      await throwApiError(response);
    }
    return (await response.json()) as ReservationDto[];
  },

  async createReservation(request: CreateReservationRequest): Promise<ReservationDto> {
    const response = await fetch('/api/reservations', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
    });
    if (!response.ok) {
      await throwApiError(response);
    }
    return (await response.json()) as ReservationDto;
  },

  async cancelReservation(id: number, reserverName: string): Promise<void> {
    const response = await fetch(`/api/reservations/${id}/cancel`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ reserverName }),
    });
    if (!response.ok) {
      await throwApiError(response);
    }
  },
};
