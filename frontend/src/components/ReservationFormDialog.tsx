import { useId, useRef, useState } from 'react';
import { flushSync } from 'react-dom';
import type { CreateReservationRequest, SlotTime } from '../types';
import { generateTimeSlots, nextSlot, toServerDateTime } from '../domain/timeSlots';
import { useDialogFocus } from './useDialogFocus';
import { toErrorMessage, toNextAction } from './errorCopy';

export type ReservationFormDialogProps = {
  open: boolean;
  roomId: number;
  date: string;
  initialStartTime: SlotTime;
  onSubmit: (request: CreateReservationRequest) => void;
  onClose: () => void;
  /** 이 폼의 제출이 서버에 거절됐을 때의 코드. 오류를 사고 지점에서 보여 준다. */
  errorCode?: string | null;
};

const NAME_LIMIT = 20;
const PURPOSE_LIMIT = 50;
const BUSINESS_END_MINUTES = 18 * 60;
const MAX_DURATION_MINUTES = 4 * 60;
const TIME_PATTERN = /^([01]\d|2[0-3]):([0-5]\d)$/;

// 종료 시각 후보 09:30~18:00. 자유 입력을 막지 않으려고 select 가 아니라 datalist 로 제안만 한다.
const END_TIME_OPTIONS = generateTimeSlots().map(nextSlot);

function toMinutes(hhmm: string): number {
  const [hours, minutes] = hhmm.split(':').map(Number);
  return hours * 60 + minutes;
}

function endTimeError(start: SlotTime, end: string): string | null {
  if (end.trim() === '') {
    return '종료 시각을 입력해 주세요.';
  }
  if (!TIME_PATTERN.test(end)) {
    return 'HH:mm 형식으로 입력해 주세요. 예: 11:00';
  }
  const endMinutes = toMinutes(end);
  const startMinutes = toMinutes(start);
  if (endMinutes % 30 !== 0) {
    return '종료 시각은 30분 단위여야 합니다.';
  }
  if (endMinutes <= startMinutes) {
    return `종료 시각은 시작 시각(${start})보다 뒤여야 합니다.`;
  }
  if (endMinutes > BUSINESS_END_MINUTES) {
    return '18:00까지만 예약할 수 있습니다.';
  }
  if (endMinutes - startMinutes > MAX_DURATION_MINUTES) {
    return '한 번에 최대 4시간까지 예약할 수 있습니다.';
  }
  return null;
}

function reserverNameError(name: string): string | null {
  const trimmed = name.trim();
  if (trimmed.length === 0) {
    return '예약자명을 입력해 주세요. 취소할 때 본인 확인에 쓰입니다.';
  }
  if (trimmed.length > NAME_LIMIT) {
    return `예약자명은 ${NAME_LIMIT}자 이내로 입력해 주세요.`;
  }
  return null;
}

export default function ReservationFormDialog({
  open,
  roomId,
  date,
  initialStartTime,
  onSubmit,
  onClose,
  errorCode = null,
}: ReservationFormDialogProps) {
  const [endTime, setEndTime] = useState(nextSlot(initialStartTime));
  const [reserverName, setReserverName] = useState('');
  const [purpose, setPurpose] = useState('');
  const [endTouched, setEndTouched] = useState(false);
  const [attempted, setAttempted] = useState(false);

  const panelRef = useDialogFocus(open, onClose);
  const endRef = useRef<HTMLInputElement>(null);
  const nameRef = useRef<HTMLInputElement>(null);
  const fieldId = useId();
  const startId = `${fieldId}-start`;
  const endId = `${fieldId}-end`;
  const endListId = `${fieldId}-end-options`;
  const endErrorId = `${fieldId}-end-error`;
  const nameId = `${fieldId}-name`;
  const nameErrorId = `${fieldId}-name-error`;
  const purposeId = `${fieldId}-purpose`;

  if (!open) {
    return null;
  }

  const endError = endTimeError(initialStartTime, endTime);
  const nameError = reserverNameError(reserverName);
  const shownEndError = endTouched || attempted ? endError : null;
  const shownNameError = attempted ? nameError : null;

  const handleConfirm = () => {
    if (endError !== null || nameError !== null) {
      // 검증 실패를 초점으로 알린다. 버튼에 초점이 남으면 키보드·스크린리더 사용자에게는
      // 아무 일도 일어나지 않은 것처럼 보인다. flushSync 로 오류 문구와 aria-describedby 를
      // 먼저 붙여 두어야 초점이 닿는 순간 사유까지 함께 읽힌다.
      flushSync(() => setAttempted(true));
      // DOM 순서상 종료 시각이 예약자명보다 앞이다 — 첫 번째 무효 필드로 옮긴다.
      (endError !== null ? endRef.current : nameRef.current)?.focus();
      return;
    }
    onSubmit({
      roomId,
      reserverName,
      purpose,
      startAt: toServerDateTime(date, initialStartTime),
      endAt: toServerDateTime(date, endTime),
    });
  };

  return (
    <div className="overlay">
      <div className="dialog" role="dialog" aria-modal="true" aria-label="예약 폼" ref={panelRef}>
        <h2 className="dialog__title">예약 만들기</h2>
        <p className="dialog__lead">
          {date} · 30분 단위로 최소 30분, 최대 4시간까지 잡을 수 있습니다.
        </p>

        <div className="dialog__body">
          <div className="field__row">
            <div className="field">
              <div className="field__head">
                <label className="field__label" htmlFor={startId}>
                  시작 시각
                </label>
              </div>
              <input
                id={startId}
                className="field__input"
                aria-label="시작 시각"
                value={initialStartTime}
                readOnly
              />
              <span className="field__hint">격자에서 고른 칸으로 고정됩니다.</span>
            </div>

            <div className="field">
              <div className="field__head">
                <label className="field__label" htmlFor={endId}>
                  종료 시각
                </label>
              </div>
              <input
                id={endId}
                ref={endRef}
                className="field__input"
                aria-label="종료 시각"
                list={endListId}
                value={endTime}
                aria-invalid={shownEndError !== null}
                aria-describedby={shownEndError !== null ? endErrorId : undefined}
                onChange={(e) => {
                  setEndTouched(true);
                  setEndTime(e.target.value);
                }}
              />
              <datalist id={endListId}>
                {END_TIME_OPTIONS.map((option) => (
                  <option key={option} value={option} />
                ))}
              </datalist>
              {shownEndError !== null ? (
                <span className="field__error" id={endErrorId}>
                  {shownEndError}
                </span>
              ) : (
                <span className="field__hint">09:30부터 18:00까지, 30분 단위</span>
              )}
            </div>
          </div>

          <div className="field">
            <div className="field__head">
              <label className="field__label" htmlFor={nameId}>
                예약자명 <span className="field__required">*</span>
              </label>
              <span className="field__count">
                {reserverName.length} / {NAME_LIMIT}
              </span>
            </div>
            <input
              id={nameId}
              ref={nameRef}
              className="field__input"
              aria-label="예약자명"
              data-autofocus
              maxLength={NAME_LIMIT}
              value={reserverName}
              aria-invalid={shownNameError !== null}
              aria-describedby={shownNameError !== null ? nameErrorId : undefined}
              onChange={(e) => setReserverName(e.target.value)}
            />
            {shownNameError !== null && (
              <span className="field__error" id={nameErrorId}>
                {shownNameError}
              </span>
            )}
          </div>

          <div className="field">
            <div className="field__head">
              <label className="field__label" htmlFor={purposeId}>
                회의 목적
              </label>
              <span className="field__count">
                {purpose.length} / {PURPOSE_LIMIT}
              </span>
            </div>
            <input
              id={purposeId}
              className="field__input"
              aria-label="회의 목적"
              maxLength={PURPOSE_LIMIT}
              value={purpose}
              onChange={(e) => setPurpose(e.target.value)}
            />
            <span className="field__hint">비워 두어도 예약됩니다.</span>
          </div>
        </div>

        {/* 서버 거절은 이 패널 안에서 읽힌다. 페이지 배너는 오버레이 아래라 닿지 않는다.
            role="alert" 는 쓰지 않는다 — 페이지 배너가 이미 그 역할을 맡고 있다. */}
        {errorCode !== null && (
          <div className="dialog__error" role="status">
            <span className="dialog__error-message">{toErrorMessage(errorCode)}</span>
            <span className="dialog__error-next">{toNextAction(errorCode)}</span>
          </div>
        )}

        <div className="dialog__actions">
          <button type="button" className="btn" onClick={onClose}>
            닫기
          </button>
          <button type="button" className="btn btn--primary" onClick={handleConfirm}>
            확인
          </button>
        </div>
      </div>
    </div>
  );
}
