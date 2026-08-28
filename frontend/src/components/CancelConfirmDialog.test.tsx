import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { ReservationDto } from '../types';
import CancelConfirmDialog from './CancelConfirmDialog';

const RESERVATION: ReservationDto = {
  id: 7,
  roomId: 1,
  reserverName: '김본승',
  purpose: '주간 회의',
  startAt: '2026-08-26T09:00:00',
  endAt: '2026-08-26T10:00:00',
  status: 'ACTIVE',
};

type RenderOverrides = {
  open?: boolean;
  reservation: ReservationDto | null;
  onConfirm?: (reservation: ReservationDto) => void;
  onClose?: () => void;
};

function renderDialog(overrides: RenderOverrides) {
  const onConfirm = overrides.onConfirm ?? vi.fn();
  const onClose = overrides.onClose ?? vi.fn();
  render(
    <CancelConfirmDialog
      open={overrides.open ?? true}
      reservation={overrides.reservation}
      onConfirm={onConfirm}
      onClose={onClose}
    />,
  );
  return { onConfirm, onClose };
}

describe('CancelConfirmDialog', () => {
  it('open이 false이면 대화상자가 렌더링되지 않는다', () => {
    renderDialog({ open: false, reservation: RESERVATION });

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('open이 true이고 reservation이 있으면 aria-label이 "예약 취소 확인"인 대화상자가 열린다', () => {
    renderDialog({ open: true, reservation: RESERVATION });

    expect(screen.getByRole('dialog', { name: '예약 취소 확인' })).toBeInTheDocument();
  });

  it('open이 true여도 reservation이 null이면 대화상자가 열리지 않는다 (취소할 대상 없음)', () => {
    renderDialog({ open: true, reservation: null });

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('확인 버튼을 클릭하면 onConfirm이 reservation 객체 그대로 정확히 1회 호출되고 onClose는 호출되지 않는다 (§9.8)', async () => {
    const user = userEvent.setup();
    const { onConfirm, onClose } = renderDialog({ reservation: RESERVATION });

    await user.click(screen.getByRole('button', { name: '확인' }));

    expect(onConfirm).toHaveBeenCalledTimes(1);
    expect(onConfirm).toHaveBeenCalledWith(RESERVATION);
    expect(onClose).not.toHaveBeenCalled();
  });

  it('대화상자가 열려도 내부에 텍스트 입력은 존재하지 않는다 — 취소 시 이름을 재입력받지 않는다 (§9.8)', () => {
    renderDialog({ reservation: RESERVATION });

    expect(screen.getByRole('dialog', { name: '예약 취소 확인' })).toBeInTheDocument();
    expect(screen.queryByRole('textbox')).not.toBeInTheDocument();
  });

  it('닫기 버튼을 클릭하면 onClose가 정확히 1회 호출되고 onConfirm은 호출되지 않는다', async () => {
    const user = userEvent.setup();
    const { onConfirm, onClose } = renderDialog({ reservation: RESERVATION });

    await user.click(screen.getByRole('button', { name: '닫기' }));

    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onConfirm).not.toHaveBeenCalled();
  });
});
