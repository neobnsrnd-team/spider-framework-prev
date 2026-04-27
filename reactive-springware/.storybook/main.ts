/**
 * @file main.ts
 * @description Storybook 빌드 및 개발 서버 설정.
 *
 * Storybook 10 특이사항:
 * - controls, actions, viewport, backgrounds 등 essentials 기능이 코어에 내장.
 *   → @storybook/addon-essentials 불필요, addons 배열 비움.
 * - viteFinal: @tailwindcss/vite 플러그인 주입 및 @lib 경로 별칭 설정.
 */
import type { StorybookConfig } from '@storybook/react-vite';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname  = path.dirname(__filename);

const config: StorybookConfig = {
  /**
   * 스토리 파일 탐색 경로.
   * 컴포넌트 파일과 같은 디렉토리에 *.stories.tsx 파일을 위치시킨다.
   *   예) core/Button/Button.stories.tsx
   */
  stories: ['../component-library/**/*.stories.@(ts|tsx)'],

  /* Storybook 10: essentials이 코어 내장이므로 addons 불필요 */
  addons: [],

  framework: {
    name: '@storybook/react-vite',
    options: {},
  },

  viteFinal: async (viteConfig) => {
    /* @tailwindcss/vite 플러그인을 최우선으로 주입 — CSS 변수 토큰 처리에 필수 */
    const { default: tailwindcss } = await import('@tailwindcss/vite');
    viteConfig.plugins = [tailwindcss(), ...(viteConfig.plugins ?? [])];

    viteConfig.resolve = viteConfig.resolve ?? {};
    viteConfig.resolve.alias = {
      ...(viteConfig.resolve.alias as Record<string, string> | undefined),
      /* 컴포넌트 내부의 ../../../../lib/cn 경로를 @lib/cn으로 대체 가능하도록 제공 */
      '@lib': path.resolve(__dirname, '../lib'),
    };

    /* React 중복 로드 방지 */
    viteConfig.resolve.dedupe = ['react', 'react-dom'];

    return viteConfig;
  },
};

export default config;