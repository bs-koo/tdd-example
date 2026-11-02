import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import ErrorBanner from './ErrorBanner';

describe('ErrorBanner', () => {
  it('code가 null이면 오류 배너가 렌더링되지 않는다', () => {
    render(<ErrorBanner code={null} />);

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });

  it('code가 OVERLAPPING_RESERVATION이면 "해당 시간대에 이미 예약이 있습니다."가 표시된다 (AC-34)', () => {
    render(<ErrorBanner code="OVERLAPPING_RESERVATION" />);

    expect(screen.getByRole('alert')).toHaveTextContent('해당 시간대에 이미 예약이 있습니다.');
  });

  it('code가 NOT_RESERVER이면 "예약자 본인만 취소할 수 있습니다."가 표시된다', () => {
    render(<ErrorBanner code="NOT_RESERVER" />);

    expect(screen.getByRole('alert')).toHaveTextContent('예약자 본인만 취소할 수 있습니다.');
  });

  it('알려지지 않은 코드는 기본 오류 메시지 "요청을 처리하지 못했습니다."로 표시된다', () => {
    render(<ErrorBanner code="UNKNOWN" />);

    expect(screen.getByRole('alert')).toHaveTextContent('요청을 처리하지 못했습니다.');
  });

  it('code가 INVALID_CREDENTIALS이면 사유와 함께 다음에 할 일까지 안내한다', () => {
    render(<ErrorBanner code="INVALID_CREDENTIALS" />);

    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('이메일 또는 비밀번호가 올바르지 않습니다.');
    expect(alert).toHaveTextContent('이메일과 비밀번호를 다시 확인해 주세요.');
  });

  it('code가 MEMBER_NOT_FOUND이면 초대받을 사람이 먼저 가입해야 한다고 알려 준다', () => {
    render(<ErrorBanner code="MEMBER_NOT_FOUND" />);

    const alert = screen.getByRole('alert');
    expect(alert).toHaveTextContent('해당 이메일의 사용자를 찾을 수 없습니다.');
    expect(alert).toHaveTextContent('초대할 사람이 먼저 가입해야 합니다. 이메일을 다시 확인해 주세요.');
  });

  it('code가 NOT_GROUP_OWNER이면 그룹장에게 요청하라고 안내한다', () => {
    render(<ErrorBanner code="NOT_GROUP_OWNER" />);

    expect(screen.getByRole('alert')).toHaveTextContent('그룹장에게 대신 처리해 달라고 요청해 주세요.');
  });

  it('code가 바뀌면 배너 문구도 그에 맞게 갱신된다 (하드코딩 방지)', () => {
    const { rerender } = render(<ErrorBanner code="OVERLAPPING_RESERVATION" />);
    expect(screen.getByRole('alert')).toHaveTextContent('해당 시간대에 이미 예약이 있습니다.');

    rerender(<ErrorBanner code="ALREADY_CANCELLED" />);

    expect(screen.getByRole('alert')).toHaveTextContent('이미 취소된 예약입니다.');
  });
});
