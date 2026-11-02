import { describe, it, expect, vi } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import type { GroupDto, GroupMemberDto, InvitationDto } from '../types';
import type { GroupApi } from '../api/types';
import { ApiError } from '../api/client';
import { useGroups } from './useGroups';

const DEV_TEAM: GroupDto = {
  id: 1,
  name: 'SQI 개발팀',
  ownerId: 1,
  role: 'OWNER',
  status: 'APPROVED',
};

const DESIGN_TEAM: GroupDto = {
  id: 2,
  name: '디자인팀',
  ownerId: 9,
  role: 'MEMBER',
  status: 'APPROVED',
};

const OWNER_MEMBER: GroupMemberDto = {
  membershipId: 1,
  memberId: 1,
  name: '김본승',
  email: 'owner@sqisoft.com',
  role: 'OWNER',
  status: 'APPROVED',
};

const INVITED_MEMBER: GroupMemberDto = {
  membershipId: 3,
  memberId: 3,
  name: '박초대',
  email: 'invitee@sqisoft.com',
  role: 'MEMBER',
  status: 'PENDING',
};

const INVITATION: InvitationDto = {
  id: 7,
  groupId: 2,
  groupName: '디자인팀',
  invitedBy: '최디자',
  status: 'PENDING',
};

function createFakeApi() {
  return {
    fetchGroups: vi.fn().mockResolvedValue([]),
    createGroup: vi.fn(),
    fetchMembers: vi.fn().mockResolvedValue([]),
    invite: vi.fn(),
    fetchInvitations: vi.fn().mockResolvedValue([]),
    acceptInvitation: vi.fn(),
    rejectInvitation: vi.fn(),
  };
}

function renderGroups(fakeApi: ReturnType<typeof createFakeApi>) {
  return renderHook(() => useGroups({ api: fakeApi as unknown as GroupApi }));
}

describe('useGroups 초기 로드', () => {
  it('마운트하면 내 그룹과 받은 초대를 함께 가져온다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValue([DEV_TEAM]);
    api.fetchInvitations.mockResolvedValue([INVITATION]);

    const { result } = renderGroups(api);

    await waitFor(() => expect(result.current.groups).toEqual([DEV_TEAM]));
    expect(result.current.invitations).toEqual([INVITATION]);
  });

  it('그룹이 있으면 첫 그룹이 자동으로 선택되고 그 그룹의 멤버까지 채워진다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValue([DEV_TEAM, DESIGN_TEAM]);
    api.fetchMembers.mockResolvedValue([OWNER_MEMBER]);

    const { result } = renderGroups(api);

    await waitFor(() => expect(result.current.selectedGroupId).toBe(1));
    expect(api.fetchMembers).toHaveBeenCalledWith(1);
    await waitFor(() => expect(result.current.members).toEqual([OWNER_MEMBER]));
  });

  it('속한 그룹이 하나도 없으면 멤버를 조회하지 않고 선택된 그룹도 없다', async () => {
    const api = createFakeApi();

    const { result } = renderGroups(api);

    await waitFor(() => expect(result.current.loading).toBe(false));
    expect(result.current.selectedGroupId).toBeNull();
    expect(api.fetchMembers).not.toHaveBeenCalled();
  });

  it('초기 로드가 실패하면 errorCode가 채워진다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockRejectedValue(new ApiError('UNAUTHENTICATED', 401));

    const { result } = renderGroups(api);

    await waitFor(() => expect(result.current.errorCode).toBe('UNAUTHENTICATED'));
  });
});

describe('useGroups 그룹 선택', () => {
  it('selectGroup을 호출하면 선택이 바뀌고 그 그룹의 멤버를 새로 가져온다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValue([DEV_TEAM, DESIGN_TEAM]);
    api.fetchMembers.mockResolvedValue([OWNER_MEMBER]);

    const { result } = renderGroups(api);
    await waitFor(() => expect(result.current.selectedGroupId).toBe(1));

    await act(async () => {
      await result.current.selectGroup(2);
    });

    expect(result.current.selectedGroupId).toBe(2);
    expect(api.fetchMembers).toHaveBeenCalledWith(2);
  });
});

describe('useGroups 그룹 생성', () => {
  it('createGroup이 성공하면 목록이 갱신되고 방금 만든 그룹이 선택된다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValueOnce([]).mockResolvedValue([DEV_TEAM]);
    api.createGroup.mockResolvedValue(DEV_TEAM);

    const { result } = renderGroups(api);
    await waitFor(() => expect(result.current.loading).toBe(false));

    await act(async () => {
      await result.current.createGroup('SQI 개발팀');
    });

    expect(api.createGroup).toHaveBeenCalledWith('SQI 개발팀');
    expect(result.current.groups).toEqual([DEV_TEAM]);
    expect(result.current.selectedGroupId).toBe(1);
  });

  it('createGroup이 거절되면 errorCode만 남고 목록은 그대로다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValue([DEV_TEAM]);
    api.createGroup.mockRejectedValue(new ApiError('INVALID_GROUP_NAME', 400));

    const { result } = renderGroups(api);
    await waitFor(() => expect(result.current.groups).toEqual([DEV_TEAM]));

    await act(async () => {
      await result.current.createGroup('');
    });

    expect(result.current.errorCode).toBe('INVALID_GROUP_NAME');
    expect(result.current.groups).toEqual([DEV_TEAM]);
  });
});

describe('useGroups 초대', () => {
  it('invite가 성공하면 선택된 그룹의 멤버 목록에 대기 중 멤버가 반영된다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValue([DEV_TEAM]);
    api.fetchMembers.mockResolvedValueOnce([OWNER_MEMBER]).mockResolvedValue([OWNER_MEMBER, INVITED_MEMBER]);
    api.invite.mockResolvedValue(INVITED_MEMBER);

    const { result } = renderGroups(api);
    await waitFor(() => expect(result.current.members).toEqual([OWNER_MEMBER]));

    await act(async () => {
      await result.current.invite('invitee@sqisoft.com');
    });

    expect(api.invite).toHaveBeenCalledWith(1, 'invitee@sqisoft.com');
    expect(result.current.members).toEqual([OWNER_MEMBER, INVITED_MEMBER]);
  });

  it('이미 초대한 사람을 다시 초대하면 errorCode가 채워진다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValue([DEV_TEAM]);
    api.fetchMembers.mockResolvedValue([OWNER_MEMBER]);
    api.invite.mockRejectedValue(new ApiError('ALREADY_INVITED', 409));

    const { result } = renderGroups(api);
    await waitFor(() => expect(result.current.selectedGroupId).toBe(1));

    await act(async () => {
      await result.current.invite('invitee@sqisoft.com');
    });

    expect(result.current.errorCode).toBe('ALREADY_INVITED');
  });
});

describe('useGroups 초대 응답', () => {
  it('초대를 수락하면 그룹 목록에 그 그룹이 나타나고 초대 목록에서는 사라진다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValueOnce([]).mockResolvedValue([DESIGN_TEAM]);
    api.fetchInvitations.mockResolvedValueOnce([INVITATION]).mockResolvedValue([]);
    api.acceptInvitation.mockResolvedValue({ ...INVITATION, status: 'APPROVED' });

    const { result } = renderGroups(api);
    await waitFor(() => expect(result.current.invitations).toEqual([INVITATION]));

    await act(async () => {
      await result.current.acceptInvitation(7);
    });

    expect(api.acceptInvitation).toHaveBeenCalledWith(7);
    expect(result.current.groups).toEqual([DESIGN_TEAM]);
    expect(result.current.invitations).toEqual([]);
  });

  it('초대를 거절하면 초대 목록에서만 사라지고 그룹 목록은 그대로다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValue([DEV_TEAM]);
    api.fetchInvitations.mockResolvedValueOnce([INVITATION]).mockResolvedValue([]);
    api.rejectInvitation.mockResolvedValue({ ...INVITATION, status: 'REJECTED' });

    const { result } = renderGroups(api);
    await waitFor(() => expect(result.current.invitations).toEqual([INVITATION]));

    await act(async () => {
      await result.current.rejectInvitation(7);
    });

    expect(api.rejectInvitation).toHaveBeenCalledWith(7);
    expect(result.current.invitations).toEqual([]);
    expect(result.current.groups).toEqual([DEV_TEAM]);
  });

  it('이미 응답한 초대를 다시 수락하면 errorCode가 채워지고 훅은 reject하지 않는다', async () => {
    const api = createFakeApi();
    api.fetchInvitations.mockResolvedValue([INVITATION]);
    api.acceptInvitation.mockRejectedValue(new ApiError('INVITATION_ALREADY_RESPONDED', 409));

    const { result } = renderGroups(api);
    await waitFor(() => expect(result.current.invitations).toEqual([INVITATION]));

    await act(async () => {
      await expect(result.current.acceptInvitation(7)).resolves.toBeUndefined();
    });

    expect(result.current.errorCode).toBe('INVITATION_ALREADY_RESPONDED');
  });

  it('clearError를 호출하면 errorCode가 null로 돌아간다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockRejectedValue(new ApiError('UNAUTHENTICATED', 401));

    const { result } = renderGroups(api);
    await waitFor(() => expect(result.current.errorCode).toBe('UNAUTHENTICATED'));

    act(() => {
      result.current.clearError();
    });

    expect(result.current.errorCode).toBeNull();
  });
});
