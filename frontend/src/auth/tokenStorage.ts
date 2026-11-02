/**
 * 발급받은 불투명 토큰의 유일한 보관처.
 *
 * 프라이빗 모드·저장소 차단 환경에서는 localStorage 접근 자체가 던진다. 토큰을 못 읽는 것은
 * "로그인 안 된 상태"와 같은 뜻이므로, 예외를 삼키고 null 로 떨어뜨려 앱을 계속 살려 둔다.
 */
export const TOKEN_KEY = 'roomReservation.token';

export function readToken(): string | null {
  try {
    return localStorage.getItem(TOKEN_KEY);
  } catch {
    return null;
  }
}

export function writeToken(token: string): void {
  try {
    localStorage.setItem(TOKEN_KEY, token);
  } catch {
    // 저장에 실패하면 이번 세션 안에서만 로그인이 유지된다. 앱을 멈추지는 않는다.
  }
}

export function clearToken(): void {
  try {
    localStorage.removeItem(TOKEN_KEY);
  } catch {
    // 지울 수 없는 저장소라면 애초에 저장되지도 않았다.
  }
}
