import type { ReservationDto, RoomDto, SlotTime } from '../types';
import { generateTimeSlots } from '../domain/timeSlots';
import { findReservationAt } from '../domain/reservationLookup';

export type TimeSlotGridProps = {
  rooms: RoomDto[];
  reservations: ReservationDto[];
  onEmptySlotClick: (roomId: number, slot: SlotTime) => void;
  onReservedSlotClick: (reservation: ReservationDto) => void;
};

export default function TimeSlotGrid({
  rooms,
  reservations,
  onEmptySlotClick,
  onReservedSlotClick,
}: TimeSlotGridProps) {
  const slots = generateTimeSlots();

  return (
    <div>
      {rooms.map((room) => (
        <div key={room.id}>
          <span>{room.name}</span>
          {slots.map((slot) => {
            const reservation = findReservationAt(reservations, room.id, slot);
            const label = reservation
              ? `${room.name} ${slot} ${reservation.reserverName} ${reservation.purpose}`
              : `${room.name} ${slot} 빈 슬롯`;
            return (
              <button
                key={slot}
                type="button"
                data-testid={`slot-${room.id}-${slot}`}
                aria-label={label}
                onClick={() =>
                  reservation
                    ? onReservedSlotClick(reservation)
                    : onEmptySlotClick(room.id, slot)
                }
              >
                {reservation && (
                  <>
                    <span>{reservation.reserverName}</span>
                    <span>{reservation.purpose}</span>
                  </>
                )}
              </button>
            );
          })}
        </div>
      ))}
    </div>
  );
}
