import { toErrorMessage, toNextAction } from './errorCopy';

export type ErrorBannerProps = {
  code: string | null;
};

export default function ErrorBanner({ code }: ErrorBannerProps) {
  if (code === null) {
    return null;
  }

  return (
    <div role="alert" className="notice__banner">
      <span className="notice__message">{toErrorMessage(code)}</span>
      <span className="notice__next">{toNextAction(code)}</span>
    </div>
  );
}
