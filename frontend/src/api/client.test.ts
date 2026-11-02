import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { ApiError, authorizedFetch } from './client';
import { readToken, writeToken } from '../auth/tokenStorage';

const fetchMock = vi.fn();

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock);
  fetchMock.mockReset();
  localStorage.clear();
});

afterEach(() => {
  vi.unstubAllGlobals();
  localStorage.clear();
});

function jsonResponse(status: number, body: unknown) {
  return { ok: status >= 200 && status < 300, status, json: async () => body };
}

describe('ApiError', () => {
  it('code와 status를 생성자 인자로 그대로 보관한다', () => {
    const error = new ApiError('OVERLAPPING_RESERVATION', 409);

    expect(error.code).toBe('OVERLAPPING_RESERVATION');
    expect(error.status).toBe(409);
  });

  it('Error의 인스턴스다 — 호출부가 catch(e)에서 표준 처리를 할 수 있어야 한다', () => {
    const error = new ApiError('ROOM_NOT_FOUND', 404);

    expect(error).toBeInstanceOf(Error);
  });

  it('message를 넘기면 error.message에 담긴다', () => {
    const error = new ApiError('ROOM_NOT_FOUND', 404, '회의실을 찾을 수 없습니다.');

    expect(error.message).toBe('회의실을 찾을 수 없습니다.');
  });

  it('message를 생략해도 인스턴스가 생성되고 code·status는 유지된다', () => {
    const error = new ApiError('UNKNOWN', 500);

    expect(error).toBeInstanceOf(ApiError);
    expect(error.code).toBe('UNKNOWN');
    expect(error.status).toBe(500);
  });
});

describe('authorizedFetch', () => {
  it('저장된 토큰이 있으면 Authorization 헤더에 Bearer 토큰을 실어 보낸다', async () => {
    writeToken('opaque-token-1');
    fetchMock.mockResolvedValue(jsonResponse(200, []) as unknown as Response);

    await authorizedFetch('/api/groups');

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/groups',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer opaque-token-1' }),
      }),
    );
  });

  it('저장된 토큰이 없으면 Authorization 헤더를 붙이지 않는다', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, []) as unknown as Response);

    await authorizedFetch('/api/groups');

    const [, init] = fetchMock.mock.calls[0] as [string, { headers: Record<string, string> }];
    expect(init.headers).not.toHaveProperty('Authorization');
  });

  it('호출부가 넘긴 메서드·바디·헤더를 그대로 유지한 채 Authorization만 더한다', async () => {
    writeToken('opaque-token-1');
    fetchMock.mockResolvedValue(jsonResponse(201, { id: 1 }) as unknown as Response);

    await authorizedFetch('/api/groups', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ name: '개발팀' }),
    });

    expect(fetchMock).toHaveBeenCalledWith('/api/groups', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer opaque-token-1',
      },
      body: JSON.stringify({ name: '개발팀' }),
    });
  });

  it('성공 응답은 가공하지 않고 Response 그대로 돌려준다 — 본문 파싱은 호출부의 몫이다', async () => {
    const response = jsonResponse(200, [{ id: 1 }]);
    fetchMock.mockResolvedValue(response as unknown as Response);

    const result = await authorizedFetch('/api/groups');

    expect(result).toBe(response);
  });

  it('401 응답이면 ApiError(UNAUTHENTICATED, 401)로 reject된다', async () => {
    writeToken('만료된-토큰');
    fetchMock.mockResolvedValue(
      jsonResponse(401, { code: 'UNAUTHENTICATED', message: '로그인이 필요합니다.' }) as unknown as Response,
    );

    await expect(authorizedFetch('/api/groups')).rejects.toBeInstanceOf(ApiError);
    await expect(authorizedFetch('/api/groups')).rejects.toMatchObject({
      code: 'UNAUTHENTICATED',
      status: 401,
    });
  });

  it('401을 받으면 저장된 토큰을 지운다 — 죽은 토큰으로 재시도하지 않게 한다', async () => {
    writeToken('만료된-토큰');
    fetchMock.mockResolvedValue(
      jsonResponse(401, { code: 'UNAUTHENTICATED' }) as unknown as Response,
    );

    await expect(authorizedFetch('/api/groups')).rejects.toBeInstanceOf(ApiError);

    expect(readToken()).toBeNull();
  });

  it('401이 아닌 실패 응답은 던지지 않고 그대로 반환한다 — 코드 해석은 각 API가 맡는다', async () => {
    writeToken('opaque-token-1');
    const response = jsonResponse(403, { code: 'NOT_GROUP_MEMBER' });
    fetchMock.mockResolvedValue(response as unknown as Response);

    const result = await authorizedFetch('/api/groups/1/rooms');

    expect(result).toBe(response);
    expect(readToken()).toBe('opaque-token-1');
  });
});
