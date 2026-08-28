import { describe, it, expect } from 'vitest';
import { generateTimeSlots, nextSlot, toServerDateTime } from './timeSlots';

describe('generateTimeSlots', () => {
  it('18개의 슬롯을 반환한다', () => {
    expect(generateTimeSlots().length).toBe(18);
  });

  it('첫 슬롯은 09:00, 마지막 슬롯은 17:30이다', () => {
    const slots = generateTimeSlots();
    expect(slots[0]).toBe('09:00');
    expect(slots[17]).toBe('17:30');
  });

  it('09:00부터 17:30까지 30분 간격 18개 슬롯과 정확히 일치한다', () => {
    expect(generateTimeSlots()).toEqual([
      '09:00', '09:30', '10:00', '10:30', '11:00', '11:30',
      '12:00', '12:30', '13:00', '13:30', '14:00', '14:30',
      '15:00', '15:30', '16:00', '16:30', '17:00', '17:30',
    ]);
  });
});

describe('nextSlot', () => {
  it('정시 슬롯의 다음은 30분 슬롯이다', () => {
    expect(nextSlot('09:00')).toBe('09:30');
  });

  it('30분 슬롯의 다음은 정시를 넘겨 다음 시각 슬롯이다', () => {
    expect(nextSlot('09:30')).toBe('10:00');
  });

  it('마지막 슬롯 17:30의 다음은 영업 종료 시각 18:00이다', () => {
    expect(nextSlot('17:30')).toBe('18:00');
  });
});

describe('toServerDateTime', () => {
  it('날짜와 HH:mm을 초 단위가 포함된 서버 전송용 문자열로 변환한다', () => {
    expect(toServerDateTime('2026-08-26', '09:00')).toBe('2026-08-26T09:00:00');
  });
});
