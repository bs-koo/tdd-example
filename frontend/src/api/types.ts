import type { RoomDto, ReservationDto, CreateReservationRequest } from '../types';

export type ReservationApi = {
  fetchRooms(): Promise<RoomDto[]>;
  fetchReservations(roomId: number, date: string): Promise<ReservationDto[]>;
  createReservation(request: CreateReservationRequest): Promise<ReservationDto>;
  cancelReservation(id: number, reserverName: string): Promise<void>;
};
