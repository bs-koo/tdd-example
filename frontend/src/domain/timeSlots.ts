import type { SlotTime } from '../types';

const BUSINESS_START_MINUTES = 9 * 60;
const BUSINESS_END_MINUTES = 18 * 60;
const SLOT_STEP_MINUTES = 30;

function toHHmm(totalMinutes: number): SlotTime {
  const hours = Math.floor(totalMinutes / 60);
  const minutes = totalMinutes % 60;
  return `${String(hours).padStart(2, '0')}:${String(minutes).padStart(2, '0')}`;
}

function toMinutes(hhmm: SlotTime): number {
  const [hours, minutes] = hhmm.split(':').map(Number);
  return hours * 60 + minutes;
}

export function generateTimeSlots(): SlotTime[] {
  const slots: SlotTime[] = [];
  for (
    let minutes = BUSINESS_START_MINUTES;
    minutes < BUSINESS_END_MINUTES;
    minutes += SLOT_STEP_MINUTES
  ) {
    slots.push(toHHmm(minutes));
  }
  return slots;
}

export function nextSlot(slot: SlotTime): SlotTime {
  return toHHmm(toMinutes(slot) + SLOT_STEP_MINUTES);
}

export function toServerDateTime(date: string, hhmm: string): string {
  return `${date}T${hhmm}:00`;
}
