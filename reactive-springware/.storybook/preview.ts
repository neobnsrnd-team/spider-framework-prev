/// <reference types="vite/client" />

/**
 * @file preview.ts
 * @description Storybook 전역 미리보기 설정.
 *
 * - 전역 CSS(Tailwind + 브랜드 토큰) 적용
 * - 기본 뷰포트(모바일 390px) 설정
 * - 상단 툴바의 Brand / Domain 콤보박스로 CSS 변수 실시간 전환
 * - data-brand / data-domain 전역 데코레이터로 CSS 변수 활성화
 *
 * @example
 * // 스토리에서 특정 브랜드를 기본값으로 고정하려면 parameters 사용
 * export default {
 *   parameters: { brand: 'kb', domain: 'banking' },
 * } satisfies Meta;
 */
import type { Preview } from '@storybook/react';

/* 전역 스타일 — Tailwind CSS v4 + 브랜드/도메인 CSS 변수 포함 */
import '../component-library/dist/styles.css';

/** 툴바 Brand 콤보박스 — globals.css [data-brand='*'] 정의와 동기화 */
export const globalTypes = {
  brand: {
    name: 'Brand',
    description: '브랜드 선택',
    defaultValue: 'hana',
    toolbar: {
      icon: 'paintbrush',
      items: [
        { value: 'hana',    title: '하나' },
        { value: 'ibk',     title: 'IBK기업' },
        { value: 'kb',      title: 'KB국민' },
        { value: 'nh',      title: 'NH농협' },
        { value: 'shinhan', title: '신한' },
        { value: 'woori',   title: '우리' },
      ],
      dynamicTitle: true,
    },
  },
  domain: {
    name: 'Domain',
    description: '도메인 선택',
    defaultValue: 'card',
    toolbar: {
      icon: 'category',
      items: [
        { value: 'banking',   title: '뱅킹' },
        { value: 'card',      title: '카드' },
        { value: 'giro',      title: '지로' },
        { value: 'insurance', title: '보험' },
      ],
      dynamicTitle: true,
    },
  },
};

const preview: Preview = {
  parameters: {
    /* 기본 뷰포트: 모바일(390px) 우선 */
    viewport: {
      viewports: {
        mobile: {
          name: 'Mobile (390px)',
          styles: { width: '390px', height: '844px' },
          type: 'mobile',
        },
        tablet: {
          name: 'Tablet (768px)',
          styles: { width: '768px', height: '1024px' },
          type: 'tablet',
        },
        desktop: {
          name: 'Desktop (1280px)',
          styles: { width: '1280px', height: '800px' },
          type: 'desktop',
        },
      },
      defaultViewport: 'mobile',
    },

    backgrounds: {
      default: 'page',
      values: [
        { name: 'page',  value: '#f5f8f8' },
        { name: 'white', value: '#ffffff' },
        { name: 'dark',  value: '#1e293b' },
      ],
    },

    controls: { matchers: {}, sort: 'alpha' },
  },

  /**
   * 전역 데코레이터 — HTML 루트에 data-brand/data-domain 속성을 주입해
   * CSS 변수(--brand-primary 등)가 올바르게 cascade 되도록 한다.
   *
   * 우선순위: 툴바(globals) > 스토리 고정값(parameters) > 기본값
   */
  decorators: [
    (Story, context) => {
      const brand  = (context.globals['brand']  as string | undefined)
                  ?? (context.parameters['brand']  as string | undefined)
                  ?? 'hana';
      const domain = (context.globals['domain'] as string | undefined)
                  ?? (context.parameters['domain'] as string | undefined)
                  ?? 'banking';

      document.documentElement.setAttribute('data-brand',  brand);
      document.documentElement.setAttribute('data-domain', domain);

      return Story();
    },
  ],
};

export default preview;
