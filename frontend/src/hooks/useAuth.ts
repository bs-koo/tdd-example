import { useCallback, useEffect, useState } from 'react';
import type { MemberDto } from '../types';
import type { AuthApi } from '../api/types';
import { ApiError } from '../api/client';
import { clearToken, readToken, writeToken } from '../auth/tokenStorage';

function toErrorCode(e: unknown): string {
  return e instanceof ApiError ? e.code : 'UNKNOWN';
}

export type AuthStatus = 'loading' | 'anonymous' | 'authenticated';

export type Auth = {
  member: MemberDto | null;
  status: AuthStatus;
  errorCode: string | null;
  login: (request: { email: string; password: string }) => Promise<void>;
  signup: (request: { email: string; password: string; name: string }) => Promise<void>;
  logout: () => Promise<void>;
  clearError: () => void;
};

export function useAuth(options: { api: AuthApi }): Auth {
  const { api } = options;
  const [member, setMember] = useState<MemberDto | null>(null);
  // 토큰이 없다는 것은 마운트 시점에 이미 아는 사실이다. 그때는 loading 을 거치지 않는다 —
  // 한 프레임짜리 로딩 표시가 깜빡이는 편이 더 어수선하다.
  const [status, setStatus] = useState<AuthStatus>(() =>
    readToken() === null ? 'anonymous' : 'loading',
  );
  const [errorCode, setErrorCode] = useState<string | null>(null);

  useEffect(() => {
    if (readToken() === null) {
      return;
    }
    api
      .me()
      .then((restored) => {
        setMember(restored);
        setStatus('authenticated');
      })
      .catch(() => {
        // 복원 실패는 사용자가 시킨 조작이 아니다. 오류로 알리지 않고 조용히 로그인 화면으로 보낸다.
        clearToken();
        setMember(null);
        setStatus('anonymous');
      });
    // 마운트 시 1회만 복원한다.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const login = useCallback(
    async (request: { email: string; password: string }) => {
      try {
        const { token, member: loggedIn } = await api.login(request);
        writeToken(token);
        setMember(loggedIn);
        setStatus('authenticated');
        setErrorCode(null);
      } catch (e) {
        setErrorCode(toErrorCode(e));
      }
    },
    [api],
  );

  const signup = useCallback(
    async (request: { email: string; password: string; name: string }) => {
      try {
        await api.signup(request);
      } catch (e) {
        setErrorCode(toErrorCode(e));
        return;
      }
      // 가입 직후 다시 자격을 묻지 않는다 — 방금 입력한 것을 그대로 써서 로그인까지 잇는다.
      await login({ email: request.email, password: request.password });
    },
    [api, login],
  );

  const logout = useCallback(async () => {
    try {
      await api.logout();
    } catch {
      // 서버가 거절해도(이미 만료된 토큰 등) 로그아웃은 되돌릴 수 없어야 한다.
    }
    clearToken();
    setMember(null);
    setStatus('anonymous');
    setErrorCode(null);
  }, [api]);

  const clearError = useCallback(() => setErrorCode(null), []);

  return { member, status, errorCode, login, signup, logout, clearError };
}
