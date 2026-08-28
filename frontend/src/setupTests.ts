import '@testing-library/jest-dom/vitest';
import { cleanup } from '@testing-library/react';
import { afterEach } from 'vitest';

// globals: false 이므로 Testing Library의 자동 cleanup이 등록되지 않는다. 명시적으로 건다.
afterEach(() => {
  cleanup();
});
