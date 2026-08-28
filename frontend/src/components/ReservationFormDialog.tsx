import { useState } from 'react';
import type { CreateReservationRequest, SlotTime } from '../types';
import { nextSlot, toServerDateTime } from '../domain/timeSlots';

export type ReservationFormDialogProps = {
  open: boolean;
  roomId: number;
  date: string;
  initialStartTime: SlotTime;
  onSubmit: (request: CreateReservationRequest) => void;
  onClose: () => void;
};

export default function ReservationFormDialog({
  open,
  roomId,
  date,
  initialStartTime,
  onSubmit,
  onClose,
}: ReservationFormDialogProps) {
  const [endTime, setEndTime] = useState(nextSlot(initialStartTime));
  const [reserverName, setReserverName] = useState('');
  const [purpose, setPurpose] = useState('');

  if (!open) {
    return null;
  }

  const handleConfirm = () => {
    onSubmit({
      roomId,
      reserverName,
      purpose,
      startAt: toServerDateTime(date, initialStartTime),
      endAt: toServerDateTime(date, endTime),
    });
  };

  return (
    <div role="dialog" aria-label="예약 폼">
      <input aria-label="시작 시각" value={initialStartTime} readOnly />
      <input
        aria-label="종료 시각"
        value={endTime}
        onChange={(e) => setEndTime(e.target.value)}
      />
      <input
        aria-label="예약자명"
        value={reserverName}
        onChange={(e) => setReserverName(e.target.value)}
      />
      <input
        aria-label="회의 목적"
        value={purpose}
        onChange={(e) => setPurpose(e.target.value)}
      />
      <button type="button" onClick={handleConfirm}>
        확인
      </button>
      <button type="button" onClick={onClose}>
        닫기
      </button>
    </div>
  );
}
