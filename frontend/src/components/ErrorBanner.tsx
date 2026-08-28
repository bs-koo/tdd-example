import { toKoreanMessage } from '../domain/errorMessages';

export type ErrorBannerProps = {
  code: string | null;
};

export default function ErrorBanner({ code }: ErrorBannerProps) {
  if (code === null) {
    return null;
  }

  return <div role="alert">{toKoreanMessage(code)}</div>;
}
