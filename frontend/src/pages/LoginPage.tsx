import { useId, useRef, useState } from 'react';
import { flushSync } from 'react-dom';
import type { Auth } from '../hooks/useAuth';
import ErrorBanner from '../components/ErrorBanner';

export type LoginPageProps = {
  auth: Auth;
};

type Mode = 'login' | 'signup';

const NAME_LIMIT = 20;
const PASSWORD_MIN = 8;
const PASSWORD_MAX = 64;
// 서버 규칙(INVALID_EMAIL_FORMAT)에 닿기 전에 명백한 오타를 걸러내는 정도로만 본다.
const EMAIL_PATTERN = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

function nameError(name: string): string | null {
  const trimmed = name.trim();
  if (trimmed.length === 0) {
    return '이름을 입력해 주세요.';
  }
  if (trimmed.length > NAME_LIMIT) {
    return `이름은 ${NAME_LIMIT}자 이내로 입력해 주세요.`;
  }
  return null;
}

function emailError(email: string): string | null {
  if (email.trim().length === 0) {
    return '이메일을 입력해 주세요.';
  }
  if (!EMAIL_PATTERN.test(email.trim())) {
    return '이메일 주소 형식으로 입력해 주세요. 예: name@sqisoft.com';
  }
  return null;
}

function passwordError(password: string, mode: Mode): string | null {
  if (password.length === 0) {
    return '비밀번호를 입력해 주세요.';
  }
  // 길이 규칙은 가입에서만 미리 본다. 이미 만든 계정의 비밀번호를 여기서 되물으면
  // 로그인할 수 있는 사람을 문 앞에서 막는 셈이 된다.
  if (mode === 'signup' && (password.length < PASSWORD_MIN || password.length > PASSWORD_MAX)) {
    return `비밀번호는 ${PASSWORD_MIN}자 이상 ${PASSWORD_MAX}자 이하로 입력해 주세요.`;
  }
  return null;
}

export default function LoginPage({ auth }: LoginPageProps) {
  const [mode, setMode] = useState<Mode>('login');
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [attempted, setAttempted] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  const nameRef = useRef<HTMLInputElement>(null);
  const emailRef = useRef<HTMLInputElement>(null);
  const passwordRef = useRef<HTMLInputElement>(null);

  const fieldId = useId();
  const nameId = `${fieldId}-name`;
  const emailId = `${fieldId}-email`;
  const passwordId = `${fieldId}-password`;
  const panelId = `${fieldId}-panel`;

  const isSignup = mode === 'signup';
  const shownNameError = attempted && isSignup ? nameError(name) : null;
  const shownEmailError = attempted ? emailError(email) : null;
  const shownPasswordError = attempted ? passwordError(password, mode) : null;

  const switchMode = (next: Mode) => {
    if (next === mode) {
      return;
    }
    // 앞 탭에서 난 서버 오류는 이 탭의 이야기가 아니다. 입력은 남기고 판정만 지운다.
    auth.clearError();
    setAttempted(false);
    setMode(next);
  };

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();

    const errors = {
      name: isSignup ? nameError(name) : null,
      email: emailError(email),
      password: passwordError(password, mode),
    };
    if (errors.name !== null || errors.email !== null || errors.password !== null) {
      // 오류 문구와 aria-describedby 를 먼저 붙여 두어야 초점이 닿는 순간 사유까지 읽힌다.
      flushSync(() => setAttempted(true));
      const target =
        errors.name !== null ? nameRef : errors.email !== null ? emailRef : passwordRef;
      target.current?.focus();
      return;
    }

    setSubmitting(true);
    try {
      if (isSignup) {
        await auth.signup({ email: email.trim(), password, name: name.trim() });
      } else {
        await auth.login({ email: email.trim(), password });
      }
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="auth">
      <main className="auth__card">
        <header className="auth__head">
          <h1 className="auth__title">회의실 예약</h1>
          <p className="auth__lead">그룹의 회의실을 함께 쓰려면 먼저 로그인하세요.</p>
        </header>

        <div className="tabs" role="tablist" aria-label="로그인 또는 회원가입">
          <button
            type="button"
            role="tab"
            id={`${fieldId}-tab-login`}
            className="tab"
            aria-selected={!isSignup}
            aria-controls={panelId}
            onClick={() => switchMode('login')}
          >
            로그인
          </button>
          <button
            type="button"
            role="tab"
            id={`${fieldId}-tab-signup`}
            className="tab"
            aria-selected={isSignup}
            aria-controls={panelId}
            onClick={() => switchMode('signup')}
          >
            회원가입
          </button>
        </div>

        <form
          className="auth__form"
          id={panelId}
          role="tabpanel"
          aria-labelledby={`${fieldId}-tab-${mode}`}
          onSubmit={handleSubmit}
          noValidate
        >
          {isSignup && (
            <div className="field">
              <div className="field__head">
                <label className="field__label" htmlFor={nameId}>
                  이름 <span className="field__required">*</span>
                </label>
                <span className="field__count">
                  {name.length} / {NAME_LIMIT}
                </span>
              </div>
              <input
                id={nameId}
                ref={nameRef}
                className="field__input"
                aria-label="이름"
                autoComplete="name"
                maxLength={NAME_LIMIT}
                value={name}
                aria-invalid={shownNameError !== null}
                aria-describedby={shownNameError !== null ? `${nameId}-error` : undefined}
                onChange={(e) => setName(e.target.value)}
              />
              {shownNameError !== null ? (
                <span className="field__error" id={`${nameId}-error`}>
                  {shownNameError}
                </span>
              ) : (
                <span className="field__hint">예약 격자에 표시되는 이름입니다.</span>
              )}
            </div>
          )}

          <div className="field">
            <div className="field__head">
              <label className="field__label" htmlFor={emailId}>
                이메일 <span className="field__required">*</span>
              </label>
            </div>
            <input
              id={emailId}
              ref={emailRef}
              type="email"
              className="field__input"
              aria-label="이메일"
              autoComplete="email"
              value={email}
              aria-invalid={shownEmailError !== null}
              aria-describedby={shownEmailError !== null ? `${emailId}-error` : undefined}
              onChange={(e) => setEmail(e.target.value)}
            />
            {shownEmailError !== null && (
              <span className="field__error" id={`${emailId}-error`}>
                {shownEmailError}
              </span>
            )}
          </div>

          <div className="field">
            <div className="field__head">
              <label className="field__label" htmlFor={passwordId}>
                비밀번호 <span className="field__required">*</span>
              </label>
            </div>
            <input
              id={passwordId}
              ref={passwordRef}
              type="password"
              className="field__input"
              aria-label="비밀번호"
              autoComplete={isSignup ? 'new-password' : 'current-password'}
              maxLength={PASSWORD_MAX}
              value={password}
              aria-invalid={shownPasswordError !== null}
              aria-describedby={shownPasswordError !== null ? `${passwordId}-error` : undefined}
              onChange={(e) => setPassword(e.target.value)}
            />
            {shownPasswordError !== null ? (
              <span className="field__error" id={`${passwordId}-error`}>
                {shownPasswordError}
              </span>
            ) : (
              isSignup && <span className="field__hint">8자 이상 64자 이하</span>
            )}
          </div>

          <div className="auth__notice">
            <ErrorBanner code={auth.errorCode} />
          </div>

          <button
            type="submit"
            className="btn btn--primary btn--block"
            disabled={submitting}
            aria-busy={submitting}
          >
            {isSignup ? '가입하고 시작하기' : '로그인'}
          </button>
        </form>
      </main>
    </div>
  );
}
