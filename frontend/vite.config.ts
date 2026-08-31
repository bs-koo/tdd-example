import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';

// 이 파일만 Node 환경에서 실행된다. @types/node 를 의존성에 더하지 않으려고
// 여기서 필요한 만큼만 선언한다 (src/** 는 이 선언을 보지 않는다).
declare const process: { env: Record<string, string | undefined> };

export default defineConfig({
  plugins: [react()],
  // API 클라이언트가 상대 경로(/api/...)만 쓰므로 개발 서버에서 이 설정 하나로 백엔드에 연결된다.
  // vitest는 dev server를 띄우지 않으므로 테스트에는 영향이 없다. (설계 §9.14.4)
  //
  // 백엔드 포트는 VITE_API_TARGET 으로 덮어쓴다 — 8080이 다른 프로세스에 점유된 환경 때문에
  // 이 파일을 고치면 그 값이 커밋되어 다른 개발자의 로컬이 조용히 깨진다.
  //   VITE_API_TARGET=http://localhost:18080 npm run dev
  server: {
    proxy: {
      '/api': process.env.VITE_API_TARGET ?? 'http://localhost:8080',
    },
  },
  test: {
    environment: 'jsdom',
    setupFiles: ['./src/setupTests.ts'],
  },
});
