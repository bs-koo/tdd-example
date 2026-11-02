import { useState, useEffect, useCallback, useRef } from 'react';
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
  reload: () => Promise<void>;
  clearError: () => void;
};

export function useReservationBoard(options: {
  initialDate: string;
  groupId: number | null;
  api: ReservationApi;
}): ReservationBoard {
  const { initialDate, groupId, api } = options;
  const [date, setDate] = useState(initialDate);
  const [rooms, setRooms] = useState<RoomDto[]>([]);
  const [reservations, setReservations] = useState<ReservationDto[]>([]);
  const [errorCode, setErrorCode] = useState<string | null>(null);

  // 그룹이 바뀌면 지금 보고 있는 날짜 그대로 다시 채운다. date 를 의존성에 넣으면
  // 날짜를 바꿀 때마다 이 효과까지 덩달아 돌아 같은 조회가 두 번 나간다.
  const dateRef = useRef(date);
  dateRef.current = date;

  const load = useCallback(
    async (targetGroupId: number, targetDate: string) => {
      const fetchedRooms = await api.fetchRooms(targetGroupId);
      const perRoom = await Promise.all(
        fetchedRooms.map((room) => api.fetchReservations(room.id, targetDate)),
      );
      setRooms(fetchedRooms);
      setReservations(perRoom.flat());
    },
    [api],
  );

  useEffect(() => {
    if (groupId === null) {
      setRooms([]);
      setReservations([]);
      return;
    }
    load(groupId, dateRef.current).catch((e: unknown) => {
      setErrorCode(toErrorCode(e));
    });
  }, [groupId, load]);

  const reloadCurrent = useCallback(
    async (targetDate: string) => {
      if (groupId === null) {
        return;
      }
      await load(groupId, targetDate);
    },
    [groupId, load],
  );

  const changeDate = useCallback(
    async (next: string) => {
      setDate(next);
      try {
        await reloadCurrent(next);
        setErrorCode(null);
      } catch (e) {
        setErrorCode(toErrorCode(e));
      }
    },
    [reloadCurrent],
  );

  const reload = useCallback(async () => {
    try {
      await reloadCurrent(date);
      setErrorCode(null);
    } catch (e) {
      setErrorCode(toErrorCode(e));
    }
  }, [reloadCurrent, date]);

  const createReservation = useCallback(
    async (request: CreateReservationRequest) => {
      try {
        await api.createReservation(request);
        await reloadCurrent(date);
        setErrorCode(null);
      } catch (e) {
        setErrorCode(toErrorCode(e));
      }
    },
    [api, reloadCurrent, date],
  );

  const cancelReservation = useCallback(
    async (reservation: ReservationDto) => {
      try {
        await api.cancelReservation(reservation.id);
        await reloadCurrent(date);
        setErrorCode(null);
      } catch (e) {
        setErrorCode(toErrorCode(e));
      }
    },
    [api, reloadCurrent, date],
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
    reload,
    clearError,
  };
}
