import type { ErrorCode } from '../types';

export const ERROR_MESSAGES: Record<ErrorCode, string> = {
  OVERLAPPING_RESERVATION: '해당 시간대에 이미 예약이 있습니다.',
  INVALID_TIME_UNIT: '예약 시각은 30분 단위여야 합니다.',
  OUTSIDE_BUSINESS_HOURS: '09:00~18:00 사이만 예약할 수 있습니다.',
  INVALID_DURATION: '예약은 30분 이상 4시간 이하여야 합니다.',
  PAST_DATETIME: '지난 시간은 예약할 수 없습니다.',
  TOO_FAR_IN_FUTURE: '14일 이내만 예약할 수 있습니다.',
  NOT_RESERVER: '예약자 본인만 취소할 수 있습니다.',
  ALREADY_CANCELLED: '이미 취소된 예약입니다.',
  RESERVATION_NOT_FOUND: '예약을 찾을 수 없습니다.',
  ROOM_NOT_FOUND: '회의실을 찾을 수 없습니다.',
  INVALID_RESERVER_NAME: '예약자명은 1~20자로 입력해주세요.',
  INVALID_PURPOSE_LENGTH: '회의 목적은 50자 이내로 입력해주세요.',
  INVALID_DATE_FORMAT: '날짜 형식이 올바르지 않습니다.',
};

export const DEFAULT_ERROR_MESSAGE = '요청을 처리하지 못했습니다.';

function isKnownErrorCode(code: string): code is ErrorCode {
  return Object.prototype.hasOwnProperty.call(ERROR_MESSAGES, code);
}

export function toKoreanMessage(code: string): string {
  return isKnownErrorCode(code) ? ERROR_MESSAGES[code] : DEFAULT_ERROR_MESSAGE;
}
