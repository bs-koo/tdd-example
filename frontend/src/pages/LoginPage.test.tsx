import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import type { MemberDto } from '../types';
import type { AuthApi } from '../api/types';
import { ApiError } from '../api/client';
import { useAuth } from '../hooks/useAuth';
import LoginPage from './LoginPage';

const MEMBER: MemberDto = { id: 1, email: 'owner@sqisoft.com', name: '김본승' };

beforeEach(() => {
  localStorage.clear();
});

afterEach(() => {
  localStorage.clear();
});

function createFakeApi() {
  return {
    signup: vi.fn(),
    login: vi.fn(),
    logout: vi.fn(),
    me: vi.fn(),
  };
}

/** 실제 useAuth 와 붙여 렌더한다 — 오류가 뜨고 사라지는 것까지 화면에서 확인하기 위해서다. */
function Harness({ api }: { api: AuthApi }) {
  const auth = useAuth({ api });
  return <LoginPage auth={auth} />;
}

function renderLogin(fakeApi: ReturnType<typeof createFakeApi>) {
  render(<Harness api={fakeApi as unknown as AuthApi} />);
}

describe('LoginPage 탭 전환', () => {
  it('처음에는 로그인 탭이 선택돼 있고 이름 입력은 없다', () => {
    renderLogin(createFakeApi());

    expect(screen.getByRole('tab', { name: '로그인' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tab', { name: '회원가입' })).toHaveAttribute('aria-selected', 'false');
    expect(screen.queryByLabelText('이름')).not.toBeInTheDocument();
  });

  it('회원가입 탭을 누르면 이름 입력이 나타나고 선택 상태가 옮겨간다', async () => {
    const user = userEvent.setup();
    renderLogin(createFakeApi());

    await user.click(screen.getByRole('tab', { name: '회원가입' }));

    expect(screen.getByRole('tab', { name: '회원가입' })).toHaveAttribute('aria-selected', 'true');
    expect(screen.getByRole('tab', { name: '로그인' })).toHaveAttribute('aria-selected', 'false');
    expect(screen.getByLabelText('이름')).toBeInTheDocument();
  });

  it('이메일·비밀번호 입력은 라벨과 연결돼 있고 비밀번호는 가려진다', () => {
    renderLogin(createFakeApi());

    expect(screen.getByLabelText('이메일')).toHaveAttribute('type', 'email');
    expect(screen.getByLabelText('비밀번호')).toHaveAttribute('type', 'password');
  });
});

describe('LoginPage 로그인', () => {
  it('이메일과 비밀번호를 입력하고 로그인하면 입력한 값 그대로 로그인 요청이 나간다', async () => {
    const api = createFakeApi();
    api.login.mockResolvedValue({ token: 'opaque-token-1', member: MEMBER });
    const user = userEvent.setup();
    renderLogin(api);

    await user.type(screen.getByLabelText('이메일'), 'owner@sqisoft.com');
    await user.type(screen.getByLabelText('비밀번호'), 'password123');
    await user.click(screen.getByRole('button', { name: '로그인' }));

    await waitFor(() => {
      expect(api.login).toHaveBeenCalledWith({
        email: 'owner@sqisoft.com',
        password: 'password123',
      });
    });
  });

  it('자격이 틀리면 한국어 오류가 role="alert"로 표시된다', async () => {
    const api = createFakeApi();
    api.login.mockRejectedValue(new ApiError('INVALID_CREDENTIALS', 401));
    const user = userEvent.setup();
    renderLogin(api);

    await user.type(screen.getByLabelText('이메일'), 'owner@sqisoft.com');
    await user.type(screen.getByLabelText('비밀번호'), 'wrong-password');
    await user.click(screen.getByRole('button', { name: '로그인' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('이메일 또는 비밀번호가 올바르지 않습니다.');
  });

  it('이메일을 비운 채 제출하면 서버를 부르지 않고 그 자리에서 사유를 알려 준다', async () => {
    const api = createFakeApi();
    const user = userEvent.setup();
    renderLogin(api);

    await user.type(screen.getByLabelText('비밀번호'), 'password123');
    await user.click(screen.getByRole('button', { name: '로그인' }));

    expect(api.login).not.toHaveBeenCalled();
    expect(screen.getByLabelText('이메일')).toHaveAttribute('aria-invalid', 'true');
    expect(screen.getByText('이메일을 입력해 주세요.')).toBeInTheDocument();
  });

  it('탭을 바꾸면 이전 탭에서 난 오류 알림이 사라진다', async () => {
    const api = createFakeApi();
    api.login.mockRejectedValue(new ApiError('INVALID_CREDENTIALS', 401));
    const user = userEvent.setup();
    renderLogin(api);

    await user.type(screen.getByLabelText('이메일'), 'owner@sqisoft.com');
    await user.type(screen.getByLabelText('비밀번호'), 'wrong-password');
    await user.click(screen.getByRole('button', { name: '로그인' }));
    await screen.findByRole('alert');

    await user.click(screen.getByRole('tab', { name: '회원가입' }));

    expect(screen.queryByRole('alert')).not.toBeInTheDocument();
  });
});

describe('LoginPage 회원가입', () => {
  it('이름까지 채워 가입하면 가입 요청에 이름·이메일·비밀번호가 함께 실린다', async () => {
    const api = createFakeApi();
    api.signup.mockResolvedValue(MEMBER);
    api.login.mockResolvedValue({ token: 'opaque-token-1', member: MEMBER });
    const user = userEvent.setup();
    renderLogin(api);

    await user.click(screen.getByRole('tab', { name: '회원가입' }));
    await user.type(screen.getByLabelText('이름'), '김본승');
    await user.type(screen.getByLabelText('이메일'), 'owner@sqisoft.com');
    await user.type(screen.getByLabelText('비밀번호'), 'password123');
    await user.click(screen.getByRole('button', { name: '가입하고 시작하기' }));

    await waitFor(() => {
      expect(api.signup).toHaveBeenCalledWith({
        email: 'owner@sqisoft.com',
        password: 'password123',
        name: '김본승',
      });
    });
  });

  it('이미 가입된 이메일이면 그 사유가 한국어로 표시된다', async () => {
    const api = createFakeApi();
    api.signup.mockRejectedValue(new ApiError('EMAIL_ALREADY_REGISTERED', 409));
    const user = userEvent.setup();
    renderLogin(api);

    await user.click(screen.getByRole('tab', { name: '회원가입' }));
    await user.type(screen.getByLabelText('이름'), '김본승');
    await user.type(screen.getByLabelText('이메일'), 'owner@sqisoft.com');
    await user.type(screen.getByLabelText('비밀번호'), 'password123');
    await user.click(screen.getByRole('button', { name: '가입하고 시작하기' }));

    const alert = await screen.findByRole('alert');
    expect(alert).toHaveTextContent('이미 가입된 이메일입니다.');
  });

  it('비밀번호가 8자 미만이면 서버에 보내지 않고 길이 조건을 알려 준다', async () => {
    const api = createFakeApi();
    const user = userEvent.setup();
    renderLogin(api);

    await user.click(screen.getByRole('tab', { name: '회원가입' }));
    await user.type(screen.getByLabelText('이름'), '김본승');
    await user.type(screen.getByLabelText('이메일'), 'owner@sqisoft.com');
    await user.type(screen.getByLabelText('비밀번호'), 'short');
    await user.click(screen.getByRole('button', { name: '가입하고 시작하기' }));

    expect(api.signup).not.toHaveBeenCalled();
    expect(screen.getByText('비밀번호는 8자 이상 64자 이하로 입력해 주세요.')).toBeInTheDocument();
  });
});
