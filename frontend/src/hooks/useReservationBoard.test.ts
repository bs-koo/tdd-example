import { describe, it, expect, vi } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import type { RoomDto, ReservationDto, CreateReservationRequest } from '../types';
import type { ReservationApi } from '../api/types';
import { ApiError } from '../api/client';
import { useReservationBoard } from './useReservationBoard';

const DEFAULT_DATE = '2026-08-26';

const ROOMS: RoomDto[] = [
  { id: 1, name: '대회의실', capacity: 20, location: '3층' },
  { id: 2, name: '중회의실', capacity: 10, location: '3층' },
];

const RESERVATION_ROOM1: ReservationDto = {
  id: 11,
  roomId: 1,
  reserverName: '김본승',
  purpose: '주간 회의',
  startAt: '2026-08-26T09:00:00',
  endAt: '2026-08-26T10:00:00',
  status: 'ACTIVE',
};

const RESERVATION_ROOM2: ReservationDto = {
  id: 22,
  roomId: 2,
  reserverName: '박다른',
  purpose: '설계 리뷰',
  startAt: '2026-08-26T13:00:00',
  endAt: '2026-08-26T14:00:00',
  status: 'ACTIVE',
};

const CREATE_REQUEST: CreateReservationRequest = {
  roomId: 1,
  reserverName: '최신규',
  purpose: '신규 회의',
  startAt: '2026-08-26T11:00:00',
  endAt: '2026-08-26T11:30:00',
};

const NEW_RESERVATION: ReservationDto = {
  id: 33,
  roomId: 1,
  reserverName: '최신규',
  purpose: '신규 회의',
  startAt: '2026-08-26T11:00:00',
  endAt: '2026-08-26T11:30:00',
  status: 'ACTIVE',
};

function createFakeApi() {
  return {
    fetchRooms: vi.fn(),
    fetchReservations: vi.fn(),
    createReservation: vi.fn(),
    cancelReservation: vi.fn(),
  };
}

function renderBoard(fakeApi: ReturnType<typeof createFakeApi>, initialDate: string = DEFAULT_DATE) {
  return renderHook(() => useReservationBoard({ initialDate, api: fakeApi as unknown as ReservationApi }));
}

describe('useReservationBoard', () => {
  it('최초 로드 시 N+1 조회로 회의실 전체의 예약을 합쳐 가져온다', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations.mockImplementation(async (roomId: number) =>
      roomId === 1 ? [RESERVATION_ROOM1] : [RESERVATION_ROOM2],
    );

    const { result } = renderBoard(api);

    await waitFor(() => expect(result.current.reservations).toHaveLength(2));

    expect(api.fetchRooms).toHaveBeenCalledTimes(1);
    expect(api.fetchReservations).toHaveBeenCalledTimes(2);
    expect(api.fetchReservations).toHaveBeenCalledWith(1, DEFAULT_DATE);
    expect(api.fetchReservations).toHaveBeenCalledWith(2, DEFAULT_DATE);
    expect(result.current.rooms).toHaveLength(2);
    expect(result.current.reservations.map((r) => r.id)).toEqual([11, 22]);
  });

  it('date는 initialDate로 시작한다', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations.mockResolvedValue([]);

    const { result } = renderBoard(api);

    await waitFor(() => expect(api.fetchRooms).toHaveBeenCalledTimes(1));
    expect(result.current.date).toBe(DEFAULT_DATE);
  });

  it('최초 로드가 ApiError로 실패하면 errorCode가 해당 code로 설정된다', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockRejectedValue(new ApiError('ROOM_NOT_FOUND', 404));

    const { result } = renderBoard(api);

    await waitFor(() => expect(result.current.errorCode).toBe('ROOM_NOT_FOUND'));
  });

  it('ApiError가 아닌 예외로 최초 로드가 실패하면 errorCode가 UNKNOWN이 된다', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockRejectedValue(new Error('네트워크 끊김'));

    const { result } = renderBoard(api);

    await waitFor(() => expect(result.current.errorCode).toBe('UNKNOWN'));
  });

  it('createReservation 성공 후 현재 날짜로 재조회하여 reservations를 갱신한다', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations
      .mockResolvedValueOnce([RESERVATION_ROOM1])
      .mockResolvedValueOnce([RESERVATION_ROOM2])
      .mockResolvedValueOnce([RESERVATION_ROOM1, NEW_RESERVATION])
      .mockResolvedValueOnce([RESERVATION_ROOM2]);
    api.createReservation.mockResolvedValue(NEW_RESERVATION);

    const { result } = renderBoard(api);
    await waitFor(() => expect(result.current.reservations).toHaveLength(2));

    await act(async () => {
      await result.current.createReservation(CREATE_REQUEST);
    });

    expect(api.createReservation).toHaveBeenCalledTimes(1);
    expect(api.createReservation).toHaveBeenCalledWith(CREATE_REQUEST);
    expect(api.fetchReservations).toHaveBeenCalledTimes(4);
    expect(result.current.reservations.map((r) => r.id)).toEqual([11, 33, 22]);
  });

  it('createReservation이 실패하면 재조회하지 않고 errorCode만 설정되며 reservations는 기존 스냅샷을 유지한다', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations.mockResolvedValueOnce([RESERVATION_ROOM1]).mockResolvedValueOnce([RESERVATION_ROOM2]);
    api.createReservation.mockRejectedValue(new ApiError('OVERLAPPING_RESERVATION', 409));

    const { result } = renderBoard(api);
    await waitFor(() => expect(result.current.reservations).toHaveLength(2));

    await act(async () => {
      await result.current.createReservation(CREATE_REQUEST);
    });

    expect(result.current.errorCode).toBe('OVERLAPPING_RESERVATION');
    expect(api.fetchReservations).toHaveBeenCalledTimes(2);
    expect(result.current.reservations.map((r) => r.id)).toEqual([11, 22]);
  });

  it('createReservation이 실패해도 훅의 async 메서드는 reject하지 않고 정상 resolve한다', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations.mockResolvedValue([]);
    api.createReservation.mockRejectedValue(new ApiError('OVERLAPPING_RESERVATION', 409));

    const { result } = renderBoard(api);
    await waitFor(() => expect(api.fetchRooms).toHaveBeenCalledTimes(1));

    await act(async () => {
      await expect(result.current.createReservation(CREATE_REQUEST)).resolves.toBeUndefined();
    });
  });

  it('cancelReservation은 reservation에서 id와 reserverName을 분해해 api.cancelReservation을 호출한다 (§9.8)', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations
      .mockResolvedValueOnce([RESERVATION_ROOM1])
      .mockResolvedValueOnce([RESERVATION_ROOM2])
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([RESERVATION_ROOM2]);
    api.cancelReservation.mockResolvedValue(undefined);

    const { result } = renderBoard(api);
    await waitFor(() => expect(result.current.reservations).toHaveLength(2));

    await act(async () => {
      await result.current.cancelReservation(RESERVATION_ROOM1);
    });

    expect(api.cancelReservation).toHaveBeenCalledTimes(1);
    expect(api.cancelReservation).toHaveBeenCalledWith(11, '김본승');
    expect(api.fetchReservations).toHaveBeenCalledTimes(4);
  });

  it('실패로 errorCode가 채워진 뒤 다른 동작이 성공하면 errorCode가 null로 되돌아온다', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations.mockResolvedValue([]);
    api.createReservation.mockRejectedValue(new ApiError('OVERLAPPING_RESERVATION', 409));
    api.cancelReservation.mockResolvedValue(undefined);

    const { result } = renderBoard(api);
    await waitFor(() => expect(api.fetchRooms).toHaveBeenCalledTimes(1));

    await act(async () => {
      await result.current.createReservation(CREATE_REQUEST);
    });
    expect(result.current.errorCode).toBe('OVERLAPPING_RESERVATION');

    await act(async () => {
      await result.current.cancelReservation(RESERVATION_ROOM1);
    });
    expect(result.current.errorCode).toBeNull();
  });

  it('clearError를 호출하면 errorCode가 null이 된다', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockRejectedValue(new ApiError('ROOM_NOT_FOUND', 404));

    const { result } = renderBoard(api);
    await waitFor(() => expect(result.current.errorCode).toBe('ROOM_NOT_FOUND'));

    act(() => {
      result.current.clearError();
    });

    expect(result.current.errorCode).toBeNull();
  });

  it('changeDate를 호출하면 date가 갱신되고 새 날짜로 재조회한다', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations.mockResolvedValue([]);

    const { result } = renderBoard(api);
    await waitFor(() => expect(result.current.date).toBe(DEFAULT_DATE));

    await act(async () => {
      await result.current.changeDate('2026-08-27');
    });

    expect(result.current.date).toBe('2026-08-27');
    expect(api.fetchReservations).toHaveBeenCalledWith(1, '2026-08-27');
  });
});
