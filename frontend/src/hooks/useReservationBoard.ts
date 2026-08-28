import { useState, useEffect, useCallback } from 'react';
import type { RoomDto, ReservationDto, CreateReservationRequest } from '../types';
import type { ReservationApi } from '../api/types';
import { ApiError } from '../api/client';

function toErrorCode(e: unknown): string {
  return e instanceof ApiError ? e.code : 'UNKNOWN';
}

export type ReservationBoard = {
  date: string;
  rooms: RoomDto[];
  reservations: ReservationDto[];
  errorCode: string | null;
  changeDate: (date: string) => Promise<void>;
  createReservation: (request: CreateReservationRequest) => Promise<void>;
  cancelReservation: (reservation: ReservationDto) => Promise<void>;
  clearError: () => void;
};

export function useReservationBoard(options: {
  initialDate: string;
  api: ReservationApi;
}): ReservationBoard {
  const { initialDate, api } = options;
  const [date, setDate] = useState(initialDate);
  const [rooms, setRooms] = useState<RoomDto[]>([]);
  const [reservations, setReservations] = useState<ReservationDto[]>([]);
  const [errorCode, setErrorCode] = useState<string | null>(null);

  const load = useCallback(
    async (targetDate: string) => {
      const fetchedRooms = await api.fetchRooms();
      const perRoom = await Promise.all(
        fetchedRooms.map((room) => api.fetchReservations(room.id, targetDate)),
      );
      setRooms(fetchedRooms);
      setReservations(perRoom.flat());
    },
    [api],
  );

  useEffect(() => {
    load(initialDate).catch((e: unknown) => {
      setErrorCode(toErrorCode(e));
    });
    // 마운트 시 1회만 조회한다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const changeDate = useCallback(
    async (next: string) => {
      setDate(next);
      try {
        await load(next);
        setErrorCode(null);
      } catch (e) {
        setErrorCode(toErrorCode(e));
      }
    },
    [load],
  );

  const createReservation = useCallback(
    async (request: CreateReservationRequest) => {
      try {
        await api.createReservation(request);
        await load(date);
        setErrorCode(null);
      } catch (e) {
        setErrorCode(toErrorCode(e));
      }
    },
    [api, load, date],
  );

  const cancelReservation = useCallback(
    async (reservation: ReservationDto) => {
      try {
        await api.cancelReservation(reservation.id, reservation.reserverName);
        await load(date);
        setErrorCode(null);
      } catch (e) {
        setErrorCode(toErrorCode(e));
      }
    },
    [api, load, date],
  );

  const clearError = useCallback(() => setErrorCode(null), []);

  return {
    date,
    rooms,
    reservations,
    errorCode,
    changeDate,
    createReservation,
    cancelReservation,
    clearError,
  };
}
