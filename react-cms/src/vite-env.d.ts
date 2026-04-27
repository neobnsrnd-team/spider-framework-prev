/// <reference types="vite/client" />

interface ImportMetaEnv {
  /** CMS 빌더 셸 브랜드 테마. index.css의 [data-theme="*"] 블록과 매핑. */
  readonly VITE_CMS_BRAND?: 'hana' | 'kb' | 'ibk' | 'woori' | 'shinhan' | 'nh';
}

interface ImportMeta {
  readonly env: ImportMetaEnv;
}
