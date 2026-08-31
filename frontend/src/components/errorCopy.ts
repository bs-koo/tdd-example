import { toKoreanMessage } from '../domain/errorMessages';

/**
 * 무엇이 잘못됐는지는 domain/errorMessages.ts 가 말한다(그 문구는 원본이라 건드리지 않는다).
 * 여기에는 "그래서 지금 무엇을 하면 되는지"만 둔다.
 *
 * 페이지 배너와 다이얼로그 안 오류 블록이 같은 문구를 써야 하므로 컴포넌트 밖에 둔다.
 */
const NEXT_ACTIONS: Record<string, string | undefined> = {
  OVERLAPPING_RESERVATION: '격자에서 비어 있는 다른 시간대를 골라 주세요.',
  INVALID_TIME_UNIT: '종료 시각을 10:00, 10:30처럼 30분 단위로 맞춰 주세요.',
  OUTSIDE_BUSINESS_HOURS: '시작과 종료를 09:00에서 18:00 사이로 다시 잡아 주세요.',
  INVALID_DURATION: '종료 시각을 시작 시각으로부터 30분 이상 4시간 이내로 바꿔 주세요.',
  PAST_DATETIME: '오늘 남은 시간대나 이후 날짜를 선택해 주세요.',
  TOO_FAR_IN_FUTURE: '날짜를 오늘부터 14일 이내로 되돌려 주세요.',
  NOT_RESERVER: '예약을 잡은 사람에게 취소를 요청해 주세요.',
  ALREADY_CANCELLED: '다시 시도를 눌러 최신 현황을 불러오세요.',
  RESERVATION_NOT_FOUND: '누군가 먼저 바꿨을 수 있습니다. 다시 시도를 눌러 현황을 새로 받아 주세요.',
  ROOM_NOT_FOUND: '다시 시도를 눌러 회의실 목록을 새로 받아 주세요.',
  INVALID_RESERVER_NAME: '예약자명을 1자 이상 20자 이내로 입력해 주세요.',
  INVALID_PURPOSE_LENGTH: '회의 목적을 50자 이내로 줄여 주세요.',
  INVALID_DATE_FORMAT: '날짜를 2026-08-26 형식으로 입력해 주세요.',
};

const DEFAULT_NEXT_ACTION = '잠시 후 다시 시도를 눌러 주세요. 계속되면 담당자에게 알려 주세요.';

export function toErrorMessage(code: string): string {
  return toKoreanMessage(code);
}

export function toNextAction(code: string): string {
  return NEXT_ACTIONS[code] ?? DEFAULT_NEXT_ACTION;
}
