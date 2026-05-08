/**
 * @file paths.ts
 * @description 애플리케이션 전체 라우트 경로 상수.
 *
 * 경로를 문자열 리터럴로 직접 사용하면 오타·변경 시 누락으로 인한 버그가 발생하기 쉽다.
 * 모든 경로 참조는 반드시 이 상수를 통해 한다.
 *
 * @example
 * navigate(PATHS.CARD.DASHBOARD)
 * <Route path={PATHS.CARD.USAGE_HISTORY} element={<UsageHistoryRoute />} />
 */
export const PATHS = {
  LOGIN: "/login",

  /** 긴급공지 미리보기 전용 경로 — 배포 전 초안 확인, 인증 없이 접근 가능 */
  PREVIEW: {
    NOTICE: "/preview/notice",
  },

  /** 승인 완료된 React 컴포넌트 뷰어 경로 — 인증 없이 접근 가능 */
  VIEWER: {
    /** 승인된 React 컴포넌트 뷰어. :codeId = FWK_RPS_CODE_HIS.CODE_ID */
    REACT_PLATFORM: "/reactplatform/viewer/:componentName",
    /** React CMS 배포 페이지 뷰어. :pageId = SPW_CMS_PAGE.PAGE_ID */
    REACT_CMS: "/react-cms/viewer/:pageId",
  },

  CARD: {
    DASHBOARD: "/card/dashboard",
    MENU: "/card/menu",
    USAGE_HISTORY: "/card/usage-history",
    PAYMENT_STATEMENT: "/card/payment-statement",
    IMMEDIATE_PAYMENT: "/card/immediate-payment",
    IMMEDIATE_PAY: "/card/immediate-pay",
    IMMEDIATE_PAY_REQUEST: "/card/immediate-pay-request",
    IMMEDIATE_PAY_METHOD: "/card/immediate-pay-method",
    IMMEDIATE_PAY_COMPLETE: "/card/immediate-pay-complete",
    MY_CARD_MANAGEMENT: "/card/my-card-management",
    USER_MANAGEMENT: "/card/user-management",
  },
} as const;
