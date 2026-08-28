import { describe, it, expect, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { RoomDto, ReservationDto } from '../types';
import TimeSlotGrid from './TimeSlotGrid';

const ROOMS: RoomDto[] = [
  { id: 1, name: '대회의실', capacity: 20, location: '3층' },
  { id: 2, name: '중회의실', capacity: 10, location: '3층' },
  { id: 3, name: '소회의실1', capacity: 4, location: '4층' },
  { id: 4, name: '소회의실2', capacity: 4, location: '4층' },
];

const ACTIVE_RESERVATION: ReservationDto = {
  id: 1,
  roomId: 1,
  reserverName: '김본승',
  purpose: '주간 회의',
  startAt: '2026-08-26T09:00:00',
  endAt: '2026-08-26T10:00:00',
  status: 'ACTIVE',
};

function renderGrid(
  reservations: ReservationDto[],
  onEmptySlotClick: (roomId: number, slot: string) => void,
  onReservedSlotClick: (reservation: ReservationDto) => void,
) {
  render(
    <TimeSlotGrid
      rooms={ROOMS}
      reservations={reservations}
      onEmptySlotClick={onEmptySlotClick}
      onReservedSlotClick={onReservedSlotClick}
    />,
  );
}

describe('TimeSlotGrid', () => {
  it('회의실 4개 × 슬롯 18개 = 72개의 그리드 셀이 렌더링된다 (AC-31)', () => {
    renderGrid([], vi.fn(), vi.fn());

    expect(screen.getAllByTestId(/^slot-/)).toHaveLength(72);
  });

  it('회의실 4개의 이름이 모두 표시된다 (AC-31)', () => {
    renderGrid([], vi.fn(), vi.fn());

    expect(screen.getByText('대회의실')).toBeInTheDocument();
    expect(screen.getByText('중회의실')).toBeInTheDocument();
    expect(screen.getByText('소회의실1')).toBeInTheDocument();
    expect(screen.getByText('소회의실2')).toBeInTheDocument();
  });

  it('예약자명과 목적이 예약된 슬롯 셀 안에 함께 표시된다 (AC-32)', () => {
    renderGrid([ACTIVE_RESERVATION], vi.fn(), vi.fn());

    const cell = screen.getByTestId('slot-1-09:00');
    expect(within(cell).getByText('김본승')).toBeInTheDocument();
    expect(within(cell).getByText('주간 회의')).toBeInTheDocument();
  });

  it('구간 포함 매칭 — 09:30 슬롯 셀에는 예약자명이 표시되고, 종료 경계인 10:00 슬롯 셀에는 표시되지 않는다 (B-5)', () => {
    renderGrid([ACTIVE_RESERVATION], vi.fn(), vi.fn());

    expect(within(screen.getByTestId('slot-1-09:30')).getByText('김본승')).toBeInTheDocument();
    expect(within(screen.getByTestId('slot-1-10:00')).queryByText('김본승')).not.toBeInTheDocument();
  });

  it('예약자명이 같은 시각의 슬롯(slot-1-09:00)에는 표시되지만 다른 회의실 슬롯(slot-2-09:00)에는 표시되지 않는다 (AC-32 격리)', () => {
    renderGrid([ACTIVE_RESERVATION], vi.fn(), vi.fn());

    expect(within(screen.getByTestId('slot-1-09:00')).getByText('김본승')).toBeInTheDocument();
    expect(within(screen.getByTestId('slot-2-09:00')).queryByText('김본승')).not.toBeInTheDocument();
  });

  it('빈 슬롯을 클릭하면 onEmptySlotClick이 회의실ID와 슬롯으로 정확히 1회 호출된다', async () => {
    const user = userEvent.setup();
    const onEmptySlotClick = vi.fn();
    const onReservedSlotClick = vi.fn();
    renderGrid([], onEmptySlotClick, onReservedSlotClick);

    await user.click(screen.getByTestId('slot-3-14:00'));

    expect(onEmptySlotClick).toHaveBeenCalledTimes(1);
    expect(onEmptySlotClick).toHaveBeenCalledWith(3, '14:00');
    expect(onReservedSlotClick).not.toHaveBeenCalled();
  });

  it('예약된 슬롯을 클릭하면 onReservedSlotClick이 해당 예약 객체로 정확히 1회 호출된다', async () => {
    const user = userEvent.setup();
    const onEmptySlotClick = vi.fn();
    const onReservedSlotClick = vi.fn();
    renderGrid([ACTIVE_RESERVATION], onEmptySlotClick, onReservedSlotClick);

    await user.click(screen.getByTestId('slot-1-09:30'));

    expect(onReservedSlotClick).toHaveBeenCalledTimes(1);
    expect(onReservedSlotClick).toHaveBeenCalledWith(ACTIVE_RESERVATION);
    expect(onEmptySlotClick).not.toHaveBeenCalled();
  });

  it('CANCELLED 상태인 예약이 덮는 슬롯은 빈 슬롯으로 취급되어 클릭 시 onEmptySlotClick이 호출된다', async () => {
    const user = userEvent.setup();
    const cancelled: ReservationDto = { ...ACTIVE_RESERVATION, status: 'CANCELLED' };
    const onEmptySlotClick = vi.fn();
    const onReservedSlotClick = vi.fn();
    renderGrid([cancelled], onEmptySlotClick, onReservedSlotClick);

    await user.click(screen.getByTestId('slot-1-09:00'));

    expect(onEmptySlotClick).toHaveBeenCalledTimes(1);
    expect(onEmptySlotClick).toHaveBeenCalledWith(1, '09:00');
    expect(onReservedSlotClick).not.toHaveBeenCalled();
  });

  it('빈 슬롯이 접근 가능한 이름을 가진 버튼으로 노출된다 (AC-37)', () => {
    renderGrid([], vi.fn(), vi.fn());

    expect(screen.getByRole('button', { name: '대회의실 09:30 빈 슬롯' })).toBeInTheDocument();
  });

  it('예약된 슬롯의 접근 가능한 이름에 예약자명과 목적이 포함된다 (AC-37)', () => {
    renderGrid([ACTIVE_RESERVATION], vi.fn(), vi.fn());

    expect(screen.getByRole('button', { name: '대회의실 09:00 김본승 주간 회의' })).toBeInTheDocument();
  });

  it('72개 그리드 셀이 전부 버튼 역할로 노출된다 (AC-37)', () => {
    renderGrid([], vi.fn(), vi.fn());

    expect(screen.getAllByRole('button')).toHaveLength(72);
  });

  it('빈 슬롯에 포커스를 두고 Enter를 누르면 onEmptySlotClick이 호출된다 (AC-37 키보드 조작)', async () => {
    const user = userEvent.setup();
    const onEmptySlotClick = vi.fn();
    const onReservedSlotClick = vi.fn();
    renderGrid([], onEmptySlotClick, onReservedSlotClick);

    const button = screen.getByTestId('slot-3-14:00');
    button.focus();
    await user.keyboard('{Enter}');

    expect(onEmptySlotClick).toHaveBeenCalledTimes(1);
    expect(onEmptySlotClick).toHaveBeenCalledWith(3, '14:00');
  });

  it('예약된 슬롯에 포커스를 두고 Enter를 누르면 onReservedSlotClick이 호출된다 (AC-37 키보드 조작)', async () => {
    const user = userEvent.setup();
    const onEmptySlotClick = vi.fn();
    const onReservedSlotClick = vi.fn();
    renderGrid([ACTIVE_RESERVATION], onEmptySlotClick, onReservedSlotClick);

    const button = screen.getByTestId('slot-1-09:00');
    button.focus();
    await user.keyboard('{Enter}');

    expect(onReservedSlotClick).toHaveBeenCalledTimes(1);
    expect(onReservedSlotClick).toHaveBeenCalledWith(ACTIVE_RESERVATION);
  });

  it('빈 슬롯에 포커스를 두고 Space를 누르면 onEmptySlotClick이 호출된다 (AC-37 키보드 조작)', async () => {
    const user = userEvent.setup();
    const onEmptySlotClick = vi.fn();
    const onReservedSlotClick = vi.fn();
    renderGrid([], onEmptySlotClick, onReservedSlotClick);

    const button = screen.getByTestId('slot-3-14:00');
    button.focus();
    await user.keyboard(' ');

    expect(onEmptySlotClick).toHaveBeenCalledTimes(1);
    expect(onEmptySlotClick).toHaveBeenCalledWith(3, '14:00');
  });
});
