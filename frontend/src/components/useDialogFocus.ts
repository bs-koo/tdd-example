import { useEffect, useRef } from 'react';

const FOCUSABLE = [
  'a[href]',
  'button:not([disabled])',
  'input:not([disabled])',
  'select:not([disabled])',
  'textarea:not([disabled])',
  '[tabindex]:not([tabindex="-1"])',
].join(', ');

/**
 * 다이얼로그 패널에 붙이는 ref 를 돌려준다.
 * - 열릴 때 [data-autofocus] 요소(없으면 첫 초점 요소)로 초점을 옮긴다
 * - Tab 을 패널 안에 가둔다
 * - Escape 로 닫는다
 * - 닫힐 때 열기 전 초점 위치로 되돌린다
 *
 * onClose 는 ref 로 들고 있는다. 의존성에 넣으면 부모가 인라인 함수를 넘길 때마다
 * 효과가 다시 돌아 입력 중에 초점이 첫 칸으로 튄다.
 */
export function useDialogFocus(open: boolean, onClose: () => void) {
  const panelRef = useRef<HTMLDivElement>(null);
  const closeRef = useRef(onClose);
  closeRef.current = onClose;

  useEffect(() => {
    const panel = panelRef.current;
    if (!open || panel === null) {
      return;
    }

    const restoreTo = document.activeElement as HTMLElement | null;
    const initial =
      panel.querySelector<HTMLElement>('[data-autofocus]') ??
      panel.querySelector<HTMLElement>(FOCUSABLE);
    initial?.focus();

    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        event.stopPropagation();
        closeRef.current();
        return;
      }
      if (event.key !== 'Tab') {
        return;
      }
      const items = Array.from(panel.querySelectorAll<HTMLElement>(FOCUSABLE));
      if (items.length === 0) {
        return;
      }
      const head = items[0];
      const tail = items[items.length - 1];
      // 초점이 이미 패널 밖에 있으면(오버레이 여백 클릭 등) 먼저 회수한다.
      // 이 회수가 없으면 다음 Tab 이 스킵 링크로 나가 트랩이 뚫린다.
      if (!panel.contains(document.activeElement)) {
        event.preventDefault();
        (event.shiftKey ? tail : head).focus();
        return;
      }
      if (event.shiftKey && document.activeElement === head) {
        event.preventDefault();
        tail.focus();
      } else if (!event.shiftKey && document.activeElement === tail) {
        event.preventDefault();
        head.focus();
      }
    };

    // 오버레이 여백을 누르면 초점이 body 로 떨어진다. mousedown 의 기본 동작만 막아
    // 초점을 패널에 남긴다 — 다이얼로그를 닫지는 않는다(입력을 날리지 않기 위해).
    const handleMouseDown = (event: MouseEvent) => {
      const target = event.target as Node | null;
      if (target !== null && !panel.contains(target)) {
        event.preventDefault();
      }
    };

    document.addEventListener('keydown', handleKeyDown);
    document.addEventListener('mousedown', handleMouseDown);
    return () => {
      document.removeEventListener('keydown', handleKeyDown);
      document.removeEventListener('mousedown', handleMouseDown);
      restoreTo?.focus?.();
    };
  }, [open]);

  return panelRef;
}
