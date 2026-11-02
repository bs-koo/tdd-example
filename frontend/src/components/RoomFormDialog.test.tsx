import { describe, it, expect, vi } from 'vitest';
import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { CreateRoomRequest } from '../types';
import RoomFormDialog from './RoomFormDialog';

type RenderOverrides = {
  open?: boolean;
  errorCode?: string | null;
  onSubmit?: (request: CreateRoomRequest) => void;
  onClose?: () => void;
};

function renderDialog(overrides: RenderOverrides = {}) {
  const onSubmit = overrides.onSubmit ?? vi.fn();
  const onClose = overrides.onClose ?? vi.fn();
  render(
    <RoomFormDialog
      open={overrides.open ?? true}
      groupName="SQI 개발팀"
      errorCode={overrides.errorCode ?? null}
      onSubmit={onSubmit}
      onClose={onClose}
    />,
  );
  return { onSubmit, onClose };
}

describe('RoomFormDialog', () => {
  it('open이 false이면 대화상자가 렌더링되지 않는다', () => {
    renderDialog({ open: false });

    expect(screen.queryByRole('dialog')).not.toBeInTheDocument();
  });

  it('open이 true이면 aria-label이 "회의실 추가"인 대화상자가 열린다', () => {
    renderDialog();

    expect(screen.getByRole('dialog', { name: '회의실 추가' })).toBeInTheDocument();
  });

  it('열리면 회의실 이름 입력으로 초점이 옮겨간다', () => {
    renderDialog();

    expect(screen.getByLabelText('회의실 이름')).toHaveFocus();
  });

  it('이름·수용 인원·위치를 채우고 확인하면 수용 인원이 숫자로 담긴 요청이 1회 전달된다', async () => {
    const user = userEvent.setup();
    const { onSubmit } = renderDialog();

    await user.type(screen.getByLabelText('회의실 이름'), '대회의실');
    await user.clear(screen.getByLabelText('수용 인원'));
    await user.type(screen.getByLabelText('수용 인원'), '20');
    await user.type(screen.getByLabelText('위치'), '3층');
    await user.click(screen.getByRole('button', { name: '확인' }));

    expect(onSubmit).toHaveBeenCalledTimes(1);
    expect(onSubmit).toHaveBeenCalledWith({ name: '대회의실', capacity: 20, location: '3층' });
  });

  it('이름을 비운 채 확인하면 요청을 보내지 않고 사유를 그 자리에서 알려 준다', async () => {
    const user = userEvent.setup();
    const { onSubmit } = renderDialog();

    await user.type(screen.getByLabelText('위치'), '3층');
    await user.click(screen.getByRole('button', { name: '확인' }));

    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByText('회의실 이름을 입력해 주세요.')).toBeInTheDocument();
    expect(screen.getByLabelText('회의실 이름')).toHaveAttribute('aria-invalid', 'true');
  });

  it('수용 인원이 0이면 요청을 보내지 않고 1~999 조건을 알려 준다', async () => {
    const user = userEvent.setup();
    const { onSubmit } = renderDialog();

    await user.type(screen.getByLabelText('회의실 이름'), '대회의실');
    await user.clear(screen.getByLabelText('수용 인원'));
    await user.type(screen.getByLabelText('수용 인원'), '0');
    await user.type(screen.getByLabelText('위치'), '3층');
    await user.click(screen.getByRole('button', { name: '확인' }));

    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByText('수용 인원은 1명 이상 999명 이하로 입력해 주세요.')).toBeInTheDocument();
  });

  it('위치를 비운 채 확인하면 요청을 보내지 않고 사유를 알려 준다', async () => {
    const user = userEvent.setup();
    const { onSubmit } = renderDialog();

    await user.type(screen.getByLabelText('회의실 이름'), '대회의실');
    await user.click(screen.getByRole('button', { name: '확인' }));

    expect(onSubmit).not.toHaveBeenCalled();
    expect(screen.getByText('위치를 입력해 주세요.')).toBeInTheDocument();
  });

  it('닫기를 누르면 onClose만 호출되고 요청은 나가지 않는다', async () => {
    const user = userEvent.setup();
    const { onSubmit, onClose } = renderDialog();

    await user.click(screen.getByRole('button', { name: '닫기' }));

    expect(onClose).toHaveBeenCalledTimes(1);
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('서버가 거절한 코드는 패널 안에서 한국어로 읽힌다', () => {
    renderDialog({ errorCode: 'INVALID_ROOM_NAME' });

    const dialog = screen.getByRole('dialog', { name: '회의실 추가' });
    expect(within(dialog).getByText('회의실 이름은 1~30자로 입력해주세요.')).toBeInTheDocument();
  });

  it('어느 그룹에 회의실을 만드는지 패널에서 알려 준다', () => {
    renderDialog();

    const dialog = screen.getByRole('dialog', { name: '회의실 추가' });
    expect(within(dialog).getByText(/SQI 개발팀/)).toBeInTheDocument();
  });
});
