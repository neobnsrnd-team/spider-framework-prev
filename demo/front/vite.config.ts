import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
import { resolve } from 'path'
import { fileURLToPath } from 'url'

const __dirname = fileURLToPath(new URL('.', import.meta.url))

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    tailwindcss(),
    react(),
  ],
  resolve: {
    alias: {
      /* src/ 절대 경로 alias */
      '@': resolve(__dirname, 'src'),
      /* 컴포넌트 라이브러리 루트 alias (pages 내 상대경로 '../../../' 대체) */
      '@cl': resolve(__dirname, '../../reactive-springware/component-library'),
      /* 컴포넌트 라이브러리 내부 유틸 경로 */
      '@lib': resolve(__dirname, '../../reactive-springware/lib'),
    },
  },
  server: {
    proxy: {
      /* 모든 /api/* 요청을 Demo Backend로 프록시
         - CORS 헤더 없이 브라우저 → Vite Dev Server → Backend 흐름으로 처리
         - SSE(EventSource) 포함 모든 API 엔드포인트 커버 */
      '/api': {
        target:      'http://localhost:18080',
        changeOrigin: true,
      },
    },
  },
})
