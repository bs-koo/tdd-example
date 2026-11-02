import type { LoginResponse, MemberDto } from '../types';
import type { AuthApi } from './types';
import { authorizedFetch, throwApiError } from './client';

const JSON_HEADERS = { 'Content-Type': 'application/json' };

export const authApi: AuthApi = {
  // signup·login 은 토큰을 얻기 전의 요청이라 맨 fetch 를 쓴다.
  async signup(request: { email: string; password: string; name: string }): Promise<MemberDto> {
    const response = await fetch('/api/auth/signup', {
      method: 'POST',
      headers: JSON_HEADERS,
      body: JSON.stringify(request),
    });
    if (!response.ok) {
      await throwApiError(response);
    }
    return (await response.json()) as MemberDto;
  },

  async login(request: { email: string; password: string }): Promise<LoginResponse> {
    const response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: JSON_HEADERS,
      body: JSON.stringify(request),
    });
    if (!response.ok) {
      await throwApiError(response);
    }
    return (await response.json()) as LoginResponse;
  },

  async logout(): Promise<void> {
    const response = await authorizedFetch('/api/auth/logout', { method: 'POST' });
    if (!response.ok) {
      await throwApiError(response);
    }
  },

  async me(): Promise<MemberDto> {
    const response = await authorizedFetch('/api/auth/me');
    if (!response.ok) {
      await throwApiError(response);
    }
    return (await response.json()) as MemberDto;
  },
};
