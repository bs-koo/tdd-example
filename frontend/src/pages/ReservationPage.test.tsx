import { describe, it, expect, vi } from 'vitest';
import { render, screen, within, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { GroupDto, MemberDto, ReservationDto, RoomDto } from '../types';
import type { ReservationApi } from '../api/types';
import { ApiError } from '../api/client';
import ReservationPage from './ReservationPage';

const ME: MemberDto = { id: 1, email: 'owner@sqisoft.com', name: '김본승' };

const GROUPS: GroupDto[] = [
  { id: 1, name: 'SQI 개발팀', ownerId: 1, role: 'OWNER', status: 'APPROVED' },
  { id: 2, name: '디자인팀', ownerId: 9, role: 'MEMBER', status: 'APPROVED' },
];

const ROOMS: RoomDto[] = [
  { id: 1, groupId: 1, name: '대회의실', capacity: 20, location: '3층' },
  { id: 2, groupId: 1, name: '중회의실', capacity: 10, location: '3층' },
];

const NEW_ROOM: RoomDto = { id: 3, groupId: 1, name: '소회의실1', capacity: 6, location: '5층' };

/** 내가 잡은 예약 — 취소할 수 있어야 한다. */
const MINE: ReservationDto = {
  id: 11,
  roomId: 1,
  memberId: 1,
  reserverName: '김본승',
  purpose: '주간 회의',
  startAt: '2026-08-26T09:00:00',
  endAt: '2026-08-26T10:00:00',
  status: 'ACTIVE',
};

/** 남이 잡은 예약 — 취소를 제공하면 안 된다. */
const SOMEONE_ELSES: ReservationDto = {
  id: 22,
  roomId: 1,
  memberId: 2,
  reserverName: '박그룹',
  purpose: '설계 리뷰',
  startAt: '2026-08-26T13:00:00',
  endAt: '2026-08-26T14:00:00',
  status: 'ACTIVE',
};

const NEW_RESERVATION: ReservationDto = {
  id: 33,
  roomId: 1,
  memberId: 1,
  reserverName: '김본승',
  purpose: '기획 회의',
  startAt: '2026-08-26T09:30:00',
  endAt: '2026-08-26T10:00:00',
  status: 'ACTIVE',
};

function createFakeApi() {
  return {
    fetchRooms: vi.fn(),
    createRoom: vi.fn(),
    fetchReservations: vi.fn(),
    createReservation: vi.fn(),
    cancelReservation: vi.fn(),
  };
}

function renderPage(
  fakeApi: ReturnType<typeof createFakeApi>,
  overrides: { initialDate?: string; onSelectGroup?: (groupId: number) => void } = {},
) {
  const onSelectGroup = overrides.onSelectGroup ?? vi.fn();
  render(
    <ReservationPage
      initialDate={overrides.initialDate ?? '2026-08-26'}
      groupId={1}
      groups={GROUPS}
      member={ME}
      onSelectGroup={onSelectGroup}
      api={fakeApi as unknown as ReservationApi}
    />,
  );
  return { onSelectGroup };
}

describe('ReservationPage', () => {
  it('마운트 후 선택된 그룹의 회의실과 기존 예약자명이 그리드에 표시된다 (초기 로드)', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations.mockImplementation(async (roomId: number) => (roomId === 1 ? [MINE] : []));

    renderPage(api);

    await screen.findByText('대회의실');
    expect(screen.getByText('중회의실')).toBeInTheDocument();
    expect(api.fetchRooms).toHaveBeenCalledWith(1);
    await waitFor(() => {
      expect(within(screen.getByTestId('slot-1-09:00')).getByText('김본승')).toBeInTheDocument();
    });
  });

  it('빈 슬롯 slot-1-09:30 을 클릭하면 예약 폼이 열리고 시작 시각이 09:30으로 채워진다 (AC-33)', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations.mockResolvedValue([]);
    renderPage(api);
    await screen.findByText('대회의실');

    const user = userEvent.setup();
    await user.click(screen.getByTestId('slot-1-09:30'));

    const dialog = await screen.findByRole('dialog', { name: '예약 폼' });
    expect(within(dialog).getByLabelText('시작 시각')).toHaveValue('09:30');
  });

  it('빈 슬롯을 닫고 다른 슬롯을 연달아 열어도 이전 슬롯의 시작 시각이 남지 않는다 (§9.13.4 재마운트 함정)', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations.mockResolvedValue([]);
    renderPage(api);
    await screen.findByText('대회의실');

    const user = userEvent.setup();
    await user.click(screen.getByTestId('slot-1-09:30'));
    const firstDialog = await screen.findByRole('dialog', { name: '예약 폼' });
    await user.click(within(firstDialog).getByRole('button', { name: '닫기' }));
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '예약 폼' })).not.toBeInTheDocument();
    });

    await user.click(screen.getByTestId('slot-2-14:00'));
    const secondDialog = await screen.findByRole('dialog', { name: '예약 폼' });
    expect(within(secondDialog).getByLabelText('시작 시각')).toHaveValue('14:00');
    expect(within(secondDialog).getByLabelText('종료 시각')).toHaveValue('14:30');
  });

  it('서버 상태와 클라이언트 스냅샷이 어긋난 상황 — 빈 슬롯으로 보이지만 서버가 겹침으로 거절하면 한국어 오류 배너가 표시된다 (AC-34)', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations.mockResolvedValue([]);
    api.createReservation.mockRejectedValueOnce(new ApiError('OVERLAPPING_RESERVATION', 409));
    renderPage(api);
    await screen.findByText('대회의실');

    const user = userEvent.setup();
    await user.click(screen.getByTestId('slot-1-09:30'));
    const dialog = await screen.findByRole('dialog', { name: '예약 폼' });
    await user.click(within(dialog).getByRole('button', { name: '확인' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('해당 시간대에 이미 예약이 있습니다.');
  });

  it('예약 제출이 성공하면 폼이 닫히고 재조회된 그리드에 새 예약자명이 반영된다', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations
      .mockResolvedValueOnce([]) // room 1, 최초 로드
      .mockResolvedValueOnce([]) // room 2, 최초 로드
      .mockResolvedValueOnce([NEW_RESERVATION]) // room 1, 예약 성공 후 재조회
      .mockResolvedValueOnce([]); // room 2, 예약 성공 후 재조회
    api.createReservation.mockResolvedValueOnce(NEW_RESERVATION);
    renderPage(api);
    await screen.findByText('대회의실');

    const user = userEvent.setup();
    await user.click(screen.getByTestId('slot-1-09:30'));
    const dialog = await screen.findByRole('dialog', { name: '예약 폼' });
    await user.type(within(dialog).getByLabelText('회의 목적'), '기획 회의');
    await user.click(within(dialog).getByRole('button', { name: '확인' }));

    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '예약 폼' })).not.toBeInTheDocument();
    });
    expect(api.createReservation).toHaveBeenCalledWith({
      roomId: 1,
      purpose: '기획 회의',
      startAt: '2026-08-26T09:30:00',
      endAt: '2026-08-26T10:00:00',
    });
    await waitFor(() => {
      expect(within(screen.getByTestId('slot-1-09:30')).getByText('김본승')).toBeInTheDocument();
    });
  });

  it('내 예약 슬롯을 취소하면 예약 id만 보내고, 재조회 후 슬롯이 비며 대화상자도 닫힌다 (AC-35)', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations
      .mockResolvedValueOnce([MINE]) // room 1, 최초 로드
      .mockResolvedValueOnce([]) // room 2, 최초 로드
      .mockResolvedValueOnce([]) // room 1, 취소 성공 후 재조회
      .mockResolvedValueOnce([]); // room 2, 취소 성공 후 재조회
    api.cancelReservation.mockResolvedValueOnce(undefined);
    renderPage(api);
    await screen.findByText('대회의실');
    await waitFor(() => {
      expect(within(screen.getByTestId('slot-1-09:00')).getByText('김본승')).toBeInTheDocument();
    });

    const user = userEvent.setup();
    await user.click(screen.getByTestId('slot-1-09:00'));
    const dialog = await screen.findByRole('dialog', { name: '예약 취소 확인' });
    await user.click(within(dialog).getByRole('button', { name: '확인' }));

    await waitFor(() => {
      expect(api.cancelReservation).toHaveBeenCalledWith(11);
    });
    await waitFor(() => {
      expect(within(screen.getByTestId('slot-1-09:00')).queryByText('김본승')).not.toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '예약 취소 확인' })).not.toBeInTheDocument();
    });
  });

  it('남의 예약 슬롯을 누르면 예약자만 보여 주고 취소는 제공하지 않는다', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations.mockImplementation(async (roomId: number) =>
      roomId === 1 ? [SOMEONE_ELSES] : [],
    );
    renderPage(api);
    await screen.findByText('대회의실');
    await waitFor(() => {
      expect(within(screen.getByTestId('slot-1-13:00')).getByText('박그룹')).toBeInTheDocument();
    });

    const user = userEvent.setup();
    await user.click(screen.getByTestId('slot-1-13:00'));

    const dialog = await screen.findByRole('dialog', { name: '예약 정보' });
    expect(within(dialog).getByText('박그룹')).toBeInTheDocument();
    expect(within(dialog).queryByRole('button', { name: '확인' })).not.toBeInTheDocument();
    expect(api.cancelReservation).not.toHaveBeenCalled();
  });

  it('취소 확인 대화상자에서 닫기를 누르면 취소 요청이 전송되지 않고 대화상자만 닫힌다', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations.mockImplementation(async (roomId: number) => (roomId === 1 ? [MINE] : []));
    renderPage(api);
    await screen.findByText('대회의실');
    await waitFor(() => {
      expect(within(screen.getByTestId('slot-1-09:00')).getByText('김본승')).toBeInTheDocument();
    });

    const user = userEvent.setup();
    await user.click(screen.getByTestId('slot-1-09:00'));
    const dialog = await screen.findByRole('dialog', { name: '예약 취소 확인' });
    await user.click(within(dialog).getByRole('button', { name: '닫기' }));

    expect(api.cancelReservation).not.toHaveBeenCalled();
    expect(screen.queryByRole('dialog', { name: '예약 취소 확인' })).not.toBeInTheDocument();
  });

  it('초기 로드가 정상 완료된 상태에서는 오류 배너가 표시되지 않는다', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations.mockImplementation(async (roomId: number) => (roomId === 1 ? [MINE] : []));

    renderPage(api);

    await screen.findByText('대회의실');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('날짜 선택 입력을 바꾸면 바뀐 날짜로 예약 목록을 재조회한다', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations.mockResolvedValue([]);
    renderPage(api, { initialDate: '2026-08-26' });
    await screen.findByText('대회의실');

    const user = userEvent.setup();
    const dateInput = screen.getByLabelText('날짜 선택');
    await user.clear(dateInput);
    await user.type(dateInput, '2026-08-27');

    await waitFor(() => {
      expect(api.fetchReservations).toHaveBeenCalledWith(1, '2026-08-27');
    });
  });

  it('날짜 타이핑 중 재조회가 폭주하지 않는다 — 완성된 날짜로만 회의실 수만큼 1회분 재조회한다 (AC-38)', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations.mockResolvedValue([]);
    renderPage(api, { initialDate: '2026-08-26' });
    await screen.findByText('대회의실');

    const reservationCallsBeforeTyping = api.fetchReservations.mock.calls.length;
    const roomCallsBeforeTyping = api.fetchRooms.mock.calls.length;

    const user = userEvent.setup();
    const dateInput = screen.getByLabelText('날짜 선택');
    await user.clear(dateInput);
    await user.type(dateInput, '2026-08-27');

    await waitFor(() => {
      expect(api.fetchReservations).toHaveBeenCalledWith(1, '2026-08-27');
    });

    expect(api.fetchReservations.mock.calls.length - reservationCallsBeforeTyping).toBe(2);
    expect(api.fetchRooms.mock.calls.length - roomCallsBeforeTyping).toBe(1);
    expect(api.fetchReservations).not.toHaveBeenCalledWith(expect.anything(), '2');
    expect(api.fetchReservations).not.toHaveBeenCalledWith(expect.anything(), '2026');
  });
});

describe('ReservationPage 그룹 문맥', () => {
  it('상단에서 속한 그룹을 고를 수 있고, 고르면 선택이 위로 전달된다', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations.mockResolvedValue([]);
    const { onSelectGroup } = renderPage(api);
    await screen.findByText('대회의실');

    const selector = screen.getByLabelText('그룹 선택');
    expect(selector).toHaveValue('1');

    const user = userEvent.setup();
    await user.selectOptions(selector, '2');

    expect(onSelectGroup).toHaveBeenCalledWith(2);
  });

  it('회의실 추가로 만든 회의실이 격자에 나타난다', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValueOnce(ROOMS).mockResolvedValue([...ROOMS, NEW_ROOM]);
    api.fetchReservations.mockResolvedValue([]);
    api.createRoom.mockResolvedValue(NEW_ROOM);
    renderPage(api);
    await screen.findByText('대회의실');

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: '회의실 추가' }));
    const dialog = await screen.findByRole('dialog', { name: '회의실 추가' });
    await user.type(within(dialog).getByLabelText('회의실 이름'), '소회의실1');
    await user.clear(within(dialog).getByLabelText('수용 인원'));
    await user.type(within(dialog).getByLabelText('수용 인원'), '6');
    await user.type(within(dialog).getByLabelText('위치'), '5층');
    await user.click(within(dialog).getByRole('button', { name: '확인' }));

    await waitFor(() => {
      expect(api.createRoom).toHaveBeenCalledWith(1, {
        name: '소회의실1',
        capacity: 6,
        location: '5층',
      });
    });
    expect(await screen.findByText('소회의실1')).toBeInTheDocument();
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '회의실 추가' })).not.toBeInTheDocument();
    });
  });

  it('회의실 추가가 거절되면 패널이 열린 채 사유를 알려 준다', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations.mockResolvedValue([]);
    api.createRoom.mockRejectedValue(new ApiError('INVALID_ROOM_CAPACITY', 400));
    renderPage(api);
    await screen.findByText('대회의실');

    const user = userEvent.setup();
    await user.click(screen.getByRole('button', { name: '회의실 추가' }));
    const dialog = await screen.findByRole('dialog', { name: '회의실 추가' });
    await user.type(within(dialog).getByLabelText('회의실 이름'), '소회의실1');
    await user.type(within(dialog).getByLabelText('위치'), '5층');
    await user.click(within(dialog).getByRole('button', { name: '확인' }));

    expect(
      await within(dialog).findByText('수용 인원은 1명 이상 999명 이하여야 합니다.'),
    ).toBeInTheDocument();
    expect(screen.getByRole('dialog', { name: '회의실 추가' })).toBeInTheDocument();
  });
});
