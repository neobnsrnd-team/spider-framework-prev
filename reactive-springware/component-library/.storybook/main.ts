import type { StorybookConfig } from '@storybook/react-vite';
import { resolve } from 'path';

/**
 * 패키지 절대 경로 반환 헬퍼.
 * import.meta.resolve은 esbuild-register(CJS 변환) 환경에서 동작하지 않으므로
 * require.resolve를 사용한다.
 */
function getAbsolutePath(value: string): string {
  return require.resolve(`${value}/package.json`).replace('/package.json', '');
}

const config: StorybookConfig = {
  stories: [
    '../**/*.stories.@(js|jsx|mjs|ts|tsx)',
    '../**/*.mdx',
  ],
  addons: [
    getAbsolutePath('@storybook/addon-essentials'),
  ],
  framework: {
    name: '@storybook/react-vite',
    options: {},
  },
  viteFinal(config) {
    config.resolve = config.resolve ?? {};
    config.resolve.alias = {
      ...(config.resolve.alias as Record<string, string>),
      /* component-library 전체에서 사용하는 @lib/cn 경로 별칭
       * __dirname = .storybook/ → ../../../ = figma-react-generator/ */
      '@lib': resolve(__dirname, '../../lib'),
    };
    return config;
  },
};

export default config;
