import { describe, it, expect, vi, afterEach } from 'vitest';
import { today } from './today';

afterEach(() => {
  vi.useRealTimers();
});

describe('today', () => {
  it('UTC 자정 1초 전 시각에서는 아직 KST로 전날 23시 59분 59초라 전날 날짜를 반환한다', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-08-26T14:59:59Z'));

    expect(today()).toBe('2026-08-26');
  });

  it('UTC 15시 정각이 되어 KST 자정을 넘기면 날짜가 다음 날로 바뀐다', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-08-26T15:00:00Z'));

    expect(today()).toBe('2026-08-27');
  });

  it('연초 KST 오전 9시에는 한 자리 월·일도 0으로 패딩되어 반환된다', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-01-01T00:00:00Z'));

    expect(today()).toBe('2026-01-01');
  });

  it('반환 형식은 YYYY-MM-DD 이다', () => {
    vi.useFakeTimers();
    vi.setSystemTime(new Date('2026-08-26T15:00:00Z'));

    expect(today()).toMatch(/^\d{4}-\d{2}-\d{2}$/);
  });
});
