import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { GroupDto, MemberDto, RoomDto } from './types';
import type { AuthApi, GroupApi, ReservationApi } from './api/types';
import { ApiError } from './api/client';
import { writeToken } from './auth/tokenStorage';
import App from './App';

const ME: MemberDto = { id: 1, email: 'owner@sqisoft.com', name: '김본승' };

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

const DEV_ROOMS: RoomDto[] = [
  { id: 1, groupId: 1, name: '대회의실', capacity: 20, location: '3층' },
];

const DESIGN_ROOMS: RoomDto[] = [
  { id: 7, groupId: 2, name: '디자인 라운지', capacity: 6, location: '5층' },
];

beforeEach(() => {
  localStorage.clear();
});

afterEach(() => {
  localStorage.clear();
});

function createFakeApis() {
  return {
    auth: {
      signup: vi.fn(),
      login: vi.fn(),
      logout: vi.fn().mockResolvedValue(undefined),
      me: vi.fn().mockResolvedValue(ME),
    },
    group: {
      fetchGroups: vi.fn().mockResolvedValue([]),
      createGroup: vi.fn(),
      fetchMembers: vi.fn().mockResolvedValue([]),
      invite: vi.fn(),
      fetchInvitations: vi.fn().mockResolvedValue([]),
      acceptInvitation: vi.fn(),
      rejectInvitation: vi.fn(),
    },
    reservation: {
      fetchRooms: vi.fn().mockResolvedValue([]),
      createRoom: vi.fn(),
      fetchReservations: vi.fn().mockResolvedValue([]),
      createReservation: vi.fn(),
      cancelReservation: vi.fn(),
    },
  };
}

function renderApp(apis: ReturnType<typeof createFakeApis>) {
  render(
    <App
      authApi={apis.auth as unknown as AuthApi}
      groupApi={apis.group as unknown as GroupApi}
      reservationApi={apis.reservation as unknown as ReservationApi}
    />,
  );
}

describe('App 인증 상태', () => {
  it('저장된 토큰이 없으면 로그인 화면을 보여 준다', async () => {
    const apis = createFakeApis();

    renderApp(apis);

    expect(await screen.findByRole('tab', { name: '로그인' })).toBeInTheDocument();
    expect(apis.auth.me).not.toHaveBeenCalled();
  });

  it('세션을 복원하는 동안에는 로그인 화면 대신 불러오는 중이라고 알린다', () => {
    const apis = createFakeApis();
    apis.auth.me.mockReturnValue(new Promise(() => {}));
    writeToken('opaque-token-1');

    renderApp(apis);

    expect(screen.getByText('불러오는 중입니다.')).toBeInTheDocument();
    expect(screen.queryByRole('tab', { name: '로그인' })).not.toBeInTheDocument();
  });

  it('토큰이 만료돼 복원에 실패하면 로그인 화면으로 되돌아온다', async () => {
    const apis = createFakeApis();
    apis.auth.me.mockRejectedValue(new ApiError('UNAUTHENTICATED', 401));
    writeToken('만료된-토큰');

    renderApp(apis);

    expect(await screen.findByRole('tab', { name: '로그인' })).toBeInTheDocument();
  });

  it('로그인한 뒤에는 상단에 사용자 이름과 로그아웃 버튼이 놓인다', async () => {
    const apis = createFakeApis();
    apis.group.fetchGroups.mockResolvedValue([DEV_TEAM]);
    writeToken('opaque-token-1');

    renderApp(apis);

    expect(await screen.findByText('김본승')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '로그아웃' })).toBeInTheDocument();
  });

  it('로그아웃을 누르면 로그인 화면으로 돌아간다', async () => {
    const apis = createFakeApis();
    apis.group.fetchGroups.mockResolvedValue([DEV_TEAM]);
    writeToken('opaque-token-1');
    renderApp(apis);
    await screen.findByRole('button', { name: '로그아웃' });

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: '로그아웃' }));

    expect(await screen.findByRole('tab', { name: '로그인' })).toBeInTheDocument();
    expect(apis.auth.logout).toHaveBeenCalledTimes(1);
  });

  it('로그인에 성공하면 로그인 화면이 걷히고 예약 화면이 나타난다', async () => {
    const apis = createFakeApis();
    apis.auth.login.mockResolvedValue({ token: 'opaque-token-1', member: ME });
    apis.group.fetchGroups.mockResolvedValue([DEV_TEAM]);
    apis.reservation.fetchRooms.mockResolvedValue(DEV_ROOMS);
    renderApp(apis);
    await screen.findByRole('tab', { name: '로그인' });

    const user = userEvent.setup();
    await user.type(screen.getByLabelText('이메일'), 'owner@sqisoft.com');
    await user.type(screen.getByLabelText('비밀번호'), 'password123');
    await user.click(screen.getByRole('button', { name: '로그인' }));

    expect(await screen.findByText('대회의실')).toBeInTheDocument();
  });
});

describe('App 탭과 그룹 문맥', () => {
  it('승인된 그룹이 없으면 예약 격자 대신 그룹을 만들거나 초대를 수락하라고 안내한다', async () => {
    const apis = createFakeApis();
    writeToken('opaque-token-1');

    renderApp(apis);

    expect(await screen.findByText('그룹을 만들거나 초대를 수락하세요.')).toBeInTheDocument();
    expect(screen.queryByLabelText('회의실 시간표')).not.toBeInTheDocument();
    expect(apis.reservation.fetchRooms).not.toHaveBeenCalled();
  });

  it('그룹이 있으면 예약 탭에 그 그룹의 회의실이 보인다', async () => {
    const apis = createFakeApis();
    apis.group.fetchGroups.mockResolvedValue([DEV_TEAM]);
    apis.reservation.fetchRooms.mockResolvedValue(DEV_ROOMS);
    writeToken('opaque-token-1');

    renderApp(apis);

    expect(await screen.findByText('대회의실')).toBeInTheDocument();
    expect(apis.reservation.fetchRooms).toHaveBeenCalledWith(1);
  });

  it('그룹 탭으로 옮기면 내 그룹 화면이 나타난다', async () => {
    const apis = createFakeApis();
    apis.group.fetchGroups.mockResolvedValue([DEV_TEAM]);
    apis.reservation.fetchRooms.mockResolvedValue(DEV_ROOMS);
    writeToken('opaque-token-1');
    renderApp(apis);
    await screen.findByText('대회의실');

    const user = userEvent.setup();
    await user.click(screen.getByRole('tab', { name: '그룹' }));

    expect(await screen.findByLabelText('내 그룹')).toBeInTheDocument();
    expect(screen.getByLabelText('받은 초대')).toBeInTheDocument();
  });

  it('예약 화면에서 그룹을 바꾸면 그 그룹의 회의실로 격자가 바뀐다', async () => {
    const apis = createFakeApis();
    apis.group.fetchGroups.mockResolvedValue([DEV_TEAM, DESIGN_TEAM]);
    apis.reservation.fetchRooms.mockImplementation(async (groupId: number) =>
      groupId === 1 ? DEV_ROOMS : DESIGN_ROOMS,
    );
    writeToken('opaque-token-1');
    renderApp(apis);
    await screen.findByText('대회의실');

    const user = userEvent.setup();
    await user.selectOptions(screen.getByLabelText('그룹 선택'), '2');

    expect(await screen.findByText('디자인 라운지')).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.queryByText('대회의실')).not.toBeInTheDocument();
    });
  });
});
