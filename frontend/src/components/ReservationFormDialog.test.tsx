import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { SlotTime, CreateReservationRequest } from '../types';
import ReservationFormDialog from './ReservationFormDialog';

type RenderOverrides = {
  open?: boolean;
  roomId?: number;
  date?: string;
  initialStartTime?: SlotTime;
  onSubmit?: (request: CreateReservationRequest) => void;
  onClose?: () => void;
};

function renderDialog(overrides: RenderOverrides = {}) {
  const onSubmit = overrides.onSubmit ?? vi.fn();
  const onClose = overrides.onClose ?? vi.fn();
  render(
    <ReservationFormDialog
      open={overrides.open ?? true}
      roomId={overrides.roomId ?? 1}
      date={overrides.date ?? '2026-08-26'}
      initialStartTime={overrides.initialStartTime ?? '09:30'}
      onSubmit={onSubmit}
      onClose={onClose}
    />,
  );
  return { onSubmit, onClose };
}

describe('ReservationFormDialog', () => {
  it('open이 false이면 대화상자가 렌더링되지 않는다', () => {
    renderDialog({ open: false });

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('open이 true이면 aria-label이 "예약 폼"인 대화상자가 열린다', () => {
    renderDialog({ open: true });

    expect(screen.getByRole('dialog', { name: '예약 폼' })).toBeInTheDocument();
  });

  it('initialStartTime이 09:30이면 시작 시각 입력값이 09:30으로 채워진다 (AC-33)', () => {
    renderDialog({ initialStartTime: '09:30' });

    expect(screen.getByLabelText('시작 시각')).toHaveValue('09:30');
  });

  it('시작 시각 입력은 readOnly이며 사용자가 입력해도 값이 바뀌지 않는다', async () => {
    const user = userEvent.setup();
    renderDialog({ initialStartTime: '09:30' });

    const startTimeInput = screen.getByLabelText('시작 시각');
    expect(startTimeInput).toHaveAttribute('readonly');

    await user.type(startTimeInput, '11:00');

    expect(startTimeInput).toHaveValue('09:30');
  });

  it('종료 시각의 기본값은 시작 시각의 다음 슬롯인 10:00이다', () => {
    renderDialog({ initialStartTime: '09:30' });

    expect(screen.getByLabelText('종료 시각')).toHaveValue('10:00');
  });

  it('예약자명·목적을 입력하고 종료 시각을 변경한 뒤 확인을 누르면 onSubmit이 정확한 요청 객체로 1회 호출된다', async () => {
    const user = userEvent.setup();
    const { onSubmit } = renderDialog({ roomId: 1, date: '2026-08-26', initialStartTime: '09:30' });

    await user.type(screen.getByLabelText('예약자명'), '김본승');
    await user.type(screen.getByLabelText('회의 목적'), '주간 회의');

    const endTimeInput = screen.getByLabelText('종료 시각');
    await user.clear(endTimeInput);
    await user.type(endTimeInput, '11:00');

    await user.click(screen.getByRole('button', { name: '확인' }));

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit).toHaveBeenCalledWith({
      roomId: 1,
      reserverName: '김본승',
      purpose: '주간 회의',
      startAt: '2026-08-26T09:30:00',
      endAt: '2026-08-26T11:00:00',
    });
  });

  it('회의 목적을 비운 채 제출해도 onSubmit이 호출되고 purpose는 빈 문자열이다', async () => {
    const user = userEvent.setup();
    const { onSubmit } = renderDialog({ roomId: 1, date: '2026-08-26', initialStartTime: '09:30' });

    await user.type(screen.getByLabelText('예약자명'), '김본승');
    await user.click(screen.getByRole('button', { name: '확인' }));

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit).toHaveBeenCalledWith({
      roomId: 1,
      reserverName: '김본승',
      purpose: '',
      startAt: '2026-08-26T09:30:00',
      endAt: '2026-08-26T10:00:00',
    });
  });

  it('닫기 버튼을 누르면 onClose가 정확히 1회 호출되고 onSubmit은 호출되지 않는다', async () => {
    const user = userEvent.setup();
    const { onSubmit, onClose } = renderDialog();

    await user.click(screen.getByRole('button', { name: '닫기' }));

    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('initialStartTime이 14:00이면 시작 시각 14:00, 종료 시각 기본값 14:30으로 반영된다', () => {
    renderDialog({ initialStartTime: '14:00' });

    expect(screen.getByLabelText('시작 시각')).toHaveValue('14:00');
    expect(screen.getByLabelText('종료 시각')).toHaveValue('14:30');
  });
});
