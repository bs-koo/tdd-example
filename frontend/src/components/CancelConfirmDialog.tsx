import type { ReservationDto } from '../types';
import { useDialogFocus } from './useDialogFocus';
import { toErrorMessage, toNextAction } from './errorCopy';

export type CancelConfirmDialogProps = {
  open: boolean;
  reservation: ReservationDto | null;
  onConfirm: (reservation: ReservationDto) => void;
  onClose: () => void;
  /** 회의실 이름. 페이지가 이미 알고 있으므로 받아서 무엇을 지우는지 문장으로 보여준다. */
  roomName?: string;
  /**
   * 본인 예약인가. false 면 같은 내용을 읽기 전용으로 보여 준다 —
   * 어차피 서버가 NOT_RESERVER 로 막을 조작을 버튼으로 권하지 않는다.
   */
  canCancel?: boolean;
  /** 이 취소가 서버에 거절됐을 때의 코드. 오류를 사고 지점에서 보여 준다. */
  errorCode?: string | null;
};

export default function CancelConfirmDialog({
  open,
  reservation,
  onConfirm,
  onClose,
  roomName,
  canCancel = true,
  errorCode = null,
}: CancelConfirmDialogProps) {
  const visible = open && reservation !== null;
  const panelRef = useDialogFocus(visible, onClose);

  if (!visible || reservation === null) {
    return null;
  }

  const date = reservation.startAt.slice(0, 10);
  const startTime = reservation.startAt.slice(11, 16);
  const endTime = reservation.endAt.slice(11, 16);

  return (
    <div className="overlay">
      <div
        className="dialog"
        role="dialog"
        aria-modal="true"
        aria-label={canCancel ? '예약 취소 확인' : '예약 정보'}
        ref={panelRef}
      >
        <h2 className="dialog__title">{canCancel ? '예약 취소' : '예약 정보'}</h2>
        <p className="dialog__lead">
          {canCancel ? '아래 예약을 취소합니다.' : '이미 잡혀 있는 예약입니다.'}
        </p>

        <div className="dialog__body">
          <dl className="summary">
            {roomName !== undefined && (
              <>
                <dt className="summary__key">회의실</dt>
                <dd className="summary__value">{roomName}</dd>
              </>
            )}
            <dt className="summary__key">일시</dt>
            <dd className="summary__value">
              {date} {startTime}–{endTime}
            </dd>
            <dt className="summary__key">예약자</dt>
            <dd className="summary__value">{reservation.reserverName}</dd>
            <dt className="summary__key">목적</dt>
            <dd className="summary__value">{reservation.purpose || '입력 없음'}</dd>
          </dl>

          <p className="dialog__warning">
            {canCancel ? (
              '취소하면 이 예약은 사라지고 되돌릴 수 없습니다. 같은 시간대가 다시 필요하면 새로 예약해야 합니다.'
            ) : (
              <>
                <span className="dialog__warning-lead">예약자 본인만 취소할 수 있습니다.</span> 이
                시간대가 필요하면 예약자에게 요청해 주세요.
              </>
            )}
          </p>
        </div>

        {/* 서버 거절(본인 아님·이미 취소됨 등)을 이 패널 안에서 읽게 한다. */}
        {errorCode !== null && (
          <div className="dialog__error" role="status">
            <span className="dialog__error-message">{toErrorMessage(errorCode)}</span>
            <span className="dialog__error-next">{toNextAction(errorCode)}</span>
          </div>
        )}

        <div className="dialog__actions">
          <button type="button" className="btn" data-autofocus onClick={onClose}>
            닫기
          </button>
          {canCancel && (
            <button
              type="button"
              className="btn btn--danger"
              onClick={() => onConfirm(reservation)}
            >
              확인
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
