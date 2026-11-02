export type ErrorCode =
  | 'OVERLAPPING_RESERVATION' | 'INVALID_TIME_UNIT' | 'OUTSIDE_BUSINESS_HOURS'
  | 'INVALID_DURATION' | 'PAST_DATETIME' | 'TOO_FAR_IN_FUTURE' | 'NOT_RESERVER'
  | 'ALREADY_CANCELLED' | 'RESERVATION_NOT_FOUND' | 'ROOM_NOT_FOUND'
  | 'INVALID_PURPOSE_LENGTH' | 'INVALID_DATE_FORMAT'
  | 'UNAUTHENTICATED' | 'INVALID_CREDENTIALS' | 'EMAIL_ALREADY_REGISTERED'
  | 'INVALID_EMAIL_FORMAT' | 'INVALID_PASSWORD_LENGTH' | 'INVALID_MEMBER_NAME'
  | 'MEMBER_NOT_FOUND' | 'GROUP_NOT_FOUND' | 'INVALID_GROUP_NAME'
  | 'NOT_GROUP_OWNER' | 'NOT_GROUP_MEMBER' | 'ALREADY_INVITED' | 'SELF_INVITATION'
  | 'INVITATION_NOT_FOUND' | 'NOT_INVITEE' | 'INVITATION_ALREADY_RESPONDED'
  | 'INVALID_ROOM_NAME' | 'INVALID_ROOM_CAPACITY' | 'INVALID_ROOM_LOCATION';

export type SlotTime = string;   // "HH:mm"

export type MemberDto = { id: number; email: string; name: string };
export type LoginResponse = { token: string; member: MemberDto };

export type GroupRole = 'OWNER' | 'MEMBER';
export type MembershipStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export type GroupDto = {
  id: number; name: string; ownerId: number;
  role: GroupRole; status: MembershipStatus;
};
export type GroupMemberDto = {
  membershipId: number; memberId: number; name: string; email: string;
  role: GroupRole; status: MembershipStatus;
};
export type InvitationDto = {
  id: number; groupId: number; groupName: string;
  invitedBy: string; status: MembershipStatus;
};

export type RoomDto = {
  id: number; groupId: number; name: string; capacity: number; location: string;
};
export type CreateRoomRequest = { name: string; capacity: number; location: string };

export type ReservationDto = {
  id: number; roomId: number; memberId: number; reserverName: string; purpose: string;
  startAt: string;   // "2026-08-26T09:00:00"
  endAt: string;
  status: 'ACTIVE' | 'CANCELLED';
};
export type CreateReservationRequest = {
  roomId: number; purpose: string; startAt: string; endAt: string;
};
