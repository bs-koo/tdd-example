import { describe, it, expect, vi } from 'vitest';
import { render, screen, within, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { RoomDto, ReservationDto } from '../types';
import type { ReservationApi } from '../api/types';
import { ApiError } from '../api/client';
import ReservationPage from './ReservationPage';

const ROOMS: RoomDto[] = [
  { id: 1, name: '대회의실', capacity: 20, location: '3층' },
  { id: 2, name: '중회의실', capacity: 10, location: '3층' },
];

const EXISTING: ReservationDto = {
  id: 11,
  roomId: 1,
  reserverName: '김본승',
  purpose: '주간 회의',
  startAt: '2026-08-26T09:00:00',
  endAt: '2026-08-26T10:00:00',
  status: 'ACTIVE',
};

const NEW_RESERVATION: ReservationDto = {
  id: 33,
  roomId: 1,
  reserverName: '이수민',
  purpose: '기획 회의',
  startAt: '2026-08-26T09:30:00',
  endAt: '2026-08-26T10:00:00',
  status: 'ACTIVE',
};

function createFakeApi() {
  return {
    fetchRooms: vi.fn(),
    fetchReservations: vi.fn(),
    createReservation: vi.fn(),
    cancelReservation: vi.fn(),
  };
}

function renderPage(fakeApi: ReturnType<typeof createFakeApi>, initialDate: string = '2026-08-26') {
  return render(<ReservationPage initialDate={initialDate} api={fakeApi as unknown as ReservationApi} />);
}

describe('ReservationPage', () => {
  it('마운트 후 회의실 이름과 기존 예약자명이 그리드에 표시된다 (초기 로드)', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations.mockImplementation(async (roomId: number) => (roomId === 1 ? [EXISTING] : []));

    renderPage(api);

    await screen.findByText('대회의실');
    expect(screen.getByText('중회의실')).toBeInTheDocument();
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
    await user.type(within(dialog).getByLabelText('예약자명'), '김본승');
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
    await user.type(within(dialog).getByLabelText('예약자명'), '이수민');
    await user.click(within(dialog).getByRole('button', { name: '확인' }));

    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '예약 폼' })).not.toBeInTheDocument();
    });
    await waitFor(() => {
      expect(within(screen.getByTestId('slot-1-09:30')).getByText('이수민')).toBeInTheDocument();
    });
  });

  it('예약된 슬롯을 취소하면 서버 재조회 후 슬롯에서 예약자명이 사라지고 대화상자도 닫힌다 (AC-35)', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations
      .mockResolvedValueOnce([EXISTING]) // room 1, 최초 로드
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
      expect(api.cancelReservation).toHaveBeenCalledWith(11, '김본승');
    });
    await waitFor(() => {
      expect(within(screen.getByTestId('slot-1-09:00')).queryByText('김본승')).not.toBeInTheDocument();
    });
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '예약 취소 확인' })).not.toBeInTheDocument();
    });
  });

  it('취소 확인 대화상자에서 닫기를 누르면 취소 요청이 전송되지 않고 대화상자만 닫힌다', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations.mockImplementation(async (roomId: number) => (roomId === 1 ? [EXISTING] : []));
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
    api.fetchReservations.mockImplementation(async (roomId: number) => (roomId === 1 ? [EXISTING] : []));

    renderPage(api);

    await screen.findByText('대회의실');
    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('날짜 선택 입력을 바꾸면 바뀐 날짜로 예약 목록을 재조회한다', async () => {
    const api = createFakeApi();
    api.fetchRooms.mockResolvedValue(ROOMS);
    api.fetchReservations.mockResolvedValue([]);
    renderPage(api, '2026-08-26');
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
    renderPage(api, '2026-08-26');
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
