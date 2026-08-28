import { describe, it, expect, vi } from 'vitest';
import { useState } from 'react';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import DateSelector from './DateSelector';

// 실사용(onChange → 상태 갱신 → date prop)과 동일한 흐름을 재현하는 하네스.
// vi.fn()만으로 렌더하면 제어 컴포넌트의 value가 매 키스트로크마다 원래 prop으로
// 되돌아가 타이핑이 누적되지 않으므로, 상태를 들고 있는 이 하네스를 통해 렌더한다.
function ControlledHarness({
  initialDate,
  onChange,
}: {
  initialDate: string;
  onChange: (date: string) => void;
}) {
  const [date, setDate] = useState(initialDate);
  return (
    <DateSelector
      date={date}
      onChange={(d) => {
        setDate(d);
        onChange(d);
      }}
    />
  );
}

describe('DateSelector', () => {
  it('date prop으로 렌더하면 "날짜 선택" 입력의 값이 해당 날짜로 채워진다', () => {
    render(<DateSelector date="2026-08-26" onChange={vi.fn()} />);

    expect(screen.getByLabelText('날짜 선택')).toHaveValue('2026-08-26');
  });

  it('사용자가 날짜를 새 값으로 바꾸면 onChange가 호출되고 마지막 인자는 완성된 날짜 문자열이다', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<ControlledHarness initialDate="2026-08-26" onChange={onChange} />);

    const input = screen.getByLabelText('날짜 선택');
    await user.clear(input);
    await user.type(input, '2026-08-27');

    expect(onChange).toHaveBeenCalled();
    expect(onChange).toHaveBeenLastCalledWith('2026-08-27');
  });

  it('date prop이 다른 값으로 바뀌면 입력값도 새 날짜로 갱신된다', () => {
    const { rerender } = render(<DateSelector date="2026-08-26" onChange={vi.fn()} />);

    rerender(<DateSelector date="2026-09-01" onChange={vi.fn()} />);

    expect(screen.getByLabelText('날짜 선택')).toHaveValue('2026-09-01');
  });

  it('완성되지 않은 입력도 화면에 그대로 보인다 (AC-38 로컬 draft)', async () => {
    const user = userEvent.setup();
    render(<DateSelector date="2026-08-26" onChange={vi.fn()} />);

    const input = screen.getByLabelText('날짜 선택');
    await user.clear(input);
    await user.type(input, '2026-08');

    expect(input).toHaveValue('2026-08');
  });

  it('입력이 완성되기 전에는 onChange가 호출되지 않는다 (AC-38)', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<DateSelector date="2026-08-26" onChange={onChange} />);

    const input = screen.getByLabelText('날짜 선택');
    await user.clear(input);
    await user.type(input, '2026-08');

    expect(onChange).not.toHaveBeenCalled();
  });

  it('입력이 YYYY-MM-DD 형식으로 완성되면 그 시점에 onChange가 정확히 1회 호출된다 (AC-38)', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<DateSelector date="2026-08-26" onChange={onChange} />);

    const input = screen.getByLabelText('날짜 선택');
    await user.clear(input);
    await user.type(input, '2026-08');
    await user.type(input, '-27');

    expect(onChange).toHaveBeenCalledTimes(1);
    expect(onChange).toHaveBeenLastCalledWith('2026-08-27');
  });
});
