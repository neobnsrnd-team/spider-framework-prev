import { defineConfig, loadEnv } from 'vite'
import type { UserConfig } from 'vitest/config'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite';
import { resolve, dirname } from 'path';
import { fileURLToPath } from 'url';
import { cmsBankPlugin } from './src/vite-plugin/cmsBankPlugin';

// ESM 환경에서 __dirname 미정의 — import.meta.url로 직접 파생
const __dirname = dirname(fileURLToPath(import.meta.url));

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // Vite 5+에서 loadEnv는 더 이상 process.env에 자동 주입하지 않음.
  // prefix '' = VITE_ 접두사 없는 변수(ORACLE_*)도 포함해 전부 로드한 뒤,
  // ORACLE_ 접두사 키만 선택적으로 주입 — 민감 정보의 불필요한 노출 방지.
  const env = loadEnv(mode, process.cwd(), '');
  // 서버 사이드 전용 환경변수를 process.env에 선택 주입 (클라이언트 번들 노출 방지)
  const SERVER_ENV_KEYS = ['AUTH_BYPASS', 'SPIDER_ADMIN_API_URL'];
  Object.keys(env)
    .filter(k => k.startsWith('ORACLE_') || SERVER_ENV_KEYS.includes(k))
    .forEach(k => { process.env[k] = env[k]; });

  const testConfig: UserConfig['test'] = {
    environment: 'node', // current-user.ts는 Node.js(Vite 플러그인) 전용 모듈
    include: ['src/**/*.test.ts'],
  };

  return {
    test: testConfig,
    // VITE_BASE 환경변수로 base 경로를 제어한다.
    // 프록시 연동 시: VITE_BASE=/react-cms/ npm run dev
    // 단독 개발 시: 기본값 '/' 유지 (평소 동작 그대로)
    base: process.env.VITE_BASE ?? '/',
    server: {
      // 모든 네트워크 인터페이스에 바인딩 — Docker(nginx)에서 host.docker.internal로 접근하기 위해 필요
      host: true,
      // 다른 dev 도구(Vite default 5173)와 충돌을 피하기 위해 5273을 고정 사용.
      // spider-admin/nginx/cms-local-proxy.conf의 proxy_pass와 동일 값을 유지해야 함.
      port: 5273,
    },
    plugins: [
      react(),
      tailwindcss(),
      // CMS 빌더 저장 요청을 처리한다.
      // 저장 위치는 클라이언트(SavePageModal)가 직접 입력해 전달하며,
      // 라우트 등록은 자동 수행하지 않는다(개발자가 직접 등록).
      cmsBankPlugin(),
    ],
    resolve: {
      alias: {
        /* src/ 절대 경로 alias */
        '@': resolve(__dirname, 'src'),
        /* CMS 엔진 / 메타 단축 경로 */
        '@cms-core': resolve(__dirname, 'src/cms-core'),
        '@cms-meta': resolve(__dirname, 'src/cms-meta'),
        /* 컴포넌트 라이브러리 루트 (deep import용: @cl/modules/common/BankSelectGrid 등) */
        '@cl': resolve(__dirname, '../reactive-springware/component-library'),
        /* 컴포넌트 라이브러리 내부 유틸 경로 */
        '@lib': resolve(__dirname, '../reactive-springware/lib'),
      },
    },
  }
})
