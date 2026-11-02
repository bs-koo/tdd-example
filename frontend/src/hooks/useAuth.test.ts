import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import type { MemberDto } from '../types';
import type { AuthApi } from '../api/types';
import { ApiError } from '../api/client';
import { readToken, writeToken } from '../auth/tokenStorage';
import { useAuth } from './useAuth';

const MEMBER: MemberDto = { id: 1, email: 'owner@sqisoft.com', name: '김본승' };

beforeEach(() => {
  localStorage.clear();
});

afterEach(() => {
  localStorage.clear();
});

function createFakeApi() {
  return {
    signup: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
    me: vi.fn(),
  };
}

function renderAuth(fakeApi: ReturnType<typeof createFakeApi>) {
  return renderHook(() => useAuth({ api: fakeApi as unknown as AuthApi }));
}

describe('useAuth 세션 복원', () => {
  it('저장된 토큰이 없으면 me를 호출하지 않고 곧바로 anonymous가 된다', async () => {
    const api = createFakeApi();

    const { result } = renderAuth(api);

    await waitFor(() => expect(result.current.status).toBe('anonymous'));
    expect(api.me).not.toHaveBeenCalled();
    expect(result.current.member).toBeNull();
  });

  it('저장된 토큰이 있으면 me로 사용자를 복원해 authenticated가 되고 member가 채워진다', async () => {
    const api = createFakeApi();
    api.me.mockResolvedValue(MEMBER);
    writeToken('opaque-token-1');

    const { result } = renderAuth(api);

    await waitFor(() => expect(result.current.status).toBe('authenticated'));
    expect(api.me).toHaveBeenCalledTimes(1);
    expect(result.current.member).toEqual(MEMBER);
  });

  it('복원 전에는 status가 loading이라 화면이 로그인 폼을 깜빡이지 않는다', () => {
    const api = createFakeApi();
    api.me.mockReturnValue(new Promise(() => {}));
    writeToken('opaque-token-1');

    const { result } = renderAuth(api);

    expect(result.current.status).toBe('loading');
  });

  it('토큰이 만료돼 me가 실패하면 anonymous로 떨어지고 member는 null로 남는다', async () => {
    const api = createFakeApi();
    api.me.mockRejectedValue(new ApiError('UNAUTHENTICATED', 401));
    writeToken('만료된-토큰');

    const { result } = renderAuth(api);

    await waitFor(() => expect(result.current.status).toBe('anonymous'));
    expect(result.current.member).toBeNull();
  });

  it('세션 복원 실패는 오류로 알리지 않는다 — 사용자가 한 조작이 아니므로 errorCode는 null이다', async () => {
    const api = createFakeApi();
    api.me.mockRejectedValue(new ApiError('UNAUTHENTICATED', 401));
    writeToken('만료된-토큰');

    const { result } = renderAuth(api);

    await waitFor(() => expect(result.current.status).toBe('anonymous'));
    expect(result.current.errorCode).toBeNull();
  });
});

describe('useAuth 로그인', () => {
  it('login이 성공하면 토큰이 저장되고 authenticated 상태로 member가 채워진다', async () => {
    const api = createFakeApi();
    api.login.mockResolvedValue({ token: 'opaque-token-1', member: MEMBER });

    const { result } = renderAuth(api);
    await waitFor(() => expect(result.current.status).toBe('anonymous'));

    await act(async () => {
      await result.current.login({ email: 'owner@sqisoft.com', password: 'password123' });
    });

    expect(api.login).toHaveBeenCalledWith({ email: 'owner@sqisoft.com', password: 'password123' });
    expect(readToken()).toBe('opaque-token-1');
    expect(result.current.status).toBe('authenticated');
    expect(result.current.member).toEqual(MEMBER);
  });

  it('login이 실패하면 errorCode가 채워지고 anonymous에 머물며 토큰도 저장되지 않는다', async () => {
    const api = createFakeApi();
    api.login.mockRejectedValue(new ApiError('INVALID_CREDENTIALS', 401));

    const { result } = renderAuth(api);
    await waitFor(() => expect(result.current.status).toBe('anonymous'));

    await act(async () => {
      await result.current.login({ email: 'owner@sqisoft.com', password: 'wrong-password' });
    });

    expect(result.current.errorCode).toBe('INVALID_CREDENTIALS');
    expect(result.current.status).toBe('anonymous');
    expect(readToken()).toBeNull();
  });

  it('login이 실패해도 훅의 메서드는 reject하지 않고 정상 resolve한다', async () => {
    const api = createFakeApi();
    api.login.mockRejectedValue(new ApiError('INVALID_CREDENTIALS', 401));

    const { result } = renderAuth(api);
    await waitFor(() => expect(result.current.status).toBe('anonymous'));

    await act(async () => {
      await expect(
        result.current.login({ email: 'owner@sqisoft.com', password: 'wrong-password' }),
      ).resolves.toBeUndefined();
    });
  });

  it('ApiError가 아닌 예외로 실패하면 errorCode가 UNKNOWN이 된다', async () => {
    const api = createFakeApi();
    api.login.mockRejectedValue(new Error('네트워크 끊김'));

    const { result } = renderAuth(api);
    await waitFor(() => expect(result.current.status).toBe('anonymous'));

    await act(async () => {
      await result.current.login({ email: 'owner@sqisoft.com', password: 'password123' });
    });

    expect(result.current.errorCode).toBe('UNKNOWN');
  });

  it('clearError를 호출하면 errorCode가 null로 돌아간다', async () => {
    const api = createFakeApi();
    api.login.mockRejectedValue(new ApiError('INVALID_CREDENTIALS', 401));

    const { result } = renderAuth(api);
    await waitFor(() => expect(result.current.status).toBe('anonymous'));
    await act(async () => {
      await result.current.login({ email: 'owner@sqisoft.com', password: 'wrong-password' });
    });

    act(() => {
      result.current.clearError();
    });

    expect(result.current.errorCode).toBeNull();
  });
});

describe('useAuth 회원가입', () => {
  it('signup이 성공하면 같은 자격으로 이어서 로그인해 바로 authenticated가 된다', async () => {
    const api = createFakeApi();
    api.signup.mockResolvedValue(MEMBER);
    api.login.mockResolvedValue({ token: 'opaque-token-1', member: MEMBER });

    const { result } = renderAuth(api);
    await waitFor(() => expect(result.current.status).toBe('anonymous'));

    await act(async () => {
      await result.current.signup({
        email: 'owner@sqisoft.com',
        password: 'password123',
        name: '김본승',
      });
    });

    expect(api.signup).toHaveBeenCalledWith({
      email: 'owner@sqisoft.com',
      password: 'password123',
      name: '김본승',
    });
    expect(api.login).toHaveBeenCalledWith({ email: 'owner@sqisoft.com', password: 'password123' });
    expect(result.current.status).toBe('authenticated');
    expect(result.current.member).toEqual(MEMBER);
  });

  it('signup이 이미 가입된 이메일로 거절되면 로그인을 시도하지 않고 errorCode만 남는다', async () => {
    const api = createFakeApi();
    api.signup.mockRejectedValue(new ApiError('EMAIL_ALREADY_REGISTERED', 409));

    const { result } = renderAuth(api);
    await waitFor(() => expect(result.current.status).toBe('anonymous'));

    await act(async () => {
      await result.current.signup({
        email: 'owner@sqisoft.com',
        password: 'password123',
        name: '김본승',
      });
    });

    expect(api.login).not.toHaveBeenCalled();
    expect(result.current.errorCode).toBe('EMAIL_ALREADY_REGISTERED');
    expect(result.current.status).toBe('anonymous');
  });
});

describe('useAuth 로그아웃', () => {
  it('logout은 서버에 알린 뒤 토큰을 지우고 anonymous로 되돌린다', async () => {
    const api = createFakeApi();
    api.me.mockResolvedValue(MEMBER);
    api.logout.mockResolvedValue(undefined);
    writeToken('opaque-token-1');

    const { result } = renderAuth(api);
    await waitFor(() => expect(result.current.status).toBe('authenticated'));

    await act(async () => {
      await result.current.logout();
    });

    expect(api.logout).toHaveBeenCalledTimes(1);
    expect(readToken()).toBeNull();
    expect(result.current.status).toBe('anonymous');
    expect(result.current.member).toBeNull();
  });

  it('서버 로그아웃이 실패해도 로컬 토큰을 지우고 anonymous가 된다 — 로그아웃은 사용자가 되돌릴 수 없어야 한다', async () => {
    const api = createFakeApi();
    api.me.mockResolvedValue(MEMBER);
    api.logout.mockRejectedValue(new ApiError('UNAUTHENTICATED', 401));
    writeToken('opaque-token-1');

    const { result } = renderAuth(api);
    await waitFor(() => expect(result.current.status).toBe('authenticated'));

    await act(async () => {
      await result.current.logout();
    });

    expect(readToken()).toBeNull();
    expect(result.current.status).toBe('anonymous');
  });
});
