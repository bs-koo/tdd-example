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
  ['INVALID_PURPOSE_LENGTH', '회의 목적은 50자 이내로 입력해주세요.'],
  ['INVALID_DATE_FORMAT', '날짜 형식이 올바르지 않습니다.'],
  ['UNAUTHENTICATED', '로그인이 필요합니다.'],
  ['INVALID_CREDENTIALS', '이메일 또는 비밀번호가 올바르지 않습니다.'],
  ['EMAIL_ALREADY_REGISTERED', '이미 가입된 이메일입니다.'],
  ['INVALID_EMAIL_FORMAT', '이메일 형식이 올바르지 않습니다.'],
  ['INVALID_PASSWORD_LENGTH', '비밀번호는 8자 이상 64자 이하로 입력해주세요.'],
  ['INVALID_MEMBER_NAME', '이름은 1~20자로 입력해주세요.'],
  ['MEMBER_NOT_FOUND', '해당 이메일의 사용자를 찾을 수 없습니다.'],
  ['GROUP_NOT_FOUND', '그룹을 찾을 수 없습니다.'],
  ['INVALID_GROUP_NAME', '그룹 이름은 1~30자로 입력해주세요.'],
  ['NOT_GROUP_OWNER', '그룹장만 할 수 있는 작업입니다.'],
  ['NOT_GROUP_MEMBER', '승인된 그룹원만 할 수 있는 작업입니다.'],
  ['ALREADY_INVITED', '이미 초대했거나 참여 중인 사용자입니다.'],
  ['SELF_INVITATION', '자기 자신은 초대할 수 없습니다.'],
  ['INVITATION_NOT_FOUND', '초대를 찾을 수 없습니다.'],
  ['NOT_INVITEE', '초대받은 본인만 응답할 수 있습니다.'],
  ['INVITATION_ALREADY_RESPONDED', '이미 응답한 초대입니다.'],
  ['INVALID_ROOM_NAME', '회의실 이름은 1~30자로 입력해주세요.'],
  ['INVALID_ROOM_CAPACITY', '수용 인원은 1명 이상 999명 이하여야 합니다.'],
  ['INVALID_ROOM_LOCATION', '위치는 1~30자로 입력해주세요.'],
];

describe('ERROR_MESSAGES', () => {
  it.each(CASES)('%s 코드는 정해진 한국어 문구로 매핑되어 있다', (code, message) => {
    expect(ERROR_MESSAGES[code]).toBe(message);
  });

  it('정확히 31종의 오류 코드를 가진다 (누락 방지)', () => {
    expect(Object.keys(ERROR_MESSAGES).length).toBe(31);
  });

  it('예약자명이 요청 필드에서 사라졌으므로 INVALID_RESERVER_NAME은 더 이상 존재하지 않는다', () => {
    expect(Object.keys(ERROR_MESSAGES)).not.toContain('INVALID_RESERVER_NAME');
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
