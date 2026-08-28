import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  // API 클라이언트가 상대 경로(/api/...)만 쓰므로 개발 서버에서 이 설정 하나로 백엔드에 연결된다.
  // vitest는 dev server를 띄우지 않으므로 테스트에는 영향이 없다. (설계 §9.14.4)
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/setupTests.ts'],
  },
});
