import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import type { LoginResponse, MemberDto } from '../types';
import { authApi } from './auth';
import { ApiError } from './client';
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

function noContentResponse() {
  return {
    ok: true,
    status: 204,
    json: async () => {
      throw new Error('본문 없음');
    },
  };
}

const MEMBER: MemberDto = { id: 1, email: 'owner@sqisoft.com', name: '김본승' };
const LOGIN: LoginResponse = { token: 'opaque-token-1', member: MEMBER };

describe('authApi 성공 경로', () => {
  it('signup은 /api/auth/signup으로 이메일·비밀번호·이름을 POST하고 생성된 MemberDto를 반환한다', async () => {
    fetchMock.mockResolvedValue(jsonResponse(201, MEMBER) as unknown as Response);

    const result = await authApi.signup({
      email: 'owner@sqisoft.com',
      password: 'password123',
      name: '김본승',
    });

    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect(fetchMock).toHaveBeenCalledWith(
      '/api/auth/signup',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({
          email: 'owner@sqisoft.com',
          password: 'password123',
          name: '김본승',
        }),
      }),
    );
    expect(result).toEqual(MEMBER);
  });

  it('signup은 인증이 필요 없는 요청이라 저장된 토큰이 있어도 Authorization 헤더를 붙이지 않는다', async () => {
    writeToken('opaque-token-1');
    fetchMock.mockResolvedValue(jsonResponse(201, MEMBER) as unknown as Response);

    await authApi.signup({ email: 'a@b.com', password: 'password123', name: '박신규' });

    const [, init] = fetchMock.mock.calls[0] as [string, { headers: Record<string, string> }];
    expect(init.headers).not.toHaveProperty('Authorization');
  });

  it('login은 /api/auth/login으로 POST하고 토큰과 회원 정보를 담은 응답을 반환한다', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, LOGIN) as unknown as Response);

    const result = await authApi.login({ email: 'owner@sqisoft.com', password: 'password123' });

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/auth/login',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({ email: 'owner@sqisoft.com', password: 'password123' }),
      }),
    );
    expect(result).toEqual(LOGIN);
  });

  it('login은 토큰을 저장하지 않는다 — 저장 시점은 호출부(useAuth)가 정한다', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, LOGIN) as unknown as Response);

    await authApi.login({ email: 'owner@sqisoft.com', password: 'password123' });

    expect(readToken()).toBeNull();
  });

  it('me는 저장된 토큰을 Bearer로 실어 /api/auth/me를 조회하고 MemberDto를 반환한다', async () => {
    writeToken('opaque-token-1');
    fetchMock.mockResolvedValue(jsonResponse(200, MEMBER) as unknown as Response);

    const result = await authApi.me();

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/auth/me',
      expect.objectContaining({
        headers: expect.objectContaining({ Authorization: 'Bearer opaque-token-1' }),
      }),
    );
    expect(result).toEqual(MEMBER);
  });

  it('logout은 /api/auth/logout으로 Bearer POST하고 204 본문 없음에서도 예외 없이 완료된다', async () => {
    writeToken('opaque-token-1');
    fetchMock.mockResolvedValue(noContentResponse() as unknown as Response);

    await expect(authApi.logout()).resolves.toBeUndefined();

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/auth/logout',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({ Authorization: 'Bearer opaque-token-1' }),
      }),
    );
  });
});

describe('authApi 오류 경로', () => {
  it('401 + INVALID_CREDENTIALS 응답이면 login이 ApiError(INVALID_CREDENTIALS, 401)로 reject된다', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(401, {
        code: 'INVALID_CREDENTIALS',
        message: '이메일 또는 비밀번호가 올바르지 않습니다.',
      }) as unknown as Response,
    );

    await expect(
      authApi.login({ email: 'owner@sqisoft.com', password: 'wrong-password' }),
    ).rejects.toMatchObject({ code: 'INVALID_CREDENTIALS', status: 401 });
  });

  it('409 + EMAIL_ALREADY_REGISTERED 응답이면 signup이 ApiError로 reject되고 code가 그대로 담긴다', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(409, { code: 'EMAIL_ALREADY_REGISTERED' }) as unknown as Response,
    );

    const attempt = authApi.signup({ email: 'owner@sqisoft.com', password: 'password123', name: '김본승' });

    await expect(attempt).rejects.toBeInstanceOf(ApiError);
  });

  it('me가 401을 받으면 UNAUTHENTICATED로 reject되고 죽은 토큰은 저장소에서 지워진다', async () => {
    writeToken('만료된-토큰');
    fetchMock.mockResolvedValue(
      jsonResponse(401, { code: 'UNAUTHENTICATED' }) as unknown as Response,
    );

    await expect(authApi.me()).rejects.toMatchObject({ code: 'UNAUTHENTICATED', status: 401 });
    expect(readToken()).toBeNull();
  });
});
