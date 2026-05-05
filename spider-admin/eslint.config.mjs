import js from "@eslint/js";
import tseslint from "typescript-eslint";
import globals from "globals";

export default [
  // ── JS: src/main/resources/static/js ──
  {
    files: ["src/main/resources/static/js/**/*.js"],
    ...js.configs.recommended,
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: "script",
      globals: {
        ...globals.browser,
        $: "readonly",
        jQuery: "readonly",
        // 공통 유틸리티 (html-utils.js로 로드)
        HtmlUtils: "readonly",
        // 프로젝트 컴포넌트 클래스 (script 태그로 로드)
        SearchForm: "readonly",
        DataTable: "readonly",
        Pagination: "readonly",
        Modal: "readonly",
        PageManager: "readonly",
      },
    },
    rules: {
      "no-unused-vars": ["error", { argsIgnorePattern: "^_" }],
    },
  },

  // ── TS: e2e/**/*.ts ──
  ...tseslint.configs.recommended.map((config) => ({
    ...config,
    files: ["e2e/**/*.ts", "playwright.config.ts"],
  })),
  {
    files: ["e2e/**/*.ts", "playwright.config.ts"],
    languageOptions: {
      ecmaVersion: 2022,
      sourceType: "module",
      globals: {
        ...globals.node,
      },
    },
    rules: {
      "@typescript-eslint/no-explicit-any": "off",
      "@typescript-eslint/ban-ts-comment": "off",
      "@typescript-eslint/no-unused-vars": ["error", { argsIgnorePattern: "^_", varsIgnorePattern: "^_" }],
    },
  },

  // ── 전역 무시 ──
  {
    ignores: [
      "node_modules/",
      "target/",
      "playwright-report/",
      "playwright-results/",
    ],
  },
];
