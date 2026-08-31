import type { CSSProperties } from 'react';
import type { ReservationDto, RoomDto, SlotTime } from '../types';
import { generateTimeSlots } from '../domain/timeSlots';
import { findReservationAt } from '../domain/reservationLookup';

export type TimeSlotGridProps = {
  rooms: RoomDto[];
  reservations: ReservationDto[];
  onEmptySlotClick: (roomId: number, slot: SlotTime) => void;
  onReservedSlotClick: (reservation: ReservationDto) => void;
  /** 보고 있는 날짜("YYYY-MM-DD"). now 와 둘 다 넘어올 때만 지난 슬롯을 구분한다. */
  date?: string;
  /** 현재 시각. 넘기지 않으면 지난 슬롯 판정을 하지 않는다(기존 동작 그대로). */
  now?: Date;
};

const SEOUL_DATE = new Intl.DateTimeFormat('sv-SE', { timeZone: 'Asia/Seoul' });
const SEOUL_TIME = new Intl.DateTimeFormat('sv-SE', {
  timeZone: 'Asia/Seoul',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
});

/**
 * 지난 슬롯 경계를 "HH:mm" 으로 돌려준다. 이 값보다 앞선 슬롯이 지난 시간이다.
 * date·now 중 하나라도 없으면 null — 판정 자체를 하지 않는다.
 */
function pastBoundary(date?: string, now?: Date): SlotTime | null {
  if (date === undefined || now === undefined) {
    return null;
  }
  const nowDate = SEOUL_DATE.format(now);
  if (nowDate < date) {
    return null; // 미래 날짜 — 지난 슬롯 없음
  }
  if (nowDate > date) {
    return '24:00'; // 지난 날짜 — 하루 전체가 지남
  }
  return SEOUL_TIME.format(now);
}

export default function TimeSlotGrid({
  rooms,
  reservations,
  onEmptySlotClick,
  onReservedSlotClick,
  date,
  now,
}: TimeSlotGridProps) {
  const slots = generateTimeSlots();
  const boundary = pastBoundary(date, now);

  if (rooms.length === 0) {
    return null;
  }

  return (
    <div className="board">
      {/* 시간 축 — 전부 비-버튼. 격자의 조작 가능한 요소는 슬롯뿐이다. */}
      <div className="board__row board__row--head">
        <span className="board__corner">시간</span>
        {slots.map((slot) => (
          <span key={slot} className="board__tick" data-half={slot.endsWith(':30')}>
            {slot}
          </span>
        ))}
      </div>

      {rooms.map((room) => {
        const perSlot = slots.map((slot) => findReservationAt(reservations, room.id, slot));

        return (
          // display:contents — 행 박스를 없애 머리 행과 같은 그리드에 셀을 올린다.
          <div key={room.id} className="board__row">
            <div className="board__room">
              <span className="board__room-name">{room.name}</span>
              <span className="board__room-meta">{room.capacity}명</span>
              <span className="board__room-meta">{room.location}</span>
            </div>
            {slots.map((slot, index) => {
              const reservation = perSlot[index];
              const past = boundary !== null && slot < boundary;
              // 앞·뒤 칸이 같은 예약이면 경계를 지워 하나의 면으로 잇는다(칸 병합 아님).
              const continues =
                reservation !== undefined && index > 0 && perSlot[index - 1]?.id === reservation.id;
              const continued =
                reservation !== undefined &&
                index < slots.length - 1 &&
                perSlot[index + 1]?.id === reservation.id;

              // 이어진 면의 칸 수. 첫 칸의 글자가 그 면 전체 폭을 쓰게 하는 데 쓴다.
              let runLength = 1;
              if (reservation !== undefined && !continues) {
                while (
                  index + runLength < slots.length &&
                  perSlot[index + runLength]?.id === reservation.id
                ) {
                  runLength += 1;
                }
              }

              const label = reservation
                ? `${room.name} ${slot} ${reservation.reserverName} ${reservation.purpose}`
                : past
                  ? `${room.name} ${slot} 지난 시간 빈 슬롯`
                  : `${room.name} ${slot} 빈 슬롯`;

              return (
                <button
                  key={slot}
                  type="button"
                  className="slot"
                  data-testid={`slot-${room.id}-${slot}`}
                  data-state={reservation ? 'busy' : past ? 'past' : 'free'}
                  data-continues={continues || undefined}
                  data-continued={continued || undefined}
                  style={runLength > 1 ? ({ '--run': runLength } as CSSProperties) : undefined}
                  aria-label={label}
                  onClick={() =>
                    reservation
                      ? onReservedSlotClick(reservation)
                      : onEmptySlotClick(room.id, slot)
                  }
                >
                  {reservation && (
                    <>
                      <span className="slot__name">{reservation.reserverName}</span>
                      <span className="slot__purpose">{reservation.purpose}</span>
                    </>
                  )}
                </button>
              );
            })}
          </div>
        );
      })}
    </div>
  );
}
