import { useId, useRef, useState } from 'react';
import { flushSync } from 'react-dom';
import type { CreateRoomRequest } from '../types';
import { useDialogFocus } from './useDialogFocus';
import { toErrorMessage, toNextAction } from './errorCopy';

export type RoomFormDialogProps = {
  open: boolean;
  /** 어느 그룹에 만드는지 패널에서 밝힌다 — 여러 그룹을 오가며 쓰는 화면이라 필요하다. */
  groupName: string;
  onSubmit: (request: CreateRoomRequest) => void;
  onClose: () => void;
  /** 이 폼의 제출이 서버에 거절됐을 때의 코드. 오류를 사고 지점에서 보여 준다. */
  errorCode?: string | null;
};

const NAME_LIMIT = 30;
const LOCATION_LIMIT = 30;
const CAPACITY_MIN = 1;
const CAPACITY_MAX = 999;
const DIGITS = /^\d+$/;

function nameError(name: string): string | null {
  const trimmed = name.trim();
  if (trimmed.length === 0) {
    return '회의실 이름을 입력해 주세요.';
  }
  if (trimmed.length > NAME_LIMIT) {
    return `회의실 이름은 ${NAME_LIMIT}자 이내로 입력해 주세요.`;
  }
  return null;
}

function capacityError(capacity: string): string | null {
  const trimmed = capacity.trim();
  if (!DIGITS.test(trimmed)) {
    return '수용 인원을 숫자로 입력해 주세요.';
  }
  const value = Number(trimmed);
  if (value < CAPACITY_MIN || value > CAPACITY_MAX) {
    return `수용 인원은 ${CAPACITY_MIN}명 이상 ${CAPACITY_MAX}명 이하로 입력해 주세요.`;
  }
  return null;
}

function locationError(location: string): string | null {
  const trimmed = location.trim();
  if (trimmed.length === 0) {
    return '위치를 입력해 주세요.';
  }
  if (trimmed.length > LOCATION_LIMIT) {
    return `위치는 ${LOCATION_LIMIT}자 이내로 입력해 주세요.`;
  }
  return null;
}

export default function RoomFormDialog({
  open,
  groupName,
  onSubmit,
  onClose,
  errorCode = null,
}: RoomFormDialogProps) {
  const [name, setName] = useState('');
  const [capacity, setCapacity] = useState('8');
  const [location, setLocation] = useState('');
  const [attempted, setAttempted] = useState(false);

  const panelRef = useDialogFocus(open, onClose);
  const nameRef = useRef<HTMLInputElement>(null);
  const capacityRef = useRef<HTMLInputElement>(null);
  const locationRef = useRef<HTMLInputElement>(null);

  const fieldId = useId();
  const nameId = `${fieldId}-name`;
  const capacityId = `${fieldId}-capacity`;
  const locationId = `${fieldId}-location`;

  if (!open) {
    return null;
  }

  const errors = {
    name: nameError(name),
    capacity: capacityError(capacity),
    location: locationError(location),
  };
  const shown = attempted ? errors : { name: null, capacity: null, location: null };

  const handleConfirm = () => {
    if (errors.name !== null || errors.capacity !== null || errors.location !== null) {
      // 오류 문구와 aria-describedby 를 먼저 붙여 두어야 초점이 닿는 순간 사유까지 읽힌다.
      flushSync(() => setAttempted(true));
      const target =
        errors.name !== null ? nameRef : errors.capacity !== null ? capacityRef : locationRef;
      target.current?.focus();
      return;
    }
    onSubmit({
      name: name.trim(),
      capacity: Number(capacity.trim()),
      location: location.trim(),
    });
  };

  return (
    <div className="overlay">
      <div className="dialog" role="dialog" aria-modal="true" aria-label="회의실 추가" ref={panelRef}>
        <h2 className="dialog__title">회의실 추가</h2>
        <p className="dialog__lead">{groupName}의 모든 그룹원이 이 회의실을 예약할 수 있습니다.</p>

        <div className="dialog__body">
          <div className="field">
            <div className="field__head">
              <label className="field__label" htmlFor={nameId}>
                회의실 이름 <span className="field__required">*</span>
              </label>
              <span className="field__count">
                {name.length} / {NAME_LIMIT}
              </span>
            </div>
            <input
              id={nameId}
              ref={nameRef}
              className="field__input"
              aria-label="회의실 이름"
              data-autofocus
              maxLength={NAME_LIMIT}
              placeholder="예: 대회의실"
              value={name}
              aria-invalid={shown.name !== null}
              aria-describedby={shown.name !== null ? `${nameId}-error` : undefined}
              onChange={(e) => setName(e.target.value)}
            />
            {shown.name !== null && (
              <span className="field__error" id={`${nameId}-error`}>
                {shown.name}
              </span>
            )}
          </div>

          <div className="field__row">
            <div className="field">
              <div className="field__head">
                <label className="field__label" htmlFor={capacityId}>
                  수용 인원 <span className="field__required">*</span>
                </label>
              </div>
              <input
                id={capacityId}
                ref={capacityRef}
                className="field__input"
                aria-label="수용 인원"
                inputMode="numeric"
                maxLength={3}
                value={capacity}
                aria-invalid={shown.capacity !== null}
                aria-describedby={shown.capacity !== null ? `${capacityId}-error` : undefined}
                onChange={(e) => setCapacity(e.target.value)}
              />
              {shown.capacity !== null ? (
                <span className="field__error" id={`${capacityId}-error`}>
                  {shown.capacity}
                </span>
              ) : (
                <span className="field__hint">1~999명</span>
              )}
            </div>

            <div className="field">
              <div className="field__head">
                <label className="field__label" htmlFor={locationId}>
                  위치 <span className="field__required">*</span>
                </label>
              </div>
              <input
                id={locationId}
                ref={locationRef}
                className="field__input"
                aria-label="위치"
                maxLength={LOCATION_LIMIT}
                placeholder="예: 3층"
                value={location}
                aria-invalid={shown.location !== null}
                aria-describedby={shown.location !== null ? `${locationId}-error` : undefined}
                onChange={(e) => setLocation(e.target.value)}
              />
              {shown.location !== null && (
                <span className="field__error" id={`${locationId}-error`}>
                  {shown.location}
                </span>
              )}
            </div>
          </div>
        </div>

        {/* 서버 거절은 이 패널 안에서 읽힌다. 페이지 배너는 오버레이 아래라 닿지 않는다. */}
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
