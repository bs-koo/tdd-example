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
  INVALID_PURPOSE_LENGTH: '회의 목적을 50자 이내로 줄여 주세요.',
  INVALID_DATE_FORMAT: '날짜를 2026-08-26 형식으로 입력해 주세요.',

  // 인증
  UNAUTHENTICATED: '로그인이 풀렸습니다. 다시 로그인해 주세요.',
  INVALID_CREDENTIALS: '이메일과 비밀번호를 다시 확인해 주세요.',
  EMAIL_ALREADY_REGISTERED: '로그인 탭으로 옮겨 그 이메일로 로그인해 주세요.',
  INVALID_EMAIL_FORMAT: '이메일을 name@sqisoft.com 형식으로 입력해 주세요.',
  INVALID_PASSWORD_LENGTH: '비밀번호를 8자 이상 64자 이하로 입력해 주세요.',
  INVALID_MEMBER_NAME: '이름을 1자 이상 20자 이내로 입력해 주세요.',

  // 그룹 · 초대
  MEMBER_NOT_FOUND: '초대할 사람이 먼저 가입해야 합니다. 이메일을 다시 확인해 주세요.',
  GROUP_NOT_FOUND: '그룹 목록에서 다른 그룹을 골라 주세요.',
  INVALID_GROUP_NAME: '그룹 이름을 1자 이상 30자 이내로 입력해 주세요.',
  NOT_GROUP_OWNER: '그룹장에게 대신 처리해 달라고 요청해 주세요.',
  NOT_GROUP_MEMBER: '초대를 수락해 그룹에 참여한 뒤 다시 시도해 주세요.',
  ALREADY_INVITED: '그룹원 목록에서 그 사람의 상태를 확인해 주세요.',
  SELF_INVITATION: '초대할 사람의 이메일을 입력해 주세요.',
  INVITATION_NOT_FOUND: '초대가 취소됐을 수 있습니다. 화면을 새로 고쳐 주세요.',
  NOT_INVITEE: '초대받은 본인의 계정으로 로그인해 응답해 주세요.',
  INVITATION_ALREADY_RESPONDED: '화면을 새로 고쳐 최신 초대 목록을 확인해 주세요.',

  // 회의실
  INVALID_ROOM_NAME: '회의실 이름을 1자 이상 30자 이내로 입력해 주세요.',
  INVALID_ROOM_CAPACITY: '수용 인원을 1명 이상 999명 이하로 입력해 주세요.',
  INVALID_ROOM_LOCATION: '위치를 1자 이상 30자 이내로 입력해 주세요.',
};

const DEFAULT_NEXT_ACTION = '잠시 후 다시 시도를 눌러 주세요. 계속되면 담당자에게 알려 주세요.';

export function toErrorMessage(code: string): string {
  return toKoreanMessage(code);
}

export function toNextAction(code: string): string {
  return NEXT_ACTIONS[code] ?? DEFAULT_NEXT_ACTION;
}
