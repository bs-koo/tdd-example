import type { ReservationDto } from '../types';

export type CancelConfirmDialogProps = {
  open: boolean;
  reservation: ReservationDto | null;
  onConfirm: (reservation: ReservationDto) => void;
  onClose: () => void;
};

export default function CancelConfirmDialog({
  open,
  reservation,
  onConfirm,
  onClose,
}: CancelConfirmDialogProps) {
  if (!open || reservation === null) {
    return null;
  }

  return (
    <div role="dialog" aria-label="예약 취소 확인">
      <button type="button" onClick={() => onConfirm(reservation)}>
        확인
      </button>
      <button type="button" onClick={onClose}>
        닫기
      </button>
    </div>
  );
}
