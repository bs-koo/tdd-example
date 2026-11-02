import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import type { GroupDto, GroupMemberDto, InvitationDto } from '../types';
import { groupApi } from './groups';
import { ApiError } from './client';
import { writeToken } from '../auth/tokenStorage';

const fetchMock = vi.fn();

beforeEach(() => {
  vi.stubGlobal('fetch', fetchMock);
  fetchMock.mockReset();
  localStorage.clear();
  writeToken('opaque-token-1');
});

afterEach(() => {
  vi.unstubAllGlobals();
  localStorage.clear();
});

function jsonResponse(status: number, body: unknown) {
  return { ok: status >= 200 && status < 300, status, json: async () => body };
}

const BEARER = expect.objectContaining({
  headers: expect.objectContaining({ Authorization: 'Bearer opaque-token-1' }),
});

const GROUPS: GroupDto[] = [
  { id: 1, name: 'SQI 개발팀', ownerId: 1, role: 'OWNER', status: 'APPROVED' },
];

const MEMBERS: GroupMemberDto[] = [
  {
    membershipId: 1,
    memberId: 1,
    name: '김본승',
    email: 'owner@sqisoft.com',
    role: 'OWNER',
    status: 'APPROVED',
  },
  {
    membershipId: 2,
    memberId: 2,
    name: '박그룹',
    email: 'member@sqisoft.com',
    role: 'MEMBER',
    status: 'PENDING',
  },
];

const INVITATIONS: InvitationDto[] = [
  { id: 2, groupId: 1, groupName: 'SQI 개발팀', invitedBy: '김본승', status: 'PENDING' },
];

describe('groupApi 성공 경로', () => {
  it('fetchGroups는 Bearer 토큰과 함께 /api/groups를 조회하고 그룹 배열을 반환한다', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, GROUPS) as unknown as Response);

    const result = await groupApi.fetchGroups();

    expect(fetchMock).toHaveBeenCalledWith('/api/groups', BEARER);
    expect(result).toEqual(GROUPS);
  });

  it('createGroup은 이름만 담은 바디로 /api/groups에 POST하고 생성된 그룹을 반환한다', async () => {
    fetchMock.mockResolvedValue(jsonResponse(201, GROUPS[0]) as unknown as Response);

    const result = await groupApi.createGroup('SQI 개발팀');

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/groups',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({
          'Content-Type': 'application/json',
          Authorization: 'Bearer opaque-token-1',
        }),
        body: JSON.stringify({ name: 'SQI 개발팀' }),
      }),
    );
    expect(result).toEqual(GROUPS[0]);
  });

  it('fetchMembers(1)은 /api/groups/1/members를 조회하고 PENDING 멤버까지 그대로 반환한다', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, MEMBERS) as unknown as Response);

    const result = await groupApi.fetchMembers(1);

    expect(fetchMock).toHaveBeenCalledWith('/api/groups/1/members', BEARER);
    expect(result).toEqual(MEMBERS);
    expect(result[1].status).toBe('PENDING');
  });

  it('invite(1, 이메일)은 /api/groups/1/invitations에 POST하고 PENDING 멤버십을 반환한다', async () => {
    fetchMock.mockResolvedValue(jsonResponse(201, MEMBERS[1]) as unknown as Response);

    const result = await groupApi.invite(1, 'member@sqisoft.com');

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/groups/1/invitations',
      expect.objectContaining({
        method: 'POST',
        headers: expect.objectContaining({ 'Content-Type': 'application/json' }),
        body: JSON.stringify({ email: 'member@sqisoft.com' }),
      }),
    );
    expect(result).toEqual(MEMBERS[1]);
  });

  it('fetchInvitations는 /api/invitations를 조회하고 내가 받은 대기 중 초대를 반환한다', async () => {
    fetchMock.mockResolvedValue(jsonResponse(200, INVITATIONS) as unknown as Response);

    const result = await groupApi.fetchInvitations();

    expect(fetchMock).toHaveBeenCalledWith('/api/invitations', BEARER);
    expect(result).toEqual(INVITATIONS);
  });

  it('acceptInvitation(2)는 /api/invitations/2/accept로 POST하고 APPROVED로 바뀐 초대를 반환한다', async () => {
    const approved: InvitationDto = { ...INVITATIONS[0], status: 'APPROVED' };
    fetchMock.mockResolvedValue(jsonResponse(200, approved) as unknown as Response);

    const result = await groupApi.acceptInvitation(2);

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/invitations/2/accept',
      expect.objectContaining({ method: 'POST' }),
    );
    expect(result.status).toBe('APPROVED');
  });

  it('rejectInvitation(2)는 /api/invitations/2/reject로 POST하고 REJECTED로 바뀐 초대를 반환한다', async () => {
    const rejected: InvitationDto = { ...INVITATIONS[0], status: 'REJECTED' };
    fetchMock.mockResolvedValue(jsonResponse(200, rejected) as unknown as Response);

    const result = await groupApi.rejectInvitation(2);

    expect(fetchMock).toHaveBeenCalledWith(
      '/api/invitations/2/reject',
      expect.objectContaining({ method: 'POST' }),
    );
    expect(result.status).toBe('REJECTED');
  });
});

describe('groupApi 오류 경로', () => {
  it('403 + NOT_GROUP_OWNER 응답이면 invite가 ApiError(NOT_GROUP_OWNER, 403)로 reject된다', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(403, { code: 'NOT_GROUP_OWNER', message: '그룹장만 할 수 있는 작업입니다.' }) as unknown as Response,
    );

    await expect(groupApi.invite(1, 'member@sqisoft.com')).rejects.toBeInstanceOf(ApiError);
    await expect(groupApi.invite(1, 'member@sqisoft.com')).rejects.toMatchObject({
      code: 'NOT_GROUP_OWNER',
      status: 403,
    });
  });

  it('400 + INVALID_GROUP_NAME 응답이면 createGroup이 해당 code로 reject된다', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(400, { code: 'INVALID_GROUP_NAME' }) as unknown as Response,
    );

    await expect(groupApi.createGroup('')).rejects.toMatchObject({
      code: 'INVALID_GROUP_NAME',
      status: 400,
    });
  });

  it('409 + INVITATION_ALREADY_RESPONDED 응답이면 acceptInvitation이 해당 code로 reject된다', async () => {
    fetchMock.mockResolvedValue(
      jsonResponse(409, { code: 'INVITATION_ALREADY_RESPONDED' }) as unknown as Response,
    );

    await expect(groupApi.acceptInvitation(2)).rejects.toMatchObject({
      code: 'INVITATION_ALREADY_RESPONDED',
      status: 409,
    });
  });
});
