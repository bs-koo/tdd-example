import type {
  CreateReservationRequest,
  CreateRoomRequest,
  GroupDto,
  GroupMemberDto,
  InvitationDto,
  LoginResponse,
  MemberDto,
  ReservationDto,
  RoomDto,
} from '../types';

export type AuthApi = {
  signup(request: { email: string; password: string; name: string }): Promise<MemberDto>;
  login(request: { email: string; password: string }): Promise<LoginResponse>;
  logout(): Promise<void>;
  me(): Promise<MemberDto>;
};

export type GroupApi = {
  fetchGroups(): Promise<GroupDto[]>;
  createGroup(name: string): Promise<GroupDto>;
  fetchMembers(groupId: number): Promise<GroupMemberDto[]>;
  invite(groupId: number, email: string): Promise<GroupMemberDto>;
  fetchInvitations(): Promise<InvitationDto[]>;
  acceptInvitation(membershipId: number): Promise<InvitationDto>;
  rejectInvitation(membershipId: number): Promise<InvitationDto>;
};

export type ReservationApi = {
  fetchRooms(groupId: number): Promise<RoomDto[]>;
  createRoom(groupId: number, request: CreateRoomRequest): Promise<RoomDto>;
  fetchReservations(roomId: number, date: string): Promise<ReservationDto[]>;
  createReservation(request: CreateReservationRequest): Promise<ReservationDto>;
  cancelReservation(id: number): Promise<void>;
};
