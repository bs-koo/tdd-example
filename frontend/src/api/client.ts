import { readToken, clearToken } from '../auth/tokenStorage';

export class ApiError extends Error {
  readonly code: string;
  readonly status: number;

  constructor(code: string, status: number, message?: string) {
    super(message);
    this.name = 'ApiError';
    this.code = code;
    this.status = status;
  }
}

export type AuthorizedInit = Omit<RequestInit, 'headers'> & { headers?: Record<string, string> };

/**
 * 보호 엔드포인트 전용 fetch. 토큰을 붙이고, 죽은 토큰을 스스로 정리한다.
 *
 * 401 만 여기서 가로챈다 — 토큰이 무효하다는 사실은 어느 화면에서 났든 대응이 같기 때문이다
 * (저장소를 비우고 로그인으로 되돌린다). 그 밖의 실패는 코드마다 문구와 다음 행동이 달라서
 * 응답을 그대로 넘기고 각 API 가 throwApiError 로 해석한다.
 */
export async function authorizedFetch(input: string, init: AuthorizedInit = {}): Promise<Response> {
  const token = readToken();
  const headers = { ...init.headers, ...(token !== null ? { Authorization: `Bearer ${token}` } : {}) };

  const response = await fetch(input, { ...init, headers });
  if (response.status === 401) {
    clearToken();
    throw new ApiError('UNAUTHENTICATED', 401);
  }
  return response;
}

export async function throwApiError(response: Response): Promise<never> {
  let body: { code: string; message?: string };
  try {
    body = (await response.json()) as { code: string; message?: string };
  } catch {
    throw new ApiError('UNKNOWN', response.status);
  }
  throw new ApiError(body.code, response.status, body.message);
}
