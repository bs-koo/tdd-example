import { describe, it, expect, vi } from 'vitest';
import { render, screen, within, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { GroupDto, GroupMemberDto, InvitationDto } from '../types';
import type { GroupApi } from '../api/types';
import { ApiError } from '../api/client';
import { useGroups } from '../hooks/useGroups';
import GroupsPage from './GroupsPage';

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

const PENDING_MEMBER: GroupMemberDto = {
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

/** 실제 useGroups 와 붙여 렌더한다 — 수락 뒤 목록이 갱신되는 것까지 화면에서 확인한다. */
function Harness({ api }: { api: GroupApi }) {
  const board = useGroups({ api });
  return <GroupsPage board={board} />;
}

function renderGroups(fakeApi: ReturnType<typeof createFakeApi>) {
  render(<Harness api={fakeApi as unknown as GroupApi} />);
}

describe('GroupsPage 그룹 목록', () => {
  it('내가 속한 그룹이 목록에 이름으로 나열되고 첫 그룹이 선택돼 있다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValue([DEV_TEAM, DESIGN_TEAM]);
    renderGroups(api);

    const selected = await screen.findByRole('button', { name: 'SQI 개발팀' });
    expect(selected).toHaveAttribute('aria-current', 'true');
    expect(screen.getByRole('button', { name: '디자인팀' })).toHaveAttribute(
      'aria-current',
      'false',
    );
  });

  it('속한 그룹이 없으면 그룹을 만들거나 초대를 받으라고 안내한다', async () => {
    const api = createFakeApi();
    renderGroups(api);

    expect(await screen.findByText('아직 속한 그룹이 없습니다.')).toBeInTheDocument();
  });

  it('다른 그룹을 누르면 선택이 옮겨가고 그 그룹의 그룹원을 불러온다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValue([DEV_TEAM, DESIGN_TEAM]);
    renderGroups(api);
    await screen.findByRole('button', { name: 'SQI 개발팀' });

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: '디자인팀' }));

    await waitFor(() => {
      expect(screen.getByRole('button', { name: '디자인팀' })).toHaveAttribute(
        'aria-current',
        'true',
      );
    });
    expect(api.fetchMembers).toHaveBeenCalledWith(2);
  });
});

describe('GroupsPage 그룹원', () => {
  it('선택된 그룹의 그룹원이 이름·이메일·역할·상태와 함께 표시된다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValue([DEV_TEAM]);
    api.fetchMembers.mockResolvedValue([OWNER_MEMBER, PENDING_MEMBER]);
    renderGroups(api);

    const roster = await screen.findByRole('list', { name: '그룹원' });
    const rows = within(roster).getAllByRole('listitem');

    expect(within(rows[0]).getByText('김본승')).toBeInTheDocument();
    expect(within(rows[0]).getByText('owner@sqisoft.com')).toBeInTheDocument();
    expect(within(rows[0]).getByText('그룹장')).toBeInTheDocument();
    expect(within(rows[0]).getByText('참여 중')).toBeInTheDocument();
    expect(within(rows[1]).getByText('박초대')).toBeInTheDocument();
    expect(within(rows[1]).getByText('그룹원')).toBeInTheDocument();
    expect(within(rows[1]).getByText('수락 대기')).toBeInTheDocument();
  });
});

describe('GroupsPage 그룹 만들기', () => {
  it('이름을 입력하고 만들면 목록에 새 그룹이 나타난다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValueOnce([]).mockResolvedValue([DEV_TEAM]);
    api.createGroup.mockResolvedValue(DEV_TEAM);
    renderGroups(api);
    await screen.findByText('아직 속한 그룹이 없습니다.');

    const user = userEvent.setup();
    await user.type(screen.getByLabelText('그룹 이름'), 'SQI 개발팀');
    await user.click(screen.getByRole('button', { name: '그룹 만들기' }));

    expect(await screen.findByRole('button', { name: 'SQI 개발팀' })).toBeInTheDocument();
    expect(api.createGroup).toHaveBeenCalledWith('SQI 개발팀');
  });

  it('이름을 비운 채 만들면 서버를 부르지 않고 그 자리에서 사유를 알려 준다', async () => {
    const api = createFakeApi();
    renderGroups(api);
    await screen.findByText('아직 속한 그룹이 없습니다.');

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: '그룹 만들기' }));

    expect(api.createGroup).not.toHaveBeenCalled();
    expect(screen.getByText('그룹 이름을 입력해 주세요.')).toBeInTheDocument();
  });

  it('그룹을 만든 뒤 입력칸이 비워져 같은 이름을 두 번 만들지 않게 한다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValueOnce([]).mockResolvedValue([DEV_TEAM]);
    api.createGroup.mockResolvedValue(DEV_TEAM);
    renderGroups(api);
    await screen.findByText('아직 속한 그룹이 없습니다.');

    const user = userEvent.setup();
    await user.type(screen.getByLabelText('그룹 이름'), 'SQI 개발팀');
    await user.click(screen.getByRole('button', { name: '그룹 만들기' }));

    await screen.findByRole('button', { name: 'SQI 개발팀' });
    expect(screen.getByLabelText('그룹 이름')).toHaveValue('');
  });
});

describe('GroupsPage 초대 보내기', () => {
  it('그룹장에게는 초대 폼이 보인다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValue([DEV_TEAM]);
    renderGroups(api);

    expect(await screen.findByLabelText('초대할 이메일')).toBeInTheDocument();
  });

  it('그룹장이 아닌 그룹을 보고 있으면 초대 폼이 보이지 않는다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValue([DESIGN_TEAM]);
    renderGroups(api);

    await screen.findByRole('button', { name: '디자인팀' });
    expect(screen.queryByLabelText('초대할 이메일')).not.toBeInTheDocument();
  });

  it('이메일로 초대하면 그룹원 목록에 수락 대기 상태로 나타난다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValue([DEV_TEAM]);
    api.fetchMembers
      .mockResolvedValueOnce([OWNER_MEMBER])
      .mockResolvedValue([OWNER_MEMBER, PENDING_MEMBER]);
    api.invite.mockResolvedValue(PENDING_MEMBER);
    renderGroups(api);
    await screen.findByText('김본승');

    const user = userEvent.setup();
    await user.type(screen.getByLabelText('초대할 이메일'), 'invitee@sqisoft.com');
    await user.click(screen.getByRole('button', { name: '초대 보내기' }));

    expect(await screen.findByText('박초대')).toBeInTheDocument();
    expect(screen.getByText('수락 대기')).toBeInTheDocument();
    expect(api.invite).toHaveBeenCalledWith(1, 'invitee@sqisoft.com');
  });

  it('이미 초대한 사람이면 한국어 오류가 role="alert"로 표시된다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValue([DEV_TEAM]);
    api.fetchMembers.mockResolvedValue([OWNER_MEMBER]);
    api.invite.mockRejectedValue(new ApiError('ALREADY_INVITED', 409));
    renderGroups(api);
    await screen.findByText('김본승');

    const user = userEvent.setup();
    await user.type(screen.getByLabelText('초대할 이메일'), 'invitee@sqisoft.com');
    await user.click(screen.getByRole('button', { name: '초대 보내기' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('이미 초대했거나 참여 중인 사용자입니다.');
  });
});

describe('GroupsPage 받은 초대', () => {
  it('받은 초대가 없으면 그 사실을 알려 준다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValue([DEV_TEAM]);
    renderGroups(api);

    expect(await screen.findByText('받은 초대가 없습니다.')).toBeInTheDocument();
  });

  it('받은 초대에 그룹 이름과 초대한 사람이 함께 표시된다', async () => {
    const api = createFakeApi();
    api.fetchInvitations.mockResolvedValue([INVITATION]);
    renderGroups(api);

    const invites = await screen.findByRole('list', { name: '받은 초대' });
    expect(within(invites).getByText('디자인팀')).toBeInTheDocument();
    expect(within(invites).getByText('최디자님이 초대했습니다')).toBeInTheDocument();
  });

  it('초대를 수락하면 초대 카드가 사라지고 그룹 목록에 그 그룹이 나타난다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValueOnce([]).mockResolvedValue([DESIGN_TEAM]);
    api.fetchInvitations.mockResolvedValueOnce([INVITATION]).mockResolvedValue([]);
    api.acceptInvitation.mockResolvedValue({ ...INVITATION, status: 'APPROVED' });
    renderGroups(api);
    await screen.findByRole('list', { name: '받은 초대' });

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: '디자인팀 초대 수락' }));

    expect(await screen.findByRole('button', { name: '디자인팀' })).toBeInTheDocument();
    expect(screen.getByText('받은 초대가 없습니다.')).toBeInTheDocument();
    expect(api.acceptInvitation).toHaveBeenCalledWith(7);
  });

  it('초대를 거절하면 초대 카드만 사라지고 그룹 목록은 그대로다', async () => {
    const api = createFakeApi();
    api.fetchGroups.mockResolvedValue([DEV_TEAM]);
    api.fetchInvitations.mockResolvedValueOnce([INVITATION]).mockResolvedValue([]);
    api.rejectInvitation.mockResolvedValue({ ...INVITATION, status: 'REJECTED' });
    renderGroups(api);
    await screen.findByRole('list', { name: '받은 초대' });

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: '디자인팀 초대 거절' }));

    expect(await screen.findByText('받은 초대가 없습니다.')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: 'SQI 개발팀' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '디자인팀' })).not.toBeInTheDocument();
  });
});
