import { useState } from 'react';
import type { CreateReservationRequest, ReservationDto, SlotTime } from '../types';
import type { ReservationApi } from '../api/types';
import { reservationApi } from '../api/reservations';
import { useReservationBoard } from '../hooks/useReservationBoard';
import DateSelector from '../components/DateSelector';
import TimeSlotGrid from '../components/TimeSlotGrid';
import ReservationFormDialog from '../components/ReservationFormDialog';
import CancelConfirmDialog from '../components/CancelConfirmDialog';
import ErrorBanner from '../components/ErrorBanner';

type FormTarget = { roomId: number; slot: SlotTime };

type ReservationPageProps = {
  initialDate: string;
  api?: ReservationApi;
};

export default function ReservationPage({ initialDate, api = reservationApi }: ReservationPageProps) {
  const board = useReservationBoard({ initialDate, api });
  const [formTarget, setFormTarget] = useState<FormTarget | null>(null);
  const [cancelTarget, setCancelTarget] = useState<ReservationDto | null>(null);

  const handleFormSubmit = async (request: CreateReservationRequest) => {
    await board.createReservation(request);
    setFormTarget(null);
  };

  const handleCancelConfirm = async (reservation: ReservationDto) => {
    await board.cancelReservation(reservation);
    setCancelTarget(null);
  };

  return (
    <div>
      <ErrorBanner code={board.errorCode} />
      <DateSelector date={board.date} onChange={(d) => { void board.changeDate(d); }} />
      <TimeSlotGrid
        rooms={board.rooms}
        reservations={board.reservations}
        onEmptySlotClick={(roomId, slot) => setFormTarget({ roomId, slot })}
        onReservedSlotClick={(reservation) => setCancelTarget(reservation)}
      />
      {formTarget && (
        <ReservationFormDialog
          open
          roomId={formTarget.roomId}
          date={board.date}
          initialStartTime={formTarget.slot}
          onSubmit={handleFormSubmit}
          onClose={() => setFormTarget(null)}
        />
      )}
      <CancelConfirmDialog
        open={cancelTarget !== null}
        reservation={cancelTarget}
        onConfirm={handleCancelConfirm}
        onClose={() => setCancelTarget(null)}
      />
    </div>
  );
}
