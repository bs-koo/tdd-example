import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import type {
  CreateReservationRequest,
  CreateRoomRequest,
  ReservationDto,
  RoomDto,
} from '../types';
import { reservationApi } from './reservations';
import { ApiError } from './client';
import { writeToken } from '../auth/tokenStorage';

const fetchMock = vi.fn();

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock);
  fetchMock.mockReset();
  localStorage.clear();
  writeToken('opaque-token-1');
});

afterEach(() => {
  vi.unstubAllGlobals();
  localStorage.clear();
});

function jsonResponse(status: number, body: unknown) {
  return { ok: status >= 200 && status < 300, status, json: async () => body };
}

function noContentResponse() {
  return {
    ok: true,
    status: 204,
    json: async () => {
      throw new Error('본문 없음');
    },
  };
}

function brokenJsonResponse(status: number) {
  return {
    ok: status >= 200 && status < 300,
    status,
    json: async () => {
      throw new Error('JSON 파싱 실패');
    },
  };
}

const BEARER = expect.objectContaining({
  headers: expect.objectContaining({ Authorization: 'Bearer opaque-token-1' }),
});

const ROOMS: RoomDto[] = [
  { id: 1, groupId: 1, name: '대회의실', capacity: 20, location: '3층' },
  { id: 2, groupId: 1, name: '중회의실', capacity: 10, location: '3층' },
];

const RESERVATIONS: ReservationDto[] = [
  {
    id: 1,
    roomId: 1,
    memberId: 1,
    reserverName: '김본승',
    purpose: '주간 회의',
    startAt: '2026-08-26T09:00:00',
    endAt: '2026-08-26T10:00:00',
    status: 'ACTIVE',
  },
];

const CREATE_REQUEST: CreateReservationRequest = {
  roomId: 1,
  purpose: '주간 회의',
  startAt: '2026-08-26T09:00:00',
  endAt: '2026-08-26T09:30:00',
};

const CREATED_RESERVATION: ReservationDto = {
  id: 10,
  roomId: 1,
  memberId: 1,
  reserverName: '김본승',
  purpose: '주간 회의',
  startAt: '2026-08-26T09:00:00',
  endAt: '2026-08-26T09:30:00',
  status: 'ACTIVE',
};

const CREATE_ROOM_REQUEST: CreateRoomRequest = {
  name: '대회의실',
  capacity: 20,
  location: '3층',
};

describe('reservationApi 성공 경로', () => {
  it('fetchRooms(1)은 그룹 문맥의 /api/groups/1/rooms를 Bearer로 호출하고 200 응답의 배열을 그대로 반환한다', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, ROOMS) as unknown as Response);

    const result = await reservationApi.fetchRooms(1);

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith('/api/groups/1/rooms', BEARER);
    expect(result).toEqual(ROOMS);
  });

  it('createRoom(1, 요청)은 /api/groups/1/rooms에 POST하고 생성된 회의실을 반환한다', async () => {
    fetchMock.mockResolvedValue(jsonResponse(201, ROOMS[0]) as unknown as Response);

    const result = await reservationApi.createRoom(1, CREATE_ROOM_REQUEST);

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/groups/1/rooms',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
          Authorization: 'Bearer opaque-token-1',
        }),
        body: JSON.stringify(CREATE_ROOM_REQUEST),
      }),
    );
    expect(result).toEqual(ROOMS[0]);
  });

  it('fetchReservations(1, "2026-08-26")는 쿼리스트링이 포함된 URL을 호출하고 배열을 반환한다', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, RESERVATIONS) as unknown as Response);

    const result = await reservationApi.fetchReservations(1, '2026-08-26');

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith('/api/rooms/1/reservations?date=2026-08-26', BEARER);
    expect(result).toEqual(RESERVATIONS);
  });

  it('createReservation은 POST로 요청 바디를 JSON 직렬화해 보내고, 201 응답의 ReservationDto를 반환한다', async () => {
    fetchMock.mockResolvedValue(jsonResponse(201, CREATED_RESERVATION) as unknown as Response);

    const result = await reservationApi.createReservation(CREATE_REQUEST);

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/reservations',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
        body: JSON.stringify(CREATE_REQUEST),
      }),
    );
    expect(result).toEqual(CREATED_RESERVATION);
  });

  it('createReservation 요청 바디에 reserverName이 들어가지 않는다 — 예약자는 서버가 토큰에서 유도한다', async () => {
    fetchMock.mockResolvedValue(jsonResponse(201, CREATED_RESERVATION) as unknown as Response);

    await reservationApi.createReservation(CREATE_REQUEST);

    const [, init] = fetchMock.mock.calls[0] as [string, { body: string }];
    expect(JSON.parse(init.body)).not.toHaveProperty('reserverName');
  });

  it('cancelReservation(5)는 .../5/cancel로 바디 없이 POST하고 204 본문 없음에서도 예외 없이 완료된다', async () => {
    fetchMock.mockResolvedValue(noContentResponse() as unknown as Response);

    await expect(reservationApi.cancelReservation(5)).resolves.toBeUndefined();

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/reservations/5/cancel',
      expect.objectContaining({ method: 'POST' }),
    );
    const [, init] = fetchMock.mock.calls[0] as [string, { body?: string }];
    expect(init.body).toBeUndefined();
  });
});

describe('reservationApi 오류 경로', () => {
  it('409 + OVERLAPPING_RESERVATION 응답이면 createReservation이 ApiError로 reject되고 code·status가 그대로 담긴다', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(409, {
        code: 'OVERLAPPING_RESERVATION',
        message: '해당 시간대에 이미 예약이 있습니다.',
      }) as unknown as Response,
    );

    await expect(reservationApi.createReservation(CREATE_REQUEST)).rejects.toBeInstanceOf(ApiError);
    await expect(reservationApi.createReservation(CREATE_REQUEST)).rejects.toMatchObject({
      code: 'OVERLAPPING_RESERVATION',
      status: 409,
    });
  });

  it('404 + ROOM_NOT_FOUND 응답이면 fetchReservations가 ApiError(ROOM_NOT_FOUND, 404)로 reject된다', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(404, { code: 'ROOM_NOT_FOUND', message: '회의실을 찾을 수 없습니다.' }) as unknown as Response,
    );

    await expect(reservationApi.fetchReservations(999, '2026-08-26')).rejects.toBeInstanceOf(ApiError);
    await expect(reservationApi.fetchReservations(999, '2026-08-26')).rejects.toMatchObject({
      code: 'ROOM_NOT_FOUND',
      status: 404,
    });
  });

  it('403 + NOT_GROUP_MEMBER 응답이면 fetchRooms가 해당 code로 reject된다', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(403, { code: 'NOT_GROUP_MEMBER' }) as unknown as Response,
    );

    await expect(reservationApi.fetchRooms(9)).rejects.toMatchObject({
      code: 'NOT_GROUP_MEMBER',
      status: 403,
    });
  });

  it('400 + INVALID_ROOM_CAPACITY 응답이면 createRoom이 해당 code로 reject된다', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(400, { code: 'INVALID_ROOM_CAPACITY' }) as unknown as Response,
    );

    await expect(
      reservationApi.createRoom(1, { name: '대회의실', capacity: 0, location: '3층' }),
    ).rejects.toMatchObject({ code: 'INVALID_ROOM_CAPACITY', status: 400 });
  });

  it('500 응답 본문을 JSON으로 읽지 못하면 code가 UNKNOWN, status가 500인 ApiError로 reject된다', async () => {
    fetchMock.mockResolvedValue(brokenJsonResponse(500) as unknown as Response);

    await expect(reservationApi.fetchRooms(1)).rejects.toBeInstanceOf(ApiError);
    await expect(reservationApi.fetchRooms(1)).rejects.toMatchObject({
      code: 'UNKNOWN',
      status: 500,
    });
  });

  it('401 응답이면 어느 호출이든 UNAUTHENTICATED로 reject된다 — 만료 토큰은 authorizedFetch가 가로챈다', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(401, { code: 'UNAUTHENTICATED' }) as unknown as Response,
    );

    await expect(reservationApi.fetchRooms(1)).rejects.toMatchObject({
      code: 'UNAUTHENTICATED',
      status: 401,
    });
  });
});
