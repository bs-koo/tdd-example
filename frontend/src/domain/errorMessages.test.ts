import { describe, it, expect } from 'vitest';
import type { ErrorCode } from '../types';
import { ERROR_MESSAGES, DEFAULT_ERROR_MESSAGE, toKoreanMessage } from './errorMessages';

const CASES: Array<[ErrorCode, string]> = [
  ['OVERLAPPING_RESERVATION', '해당 시간대에 이미 예약이 있습니다.'],
  ['INVALID_TIME_UNIT', '예약 시각은 30분 단위여야 합니다.'],
  ['OUTSIDE_BUSINESS_HOURS', '09:00~18:00 사이만 예약할 수 있습니다.'],
  ['INVALID_DURATION', '예약은 30분 이상 4시간 이하여야 합니다.'],
  ['PAST_DATETIME', '지난 시간은 예약할 수 없습니다.'],
  ['TOO_FAR_IN_FUTURE', '14일 이내만 예약할 수 있습니다.'],
  ['NOT_RESERVER', '예약자 본인만 취소할 수 있습니다.'],
  ['ALREADY_CANCELLED', '이미 취소된 예약입니다.'],
  ['RESERVATION_NOT_FOUND', '예약을 찾을 수 없습니다.'],
  ['ROOM_NOT_FOUND', '회의실을 찾을 수 없습니다.'],
  ['INVALID_RESERVER_NAME', '예약자명은 1~20자로 입력해주세요.'],
  ['INVALID_PURPOSE_LENGTH', '회의 목적은 50자 이내로 입력해주세요.'],
  ['INVALID_DATE_FORMAT', '날짜 형식이 올바르지 않습니다.'],
];

describe('ERROR_MESSAGES', () => {
  it.each(CASES)('%s 코드는 정해진 한국어 문구로 매핑되어 있다', (code, message) => {
    expect(ERROR_MESSAGES[code]).toBe(message);
  });

  it('정확히 13종의 오류 코드를 가진다 (누락 방지)', () => {
    expect(Object.keys(ERROR_MESSAGES).length).toBe(13);
  });
});

describe('toKoreanMessage', () => {
  it('기본 오류 메시지는 확정된 문구다', () => {
    expect(DEFAULT_ERROR_MESSAGE).toBe('요청을 처리하지 못했습니다.');
  });

  it('알려지지 않은 코드는 기본 오류 메시지로 변환한다', () => {
    expect(toKoreanMessage('알 수 없는 코드')).toBe(DEFAULT_ERROR_MESSAGE);
  });

  it('OVERLAPPING_RESERVATION 코드를 대응하는 한국어 문구로 변환한다', () => {
    expect(toKoreanMessage('OVERLAPPING_RESERVATION')).toBe('해당 시간대에 이미 예약이 있습니다.');
  });
});
