import { useEffect, useMemo, useRef, useState } from 'react';
import type { CreateReservationRequest, ReservationDto, SlotTime } from '../types';
import type { ReservationApi } from '../api/types';
import { reservationApi } from '../api/reservations';
import { useReservationBoard } from '../hooks/useReservationBoard';
import { today } from '../today';
import DateSelector, { DATE_INPUT_ID } from '../components/DateSelector';
import TimeSlotGrid from '../components/TimeSlotGrid';
import ReservationFormDialog from '../components/ReservationFormDialog';
import CancelConfirmDialog from '../components/CancelConfirmDialog';
import ErrorBanner from '../components/ErrorBanner';

type FormTarget = { roomId: number; slot: SlotTime };

type ReservationPageProps = {
  initialDate: string;
  api?: ReservationApi;
};

const BOOKING_WINDOW_DAYS = 14;
const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토'];
const COMPLETE_DATE = /^\d{4}-\d{2}-\d{2}$/;
const SEOUL_TIME = new Intl.DateTimeFormat('sv-SE', {
  timeZone: 'Asia/Seoul',
  hour: '2-digit',
  minute: '2-digit',
  hour12: false,
});

function shiftDate(date: string, days: number): string {
  if (!COMPLETE_DATE.test(date)) {
    return date;
  }
  const [year, month, day] = date.split('-').map(Number);
  return new Date(Date.UTC(year, month - 1, day + days)).toISOString().slice(0, 10);
}

function formatHeading(date: string): string {
  if (!COMPLETE_DATE.test(date)) {
    return date;
  }
  const [year, month, day] = date.split('-').map(Number);
  const weekday = WEEKDAYS[new Date(Date.UTC(year, month - 1, day)).getUTCDay()];
  return `${year}년 ${month}월 ${day}일 ${weekday}요일`;
}

export default function ReservationPage({ initialDate, api = reservationApi }: ReservationPageProps) {
  const [pending, setPending] = useState(0);
  const [firstLoadDone, setFirstLoadDone] = useState(false);
  const lastMutation = useRef<'ok' | 'fail'>('ok');
  const sawPending = useRef(false);

  // 훅의 계약(예외를 삼키고 항상 resolve)은 그대로 둔 채, 화면이 알아야 하는
  // "직전 쓰기가 성공했는가"와 "지금 요청 중인가"만 여기서 기록한다.
  // 각 메서드는 1:1 위임이고 예외는 반드시 되던져 훅이 그대로 잡게 한다.
  const trackedApi = useMemo<ReservationApi>(() => {
    const count = async <T,>(run: () => Promise<T>): Promise<T> => {
      setPending((n) => n + 1);
      try {
        return await run();
      } finally {
        setPending((n) => n - 1);
      }
    };
    const mutate = async <T,>(run: () => Promise<T>): Promise<T> => {
      try {
        const value = await count(run);
        lastMutation.current = 'ok';
        return value;
      } catch (e) {
        lastMutation.current = 'fail';
        throw e;
      }
    };
    return {
      fetchRooms: () => count(() => api.fetchRooms()),
      fetchReservations: (roomId, date) => count(() => api.fetchReservations(roomId, date)),
      createReservation: (request) => mutate(() => api.createReservation(request)),
      cancelReservation: (id, reserverName) =>
        mutate(() => api.cancelReservation(id, reserverName)),
    };
  }, [api]);

  const board = useReservationBoard({ initialDate, api: trackedApi });
  const [formTarget, setFormTarget] = useState<FormTarget | null>(null);
  const [cancelTarget, setCancelTarget] = useState<ReservationDto | null>(null);

  useEffect(() => {
    if (pending > 0) {
      sawPending.current = true;
      return;
    }
    if (sawPending.current) {
      setFirstLoadDone(true);
    }
  }, [pending]);

  const minDate = useMemo(() => today(), []);
  const maxDate = useMemo(() => shiftDate(minDate, BOOKING_WINDOW_DAYS), [minDate]);
  const now = useMemo(() => new Date(), [board.date, pending]);

  const handleFormSubmit = async (request: CreateReservationRequest) => {
    await board.createReservation(request);
    // 실패했으면 폼을 그대로 둔다 — 입력을 날리지 않는 것이 재입력보다 싸다.
    if (lastMutation.current === 'ok') {
      setFormTarget(null);
    }
  };

  const handleCancelConfirm = async (reservation: ReservationDto) => {
    await board.cancelReservation(reservation);
    if (lastMutation.current === 'ok') {
      setCancelTarget(null);
    }
  };

  const goToDate = (next: string) => {
    void board.changeDate(next);
  };

  // 다이얼로그를 열 때 이전 오류를 지운다. 패널 안에 남은 오류는 이 조작의 결과만 말해야 한다.
  const openForm = (roomId: number, slot: SlotTime) => {
    board.clearError();
    setFormTarget({ roomId, slot });
  };

  const openCancel = (reservation: ReservationDto) => {
    board.clearError();
    setCancelTarget(reservation);
  };

  const dialogOpen = formTarget !== null || cancelTarget !== null;

  const boardMessage =
    board.errorCode !== null
      ? '예약 현황을 불러오지 못했습니다. 위의 다시 시도를 눌러 주세요.'
      : firstLoadDone
        ? '등록된 회의실이 없습니다. 관리자에게 문의해 주세요.'
        : '회의실 현황을 불러오는 중입니다.';

  const cancelRoomName = board.rooms.find((room) => room.id === cancelTarget?.roomId)?.name;

  return (
    <div className="page">
      <a className="skip-link" href="#board-end">
        격자 건너뛰기
      </a>

      <main className="shell">
        <header className="masthead">
          <h1 className="masthead__title">회의실 예약</h1>
          <p className="masthead__date">{formatHeading(board.date)}</p>
          <p className="masthead__policy">
            09:00–18:00 · 30분 단위 · 한 번에 최소 30분, 최대 4시간 · 오늘부터 14일 이내
          </p>
        </header>

        <div className="toolbar">
          <div className="datebar">
            <label className="datebar__label" htmlFor={DATE_INPUT_ID}>
              날짜 선택
            </label>
            <div className="datebar__controls">
              <button
                type="button"
                className="btn btn--icon"
                onClick={() => goToDate(shiftDate(board.date, -1))}
                disabled={board.date <= minDate}
              >
                이전 날
              </button>
              <DateSelector date={board.date} onChange={goToDate} />
              <button
                type="button"
                className="btn btn--icon"
                onClick={() => goToDate(shiftDate(board.date, 1))}
                disabled={board.date >= maxDate}
              >
                다음 날
              </button>
              <button
                type="button"
                className="btn btn--quiet"
                onClick={() => goToDate(minDate)}
                disabled={board.date === minDate}
              >
                오늘
              </button>
            </div>
          </div>

          <div className="toolbar__aside">
            <ul className="legend">
              <li className="legend__item">
                <span className="legend__swatch" aria-hidden="true" />
                가용
              </li>
              <li className="legend__item">
                <span className="legend__swatch legend__swatch--busy" aria-hidden="true" />
                예약됨
              </li>
              <li className="legend__item">
                <span className="legend__swatch legend__swatch--past" aria-hidden="true" />
                지난 시간
              </li>
            </ul>
            <span className="stamp">{SEOUL_TIME.format(now)} 기준</span>
          </div>
        </div>

        {/* 배너가 없어도 높이를 예약해 둔다 — 오류가 떠도 격자가 아래로 밀리지 않는다. */}
        <div className={dialogOpen ? 'notice notice--quiet' : 'notice'}>
          <ErrorBanner code={board.errorCode} />
          {board.errorCode !== null && (
            <div className="notice__actions">
              <button type="button" className="btn" onClick={() => goToDate(board.date)}>
                다시 시도
              </button>
              <button
                type="button"
                className="btn btn--quiet"
                aria-label="오류 알림 닫기"
                onClick={board.clearError}
              >
                알림 닫기
              </button>
            </div>
          )}
          {board.errorCode === null && pending > 0 && board.rooms.length > 0 && (
            <p className="notice__status" role="status">
              예약 현황을 새로 불러오는 중입니다.
            </p>
          )}
        </div>

        <section className="board-card" aria-label="회의실 시간표">
          {board.rooms.length > 0 ? (
            <TimeSlotGrid
              rooms={board.rooms}
              reservations={board.reservations}
              date={board.date}
              now={now}
              onEmptySlotClick={openForm}
              onReservedSlotClick={openCancel}
            />
          ) : (
            <p className="board-card__state" role="status">
              {boardMessage}
            </p>
          )}
        </section>

        <div id="board-end" tabIndex={-1} />
      </main>

      {formTarget && (
        <ReservationFormDialog
          key={`${formTarget.roomId}-${formTarget.slot}`}
          open
          roomId={formTarget.roomId}
          date={board.date}
          initialStartTime={formTarget.slot}
          errorCode={board.errorCode}
          onSubmit={handleFormSubmit}
          onClose={() => setFormTarget(null)}
        />
      )}
      <CancelConfirmDialog
        open={cancelTarget !== null}
        reservation={cancelTarget}
        roomName={cancelRoomName}
        errorCode={board.errorCode}
        onConfirm={handleCancelConfirm}
        onClose={() => setCancelTarget(null)}
      />
    </div>
  );
}
