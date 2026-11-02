import { describe, it, expect, beforeEach, afterEach } from 'vitest';
import { TOKEN_KEY, readToken, writeToken, clearToken } from './tokenStorage';

beforeEach(() => {
  localStorage.clear();
});

afterEach(() => {
  localStorage.clear();
});

/** getItem/setItem/removeItem 이 모두 던지는 저장소로 갈아 끼운다(프라이빗 모드 재현). */
function breakLocalStorage() {
  const original = Object.getOwnPropertyDescriptor(window, 'localStorage');
  const throwing = {
    getItem: () => {
      throw new Error('SecurityError: 저장소 접근 거부');
    },
    setItem: () => {
      throw new Error('QuotaExceededError: 저장소 접근 거부');
    },
    removeItem: () => {
      throw new Error('SecurityError: 저장소 접근 거부');
    },
  };
  Object.defineProperty(window, 'localStorage', { value: throwing, configurable: true });
  return () => {
    if (original !== undefined) {
      Object.defineProperty(window, 'localStorage', original);
    }
  };
}

describe('tokenStorage', () => {
  it('토큰은 localStorage 키 roomReservation.token 에 저장된다', () => {
    writeToken('opaque-token-1');

    expect(TOKEN_KEY).toBe('roomReservation.token');
    expect(localStorage.getItem('roomReservation.token')).toBe('opaque-token-1');
  });

  it('저장한 토큰을 readToken이 그대로 돌려준다', () => {
    writeToken('opaque-token-1');

    expect(readToken()).toBe('opaque-token-1');
  });

  it('저장된 토큰이 없으면 readToken은 null을 반환한다', () => {
    expect(readToken()).toBeNull();
  });

  it('clearToken 이후 readToken은 null을 반환하고 저장소에서도 키가 사라진다', () => {
    writeToken('opaque-token-1');

    clearToken();

    expect(readToken()).toBeNull();
    expect(localStorage.getItem('roomReservation.token')).toBeNull();
  });

  it('저장소 접근이 예외를 던지는 환경(프라이빗 모드)에서도 readToken은 던지지 않고 null을 반환한다', () => {
    const restore = breakLocalStorage();
    try {
      expect(readToken()).toBeNull();
    } finally {
      restore();
    }
  });

  it('저장소 접근이 예외를 던져도 writeToken·clearToken은 앱을 죽이지 않는다', () => {
    const restore = breakLocalStorage();
    try {
      expect(() => writeToken('opaque-token-1')).not.toThrow();
      expect(() => clearToken()).not.toThrow();
    } finally {
      restore();
    }
  });
});
